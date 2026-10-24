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

    List<VirtualEntity> all() {
        return entities.values().stream().map(Tracked::entity).toList();
    }

    List<Integer> shownIds() {
        return entities.values().stream().filter(Tracked::shown).map(tracked -> tracked.entity().id()).toList();
    }

    Optional<Tracked> tracked(int id) {
        return Optional.ofNullable(entities.get(id));
    }

    Optional<VirtualEntity> living(int id) {
        return tracked(id).map(Tracked::entity).filter(entity -> !entity.isDead());
    }

    Optional<VirtualEntity> newest() {
        VirtualEntity newest = null;
        for (Tracked tracked : entities.values()) {
            newest = tracked.entity();
        }
        return Optional.ofNullable(newest);
    }

    List<VirtualEntity> shownLivingIn(ChunkKey chunk) {
        return entities.values().stream()
                .filter(Tracked::shown)
                .map(Tracked::entity)
                .filter(entity -> !entity.isDead())
                .filter(entity -> VirtualEntityService.chunkOf(entity).filter(chunk::equals).isPresent())
                .toList();
    }

    List<Tracked> unshown() {
        return entities.values().stream().filter(tracked -> !tracked.shown()).toList();
    }

    void add(VirtualEntity entity) {
        entities.put(entity.id(), new Tracked(entity));
    }

    void replace(VirtualEntity entity) {
        entities.get(entity.id()).entity = entity;
    }

    Optional<Tracked> remove(int id) {
        Tracked removed = entities.remove(id);
        if (removed != null) {
            lastHits.keySet().removeIf(hit -> hit.entity() == id);
        }
        return Optional.ofNullable(removed);
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

    List<Tracked> removeDeadSince(long until) {
        List<Tracked> removed = new ArrayList<>();
        entities.values().removeIf(tracked -> {
            boolean gone = tracked.diedAt != NEVER && tracked.diedAt <= until;
            if (gone) {
                removed.add(tracked);
            }
            return gone;
        });
        removed.forEach(tracked -> lastHits.keySet().removeIf(hit -> hit.entity() == tracked.entity().id()));
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

    static final class Tracked {

        private VirtualEntity entity;
        private long diedAt = NEVER;
        private boolean shown;

        private Tracked(VirtualEntity entity) {
            this.entity = entity;
        }

        VirtualEntity entity() {
            return entity;
        }

        boolean shown() {
            return shown;
        }

        void markShown() {
            shown = true;
        }
    }
}
