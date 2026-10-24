package tlm.dev.basalt.plugin.facade;

import org.bukkit.util.BoundingBox;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;

final class BlockBoxes {

    private BlockBoxes() {
    }

    static BlockBox of(BoundingBox box) {
        int minX = first(box.getMinX());
        int minY = first(box.getMinY());
        int minZ = first(box.getMinZ());
        try {
            return new BlockBox(new BlockPosition(minX, minY, minZ),
                    new BlockPosition(last(minX, box.getMaxX()), last(minY, box.getMaxY()), last(minZ, box.getMaxZ())));
        } catch (IllegalArgumentException outsideTheWorld) {
            throw new IllegalArgumentException("the box " + box + " reaches outside the world", outsideTheWorld);
        }
    }

    private static int first(double min) {
        if (!Double.isFinite(min)) {
            throw new IllegalArgumentException("a box corner is not finite: " + min);
        }
        return (int) Math.floor(min);
    }

    private static int last(int first, double max) {
        if (!Double.isFinite(max)) {
            throw new IllegalArgumentException("a box corner is not finite: " + max);
        }
        return Math.max(first, (int) Math.ceil(max) - 1);
    }
}
