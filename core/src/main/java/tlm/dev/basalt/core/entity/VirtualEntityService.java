package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class VirtualEntityService {

    private final EntityTools tools;
    private final EntitySettings settings;
    private final Map<UUID, WorldEntities> worlds = new HashMap<>();
    private long tick;

    public VirtualEntityService(EntityTools tools, EntitySettings settings) {
        this.tools = tools;
        this.settings = settings;
    }

    public EntityResult spawn(UUID player, Point at) {
        Optional<UUID> world = tools.views().worldOf(player);
        if (world.isEmpty()) {
            return EntityResult.refused(EntityRefusal.NOT_VIRTUAL);
        }
        if (!inside(world.get(), at)) {
            return EntityResult.refused(EntityRefusal.OUTSIDE_VIRTUAL_CHUNK);
        }
        WorldEntities entities = worlds.computeIfAbsent(world.get(), id -> new WorldEntities());
        if (entities.size() >= settings.maxPerPlayer()) {
            return EntityResult.refused(EntityRefusal.TOO_MANY);
        }
        VirtualEntity entity = DebugEntityPattern.at(tools.ids().next(), at);
        entities.add(entity);
        viewersOf(world.get()).forEach(viewer -> tools.display().spawn(viewer, entity));
        tools.views().publish(world.get(), entities.ids());
        return EntityResult.done(entity);
    }

    public EntityResult move(UUID player, Point to) {
        Optional<UUID> world = tools.views().worldOf(player);
        Optional<VirtualEntity> newest = world.map(worlds::get).flatMap(WorldEntities::newestLiving);
        if (newest.isEmpty()) {
            return EntityResult.refused(EntityRefusal.NONE_TO_MOVE);
        }
        if (!inside(world.get(), to)) {
            return EntityResult.refused(EntityRefusal.OUTSIDE_VIRTUAL_CHUNK);
        }
        VirtualEntity moved = newest.get().movedTo(to);
        worlds.get(world.get()).replace(moved);
        viewersOf(world.get()).forEach(viewer -> tools.display().move(viewer, moved));
        return EntityResult.done(moved);
    }

    public int removeAll(UUID player) {
        Optional<UUID> world = tools.views().worldOf(player);
        WorldEntities entities = world.map(worlds::remove).orElse(null);
        if (entities == null) {
            return 0;
        }
        List<Integer> ids = entities.idsInOrder();
        if (!ids.isEmpty()) {
            viewersOf(world.get()).forEach(viewer -> tools.display().remove(viewer, ids));
        }
        tools.views().publish(world.get(), Set.of());
        return ids.size();
    }

    public void viewerEntered(UUID world) {
        WorldEntities entities = worlds.get(world);
        if (entities != null) {
            tools.views().publish(world, entities.ids());
        }
    }

    public void viewerLeft(UUID player, UUID world, boolean clientKeepsChunks) {
        WorldEntities entities = worlds.get(world);
        if (entities == null) {
            return;
        }
        entities.forgetHitsOf(player);
        List<Integer> ids = entities.idsInOrder();
        if (clientKeepsChunks && !ids.isEmpty()) {
            tools.display().remove(player, ids);
        }
    }

    public void forgetWorld(UUID world) {
        worlds.remove(world);
    }

    public void resend(UUID player, UUID world, ChunkKey chunk) {
        WorldEntities entities = worlds.get(world);
        if (entities != null) {
            entities.livingIn(chunk).forEach(entity -> tools.display().spawn(player, entity));
        }
    }

    public Optional<VirtualEntity> find(UUID player, int id) {
        return entitiesOf(player).flatMap(entities -> entities.living(id));
    }

    public HitOutcome attack(UUID player, int id) {
        Optional<UUID> world = tools.views().worldOf(player);
        WorldEntities entities = world.map(worlds::get).orElse(null);
        Optional<VirtualEntity> target = entities == null ? Optional.empty() : entities.living(id);
        if (target.isEmpty()) {
            return HitOutcome.GONE;
        }
        if (entities.coolingDown(player, id, tick, DebugEntityPattern.HIT_COOLDOWN_TICKS)) {
            return HitOutcome.TOO_SOON;
        }
        VirtualEntity hurt = target.get().hurt(DebugEntityPattern.DAMAGE_PER_HIT);
        entities.replace(hurt);
        entities.hit(player, id, tick);
        if (hurt.isDead()) {
            entities.died(id, tick);
            viewersOf(world.get()).forEach(viewer -> tools.display().die(viewer, hurt, player));
            return HitOutcome.KILLED;
        }
        viewersOf(world.get()).forEach(viewer -> tools.display().hurt(viewer, hurt, player));
        return HitOutcome.HURT;
    }

    public boolean use(UUID player, int id) {
        Optional<WorldEntities> entities = entitiesOf(player);
        if (entities.isEmpty() || entities.get().living(id).isEmpty()) {
            return false;
        }
        entities.get().used();
        return true;
    }

    public void tick() {
        tick++;
        long removable = tick - DebugEntityPattern.REMOVAL_AFTER_DEATH_TICKS;
        for (Map.Entry<UUID, WorldEntities> entry : worlds.entrySet()) {
            List<Integer> removed = entry.getValue().removeDeadSince(removable);
            if (!removed.isEmpty()) {
                viewersOf(entry.getKey()).forEach(viewer -> tools.display().removeDead(viewer, removed));
                tools.views().publish(entry.getKey(), entry.getValue().ids());
            }
        }
    }

    public EntityStatus status(UUID player) {
        Optional<WorldEntities> found = entitiesOf(player);
        if (found.isEmpty()) {
            return new EntityStatus(0, Optional.empty(), 0, 0);
        }
        WorldEntities entities = found.get();
        return new EntityStatus(entities.size(), entities.newest(), entities.hits(), entities.uses());
    }

    public int held() {
        return worlds.values().stream().mapToInt(WorldEntities::size).sum();
    }

    int worldsTracked() {
        return worlds.size();
    }

    private Optional<WorldEntities> entitiesOf(UUID player) {
        return tools.views().worldOf(player).map(worlds::get);
    }

    private Set<UUID> viewersOf(UUID world) {
        return tools.views().viewers(world);
    }

    private boolean inside(UUID world, Point at) {
        Optional<ChunkRange> area = tools.views().area(world);
        return area.isPresent() && ChunkKey.containing(at.x(), at.z()).filter(area.get()::contains).isPresent();
    }
}
