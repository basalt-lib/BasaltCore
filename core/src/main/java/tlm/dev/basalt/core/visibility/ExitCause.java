package tlm.dev.basalt.core.visibility;

public enum ExitCause {
    COMMAND(true, false),
    WORLD_CHANGE(false, false),
    DISCONNECT(false, true),
    SHUTDOWN(true, true);

    private final boolean clientKeepsChunks;
    private final boolean endsTracking;

    ExitCause(boolean clientKeepsChunks, boolean endsTracking) {
        this.clientKeepsChunks = clientKeepsChunks;
        this.endsTracking = endsTracking;
    }

    public boolean clientKeepsChunks() {
        return clientKeepsChunks;
    }

    public boolean endsTracking() {
        return endsTracking;
    }
}
