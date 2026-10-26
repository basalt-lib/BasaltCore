package tlm.dev.basalt.core.visibility;

public enum ExitCause {
    COMMAND(true, false, false, false),
    WORLD_CLOSED(true, false, false, true),
    WORLD_CHANGE(false, false, false, false),
    DISCONNECT(false, true, true, false),
    SHUTDOWN(true, true, false, true);

    private final boolean clientKeepsChunks;
    private final boolean endsTracking;
    private final boolean playerLeaves;
    private final boolean everyViewerLeaves;

    ExitCause(boolean clientKeepsChunks, boolean endsTracking, boolean playerLeaves, boolean everyViewerLeaves) {
        this.clientKeepsChunks = clientKeepsChunks;
        this.endsTracking = endsTracking;
        this.playerLeaves = playerLeaves;
        this.everyViewerLeaves = everyViewerLeaves;
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

    public boolean everyViewerLeaves() {
        return everyViewerLeaves;
    }
}
