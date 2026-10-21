package tlm.dev.basalt.plugin.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.EnumMap;
import java.util.Map;

public final class MessageSettings {

    private final Map<MessageKey, String> templates;

    public MessageSettings(Map<MessageKey, String> templates) {
        for (MessageKey key : MessageKey.values()) {
            if (!templates.containsKey(key)) {
                throw new IllegalArgumentException("missing message " + key.path());
            }
        }
        this.templates = new EnumMap<>(templates);
    }

    public Component render(MessageKey key, TagResolver... resolvers) {
        return MiniMessage.miniMessage().deserialize(templates.get(key), resolvers);
    }
}
