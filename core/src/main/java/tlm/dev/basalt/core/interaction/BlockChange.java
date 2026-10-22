package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;

public record BlockChange(BlockPosition position, int state) {

    public BlockChange {
        if (position == null || state < 0) {
            throw new IllegalArgumentException("a block change needs a position and a state of 0 or more");
        }
    }
}
