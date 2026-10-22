package tlm.dev.basalt.plugin.config;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public final class MessageFiles {

    public static final String FILE_NAME = "messages.yml";

    private final YamlFiles files;
    private final Logger logger;

    public MessageFiles(YamlFiles files, Logger logger) {
        this.files = files;
        this.logger = logger;
    }

    public MessageSettings load() {
        ConfigFile file = files.load(FILE_NAME);
        Map<MessageKey, String> templates = new EnumMap<>(MessageKey.class);
        for (MessageKey key : MessageKey.values()) {
            String path = key.path();
            if (file.server().contains(path) && !file.server().isString(path)) {
                logger.warning(file.path() + ": " + path + " is not a text, using the shipped one");
            }
            String template = file.server().isString(path) ? file.server().getString(path) : file.shipped().getString(path);
            if (template == null) {
                throw new IllegalStateException("the shipped " + FILE_NAME + " has no " + path);
            }
            templates.put(key, template);
        }
        return new MessageSettings(templates);
    }
}
