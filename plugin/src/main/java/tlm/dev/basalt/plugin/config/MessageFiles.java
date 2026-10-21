package tlm.dev.basalt.plugin.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

public final class MessageFiles {

    private static final String FILE_NAME = "messages.yml";

    private final Plugin plugin;

    public MessageFiles(Plugin plugin) {
        this.plugin = plugin;
    }

    public MessageSettings load() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        YamlConfiguration server = serverFile(file);
        YamlConfiguration shipped = shippedFile();
        Map<MessageKey, String> templates = new EnumMap<>(MessageKey.class);
        for (MessageKey key : MessageKey.values()) {
            String path = key.path();
            if (server.contains(path) && !server.isString(path)) {
                plugin.getLogger().warning(file.getPath() + ": " + path + " is not a text, using the shipped one");
            }
            String template = server.isString(path) ? server.getString(path) : shipped.getString(path);
            if (template == null) {
                throw new IllegalStateException("the shipped " + FILE_NAME + " has no " + path);
            }
            templates.put(key, template);
        }
        return new MessageSettings(templates);
    }

    private YamlConfiguration serverFile(File file) {
        YamlConfiguration server = new YamlConfiguration();
        if (!file.isFile()) {
            return server;
        }
        try {
            server.load(file);
            return server;
        } catch (IOException | InvalidConfigurationException invalid) {
            plugin.getLogger().severe(file.getPath() + " cannot be read, using the shipped messages: " + invalid.getMessage());
            return new YamlConfiguration();
        }
    }

    private YamlConfiguration shippedFile() {
        try (InputStream input = plugin.getResource(FILE_NAME)) {
            if (input == null) {
                throw new IllegalStateException("the plugin jar has no " + FILE_NAME);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
