package tlm.dev.basalt.core.visibility;

public enum ExitCause {
    COMMAND(true, false, false),
    WORLD_CHANGE(false, false, false),
    DISCONNECT(false, true, true),
    SHUTDOWN(true, true, false);

    private final boolean clientKeepsChunks;
    private final boolean endsTracking;
    private final boolean playerLeaves;

    ExitCause(boolean clientKeepsChunks, boolean endsTracking, boolean playerLeaves) {
        this.clientKeepsChunks = clientKeepsChunks;
        this.endsTracking = endsTracking;
        this.playerLeaves = playerLeaves;
    }

    public boolean clientKeepsChunks() {
        return clientKeepsChunks;
    }

    public boolean endsTracking() {
        return endsTracking;
    }

    public boolean playerLeaves() {
        return playerLeaves;
    }
}
