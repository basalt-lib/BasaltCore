package tlm.dev.basalt.core.overlay;

import tlm.dev.basalt.core.position.Point;

import java.util.Objects;
import java.util.UUID;

public record OverlayEntity<L>(int id, UUID uuid, String type, Point position, float yaw, float pitch, L look) {

    public OverlayEntity {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(look, "look");
        if (!position.isFinite() || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("an overlay entity needs a finite position and rotation");
        }
    }

    public int chunkX() {
        return (int) Math.floor(position.x()) >> 4;
    }

    public int chunkZ() {
        return (int) Math.floor(position.z()) >> 4;
    }

    OverlayEntity<L> movedTo(Point to, float toYaw, float toPitch) {
        return new OverlayEntity<>(id, uuid, type, to, toYaw, toPitch, look);
    }

    OverlayEntity<L> withLook(L changed) {
        return new OverlayEntity<>(id, uuid, type, position, yaw, pitch, changed);
    }
}
