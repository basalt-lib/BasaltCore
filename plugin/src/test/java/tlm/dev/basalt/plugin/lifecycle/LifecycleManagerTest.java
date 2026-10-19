package tlm.dev.basalt.plugin.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LifecycleManagerTest {

    private final List<String> events = new ArrayList<>();
    private final List<LogRecord> logged = new ArrayList<>();
    private Logger logger;

    @BeforeEach
    void captureLogs() {
        logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                logged.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    @Test
    void enablesByAscendingPriority() {
        LifecycleManager manager = new LifecycleManager(List.of(
                new Recording("late", 10, events),
                new Recording("early", -5, events),
                new Recording("middle", 0, events)), logger);

        manager.enable();

        assertEquals(List.of("early enable", "middle enable", "late enable"), events);
    }

    @Test
    void disablesInReverseOrder() {
        LifecycleManager manager = new LifecycleManager(List.of(
                new Recording("late", 10, events),
                new Recording("early", -5, events)), logger);

        manager.disable();

        assertEquals(List.of("late disable", "early disable"), events);
    }

    @Test
    void keepsRegistrationOrderForEqualPriorities() {
        LifecycleManager manager = new LifecycleManager(List.of(
                new Recording("first", 0, events),
                new Recording("second", 0, events)), logger);

        manager.enable();

        assertEquals(List.of("first enable", "second enable"), events);
    }

    @Test
    void enableGoesOnAfterAFailingComponent() {
        LifecycleManager manager = new LifecycleManager(List.of(
                new Failing(0),
                new Recording("next", 1, events)), logger);

        manager.enable();

        assertEquals(List.of("next enable"), events);
        assertEquals(1, logged.size());
        assertEquals(Level.SEVERE, logged.getFirst().getLevel());
        assertEquals(IllegalStateException.class, logged.getFirst().getThrown().getClass());
    }

    @Test
    void disableGoesOnAfterAFailingComponent() {
        LifecycleManager manager = new LifecycleManager(List.of(
                new Recording("previous", -1, events),
                new Failing(0)), logger);

        manager.disable();

        assertEquals(List.of("previous disable"), events);
        assertEquals(1, logged.size());
    }

    private record Recording(String name, int priority, List<String> events) implements Lifecycle {

        @Override
        public void onEnable() {
            events.add(name + " enable");
        }

        @Override
        public void onDisable() {
            events.add(name + " disable");
        }
    }

    private record Failing(int priority) implements Lifecycle {

        @Override
        public void onEnable() {
            throw new IllegalStateException("enable failed");
        }

        @Override
        public void onDisable() {
            throw new IllegalStateException("disable failed");
        }
    }
}
