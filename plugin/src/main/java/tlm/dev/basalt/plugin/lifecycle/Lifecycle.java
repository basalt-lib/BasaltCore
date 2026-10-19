package tlm.dev.basalt.plugin.lifecycle;

public interface Lifecycle {

    default void onEnable() {
    }

    default void onDisable() {
    }

    default int priority() {
        return 0;
    }
}
