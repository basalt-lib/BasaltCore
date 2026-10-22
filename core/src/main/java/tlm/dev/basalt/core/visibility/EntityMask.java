package tlm.dev.basalt.core.visibility;

import java.util.List;
import java.util.UUID;

public record EntityMask(List<UUID> hide, List<UUID> show) {

    public EntityMask {
        hide = List.copyOf(hide);
        show = List.copyOf(show);
    }

    public boolean isEmpty() {
        return hide.isEmpty() && show.isEmpty();
    }
}
