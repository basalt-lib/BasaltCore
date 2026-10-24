package tlm.dev.basalt.plugin.masking;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.PlayerGroup;
import tlm.dev.basalt.core.visibility.PlayerSeparation;
import tlm.dev.basalt.core.visibility.SeparationPlan;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldListener;
import tlm.dev.basalt.core.world.WorldSnapshot;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlayerSeparations implements ViewChanges, WorldListener, Lifecycle {

    private final Plugin plugin;
    private final Supplier<VirtualWorlds<?>> worlds;
    private final Map<UUID, Set<UUID>> hidden = new HashMap<>();
    private long pairsHidden;
    private long pairsShown;

    public PlayerSeparations(Plugin plugin, Supplier<VirtualWorlds<?>> worlds) {
        this.plugin = plugin;
        this.worlds = worlds;
    }

    @Override
    public void entered(UUID player, UUID world, ChunkRange area) {
        refresh(player);
    }

    @Override
    public void left(UUID player, UUID world, ExitCause cause) {
        if (cause.playerLeaves()) {
            forget(player);
        } else {
            refresh(player);
        }
    }

    @Override
    public void policyChanged(UUID world) {
        worlds.get().snapshot(world).map(WorldSnapshot::viewers).orElse(Set.of()).forEach(this::refresh);
    }

    public void joined(UUID player) {
        refresh(player);
    }

    public void forget(UUID player) {
        Set<UUID> apart = hidden.remove(player);
        if (apart != null) {
            apart.forEach(other -> hidden.getOrDefault(other, new HashSet<>()).remove(player));
        }
    }

    @Override
    public void onDisable() {
        for (UUID player : List.copyOf(hidden.keySet())) {
            Player online = plugin.getServer().getPlayer(player);
            for (UUID other : Set.copyOf(hidden.getOrDefault(player, Set.of()))) {
                Player otherOnline = plugin.getServer().getPlayer(other);
                if (online != null && otherOnline != null) {
                    show(online, otherOnline);
                }
            }
        }
        long stillApart = hidden.values().stream().mapToInt(Set::size).sum() / 2;
        plugin.getLogger().info("Separated " + pairsHidden + " pairs of players, joined " + pairsShown + " back, "
                + stillApart + " still apart");
    }

    private void refresh(UUID id) {
        Player player = plugin.getServer().getPlayer(id);
        if (player == null) {
            return;
        }
        Map<UUID, PlayerGroup> others = new HashMap<>();
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            others.put(other.getUniqueId(), groupOf(other.getUniqueId()));
        }
        SeparationPlan plan = PlayerSeparation.plan(id, groupOf(id), others, hidden.getOrDefault(id, Set.of()));
        for (UUID other : plan.hide()) {
            Player otherOnline = plugin.getServer().getPlayer(other);
            if (otherOnline != null) {
                hide(player, otherOnline);
            }
        }
        for (UUID other : plan.show()) {
            Player otherOnline = plugin.getServer().getPlayer(other);
            if (otherOnline != null) {
                show(player, otherOnline);
            }
        }
    }

    private void hide(Player first, Player second) {
        first.hideEntity(plugin, second);
        second.hideEntity(plugin, first);
        hidden.computeIfAbsent(first.getUniqueId(), id -> new HashSet<>()).add(second.getUniqueId());
        hidden.computeIfAbsent(second.getUniqueId(), id -> new HashSet<>()).add(first.getUniqueId());
        pairsHidden++;
    }

    private void show(Player first, Player second) {
        first.showEntity(plugin, second);
        second.showEntity(plugin, first);
        hidden.getOrDefault(first.getUniqueId(), new HashSet<>()).remove(second.getUniqueId());
        hidden.getOrDefault(second.getUniqueId(), new HashSet<>()).remove(first.getUniqueId());
        pairsShown++;
    }

    private PlayerGroup groupOf(UUID player) {
        VirtualWorlds<?> all = worlds.get();
        Optional<UUID> world = all.worldOf(player);
        if (world.isEmpty()) {
            return PlayerGroup.outside();
        }
        return all.snapshot(world.get())
                .map(snapshot -> PlayerGroup.of(snapshot.id(), snapshot.label().openToEveryone()))
                .orElse(PlayerGroup.outside());
    }
}
