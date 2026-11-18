package tlm.dev.basalt.plugin.command;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.api.Overlay;
import tlm.dev.basalt.api.event.OverlayBlockBreakEvent;
import tlm.dev.basalt.api.event.OverlayBlockUseEvent;
import tlm.dev.basalt.api.event.OverlayEntityUseEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class DebugOverlays implements Listener {

    private final Map<String, Entry> named = new LinkedHashMap<>();

    public void listen(Plugin plugin) {
        on(plugin, OverlayBlockUseEvent.class, event -> entryOf(event.overlay()).ifPresent(entry -> {
            entry.uses++;
            entry.last = "use " + event.player().getName() + " " + at(event.location()) + " " + event.block().getAsString();
        }));
        on(plugin, OverlayBlockBreakEvent.class, event -> entryOf(event.overlay()).ifPresent(entry -> {
            entry.breaks++;
            event.setCancelled(entry.keepsBlocks);
            entry.last = "break " + event.player().getName() + " " + at(event.location()) + " " + event.block().getAsString()
                    + (entry.keepsBlocks ? " kept" : " removed");
        }));
        on(plugin, OverlayEntityUseEvent.class, event -> entryOf(event.overlay()).ifPresent(entry -> {
            entry.entityUses++;
            entry.last = "entity-use " + event.player().getName() + " " + event.entity().id();
        }));
    }

    void put(String name, Overlay overlay) {
        named.put(name, new Entry(overlay));
    }

    Optional<Overlay> live(String name) {
        return entry(name).map(entry -> entry.overlay);
    }

    Optional<Entry> entry(String name) {
        Entry entry = named.get(name);
        if (entry != null && entry.overlay.isClosed()) {
            named.remove(name);
            return Optional.empty();
        }
        return Optional.ofNullable(entry);
    }

    String nameOf(Overlay overlay) {
        return named.entrySet().stream().filter(entry -> entry.getValue().overlay.equals(overlay)).map(Map.Entry::getKey)
                .findFirst().orElse(overlay.id().toString().substring(0, 8));
    }

    void remove(String name) {
        named.remove(name);
    }

    private Optional<Entry> entryOf(Overlay overlay) {
        return named.values().stream().filter(entry -> entry.overlay.equals(overlay)).findFirst();
    }

    private <E extends Event> void on(Plugin plugin, Class<E> type, Consumer<E> handler) {
        plugin.getServer().getPluginManager().registerEvent(type, this, EventPriority.NORMAL, (listener, event) -> {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }, plugin);
    }

    private static String at(Location location) {
        return location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    static final class Entry {

        private final Overlay overlay;
        private int uses;
        private int breaks;
        private int entityUses;
        private String last = "none";
        private boolean keepsBlocks;

        private Entry(Overlay overlay) {
            this.overlay = overlay;
        }

        Overlay overlay() {
            return overlay;
        }

        int uses() {
            return uses;
        }

        int breaks() {
            return breaks;
        }

        int entityUses() {
            return entityUses;
        }

        String last() {
            return last;
        }

        boolean keepsBlocks() {
            return keepsBlocks;
        }

        void keepBlocks(boolean keep) {
            keepsBlocks = keep;
        }
    }
}
