package tlm.dev.basalt.core.position;

public record BlockPosition(int x, int y, int z) {

    public static final int MAX_HORIZONTAL = ChunkKey.MAX_COORDINATE * 16;
    public static final int MAX_VERTICAL = 4096;

    public BlockPosition {
        if (Math.abs(x) > MAX_HORIZONTAL || Math.abs(z) > MAX_HORIZONTAL || Math.abs(y) > MAX_VERTICAL) {
            throw new IllegalArgumentException("block outside the world: " + x + ", " + y + ", " + z);
        }
    }

    public ChunkKey chunk() {
        return ChunkKey.ofBlock(x, z);
    }

    public int localX() {
        return x & 15;
    }

    public int localZ() {
        return z & 15;
    }

    public BlockPosition relative(BlockFace face) {
        return new BlockPosition(x + face.stepX(), y + face.stepY(), z + face.stepZ());
    }
}
