package tlm.dev.basalt.nms.port;

import tlm.dev.basalt.core.interaction.InteractionInbox;
import tlm.dev.basalt.core.visibility.ViewChanges;

public record ViewInputs(InteractionInbox inbox, ViewChanges changes, WorldSource source, OverlayChannel overlays) {

    public ViewInputs {
        if (inbox == null || changes == null || source == null || overlays == null) {
            throw new IllegalArgumentException("the views need the inbox, the changes, the world source and the overlay channel");
        }
    }
}
