package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.Point;

public record PlayerStance(Point eyes, Point feet, boolean onGround, double reach) {

    public static final double HALF_WIDTH = 0.3;
    public static final double GROUND_PROBE = 0.001;

    public PlayerStance {
        if (eyes == null || feet == null) {
            throw new IllegalArgumentException("a stance needs the eyes and the feet");
        }
        if (!Double.isFinite(reach) || reach < 0) {
            throw new IllegalArgumentException("reach must be a distance of 0 or more: " + reach);
        }
    }
}
