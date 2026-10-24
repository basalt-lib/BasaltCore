package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.Point;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

public record VirtualEntity(int id, UUID uuid, String type, Point position, Rotation rotation, EntityLooks looks, EntityLife life) {

    private static final Pattern TYPE = Pattern.compile("[a-z0-9_.-]{1,64}:[a-z0-9_./-]{1,128}");

    public VirtualEntity {
        if (id <= 0) {
            throw new IllegalArgumentException("a virtual entity id is positive: " + id);
        }
        if (uuid == null || position == null || rotation == null || looks == null || life == null) {
            throw new IllegalArgumentException("a virtual entity needs its uuid, position, rotation, looks and life");
        }
        if (type == null || !TYPE.matcher(type).matches()) {
            throw new IllegalArgumentException("an entity type looks like minecraft:zombie: " + type);
        }
        if (!position.isFinite()) {
            throw new IllegalArgumentException("a virtual entity stands at a finite position");
        }
    }

    public static VirtualEntity fresh(int id, String type, Point position, Rotation rotation) {
        UUID uuid = UUID.nameUUIDFromBytes(("basalt:virtual-entity:" + id).getBytes(StandardCharsets.UTF_8));
        return new VirtualEntity(id, uuid, type, position, rotation, EntityLooks.PLAIN, EntityLife.full(EntityLife.DEFAULT_MAX_HEALTH));
    }

    public VirtualEntity movedTo(Point to, Rotation turned) {
        return new VirtualEntity(id, uuid, type, to, turned, looks, life);
    }

    public VirtualEntity withLooks(EntityLooks changed) {
        return new VirtualEntity(id, uuid, type, position, rotation, changed, life);
    }

    public VirtualEntity withLife(EntityLife changed) {
        return new VirtualEntity(id, uuid, type, position, rotation, looks, changed);
    }

    public VirtualEntity hurt(double damage) {
        return withLife(life.hurt(damage));
    }

    public double health() {
        return life.health();
    }

    public boolean isDead() {
        return life.isDead();
    }
}
