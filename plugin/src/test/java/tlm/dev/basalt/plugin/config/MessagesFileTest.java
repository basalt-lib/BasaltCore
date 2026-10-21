package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesFileTest {

    private static final Pattern ENTRY = Pattern.compile("(?m)^([a-z-]+): \"(.*)\"$");

    @Test
    void shipsATemplateForEveryKey() throws IOException {
        Set<String> expected = Arrays.stream(MessageKey.values()).map(MessageKey::path).collect(Collectors.toSet());

        assertEquals(expected, entries().keySet());
    }

    @Test
    void shipsNoEmptyTemplate() throws IOException {
        entries().forEach((key, template) -> assertFalse(template.isBlank(), key + " is empty"));
    }

    @Test
    void namesThePlayerInEveryTemplate() throws IOException {
        entries().forEach((key, template) -> assertTrue(template.contains("<player>"), key + " does not name the player"));
    }

    private Map<String, String> entries() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/messages.yml")) {
            assertNotNull(input, "messages.yml is missing");
            Matcher matcher = ENTRY.matcher(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            Map<String, String> entries = new LinkedHashMap<>();
            while (matcher.find()) {
                entries.put(matcher.group(1), matcher.group(2));
            }
            return entries;
        }
    }
}
