package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.Point;

public record PlayerStance(Point eyes, Point feet, boolean onGround, Reach reach) {

    public static final double HALF_WIDTH = 0.3;
    public static final double GROUND_PROBE = 0.001;

    public PlayerStance {
        if (eyes == null || feet == null || reach == null) {
            throw new IllegalArgumentException("a stance needs the eyes, the feet and the reach");
        }
    }
}
