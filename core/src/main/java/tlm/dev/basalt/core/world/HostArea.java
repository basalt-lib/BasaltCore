package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.UUID;

public record HostArea(UUID world, ChunkRange chunks, int minY, int height) {

    public static final int LOWEST_Y = -2048;
    public static final int HIGHEST_Y = 2048;

    public HostArea {
        if (world == null || chunks == null) {
            throw new IllegalArgumentException("a host area needs its real world and its chunks");
        }
        if (minY % 16 != 0 || height % 16 != 0 || height <= 0 || minY < LOWEST_Y || minY + height > HIGHEST_Y) {
            throw new IllegalArgumentException("a host area spans whole sections from " + LOWEST_Y + " to " + HIGHEST_Y
                    + ": " + minY + " for " + height);
        }
    }

    public int maxY() {
        return minY + height - 1;
    }

    public boolean contains(BlockPosition block) {
        return chunks.containsBlock(block.x(), block.z()) && block.y() >= minY && block.y() <= maxY();
    }

    public boolean contains(BlockBox box) {
        return contains(box.min()) && contains(box.max());
    }
}
