package tlm.dev.basalt.plugin.config;

public enum MessageKey {
    DEBUG_CHUNK_SHOWN("debug-chunk-shown"),
    DEBUG_CHUNK_RESTORED("debug-chunk-restored"),
    DEBUG_CHUNK_ALREADY_VIRTUAL("debug-chunk-already-virtual"),
    DEBUG_CHUNK_NOT_VIRTUAL("debug-chunk-not-virtual"),
    DEBUG_CHUNK_PLAYER_OFFLINE("debug-chunk-player-offline"),
    DEBUG_CHUNK_NOT_TRACKED("debug-chunk-not-tracked"),
    DEBUG_CHUNK_REFUSED("debug-chunk-refused"),
    DEBUG_ENTITY_SPAWNED("debug-entity-spawned"),
    DEBUG_ENTITY_MOVED("debug-entity-moved"),
    DEBUG_ENTITY_REMOVED("debug-entity-removed"),
    DEBUG_ENTITY_STATUS("debug-entity-status"),
    DEBUG_ENTITY_NOT_VIRTUAL("debug-entity-not-virtual"),
    DEBUG_ENTITY_OUTSIDE("debug-entity-outside"),
    DEBUG_ENTITY_TOO_MANY("debug-entity-too-many"),
    DEBUG_ENTITY_NONE("debug-entity-none");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
