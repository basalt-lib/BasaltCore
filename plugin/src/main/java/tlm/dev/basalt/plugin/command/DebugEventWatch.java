package tlm.dev.basalt.plugin.command;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.api.event.VirtualBlockBreakEvent;
import tlm.dev.basalt.api.event.VirtualBlockPlaceEvent;
import tlm.dev.basalt.api.event.VirtualBlockUseEvent;
import tlm.dev.basalt.api.event.VirtualEntityDamageEvent;
import tlm.dev.basalt.api.event.VirtualEntityDeathEvent;
import tlm.dev.basalt.api.event.VirtualEntityUseEvent;
import tlm.dev.basalt.api.event.VirtualViewerAddEvent;
import tlm.dev.basalt.api.event.VirtualViewerRemoveEvent;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DebugEventWatch implements Listener {

    private static final String NONE = "none";

    private final Plugin plugin;
    private final Map<UUID, Watch> watches = new HashMap<>();

    public DebugEventWatch(Plugin plugin) {
        this.plugin = plugin;
    }

    public void listen() {
        on(VirtualBlockBreakEvent.class, event -> handle(event.player(), DebugEventKind.BREAK, event,
                at(event.world(), event.location()) + " block=" + block(event.block())));
        on(VirtualBlockPlaceEvent.class, event -> {
            watchOf(event.player()).filter(watch -> watch.mode.consumesPlaces()).ifPresent(watch -> event.setConsumeItem(true));
            handle(event.player(), DebugEventKind.PLACE, event, at(event.world(), event.location()) + " block=" + block(event.block())
                    + " hand=" + label(event.hand()) + " consume=" + event.consumeItem());
        });
        on(VirtualBlockUseEvent.class, event -> handle(event.player(), DebugEventKind.USE, event, at(event.world(), event.location())
                + " block=" + block(event.block()) + " face=" + label(event.face()) + " hand=" + label(event.hand())));
        on(VirtualEntityDamageEvent.class, event -> {
            watchOf(event.player()).ifPresent(watch -> event.setDamage(event.damage() * watch.mode.damageFactor()));
            handle(event.player(), DebugEventKind.DAMAGE, event, world(event.world()) + " entity=" + event.entity().id()
                    + " type=" + event.entity().type().getKey() + " damage=" + event.damage());
        });
        on(VirtualEntityUseEvent.class, event -> handle(event.player(), DebugEventKind.ENTITY_USE, event,
                world(event.world()) + " entity=" + event.entity().id() + " hand=" + label(event.hand())));
        on(VirtualEntityDeathEvent.class, event -> {
            String killer = event.killer() == null ? NONE : event.killer().getName();
            for (Player viewer : event.world().viewers()) {
                handle(viewer, DebugEventKind.DEATH, event, world(event.world()) + " entity=" + event.entity().id() + " killer=" + killer);
            }
        });
        on(VirtualViewerAddEvent.class, event -> handle(event.player(), DebugEventKind.ADD, event, world(event.world())));
        on(VirtualViewerRemoveEvent.class, event -> handle(event.player(), DebugEventKind.REMOVE, event,
                world(event.world()) + " reason=" + label(event.reason())));
    }

    public void watch(UUID player, DebugEventMode mode) {
        watches.put(player, new Watch(mode));
    }

    public boolean stop(UUID player) {
        return watches.remove(player) != null;
    }

    public Optional<DebugEventMode> mode(UUID player) {
        return Optional.ofNullable(watches.get(player)).map(watch -> watch.mode);
    }

    public String counts(UUID player) {
        Watch watch = watches.get(player);
        return Arrays.stream(DebugEventKind.values())
                .map(kind -> kind.label() + "=" + (watch == null ? 0 : watch.counts.getOrDefault(kind, 0)))
                .collect(Collectors.joining(" "));
    }

    public String last(UUID player) {
        Watch watch = watches.get(player);
        if (watch == null || watch.last.isEmpty()) {
            return NONE;
        }
        return watch.last.entrySet().stream()
                .map(entry -> entry.getKey().label() + " " + entry.getValue())
                .collect(Collectors.joining("; "));
    }

    private <E extends Event> void on(Class<E> type, Consumer<E> handler) {
        plugin.getServer().getPluginManager().registerEvent(type, this, EventPriority.NORMAL, (listener, event) -> {
            if (type.isInstance(event)) {
                handler.accept(type.cast(event));
            }
        }, plugin);
    }

    private void handle(Player player, DebugEventKind kind, Event event, String details) {
        Optional<Watch> watch = watchOf(player);
        if (watch.isEmpty()) {
            return;
        }
        if (event instanceof Cancellable cancellable && watch.get().mode.cancels(kind)) {
            cancellable.setCancelled(true);
        }
        boolean cancelled = event instanceof Cancellable cancellable && cancellable.isCancelled();
        watch.get().counts.merge(kind, 1, Integer::sum);
        watch.get().last.put(kind, details + (cancelled ? " cancelled" : ""));
    }

    private Optional<Watch> watchOf(Player player) {
        return Optional.ofNullable(watches.get(player.getUniqueId()));
    }

    private static String world(VirtualWorld world) {
        return "world=" + world.id().toString().substring(0, 8);
    }

    private static String at(VirtualWorld world, Location location) {
        return world(world) + " at=" + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    }

    private static String block(BlockData block) {
        return block.getAsString();
    }

    private static String label(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static final class Watch {

        private final DebugEventMode mode;
        private final Map<DebugEventKind, Integer> counts = new EnumMap<>(DebugEventKind.class);
        private final Map<DebugEventKind, String> last = new EnumMap<>(DebugEventKind.class);

        private Watch(DebugEventMode mode) {
            this.mode = mode;
        }
    }
}
