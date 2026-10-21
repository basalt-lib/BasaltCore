package tlm.dev.basalt.plugin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDescriptorTest {

    @Test
    void declaresBasaltCore() throws IOException {
        assertEquals("BasaltCore", value("name"));
    }

    @Test
    void mainClassExists() throws IOException {
        String main = value("main");

        assertNotNull(getClass().getResource("/" + main.replace('.', '/') + ".class"), main + " is not compiled");
    }

    @Test
    void versionComesFromGradle() throws IOException {
        assertEquals(System.getProperty("basalt.version"), value("version"));
    }

    @Test
    void declaresApiVersion() throws IOException {
        assertEquals("1.21.11", value("api-version"));
    }

    @Test
    void grantsTheAdminPermissionToOperators() throws IOException {
        assertTrue(descriptor().contains("permissions:\n  basalt.admin:\n    default: op\n"), "basalt.admin must default to op");
    }

    private String value(String key) throws IOException {
        Matcher matcher = Pattern.compile("(?m)^" + Pattern.quote(key) + ":\\s*'?([^'\\r\\n]*)'?\\s*$").matcher(descriptor());
        assertTrue(matcher.find(), key + " is missing from paper-plugin.yml");
        return matcher.group(1);
    }

    private String descriptor() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/paper-plugin.yml")) {
            assertNotNull(input, "paper-plugin.yml is missing");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }
}
