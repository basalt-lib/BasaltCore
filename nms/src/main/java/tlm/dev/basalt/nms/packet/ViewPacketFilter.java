package tlm.dev.basalt.nms.packet;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import tlm.dev.basalt.core.visibility.PacketFacts;
import tlm.dev.basalt.core.visibility.PacketKind;
import tlm.dev.basalt.core.visibility.PlayerView;
import tlm.dev.basalt.core.visibility.Verdict;
import tlm.dev.basalt.core.visibility.ViewFilter;
import tlm.dev.basalt.core.visibility.ViewSnapshot;
import tlm.dev.basalt.nms.port.OutboundFilter;

import java.util.ArrayList;
import java.util.List;

public final class ViewPacketFilter implements OutboundFilter {

    private final PlayerView<ClientboundLevelChunkWithLightPacket> view;

    public ViewPacketFilter(PlayerView<ClientboundLevelChunkWithLightPacket> view) {
        this.view = view;
    }

    @Override
    public Object filter(Object message) {
        ViewSnapshot<ClientboundLevelChunkWithLightPacket> snapshot = view.snapshot();
        if (!snapshot.isVirtual()) {
            return message;
        }
        return switch (message) {
            case ClientboundBundlePacket bundle -> filterBundle(bundle, snapshot);
            case ClientboundChunksBiomesPacket biomes -> filterBiomes(biomes, snapshot);
            case Packet<?> packet -> switch (ViewFilter.decide(PacketClassifier.classify(packet), snapshot)) {
                case PASS -> packet;
                case DROP -> null;
                case REPLACE_WITH_VIRTUAL -> snapshot.virtualPayload();
            };
            default -> message;
        };
    }

    private static ClientboundBundlePacket filterBundle(
            ClientboundBundlePacket bundle, ViewSnapshot<ClientboundLevelChunkWithLightPacket> snapshot) {
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
            Verdict verdict = ViewFilter.decide(PacketClassifier.classify(content), snapshot);
            changed |= verdict != Verdict.PASS;
            switch (verdict) {
                case PASS -> kept.add(content);
                case REPLACE_WITH_VIRTUAL -> kept.add(snapshot.virtualPayload());
                case DROP -> {
                }
            }
        }
        if (!changed) {
            return bundle;
        }
        return kept.isEmpty() ? null : new ClientboundBundlePacket(kept);
    }

    private static ClientboundChunksBiomesPacket filterBiomes(
            ClientboundChunksBiomesPacket packet, ViewSnapshot<ClientboundLevelChunkWithLightPacket> snapshot) {
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
