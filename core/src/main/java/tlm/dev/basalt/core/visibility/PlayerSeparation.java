package tlm.dev.basalt.core.visibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerSeparation {

    private PlayerSeparation() {
    }

    public static SeparationPlan plan(UUID player, PlayerGroup group, Map<UUID, PlayerGroup> others, Set<UUID> hidden) {
        List<UUID> hide = new ArrayList<>();
        List<UUID> show = new ArrayList<>();
        others.forEach((other, otherGroup) -> {
            if (other.equals(player)) {
                return;
            }
            boolean apart = !group.sees(otherGroup);
            if (apart && !hidden.contains(other)) {
                hide.add(other);
            } else if (!apart && hidden.contains(other)) {
                show.add(other);
            }
        });
        return new SeparationPlan(hide, show);
    }
}
