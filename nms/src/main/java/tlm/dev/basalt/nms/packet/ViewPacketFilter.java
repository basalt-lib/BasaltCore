package tlm.dev.basalt.nms.packet;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import tlm.dev.basalt.core.visibility.ChunkRequests;
import tlm.dev.basalt.core.visibility.PacketFacts;
import tlm.dev.basalt.core.visibility.PacketKind;
import tlm.dev.basalt.core.visibility.PlayerView;
import tlm.dev.basalt.core.visibility.Verdict;
import tlm.dev.basalt.core.visibility.ViewFilter;
import tlm.dev.basalt.core.visibility.ViewSnapshot;
import tlm.dev.basalt.nms.port.OutboundFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ViewPacketFilter implements OutboundFilter {

    private final UUID player;
    private final PlayerView view;
    private final ChunkRequests requests;

    public ViewPacketFilter(UUID player, PlayerView view, ChunkRequests requests) {
        this.player = player;
        this.view = view;
        this.requests = requests;
    }

    @Override
    public Object filter(Object message) {
        ViewSnapshot snapshot = view.snapshot();
        if (!snapshot.isVirtual()) {
            return message;
        }
        return switch (message) {
            case ClientboundBundlePacket bundle -> filterBundle(bundle, snapshot);
            case ClientboundChunksBiomesPacket biomes -> filterBiomes(biomes, snapshot);
            case Packet<?> packet -> keeps(packet, snapshot) ? packet : null;
            default -> message;
        };
    }

    private boolean keeps(Packet<?> packet, ViewSnapshot snapshot) {
        PacketFacts facts = PacketClassifier.classify(packet);
        return switch (ViewFilter.decide(facts, snapshot)) {
            case PASS -> true;
            case DROP -> false;
            case REPLACE_WITH_VIRTUAL -> {
                requests.offer(player, facts.chunk());
                yield false;
            }
        };
    }

    private ClientboundBundlePacket filterBundle(ClientboundBundlePacket bundle, ViewSnapshot snapshot) {
        List<Packet<? super ClientGamePacketListener>> kept = new ArrayList<>();
        boolean changed = false;
        for (Packet<? super ClientGamePacketListener> content : bundle.subPackets()) {
            if (content instanceof ClientboundChunksBiomesPacket biomes) {
                ClientboundChunksBiomesPacket filtered = filterBiomes(biomes, snapshot);
                changed |= filtered != biomes;
                if (filtered != null) {
                    kept.add(filtered);
                }
                continue;
            }
            if (keeps(content, snapshot)) {
                kept.add(content);
            } else {
                changed = true;
            }
        }
        if (!changed) {
            return bundle;
        }
        return kept.isEmpty() ? null : new ClientboundBundlePacket(kept);
    }

    private static ClientboundChunksBiomesPacket filterBiomes(ClientboundChunksBiomesPacket packet, ViewSnapshot snapshot) {
        List<ClientboundChunksBiomesPacket.ChunkBiomeData> entries = packet.chunkBiomeData();
        List<PacketFacts> facts = entries.stream()
                .map(entry -> PacketFacts.about(PacketKind.BIOMES, PacketClassifier.key(entry.pos())))
                .toList();
        List<Verdict> verdicts = ViewFilter.decideEach(facts, snapshot);
        if (!verdicts.contains(Verdict.DROP)) {
            return packet;
        }
        List<ClientboundChunksBiomesPacket.ChunkBiomeData> kept = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            if (verdicts.get(index) == Verdict.PASS) {
                kept.add(entries.get(index));
            }
        }
        return kept.isEmpty() ? null : new ClientboundChunksBiomesPacket(kept);
    }
}
