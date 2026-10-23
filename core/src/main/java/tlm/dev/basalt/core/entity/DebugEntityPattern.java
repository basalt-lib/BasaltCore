package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.Point;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class DebugEntityPattern {

    public static final String TYPE = "minecraft:zombie";
    public static final String NAME = "Basalt";
    public static final int HEALTH = 5;
    public static final int DAMAGE_PER_HIT = 1;
    public static final int HIT_COOLDOWN_TICKS = 10;
    public static final int REMOVAL_AFTER_DEATH_TICKS = 20;

    private DebugEntityPattern() {
    }

    public static VirtualEntity at(int id, Point position) {
        return new VirtualEntity(id, uuidOf(id), TYPE, position, Rotation.NONE, NAME, HEALTH);
    }

    static UUID uuidOf(int id) {
        return UUID.nameUUIDFromBytes(("basalt:virtual-entity:" + id).getBytes(StandardCharsets.UTF_8));
    }
}
