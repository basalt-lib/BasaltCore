package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.Optional;

public final class InteractionValidator {

    public static final double HIT_TOLERANCE = 1.0000001;
    public static final float STOP_THRESHOLD = 0.7F;
    public static final float FULL_PROGRESS = 1.0F;

    private InteractionValidator() {
    }

    public static Optional<Refusal> screen(InteractionRequest request, int lastSequence) {
        if (request.sequence() < 0) {
            return Optional.of(Refusal.MALFORMED);
        }
        if (request.kind().predictsBlocks() && request.sequence() <= lastSequence) {
            return Optional.of(Refusal.MALFORMED);
        }
        if (request.hit().isPresent() && !request.hit().get().isFinite()) {
            return Optional.of(Refusal.MALFORMED);
        }
        return Optional.empty();
    }

    public static boolean withinReach(Point eyes, BlockPosition block, double reach, double margin) {
        double dx = gap(eyes.x(), block.x());
        double dy = gap(eyes.y(), block.y());
        double dz = gap(eyes.z(), block.z());
        double limit = reach + margin;
        return dx * dx + dy * dy + dz * dz < limit * limit;
    }

    public static boolean hitInsideBlock(Point hit, BlockPosition block) {
        Point center = Point.centerOf(block);
        return Math.abs(hit.x() - center.x()) < HIT_TOLERANCE
                && Math.abs(hit.y() - center.y()) < HIT_TOLERANCE
                && Math.abs(hit.z() - center.z()) < HIT_TOLERANCE;
    }

    public static boolean breaksAtOnce(float progressPerTick) {
        return progressPerTick >= FULL_PROGRESS;
    }

    public static boolean breaksAtStop(float progressPerTick, long elapsedTicks) {
        return progressPerTick * (elapsedTicks + 1) >= STOP_THRESHOLD;
    }

    public static boolean breaksWhenDelayed(float progressPerTick, long elapsedTicks) {
        return progressPerTick * (elapsedTicks + 1) >= FULL_PROGRESS;
    }

    private static double gap(double coordinate, int blockMin) {
        return Math.max(0, Math.max(blockMin - coordinate, coordinate - (blockMin + 1)));
    }
}
