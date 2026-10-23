package tlm.dev.basalt.core.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class PlayerEntities {

    private static final long NEVER = Long.MIN_VALUE;

    private final Map<Integer, Tracked> entities = new LinkedHashMap<>();
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

    List<VirtualEntity> allLiving() {
        return entities.values().stream().map(Tracked::entity).filter(entity -> !entity.isDead()).toList();
    }

    void replace(VirtualEntity entity) {
        entities.get(entity.id()).entity = entity;
    }

    boolean coolingDown(int id, long now, int cooldown) {
        long last = entities.get(id).lastHit;
        return last != NEVER && now - last < cooldown;
    }

    void hit(int id, long now) {
        entities.get(id).lastHit = now;
        hits++;
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
        return removed;
    }

    int hits() {
        return hits;
    }

    int uses() {
        return uses;
    }

    private static final class Tracked {

        private VirtualEntity entity;
        private long lastHit = NEVER;
        private long diedAt = NEVER;

        private Tracked(VirtualEntity entity) {
            this.entity = entity;
        }

        private VirtualEntity entity() {
            return entity;
        }
    }
}
