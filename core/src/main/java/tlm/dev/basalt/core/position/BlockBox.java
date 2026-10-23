package tlm.dev.basalt.core.position;

import java.util.Optional;

public record BlockBox(BlockPosition min, BlockPosition max) {

    public BlockBox {
        if (min == null || max == null) {
            throw new IllegalArgumentException("a box needs both corners");
        }
        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException("box " + min + " to " + max + " is upside down");
        }
    }

    public static BlockBox of(BlockPosition block) {
        return new BlockBox(block, block);
    }

    public static BlockBox between(BlockPosition first, BlockPosition second) {
        return new BlockBox(
                new BlockPosition(Math.min(first.x(), second.x()), Math.min(first.y(), second.y()), Math.min(first.z(), second.z())),
                new BlockPosition(Math.max(first.x(), second.x()), Math.max(first.y(), second.y()), Math.max(first.z(), second.z())));
    }

    public boolean contains(BlockPosition block) {
        return block.x() >= min.x() && block.x() <= max.x() && block.y() >= min.y() && block.y() <= max.y()
                && block.z() >= min.z() && block.z() <= max.z();
    }

    public long volume() {
        return ((long) max.x() - min.x() + 1) * ((long) max.y() - min.y() + 1) * ((long) max.z() - min.z() + 1);
    }

    public ChunkRange chunks() {
        return ChunkRange.between(min.chunk(), max.chunk());
    }

    public Optional<BlockBox> within(ChunkKey chunk) {
        int fromX = Math.max(min.x(), chunk.x() << 4);
        int toX = Math.min(max.x(), (chunk.x() << 4) + 15);
        int fromZ = Math.max(min.z(), chunk.z() << 4);
        int toZ = Math.min(max.z(), (chunk.z() << 4) + 15);
        if (fromX > toX || fromZ > toZ) {
            return Optional.empty();
        }
        return Optional.of(new BlockBox(new BlockPosition(fromX, min.y(), fromZ), new BlockPosition(toX, max.y(), toZ)));
    }
}
