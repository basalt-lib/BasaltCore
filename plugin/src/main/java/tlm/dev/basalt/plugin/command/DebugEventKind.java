package tlm.dev.basalt.plugin.command;

public enum DebugEventKind {
    BREAK("break"),
    PLACE("place"),
    USE("use"),
    DAMAGE("damage"),
    ENTITY_USE("entity-use"),
    DEATH("death"),
    ADD("add"),
    REMOVE("remove");

    private final String label;

    DebugEventKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
