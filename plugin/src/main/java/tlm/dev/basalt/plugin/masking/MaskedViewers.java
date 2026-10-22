package tlm.dev.basalt.plugin.masking;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.Point;
import tlm.dev.basalt.core.visibility.EntityMask;
import tlm.dev.basalt.core.visibility.EntityMasking;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.RealEntity;
import tlm.dev.basalt.core.visibility.ViewChanges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MaskedViewers implements ViewChanges {

    private final Plugin plugin;
    private final EntityMasking masking;
    private final Map<UUID, Viewer> viewers = new HashMap<>();
    private long entitiesHidden;
    private long entitiesShown;
    private long entitiesDropped;

    public MaskedViewers(Plugin plugin, EntityMasking masking) {
        this.plugin = plugin;
        this.masking = masking;
    }

    @Override
    public void entered(UUID player, ChunkKey chunk) {
        viewers.computeIfAbsent(player, id -> new Viewer()).chunk = chunk;
        scan(player);
    }

    @Override
    public void left(UUID player, ExitCause cause) {
        release(player, cause);
    }

    public boolean isEmpty() {
        return viewers.isEmpty();
    }

    public int scanAll() {
        int read = 0;
        for (UUID player : List.copyOf(viewers.keySet())) {
            read += scan(player);
        }
        return read;
    }

    public boolean hidesOnTrack(Player player, Entity entity) {
        Viewer viewer = viewers.get(player.getUniqueId());
        if (viewer == null || rides(player, entity)
                || !EntityMasking.hides(player.getUniqueId(), Optional.of(viewer.chunk), real(entity))) {
            return false;
        }
        player.hideEntity(plugin, entity);
        viewer.hidden.add(entity.getUniqueId());
        entitiesHidden++;
        return true;
    }

    public void releaseAll() {
        for (UUID player : List.copyOf(viewers.keySet())) {
            release(player, ExitCause.SHUTDOWN);
        }
    }

    public String summary() {
        int stillHidden = viewers.values().stream().mapToInt(viewer -> viewer.hidden.size()).sum();
        return "Hid " + entitiesHidden + " real entities from players in a virtual chunk, showed " + entitiesShown + " back, "
                + entitiesDropped + " had left, " + stillHidden + " still hidden";
    }

    private int scan(UUID id) {
        Viewer viewer = viewers.get(id);
        Player player = plugin.getServer().getPlayer(id);
        if (viewer == null || player == null) {
            return 0;
        }
        Map<UUID, Entity> nearby = nearby(player.getWorld(), viewer.chunk);
        nearby.values().removeIf(entity -> rides(player, entity));
        viewer.hidden.removeIf(entity -> nearby.containsKey(entity) && player.canSee(nearby.get(entity)));
        List<RealEntity> entities = new ArrayList<>(nearby.size());
        nearby.values().forEach(entity -> entities.add(real(entity)));
        EntityMask mask = masking.decide(id, Optional.of(viewer.chunk), entities, viewer.hidden);
        for (UUID entity : mask.hide()) {
            player.hideEntity(plugin, nearby.get(entity));
            viewer.hidden.add(entity);
            entitiesHidden++;
        }
        mask.show().forEach(entity -> showBack(player, viewer, entity));
        return nearby.size();
    }

    private void release(UUID id, ExitCause cause) {
        Viewer viewer = viewers.remove(id);
        if (viewer == null) {
            return;
        }
        Player player = plugin.getServer().getPlayer(id);
        if (cause.playerLeaves() || player == null) {
            entitiesDropped += viewer.hidden.size();
            return;
        }
        EntityMask mask = masking.decide(id, Optional.empty(), List.of(), viewer.hidden);
        mask.show().forEach(entity -> showBack(player, viewer, entity));
    }

    private void showBack(Player player, Viewer viewer, UUID id) {
        viewer.hidden.remove(id);
        Entity entity = plugin.getServer().getEntity(id);
        if (entity == null) {
            entitiesDropped++;
            return;
        }
        player.showEntity(plugin, entity);
        entitiesShown++;
    }

    private static Map<UUID, Entity> nearby(World world, ChunkKey chunk) {
        Map<UUID, Entity> found = new HashMap<>();
        if (!world.isChunkLoaded(chunk.x(), chunk.z())) {
            return found;
        }
        double x = chunk.x() * 16.0;
        double z = chunk.z() * 16.0;
        BoundingBox box = new BoundingBox(x, world.getMinHeight(), z, x + 16, world.getMaxHeight(), z + 16);
        for (Entity entity : world.getNearbyEntities(box)) {
            found.put(entity.getUniqueId(), entity);
        }
        return found;
    }

    private static boolean rides(Player player, Entity entity) {
        return entity.equals(player.getVehicle()) || player.equals(entity.getVehicle());
    }

    private static RealEntity real(Entity entity) {
        Location at = entity.getLocation();
        return new RealEntity(entity.getUniqueId(), new Point(at.getX(), at.getY(), at.getZ()));
    }

    private static final class Viewer {

        private ChunkKey chunk;
        private final Set<UUID> hidden = new HashSet<>();
    }
}
