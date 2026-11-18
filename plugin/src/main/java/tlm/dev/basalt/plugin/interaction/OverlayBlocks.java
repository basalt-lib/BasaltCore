package tlm.dev.basalt.plugin.interaction;

import tlm.dev.basalt.core.interaction.PlayerBlocks;
import tlm.dev.basalt.core.overlay.OverlayEdit;
import tlm.dev.basalt.core.overlay.Overlays;
import tlm.dev.basalt.core.position.BlockPosition;

import java.util.OptionalInt;
import java.util.UUID;

public final class OverlayBlocks implements PlayerBlocks {

    private final Overlays<?> overlays;
    private final int air;

    public OverlayBlocks(Overlays<?> overlays, int air) {
        this.overlays = overlays;
        this.air = air;
    }

    @Override
    public OptionalInt stateAt(UUID player, BlockPosition position) {
        return overlays.blockSeenBy(player, position);
    }

    @Override
    public boolean set(UUID player, BlockPosition position, int state) {
        if (state == air) {
            return overlays.removeBlockSeenBy(player, position) == OverlayEdit.CHANGED;
        }
        return overlays.overlayOf(player)
                .map(overlay -> overlays.setBlock(overlay, position, state))
                .filter(result -> result == OverlayEdit.CHANGED || result == OverlayEdit.UNCHANGED)
                .isPresent();
    }
}
