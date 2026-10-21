package tlm.dev.basalt.core.chunk;

final class TestBlocks implements BlockStateTraits {

    static final int AIR = 0;
    static final int STONE = 1;
    static final int FLOWER = 2;
    static final int LEAVES = 3;
    static final int PLAINS = 4;
    static final ChunkFormats FORMATS = ChunkFormats.of(30000, 70);
    static final ChunkShape OVERWORLD = new ChunkShape(-64, 24);

    @Override
    public boolean isAir(int state) {
        return state == AIR;
    }

    @Override
    public boolean countsIn(HeightmapType type, int state) {
        boolean motionBlocking = state != AIR && state != FLOWER;
        return switch (type) {
            case WORLD_SURFACE -> state != AIR;
            case MOTION_BLOCKING -> motionBlocking;
            case MOTION_BLOCKING_NO_LEAVES -> motionBlocking && state != LEAVES;
        };
    }
}
