package tlm.dev.basalt.plugin.config;

public enum MessageKey {
    DEBUG_CHUNK_SHOWN("debug-chunk-shown"),
    DEBUG_CHUNK_RESTORED("debug-chunk-restored"),
    DEBUG_CHUNK_ALREADY_VIRTUAL("debug-chunk-already-virtual"),
    DEBUG_CHUNK_NOT_VIRTUAL("debug-chunk-not-virtual"),
    DEBUG_CHUNK_PLAYER_OFFLINE("debug-chunk-player-offline"),
    DEBUG_CHUNK_NOT_TRACKED("debug-chunk-not-tracked");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
