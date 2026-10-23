package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.Point;

import java.util.UUID;
import java.util.regex.Pattern;

public record VirtualEntity(int id, UUID uuid, String type, Point position, Rotation rotation, String name, int health) {

    public static final int MAX_NAME_LENGTH = 64;
    private static final Pattern TYPE = Pattern.compile("[a-z0-9_.-]{1,64}:[a-z0-9_./-]{1,128}");

    public VirtualEntity {
        if (id <= 0) {
            throw new IllegalArgumentException("a virtual entity id is positive: " + id);
        }
        if (uuid == null || position == null || rotation == null || name == null) {
            throw new IllegalArgumentException("a virtual entity needs its uuid, position, rotation and name");
        }
        if (type == null || !TYPE.matcher(type).matches()) {
            throw new IllegalArgumentException("an entity type looks like minecraft:zombie: " + type);
        }
        if (!position.isFinite()) {
            throw new IllegalArgumentException("a virtual entity stands at a finite position");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("a name has at most " + MAX_NAME_LENGTH + " characters");
        }
        if (health < 0) {
            throw new IllegalArgumentException("health is 0 or more: " + health);
        }
    }

    public VirtualEntity movedTo(Point to) {
        return new VirtualEntity(id, uuid, type, to, rotation, name, health);
    }

    public VirtualEntity hurt(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage is 0 or more: " + damage);
        }
        return new VirtualEntity(id, uuid, type, position, rotation, name, Math.max(0, health - damage));
    }

    public boolean isDead() {
        return health == 0;
    }
}
