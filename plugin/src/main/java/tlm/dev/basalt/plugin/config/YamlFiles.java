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

public final class YamlFiles {

    private final Plugin plugin;

    public YamlFiles(Plugin plugin) {
        this.plugin = plugin;
    }

    public ConfigFile load(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return new ConfigFile(file.getPath(), serverFile(file), shippedFile(fileName));
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
            plugin.getLogger().severe(file.getPath() + " cannot be read, using the shipped values: " + invalid.getMessage());
            return new YamlConfiguration();
        }
    }

    private YamlConfiguration shippedFile(String fileName) {
        try (InputStream input = plugin.getResource(fileName)) {
            if (input == null) {
                throw new IllegalStateException("the plugin jar has no " + fileName);
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }
}
