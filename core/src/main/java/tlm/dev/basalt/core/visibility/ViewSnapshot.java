package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.Set;
import java.util.UUID;

public record ViewSnapshot(UUID world, ChunkRange area, Set<Integer> virtualEntities) {

    public ViewSnapshot {
        if ((world == null) != (area == null)) {
            throw new IllegalArgumentException("a virtual world and its area go together");
        }
        virtualEntities = Set.copyOf(virtualEntities);
        if (world == null && !virtualEntities.isEmpty()) {
            throw new IllegalArgumentException("virtual entities live in a virtual view");
        }
    }

    public static ViewSnapshot real() {
        return new ViewSnapshot(null, null, Set.of());
    }

    public static ViewSnapshot of(UUID world, ChunkRange area) {
        if (world == null || area == null) {
            throw new IllegalArgumentException("a virtual view needs a world and its area");
        }
        return new ViewSnapshot(world, area, Set.of());
    }

    public ViewSnapshot withEntities(Set<Integer> ids) {
        return new ViewSnapshot(world, area, ids);
    }

    public boolean isVirtual() {
        return world != null;
    }

    public boolean hides(ChunkKey chunk) {
        return area != null && chunk != null && area.contains(chunk);
    }

    public boolean holdsEntity(int id) {
        return virtualEntities.contains(id);
    }
}
