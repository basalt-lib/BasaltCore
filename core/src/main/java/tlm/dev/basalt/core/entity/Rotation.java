package tlm.dev.basalt.core.entity;

public record Rotation(float yaw, float pitch) {

    public static final Rotation NONE = new Rotation(0, 0);

    public Rotation {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("a rotation has finite angles");
        }
    }
}
