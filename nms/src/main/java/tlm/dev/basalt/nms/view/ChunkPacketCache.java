package tlm.dev.basalt.nms.view;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.visibility.PacketMemory;
import tlm.dev.basalt.core.world.HostArea;
import tlm.dev.basalt.core.world.WorldSnapshot;
import tlm.dev.basalt.nms.port.ChunkPackets;
import tlm.dev.basalt.nms.port.WorldSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ChunkPacketCache {

    private static final Object UNTOUCHED = new Object();

    private final WorldSource source;
    private final ChunkPackets packets;
    private final Map<UUID, Map<ChunkKey, Encoding>> encodings = new HashMap<>();
    private long encoded;

    public ChunkPacketCache(WorldSource source, ChunkPackets packets) {
        this.source = source;
        this.packets = packets;
    }

    public Optional<CompletableFuture<ClientboundLevelChunkWithLightPacket>> packet(UUID world, ChunkKey chunk) {
        Optional<WorldSnapshot<VirtualChunk>> snapshot = source.worlds().snapshot(world);
        if (snapshot.isEmpty() || !snapshot.get().host().chunks().contains(chunk)) {
            return Optional.empty();
        }
        Optional<VirtualChunk> content = snapshot.get().chunk(chunk);
        Object version = content.isPresent() ? content.get() : UNTOUCHED;
        Map<ChunkKey, Encoding> worldEncodings = encodings.computeIfAbsent(world, id -> new HashMap<>());
        Encoding known = worldEncodings.get(chunk);
        if (known != null && known.version() == version) {
            return Optional.of(known.packet());
        }
        HostArea host = snapshot.get().host();
        Supplier<VirtualChunk> built = content.isPresent() ? content::get : () -> source.chunks().empty(chunk, host);
        CompletableFuture<ClientboundLevelChunkWithLightPacket> packet =
                CompletableFuture.supplyAsync(() -> packets.encode(built.get()), source.encoders());
        worldEncodings.put(chunk, new Encoding(version, packet));
        encoded++;
        return Optional.of(packet);
    }

    public void release(UUID world) {
        encodings.remove(world);
    }

    public PacketMemory memory(UUID world) {
        List<ClientboundLevelChunkWithLightPacket> ready = encodings.getOrDefault(world, Map.of()).values().stream()
                .map(Encoding::packet)
                .filter(packet -> packet.isDone() && !packet.isCompletedExceptionally())
                .map(CompletableFuture::join)
                .toList();
        return packets.memory(ready);
    }

    public long encoded() {
        return encoded;
    }

    private record Encoding(Object version, CompletableFuture<ClientboundLevelChunkWithLightPacket> packet) {
    }
}
