package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.BlockPosition;

public record RealBlock(BlockPosition position, String block) {

    public RealBlock {
        if (position == null || block == null || block.isBlank()) {
            throw new IllegalArgumentException("a real block needs its position and its id");
        }
    }
}
