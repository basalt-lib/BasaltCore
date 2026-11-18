package tlm.dev.basalt.core.overlay;

import tlm.dev.basalt.core.position.BlockPosition;

import java.util.Objects;

public record OverlayBlock(BlockPosition position, int state) {

    public OverlayBlock {
        Objects.requireNonNull(position, "position");
        if (state < 0) {
            throw new IllegalArgumentException("negative block state " + state);
        }
    }
}
