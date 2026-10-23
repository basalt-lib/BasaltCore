package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.Point;

public record EntityTarget(Point feet, double width, double height) {

    public EntityTarget {
        if (feet == null || !feet.isFinite()) {
            throw new IllegalArgumentException("a target stands at a finite position");
        }
        if (!(width > 0) || !(height > 0)) {
            throw new IllegalArgumentException("a target has a positive width and height");
        }
    }

    public double distanceSquaredFrom(Point eyes) {
        double half = width / 2;
        double dx = gap(eyes.x(), feet.x() - half, feet.x() + half);
        double dy = gap(eyes.y(), feet.y(), feet.y() + height);
        double dz = gap(eyes.z(), feet.z() - half, feet.z() + half);
        return dx * dx + dy * dy + dz * dz;
    }

    private static double gap(double coordinate, double low, double high) {
        return Math.max(0, Math.max(low - coordinate, coordinate - high));
    }
}
