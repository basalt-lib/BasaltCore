package tlm.dev.basalt.core.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionBudgetTest {

    @Test
    void allowsOneSecondWorthAtOnceThenRefuses() {
        ActionBudget budget = new ActionBudget(20);

        for (int action = 0; action < 20; action++) {
            assertTrue(budget.take(500));
        }
        assertFalse(budget.take(500));
    }

    @Test
    void refillsWithTime() {
        ActionBudget budget = new ActionBudget(20);
        for (int action = 0; action < 20; action++) {
            budget.take(500);
        }

        assertTrue(budget.take(501));
        assertFalse(budget.take(501));
        assertTrue(budget.take(540));
        assertTrue(budget.take(540));
    }

    @Test
    void neverRefillsBackwardsNorAboveItsCapacity() {
        ActionBudget budget = new ActionBudget(2);
        assertTrue(budget.take(1000));
        assertTrue(budget.take(1000));
        assertFalse(budget.take(900));
        assertTrue(budget.take(1010));
        assertFalse(budget.take(1010));
        assertTrue(budget.take(5000));
        assertTrue(budget.take(5000));
        assertFalse(budget.take(5000));
        assertThrows(IllegalArgumentException.class, () -> new ActionBudget(0));
    }
}
