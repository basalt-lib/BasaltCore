package tlm.dev.basalt.core.entity;

public record EntityShape(double width, double height) {

    public static final double MAX_SIZE = 64;

    public EntityShape {
        if (!(width > 0 && width <= MAX_SIZE) || !(height > 0 && height <= MAX_SIZE)) {
            throw new IllegalArgumentException("an entity shape is from 0 to " + MAX_SIZE + " blocks: " + width + " x " + height);
        }
    }
}
