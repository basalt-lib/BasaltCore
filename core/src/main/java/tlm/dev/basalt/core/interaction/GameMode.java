package tlm.dev.basalt.core.interaction;

public enum GameMode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR;

    public boolean changesBlocks() {
        return this == SURVIVAL || this == CREATIVE;
    }

    public boolean touchesEntities() {
        return this != SPECTATOR;
    }
}
