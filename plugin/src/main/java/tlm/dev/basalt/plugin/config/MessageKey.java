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
    DEBUG_ENTITY_NONE("debug-entity-none"),
    DEBUG_ENTITY_TYPE_REFUSED("debug-entity-type-refused"),
    DEBUG_ENTITY_HEALTH("debug-entity-health"),
    DEBUG_WORLD_CREATED("debug-world-created"),
    DEBUG_WORLD_BAD_NAME("debug-world-bad-name"),
    DEBUG_WORLD_EXISTS("debug-world-exists"),
    DEBUG_WORLD_REFUSED("debug-world-refused"),
    DEBUG_WORLD_UNKNOWN("debug-world-unknown"),
    DEBUG_WORLD_ADDED("debug-world-added"),
    DEBUG_WORLD_VIEWER_REFUSED("debug-world-viewer-refused"),
    DEBUG_WORLD_REMOVED("debug-world-removed"),
    DEBUG_WORLD_NOT_VIEWING("debug-world-not-viewing"),
    DEBUG_WORLD_CLOSED("debug-world-closed"),
    DEBUG_WORLD_INFO("debug-world-info"),
    DEBUG_WORLD_VISIBILITY("debug-world-visibility"),
    DEBUG_EVENTS_WATCHING("debug-events-watching"),
    DEBUG_EVENTS_OFF("debug-events-off"),
    DEBUG_EVENTS_STATUS("debug-events-status");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
