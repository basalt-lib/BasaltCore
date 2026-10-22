package tlm.dev.basalt.plugin.config;

import org.bukkit.configuration.file.YamlConfiguration;

public record ConfigFile(String path, YamlConfiguration server, YamlConfiguration shipped) {
}
