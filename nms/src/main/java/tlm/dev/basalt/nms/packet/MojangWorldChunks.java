package tlm.dev.basalt.nms.packet;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import tlm.dev.basalt.core.chunk.ChunkFormats;
import tlm.dev.basalt.core.chunk.ChunkShape;
import tlm.dev.basalt.core.chunk.LocalBox;
import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.chunk.VirtualChunkBuilder;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.world.HostArea;
import tlm.dev.basalt.core.world.WorldChunks;

public final class MojangWorldChunks implements WorldChunks<VirtualChunk> {

    private static final MojangBlockStateTraits TRAITS = new MojangBlockStateTraits();
    private static final int AIR = Block.getId(Blocks.AIR.defaultBlockState());

    private final ChunkFormats formats;
    private final int plains;

    public MojangWorldChunks(RegistryAccess registries) {
        Registry<Biome> biomes = registries.lookupOrThrow(Registries.BIOME);
        this.formats = formats(registries);
        this.plains = biomes.getId(biomes.getValueOrThrow(Biomes.PLAINS));
    }

    static ChunkFormats formats(RegistryAccess registries) {
        return ChunkFormats.of(Block.BLOCK_STATE_REGISTRY.size(), registries.lookupOrThrow(Registries.BIOME).size());
    }

    @Override
    public VirtualChunk empty(ChunkKey chunk, HostArea host) {
        return new VirtualChunkBuilder(chunk, new ChunkShape(host.minY(), host.height() / 16), AIR).build(formats, TRAITS, plains);
    }

    @Override
    public VirtualChunk fill(VirtualChunk chunk, BlockBox box, int state) {
        LocalBox local = new LocalBox(box.min().localX(), box.min().y(), box.min().localZ(),
                box.max().localX(), box.max().y(), box.max().localZ());
        return chunk.filled(local, state, TRAITS);
    }

    @Override
    public int stateAt(VirtualChunk chunk, BlockPosition position) {
        return chunk.stateAt(position.localX(), position.y(), position.localZ());
    }

    @Override
    public int air() {
        return AIR;
    }
}
