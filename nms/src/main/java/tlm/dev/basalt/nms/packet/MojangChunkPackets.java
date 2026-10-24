package tlm.dev.basalt.nms.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.chunk.ChunkPacketWriter;
import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.nms.port.ChunkPackets;

import java.util.Optional;

public final class MojangChunkPackets implements ChunkPackets {

    private final RegistryAccess registries;
    private final ChunkPacketWriter writer;

    public MojangChunkPackets(RegistryAccess registries) {
        this.registries = registries;
        this.writer = new ChunkPacketWriter(MojangWorldChunks.formats(registries));
    }

    @Override
    public ClientboundLevelChunkWithLightPacket encode(VirtualChunk chunk) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(writer.write(chunk)), registries);
        ClientboundLevelChunkWithLightPacket packet = ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(buffer);
        if (buffer.isReadable()) {
            throw new IllegalStateException(buffer.readableBytes() + " bytes left after decoding the virtual chunk " + chunk.key());
        }
        packet.setReady(true);
        return packet;
    }

    @Override
    public Optional<ClientboundLevelChunkWithLightPacket> realChunk(Player player, ChunkKey chunk) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        ServerLevel level = handle.level();
        LevelChunk levelChunk = level.getChunkSource().getChunkNow(chunk.x(), chunk.z());
        if (levelChunk == null) {
            return Optional.empty();
        }
        boolean antiXray = level.chunkPacketBlockController.shouldModify(handle, levelChunk);
        return Optional.of(new ClientboundLevelChunkWithLightPacket(levelChunk, level.getLightEngine(), null, null, antiXray));
    }
}
