package tlm.dev.basalt.nms.packet;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import tlm.dev.basalt.core.chunk.BlockStateTraits;
import tlm.dev.basalt.core.chunk.HeightmapType;

final class MojangBlockStateTraits implements BlockStateTraits {

    @Override
    public boolean isAir(int state) {
        return Block.stateById(state).isAir();
    }

    @Override
    public boolean countsIn(HeightmapType type, int state) {
        Heightmap.Types mojangType = switch (type) {
            case WORLD_SURFACE -> Heightmap.Types.WORLD_SURFACE;
            case MOTION_BLOCKING -> Heightmap.Types.MOTION_BLOCKING;
            case MOTION_BLOCKING_NO_LEAVES -> Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        };
        return mojangType.isOpaque().test(Block.stateById(state));
    }
}
