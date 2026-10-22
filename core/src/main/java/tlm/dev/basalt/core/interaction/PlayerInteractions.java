package tlm.dev.basalt.core.interaction;

final class PlayerInteractions {

    private final BreakTracker breaks = new BreakTracker();
    private final ActionBudget budget;
    private int lastSequence = -1;

    PlayerInteractions(int actionsPerSecond) {
        this.budget = new ActionBudget(actionsPerSecond);
    }

    BreakTracker breaks() {
        return breaks;
    }

    ActionBudget budget() {
        return budget;
    }

    int lastSequence() {
        return lastSequence;
    }

    void sequenced(int sequence) {
        lastSequence = Math.max(lastSequence, sequence);
    }
}
