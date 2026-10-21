package tlm.dev.basalt.nms.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.chunk.ChunkFormats;
import tlm.dev.basalt.core.chunk.ChunkPacketWriter;
import tlm.dev.basalt.core.chunk.ChunkShape;
import tlm.dev.basalt.core.chunk.DebugChunkPattern;
import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.chunk.VirtualChunkBuilder;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.nms.port.ChunkPackets;

import java.util.Optional;

public final class MojangChunkPackets implements ChunkPackets {

    private static final MojangBlockStateTraits TRAITS = new MojangBlockStateTraits();

    @Override
    public ClientboundLevelChunkWithLightPacket virtualChunk(Player player, ChunkKey chunk) {
        ServerLevel level = ((CraftPlayer) player).getHandle().level();
        Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        ChunkFormats formats = ChunkFormats.of(Block.BLOCK_STATE_REGISTRY.size(), biomes.size());
        VirtualChunkBuilder builder = new VirtualChunkBuilder(
                chunk, new ChunkShape(level.getMinY(), level.getSectionsCount()), Block.getId(Blocks.AIR.defaultBlockState()));
        DebugChunkPattern.paint(builder, MojangChunkPackets::defaultStateId);
        VirtualChunk virtualChunk = builder.build(formats, TRAITS, biomes.getId(biomes.getValueOrThrow(Biomes.PLAINS)));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(new ChunkPacketWriter(formats).write(virtualChunk)), level.registryAccess());
        ClientboundLevelChunkWithLightPacket packet = ClientboundLevelChunkWithLightPacket.STREAM_CODEC.decode(buffer);
        if (buffer.isReadable()) {
            throw new IllegalStateException(buffer.readableBytes() + " bytes left after decoding the virtual chunk " + chunk);
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

    private static int defaultStateId(String blockId) {
        Block block = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(blockId))
                .orElseThrow(() -> new IllegalArgumentException("unknown block " + blockId));
        return Block.getId(block.defaultBlockState());
    }
}
