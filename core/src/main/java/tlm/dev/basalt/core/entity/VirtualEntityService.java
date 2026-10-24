package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class VirtualEntityService {

    public static final int HIT_COOLDOWN_TICKS = 10;
    public static final int REMOVAL_AFTER_DEATH_TICKS = 20;

    private final EntityTools tools;
    private final EntitySettings settings;
    private final Map<UUID, WorldEntities> worlds = new HashMap<>();
    private long tick;

    public VirtualEntityService(EntityTools tools, EntitySettings settings) {
        this.tools = tools;
        this.settings = settings;
    }

    public EntityResult spawn(UUID world, String type, Point at, Rotation rotation) {
        Optional<EntityRefusal> misplaced = misplaced(world, at);
        if (misplaced.isPresent()) {
            return EntityResult.refused(misplaced.get());
        }
        WorldEntities entities = worlds.computeIfAbsent(world, id -> new WorldEntities());
        if (entities.size() >= settings.maxPerWorld()) {
            return EntityResult.refused(EntityRefusal.TOO_MANY);
        }
        VirtualEntity entity = VirtualEntity.fresh(tools.ids().next(), type, at, rotation);
        entities.add(entity);
        tools.views().publish(world, entities.ids());
        return EntityResult.done(entity);
    }

    public List<VirtualEntity> entities(UUID world) {
        return Optional.ofNullable(worlds.get(world)).map(WorldEntities::all).orElse(List.of());
    }

    public Optional<VirtualEntity> entity(UUID world, int id) {
        return Optional.ofNullable(worlds.get(world)).flatMap(entities -> entities.tracked(id)).map(WorldEntities.Tracked::entity);
    }

    public EntityResult teleport(UUID world, int id, Point to, Rotation rotation) {
        Optional<WorldEntities.Tracked> tracked = tracked(world, id);
        if (tracked.isEmpty()) {
            return EntityResult.refused(EntityRefusal.GONE);
        }
        if (misplaced(world, to).isPresent()) {
            return EntityResult.refused(EntityRefusal.OUTSIDE_AREA);
        }
        VirtualEntity before = tracked.get().entity();
        VirtualEntity moved = before.movedTo(to, rotation);
        worlds.get(world).replace(moved);
        if (tracked.get().shown() && !moved.isDead()) {
            for (UUID viewer : tools.views().viewers(world)) {
                boolean holdsBefore = holds(viewer, before);
                boolean holdsAfter = holds(viewer, moved);
                if (holdsAfter && !holdsBefore) {
                    tools.display().spawn(viewer, moved);
                } else if (holdsAfter || holdsBefore) {
                    tools.display().move(viewer, moved);
                }
            }
        }
        return EntityResult.done(moved);
    }

    public EntityResult rename(UUID world, int id, Optional<String> name) {
        return update(world, id, false, entity -> entity.withLooks(entity.looks().withName(name)));
    }

    public EntityResult silence(UUID world, int id, boolean silent) {
        return update(world, id, false, entity -> entity.withLooks(entity.looks().withSilent(silent)));
    }

    public EntityResult limitHealth(UUID world, int id, double maxHealth) {
        return update(world, id, true, entity -> entity.withLife(entity.life().withMaxHealth(maxHealth)));
    }

    public EntityResult heal(UUID world, int id, double health) {
        Optional<WorldEntities.Tracked> tracked = tracked(world, id);
        if (tracked.isEmpty()) {
            return EntityResult.refused(EntityRefusal.GONE);
        }
        if (tracked.get().entity().isDead()) {
            return EntityResult.refused(EntityRefusal.DYING);
        }
        VirtualEntity changed = tracked.get().entity().withLife(tracked.get().entity().life().withHealth(health));
        if (changed.isDead()) {
            worlds.get(world).replace(changed);
            kill(world, changed, Optional.empty());
            return EntityResult.done(changed);
        }
        return update(world, id, true, entity -> changed);
    }

    public boolean remove(UUID world, int id) {
        WorldEntities entities = worlds.get(world);
        Optional<WorldEntities.Tracked> removed = entities == null ? Optional.empty() : entities.remove(id);
        if (removed.isEmpty()) {
            return false;
        }
        if (removed.get().shown()) {
            tools.views().viewers(world).forEach(viewer -> tools.display().remove(viewer, List.of(id)));
        }
        tools.views().publish(world, entities.ids());
        return true;
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
        List<Integer> ids = entities.shownIds();
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
            entities.shownLivingIn(chunk).forEach(entity -> tools.display().spawn(player, entity));
        }
    }

    public Optional<VirtualEntity> find(UUID player, int id) {
        return entitiesOf(player).flatMap(entities -> entities.living(id));
    }

    public boolean hitReady(UUID player, int id) {
        Optional<WorldEntities> entities = entitiesOf(player);
        return entities.isPresent() && entities.get().living(id).isPresent()
                && !entities.get().coolingDown(player, id, tick, HIT_COOLDOWN_TICKS);
    }

    public HitOutcome attack(UUID player, int id, double damage) {
        Optional<UUID> world = tools.views().worldOf(player);
        WorldEntities entities = world.map(worlds::get).orElse(null);
        Optional<VirtualEntity> target = entities == null ? Optional.empty() : entities.living(id);
        if (target.isEmpty()) {
            return HitOutcome.GONE;
        }
        if (entities.coolingDown(player, id, tick, HIT_COOLDOWN_TICKS)) {
            return HitOutcome.TOO_SOON;
        }
        VirtualEntity hurt = target.get().hurt(damage);
        entities.replace(hurt);
        entities.hit(player, id, tick);
        if (hurt.isDead()) {
            kill(world.get(), hurt, Optional.of(player));
            return HitOutcome.KILLED;
        }
        if (entities.tracked(id).filter(WorldEntities.Tracked::shown).isPresent()) {
            holders(world.get(), hurt).forEach(viewer -> tools.display().hurt(viewer, hurt, player));
        }
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
        for (Map.Entry<UUID, WorldEntities> entry : List.copyOf(worlds.entrySet())) {
            show(entry.getKey(), entry.getValue());
            removeDead(entry.getKey(), entry.getValue());
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

    public EntitySettings settings() {
        return settings;
    }

    static Optional<ChunkKey> chunkOf(VirtualEntity entity) {
        return ChunkKey.containing(entity.position().x(), entity.position().z());
    }

    private EntityResult update(UUID world, int id, boolean living, UnaryOperator<VirtualEntity> change) {
        Optional<WorldEntities.Tracked> tracked = tracked(world, id);
        if (tracked.isEmpty()) {
            return EntityResult.refused(EntityRefusal.GONE);
        }
        if (living && tracked.get().entity().isDead()) {
            return EntityResult.refused(EntityRefusal.DYING);
        }
        VirtualEntity changed = change.apply(tracked.get().entity());
        worlds.get(world).replace(changed);
        if (tracked.get().shown() && !changed.isDead()) {
            holders(world, changed).forEach(viewer -> tools.display().update(viewer, changed));
        }
        return EntityResult.done(changed);
    }

    private void kill(UUID world, VirtualEntity dead, Optional<UUID> killer) {
        worlds.get(world).died(dead.id(), tick);
        tools.events().died(world, dead, killer);
        Optional<WorldEntities.Tracked> still = tracked(world, dead.id());
        if (still.isPresent() && still.get().shown()) {
            VirtualEntity current = still.get().entity();
            holders(world, current).forEach(viewer -> tools.display().die(viewer, current, killer));
        }
    }

    private void show(UUID world, WorldEntities entities) {
        for (WorldEntities.Tracked tracked : entities.unshown()) {
            VirtualEntity entity = tracked.entity();
            if (!entity.isDead()) {
                tracked.markShown();
                holders(world, entity).forEach(viewer -> tools.display().spawn(viewer, entity));
            }
        }
    }

    private void removeDead(UUID world, WorldEntities entities) {
        List<WorldEntities.Tracked> removed = entities.removeDeadSince(tick - REMOVAL_AFTER_DEATH_TICKS);
        if (removed.isEmpty()) {
            return;
        }
        List<Integer> shown = removed.stream().filter(WorldEntities.Tracked::shown).map(tracked -> tracked.entity().id()).toList();
        if (!shown.isEmpty()) {
            tools.views().viewers(world).forEach(viewer -> tools.display().removeDead(viewer, shown));
        }
        tools.views().publish(world, entities.ids());
    }

    private Optional<WorldEntities.Tracked> tracked(UUID world, int id) {
        return Optional.ofNullable(worlds.get(world)).flatMap(entities -> entities.tracked(id));
    }

    private Optional<WorldEntities> entitiesOf(UUID player) {
        return tools.views().worldOf(player).map(worlds::get);
    }

    private List<UUID> holders(UUID world, VirtualEntity entity) {
        return tools.views().viewers(world).stream().filter(viewer -> holds(viewer, entity)).toList();
    }

    private boolean holds(UUID viewer, VirtualEntity entity) {
        return chunkOf(entity).filter(chunk -> tools.views().holdsChunk(viewer, chunk)).isPresent();
    }

    private Optional<EntityRefusal> misplaced(UUID world, Point at) {
        Optional<ChunkRange> area = tools.views().area(world);
        if (area.isEmpty()) {
            return Optional.of(EntityRefusal.NO_WORLD);
        }
        boolean inside = ChunkKey.containing(at.x(), at.z()).filter(area.get()::contains).isPresent();
        return inside ? Optional.empty() : Optional.of(EntityRefusal.OUTSIDE_AREA);
    }
}
