package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class WorldEntities {

    private static final long NEVER = Long.MIN_VALUE;

    private final Map<Integer, Tracked> entities = new LinkedHashMap<>();
    private final Map<Hit, Long> lastHits = new HashMap<>();
    private int hits;
    private int uses;

    int size() {
        return entities.size();
    }

    Set<Integer> ids() {
        return Set.copyOf(entities.keySet());
    }

    List<Integer> idsInOrder() {
        return List.copyOf(entities.keySet());
    }

    void add(VirtualEntity entity) {
        entities.put(entity.id(), new Tracked(entity));
    }

    Optional<VirtualEntity> living(int id) {
        return Optional.ofNullable(entities.get(id)).map(Tracked::entity).filter(entity -> !entity.isDead());
    }

    Optional<VirtualEntity> newest() {
        VirtualEntity newest = null;
        for (Tracked tracked : entities.values()) {
            newest = tracked.entity();
        }
        return Optional.ofNullable(newest);
    }

    Optional<VirtualEntity> newestLiving() {
        VirtualEntity newest = null;
        for (Tracked tracked : entities.values()) {
            if (!tracked.entity().isDead()) {
                newest = tracked.entity();
            }
        }
        return Optional.ofNullable(newest);
    }

    List<VirtualEntity> livingIn(ChunkKey chunk) {
        return entities.values().stream().map(Tracked::entity)
                .filter(entity -> !entity.isDead())
                .filter(entity -> ChunkKey.containing(entity.position().x(), entity.position().z()).filter(chunk::equals).isPresent())
                .toList();
    }

    void replace(VirtualEntity entity) {
        entities.get(entity.id()).entity = entity;
    }

    boolean coolingDown(UUID player, int id, long now, int cooldown) {
        long last = lastHits.getOrDefault(new Hit(player, id), NEVER);
        return last != NEVER && now - last < cooldown;
    }

    void hit(UUID player, int id, long now) {
        lastHits.put(new Hit(player, id), now);
        hits++;
    }

    void forgetHitsOf(UUID player) {
        lastHits.keySet().removeIf(hit -> hit.player().equals(player));
    }

    void died(int id, long now) {
        entities.get(id).diedAt = now;
    }

    void used() {
        uses++;
    }

    List<Integer> removeDeadSince(long until) {
        List<Integer> removed = new ArrayList<>();
        entities.values().removeIf(tracked -> {
            boolean gone = tracked.diedAt != NEVER && tracked.diedAt <= until;
            if (gone) {
                removed.add(tracked.entity().id());
            }
            return gone;
        });
        lastHits.keySet().removeIf(hit -> removed.contains(hit.entity()));
        return removed;
    }

    int hits() {
        return hits;
    }

    int uses() {
        return uses;
    }

    private record Hit(UUID player, int entity) {
    }

    private static final class Tracked {

        private VirtualEntity entity;
        private long diedAt = NEVER;

        private Tracked(VirtualEntity entity) {
            this.entity = entity;
        }

        private VirtualEntity entity() {
            return entity;
        }
    }
}
