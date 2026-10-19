package tlm.dev.basalt.plugin.lifecycle;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LifecycleManager {

    private final List<Lifecycle> components;
    private final Logger logger;

    public LifecycleManager(List<Lifecycle> components, Logger logger) {
        this.components = components.stream()
                .sorted(Comparator.comparingInt(Lifecycle::priority))
                .toList();
        this.logger = logger;
    }

    public void enable() {
        for (Lifecycle component : components) {
            run(component, Lifecycle::onEnable, "enable");
        }
    }

    public void disable() {
        for (Lifecycle component : components.reversed()) {
            run(component, Lifecycle::onDisable, "disable");
        }
    }

    private void run(Lifecycle component, Consumer<Lifecycle> step, String stepName) {
        try {
            step.accept(component);
        } catch (RuntimeException failure) {
            logger.log(Level.SEVERE, "Could not " + stepName + " " + component.getClass().getSimpleName(), failure);
        }
    }
}
