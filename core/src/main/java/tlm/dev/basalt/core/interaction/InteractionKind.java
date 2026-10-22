package tlm.dev.basalt.core.interaction;

public enum InteractionKind {
    START_BREAK(true),
    ABORT_BREAK(false),
    STOP_BREAK(true),
    USE_ON(true);

    private final boolean predictsBlocks;

    InteractionKind(boolean predictsBlocks) {
        this.predictsBlocks = predictsBlocks;
    }

    public boolean predictsBlocks() {
        return predictsBlocks;
    }
}
