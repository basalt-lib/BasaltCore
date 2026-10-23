package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkKey;
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
    private final Map<UUID, PlayerEntities> players = new HashMap<>();
    private long tick;

    public VirtualEntityService(EntityTools tools, EntitySettings settings) {
        this.tools = tools;
        this.settings = settings;
    }

    public EntityResult spawn(UUID player, Point at) {
        Optional<EntityRefusal> refusal = placementRefusal(player, at);
        if (refusal.isPresent()) {
            return EntityResult.refused(refusal.get());
        }
        PlayerEntities entities = players.computeIfAbsent(player, id -> new PlayerEntities());
        if (entities.size() >= settings.maxPerPlayer()) {
            return EntityResult.refused(EntityRefusal.TOO_MANY);
        }
        VirtualEntity entity = DebugEntityPattern.at(tools.ids().next(), at);
        entities.add(entity);
        tools.display().spawn(player, entity);
        tools.views().publish(player, entities.ids());
        return EntityResult.done(entity);
    }

    public EntityResult move(UUID player, Point to) {
        Optional<VirtualEntity> newest = Optional.ofNullable(players.get(player)).flatMap(PlayerEntities::newestLiving);
        if (newest.isEmpty()) {
            return EntityResult.refused(EntityRefusal.NONE_TO_MOVE);
        }
        Optional<EntityRefusal> refusal = placementRefusal(player, to);
        if (refusal.isPresent()) {
            return EntityResult.refused(refusal.get());
        }
        VirtualEntity moved = newest.get().movedTo(to);
        players.get(player).replace(moved);
        tools.display().move(player, moved);
        return EntityResult.done(moved);
    }

    public int removeAll(UUID player) {
        PlayerEntities entities = players.remove(player);
        if (entities == null) {
            return 0;
        }
        List<Integer> ids = entities.idsInOrder();
        if (!ids.isEmpty()) {
            tools.display().remove(player, ids);
        }
        tools.views().publish(player, Set.of());
        return ids.size();
    }

    public void forget(UUID player) {
        players.remove(player);
    }

    public void resend(UUID player) {
        PlayerEntities entities = players.get(player);
        if (entities != null) {
            entities.allLiving().forEach(entity -> tools.display().spawn(player, entity));
        }
    }

    public Optional<VirtualEntity> find(UUID player, int id) {
        return Optional.ofNullable(players.get(player)).flatMap(entities -> entities.living(id));
    }

    public HitOutcome attack(UUID player, int id) {
        PlayerEntities entities = players.get(player);
        Optional<VirtualEntity> target = entities == null ? Optional.empty() : entities.living(id);
        if (target.isEmpty()) {
            return HitOutcome.GONE;
        }
        if (entities.coolingDown(id, tick, DebugEntityPattern.HIT_COOLDOWN_TICKS)) {
            return HitOutcome.TOO_SOON;
        }
        VirtualEntity hurt = target.get().hurt(DebugEntityPattern.DAMAGE_PER_HIT);
        entities.replace(hurt);
        entities.hit(id, tick);
        if (hurt.isDead()) {
            entities.died(id, tick);
            tools.display().die(player, hurt);
            return HitOutcome.KILLED;
        }
        tools.display().hurt(player, hurt);
        return HitOutcome.HURT;
    }

    public boolean use(UUID player, int id) {
        PlayerEntities entities = players.get(player);
        if (entities == null || entities.living(id).isEmpty()) {
            return false;
        }
        entities.used();
        return true;
    }

    public void tick() {
        tick++;
        long removable = tick - DebugEntityPattern.REMOVAL_AFTER_DEATH_TICKS;
        for (Map.Entry<UUID, PlayerEntities> entry : players.entrySet()) {
            List<Integer> removed = entry.getValue().removeDeadSince(removable);
            if (!removed.isEmpty()) {
                tools.display().removeDead(entry.getKey(), removed);
                tools.views().publish(entry.getKey(), entry.getValue().ids());
            }
        }
    }

    public EntityStatus status(UUID player) {
        PlayerEntities entities = players.get(player);
        if (entities == null) {
            return new EntityStatus(0, Optional.empty(), 0, 0);
        }
        return new EntityStatus(entities.size(), entities.newest(), entities.hits(), entities.uses());
    }

    int playersTracked() {
        return players.size();
    }

    private Optional<EntityRefusal> placementRefusal(UUID player, Point at) {
        Optional<ChunkKey> chunk = tools.views().virtualChunk(player);
        if (chunk.isEmpty()) {
            return Optional.of(EntityRefusal.NOT_VIRTUAL);
        }
        if (!ChunkKey.containing(at.x(), at.z()).equals(chunk)) {
            return Optional.of(EntityRefusal.OUTSIDE_VIRTUAL_CHUNK);
        }
        return Optional.empty();
    }
}
