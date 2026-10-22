package tlm.dev.basalt.plugin.config;

import java.util.function.Consumer;
import java.util.function.Function;

public final class ConfigReader {

    private final String path;
    private final Function<String, Object> server;
    private final Function<String, Object> shipped;
    private final Consumer<String> warnings;

    public ConfigReader(String path, Function<String, Object> server, Function<String, Object> shipped, Consumer<String> warnings) {
        this.path = path;
        this.server = server;
        this.shipped = shipped;
        this.warnings = warnings;
    }

    public int wholeNumber(String key, int least, int most) {
        if (!(shipped.apply(key) instanceof Integer fallback) || fallback < least || fallback > most) {
            throw new IllegalStateException("the shipped file of " + path + " has no valid " + key);
        }
        Object value = server.apply(key);
        if (value instanceof Integer number && number >= least && number <= most) {
            return number;
        }
        if (value != null) {
            warnings.accept(path + ": " + key + " must be a whole number from " + least + " to " + most + ", using " + fallback);
        }
        return fallback;
    }

    public double number(String key, double least, double most) {
        if (!(shipped.apply(key) instanceof Number fallback) || !within(fallback.doubleValue(), least, most)) {
            throw new IllegalStateException("the shipped file of " + path + " has no valid " + key);
        }
        Object value = server.apply(key);
        if (value instanceof Number number && within(number.doubleValue(), least, most)) {
            return number.doubleValue();
        }
        if (value != null) {
            warnings.accept(path + ": " + key + " must be a number from " + least + " to " + most + ", using " + fallback.doubleValue());
        }
        return fallback.doubleValue();
    }

    private static boolean within(double value, double least, double most) {
        return Double.isFinite(value) && value >= least && value <= most;
    }
}
