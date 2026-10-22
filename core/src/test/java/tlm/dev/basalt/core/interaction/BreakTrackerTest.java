package tlm.dev.basalt.core.interaction;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.BlockPosition;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreakTrackerTest {

    private static final BlockPosition FIRST = new BlockPosition(1029, -58, 1029);
    private static final BlockPosition SECOND = new BlockPosition(1030, -58, 1029);
    private static final float PROGRESS = 2.0F / 1.25F / 30;

    private final BreakTracker breaks = new BreakTracker();

    @Test
    void breaksWhenTheStopComesInTime() {
        breaks.start(FIRST, 100);

        assertEquals(BreakStop.BROKEN, breaks.stop(FIRST, 113, PROGRESS));
        assertEquals(BreakStop.NOT_BREAKING, breaks.stop(FIRST, 114, PROGRESS));
        assertEquals(Optional.empty(), breaks.delayed());
    }

    @Test
    void keepsAnEarlyStopAsADelayedBreakFromTheFirstStart() {
        breaks.start(FIRST, 100);

        assertEquals(BreakStop.DELAYED, breaks.stop(FIRST, 111, PROGRESS));

        assertEquals(Optional.of(FIRST), breaks.delayed());
        assertEquals(100, breaks.delayedSince());
        breaks.clearDelayed();
        assertEquals(Optional.empty(), breaks.delayed());
    }

    @Test
    void ignoresAStopOnAnotherBlockOrAfterAnAbort() {
        breaks.start(FIRST, 100);
        assertEquals(BreakStop.NOT_BREAKING, breaks.stop(SECOND, 120, PROGRESS));

        breaks.abort();

        assertEquals(BreakStop.NOT_BREAKING, breaks.stop(FIRST, 120, PROGRESS));
    }

    @Test
    void namesTheBlockItStopsBreakingForANewOne() {
        assertEquals(Optional.empty(), breaks.start(FIRST, 100));
        assertEquals(Optional.empty(), breaks.start(FIRST, 101));
        assertEquals(Optional.of(FIRST), breaks.start(SECOND, 102));
        assertEquals(BreakStop.BROKEN, breaks.stop(SECOND, 115, PROGRESS));
    }
}
