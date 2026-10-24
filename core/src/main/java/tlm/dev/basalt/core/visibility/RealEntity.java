package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.UUID;

public record RealEntity(UUID id, Point position, boolean player) {

    public RealEntity {
        if (id == null || position == null) {
            throw new IllegalArgumentException("a real entity needs its id and its position");
        }
    }

    public RealEntity(UUID id, Point position) {
        this(id, position, false);
    }

    public boolean isIn(ChunkRange area) {
        return ChunkKey.containing(position.x(), position.z()).filter(area::contains).isPresent();
    }
}
