package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class EntityMasking {

    private final int maxChangesPerScan;

    public EntityMasking(int maxChangesPerScan) {
        if (maxChangesPerScan < 1) {
            throw new IllegalArgumentException("a scan changes at least one entity");
        }
        this.maxChangesPerScan = maxChangesPerScan;
    }

    public static boolean hides(UUID viewer, Optional<ChunkRange> area, RealEntity entity) {
        return !entity.id().equals(viewer) && area.filter(entity::isIn).isPresent();
    }

    public EntityMask decide(UUID viewer, Optional<ChunkRange> area, Collection<RealEntity> nearby, Set<UUID> hidden) {
        if (area.isEmpty()) {
            return new EntityMask(List.of(), List.copyOf(hidden));
        }
        Set<UUID> inside = new HashSet<>();
        List<UUID> hide = new ArrayList<>();
        for (RealEntity entity : nearby) {
            if (!hides(viewer, area, entity)) {
                continue;
            }
            inside.add(entity.id());
            if (!hidden.contains(entity.id()) && hide.size() < maxChangesPerScan) {
                hide.add(entity.id());
            }
        }
        List<UUID> show = new ArrayList<>();
        for (UUID id : hidden) {
            if (!inside.contains(id) && hide.size() + show.size() < maxChangesPerScan) {
                show.add(id);
            }
        }
        return new EntityMask(hide, show);
    }
}
