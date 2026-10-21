package tlm.dev.basalt.core.position;

public record Point(double x, double y, double z) {

    public boolean isFinite() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    public static Point centerOf(BlockPosition block) {
        return new Point(block.x() + 0.5, block.y() + 0.5, block.z() + 0.5);
    }
}
