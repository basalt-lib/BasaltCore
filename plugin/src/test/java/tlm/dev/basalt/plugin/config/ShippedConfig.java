package tlm.dev.basalt.plugin.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ShippedConfig {

    private static final Pattern SECTION = Pattern.compile("^([a-z-]+):$");
    private static final Pattern ENTRY = Pattern.compile("^  ([a-z-]+): ([0-9.]+)$");

    private ShippedConfig() {
    }

    static Map<String, String> entries() throws IOException {
        try (InputStream input = ShippedConfig.class.getResourceAsStream("/config.yml")) {
            if (input == null) {
                throw new IOException("config.yml is missing");
            }
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            Map<String, String> entries = new LinkedHashMap<>();
            String section = null;
            for (String line : text.split("\n")) {
                Matcher sectionLine = SECTION.matcher(line);
                Matcher entryLine = ENTRY.matcher(line);
                if (sectionLine.matches()) {
                    section = sectionLine.group(1);
                } else if (entryLine.matches() && section != null) {
                    entries.put(section + "." + entryLine.group(1), entryLine.group(2));
                } else {
                    throw new IOException("unexpected line in config.yml: " + line);
                }
            }
            return entries;
        }
    }

    static Map<String, Object> values() throws IOException {
        Map<String, Object> values = new HashMap<>();
        entries().forEach((key, value) -> values.put(key, value.contains(".") ? (Object) Double.valueOf(value) : (Object) Integer.valueOf(value)));
        return values;
    }
}
