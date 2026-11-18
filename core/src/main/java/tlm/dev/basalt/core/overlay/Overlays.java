package tlm.dev.basalt.core.overlay;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class Overlays<L> {

    private final OverlayTools<L> tools;
    private final OverlaySettings settings;
    private final Map<UUID, Entry<L>> overlays = new LinkedHashMap<>();
    private final Map<UUID, UUID> viewing = new HashMap<>();

    public Overlays(OverlayTools<L> tools, OverlaySettings settings) {
        this.tools = tools;
        this.settings = settings;
    }

    public OverlaySettings settings() {
        return settings;
    }

    public Optional<UUID> create(String owner, UUID world, ChunkRange area, int minY, int maxY) {
        if (minY < OverlayBlockIndex.MIN_Y || maxY > OverlayBlockIndex.MAX_Y || minY > maxY) {
            throw new IllegalArgumentException("height " + minY + " to " + maxY + " outside " + OverlayBlockIndex.MIN_Y + ".." + OverlayBlockIndex.MAX_Y);
        }
        if (overlays.size() >= settings.maxOverlays()) {
            return Optional.empty();
        }
        UUID id = tools.ids().get();
        if (overlays.containsKey(id)) {
            throw new IllegalStateException("overlay id " + id + " is already taken");
        }
        Entry<L> entry = new Entry<>(id, owner, world, area, minY, maxY);
        entry.snapshot = entry.build();
        overlays.put(id, entry);
        return Optional.of(id);
    }

    public List<UUID> open() {
        return List.copyOf(overlays.keySet());
    }

    public boolean isOpen(UUID id) {
        return overlays.containsKey(id);
    }

    public Optional<OverlaySnapshot<L>> snapshot(UUID id) {
        Entry<L> entry = overlays.get(id);
        return entry == null ? Optional.empty() : Optional.of(entry.snapshot);
    }

    public Optional<UUID> overlayOf(UUID player) {
        return Optional.ofNullable(viewing.get(player));
    }

    public Set<UUID> viewers(UUID id) {
        Entry<L> entry = overlays.get(id);
        return entry == null ? Set.of() : Set.copyOf(entry.viewers);
    }

    public OptionalInt blockAt(UUID id, BlockPosition position) {
        Entry<L> entry = overlays.get(id);
        if (entry == null) {
            return OptionalInt.empty();
        }
        int state = entry.snapshot.stateAt(position.x(), position.y(), position.z());
        return state == OverlayBlockIndex.NONE ? OptionalInt.empty() : OptionalInt.of(state);
    }

    public OptionalInt blockSeenBy(UUID player, BlockPosition position) {
        UUID id = viewing.get(player);
        return id == null ? OptionalInt.empty() : blockAt(id, position);
    }

    public OverlayEdit setBlock(UUID id, BlockPosition position, int state) {
        if (state < 0) {
            throw new IllegalArgumentException("negative block state " + state);
        }
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlayEdit.CLOSED;
        }
        if (!entry.holds(position)) {
            return OverlayEdit.OUTSIDE;
        }
        long key = OverlayBlockIndex.key(position.x(), position.y(), position.z());
        Integer previous = entry.blocks.get(key);
        if (previous != null && previous == state) {
            return OverlayEdit.UNCHANGED;
        }
        if (previous == null && entry.blocks.size() >= settings.maxBlocks()) {
            return OverlayEdit.TOO_MANY;
        }
        entry.blocks.put(key, state);
        publish(entry);
        OverlayBlock block = new OverlayBlock(position, state);
        for (UUID viewer : entry.viewers) {
            if (tools.display().holdsChunk(viewer, position.chunk())) {
                tools.display().show(viewer, List.of(block));
            }
        }
        return OverlayEdit.CHANGED;
    }

    public OverlayEdit removeBlock(UUID id, BlockPosition position) {
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlayEdit.CLOSED;
        }
        if (!entry.holds(position)) {
            return OverlayEdit.OUTSIDE;
        }
        if (entry.blocks.remove(OverlayBlockIndex.key(position.x(), position.y(), position.z())) == null) {
            return OverlayEdit.UNCHANGED;
        }
        publish(entry);
        for (UUID viewer : entry.viewers) {
            if (tools.display().holdsChunk(viewer, position.chunk())) {
                tools.display().showReal(viewer, entry.world, List.of(position));
            }
        }
        return OverlayEdit.CHANGED;
    }

    public OverlayEdit removeBlockSeenBy(UUID player, BlockPosition position) {
        UUID id = viewing.get(player);
        return id == null ? OverlayEdit.NOT_FOUND : removeBlock(id, position);
    }

    public OverlayEdit blockAction(UUID id, BlockPosition position, int action, int parameter) {
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlayEdit.CLOSED;
        }
        OptionalInt state = blockAt(id, position);
        if (state.isEmpty()) {
            return OverlayEdit.NOT_FOUND;
        }
        OverlayBlock block = new OverlayBlock(position, state.getAsInt());
        for (UUID viewer : entry.viewers) {
            if (tools.display().holdsChunk(viewer, position.chunk())) {
                tools.display().blockAction(viewer, block, action, parameter);
            }
        }
        return OverlayEdit.CHANGED;
    }

    public OverlaySpawn<L> spawn(UUID id, String type, Point at, float yaw, float pitch, L look) {
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlaySpawn.refused(OverlayEdit.CLOSED);
        }
        if (!entry.holds(at)) {
            return OverlaySpawn.refused(OverlayEdit.OUTSIDE);
        }
        if (entry.entities.size() >= settings.maxEntities()) {
            return OverlaySpawn.refused(OverlayEdit.TOO_MANY);
        }
        OverlayEntity<L> entity = new OverlayEntity<>(tools.entityIds().getAsInt(), tools.ids().get(), type, at, yaw, pitch, look);
        entry.entities.put(entity.id(), entity);
        publish(entry);
        for (UUID viewer : entry.viewers) {
            if (holds(viewer, entity)) {
                tools.display().spawn(viewer, entity);
            }
        }
        return new OverlaySpawn<>(OverlayEdit.CHANGED, Optional.of(entity));
    }

    public List<OverlayEntity<L>> entities(UUID id) {
        Entry<L> entry = overlays.get(id);
        return entry == null ? List.of() : List.copyOf(entry.entities.values());
    }

    public Optional<OverlayEntity<L>> entity(UUID id, int entity) {
        Entry<L> entry = overlays.get(id);
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.entities.get(entity));
    }

    public Optional<OverlayEntity<L>> entitySeenBy(UUID player, int entity) {
        UUID id = viewing.get(player);
        return id == null ? Optional.empty() : entity(id, entity);
    }

    public OverlayEdit moveEntity(UUID id, int entity, Point to, float yaw, float pitch) {
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlayEdit.CLOSED;
        }
        OverlayEntity<L> current = entry.entities.get(entity);
        if (current == null) {
            return OverlayEdit.NOT_FOUND;
        }
        if (!entry.holds(to)) {
            return OverlayEdit.OUTSIDE;
        }
        OverlayEntity<L> moved = current.movedTo(to, yaw, pitch);
        entry.entities.put(entity, moved);
        publish(entry);
        for (UUID viewer : entry.viewers) {
            boolean heldBefore = holds(viewer, current);
            if (holds(viewer, moved)) {
                if (heldBefore) {
                    tools.display().move(viewer, moved);
                } else {
                    tools.display().spawn(viewer, moved);
                }
            } else if (heldBefore) {
                tools.display().remove(viewer, List.of(entity));
            }
        }
        return OverlayEdit.CHANGED;
    }

    public OverlayEdit changeLook(UUID id, int entity, L look) {
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlayEdit.CLOSED;
        }
        OverlayEntity<L> current = entry.entities.get(entity);
        if (current == null) {
            return OverlayEdit.NOT_FOUND;
        }
        OverlayEntity<L> changed = current.withLook(look);
        entry.entities.put(entity, changed);
        publish(entry);
        for (UUID viewer : entry.viewers) {
            if (holds(viewer, changed)) {
                tools.display().update(viewer, changed);
            }
        }
        return OverlayEdit.CHANGED;
    }

    public OverlayEdit removeEntity(UUID id, int entity) {
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlayEdit.CLOSED;
        }
        if (entry.entities.remove(entity) == null) {
            return OverlayEdit.NOT_FOUND;
        }
        publish(entry);
        for (UUID viewer : entry.viewers) {
            tools.display().remove(viewer, List.of(entity));
        }
        return OverlayEdit.CHANGED;
    }

    public OverlayJoin addViewer(UUID id, UUID player) {
        Entry<L> entry = editable(id);
        if (entry == null) {
            return OverlayJoin.CLOSED;
        }
        if (viewing.containsKey(player)) {
            return OverlayJoin.ALREADY_VIEWING;
        }
        viewing.put(player, id);
        entry.viewers.add(player);
        tools.views().open(player).publish(entry.snapshot);
        showEverything(player, entry.snapshot);
        return OverlayJoin.ADDED;
    }

    public boolean removeViewer(UUID player, OverlayExit cause) {
        UUID id = viewing.remove(player);
        if (id == null) {
            if (cause == OverlayExit.DISCONNECT) {
                tools.views().close(player);
            }
            return false;
        }
        Entry<L> entry = overlays.get(id);
        entry.viewers.remove(player);
        tools.views().open(player).publish(OverlaySnapshot.none());
        if (cause.restoresView()) {
            hideEverything(player, entry);
        }
        if (cause == OverlayExit.DISCONNECT) {
            tools.views().close(player);
        }
        return true;
    }

    public boolean close(UUID id, OverlayExit cause) {
        Entry<L> entry = overlays.get(id);
        if (entry == null || entry.closing) {
            return false;
        }
        entry.closing = true;
        for (UUID viewer : List.copyOf(entry.viewers)) {
            removeViewer(viewer, cause);
        }
        overlays.remove(id);
        return true;
    }

    public int closeOwnedBy(String owner, OverlayExit cause) {
        int closed = 0;
        for (Entry<L> entry : List.copyOf(overlays.values())) {
            if (entry.owner.equals(owner) && close(entry.id, cause)) {
                closed++;
            }
        }
        return closed;
    }

    public int closeAll(OverlayExit cause) {
        int closed = 0;
        for (UUID id : List.copyOf(overlays.keySet())) {
            if (close(id, cause)) {
                closed++;
            }
        }
        return closed;
    }

    public void forget(UUID player) {
        removeViewer(player, OverlayExit.DISCONNECT);
    }

    private void showEverything(UUID player, OverlaySnapshot<L> snapshot) {
        Map<ChunkKey, Boolean> held = new HashMap<>();
        List<OverlayBlock> blocks = new ArrayList<>();
        OverlayBlockIndex index = snapshot.blocks();
        for (int at = 0; at < index.size(); at++) {
            BlockPosition position = new BlockPosition(index.x(at), index.y(at), index.z(at));
            if (held.computeIfAbsent(position.chunk(), chunk -> tools.display().holdsChunk(player, chunk))) {
                blocks.add(new OverlayBlock(position, index.state(at)));
            }
        }
        if (!blocks.isEmpty()) {
            tools.display().show(player, blocks);
        }
        for (OverlayEntity<L> entity : snapshot.entities()) {
            if (holds(player, entity)) {
                tools.display().spawn(player, entity);
            }
        }
    }

    private void hideEverything(UUID player, Entry<L> entry) {
        Map<ChunkKey, Boolean> held = new HashMap<>();
        List<BlockPosition> positions = new ArrayList<>();
        OverlayBlockIndex index = entry.snapshot.blocks();
        for (int at = 0; at < index.size(); at++) {
            BlockPosition position = new BlockPosition(index.x(at), index.y(at), index.z(at));
            if (held.computeIfAbsent(position.chunk(), chunk -> tools.display().holdsChunk(player, chunk))) {
                positions.add(position);
            }
        }
        if (!positions.isEmpty()) {
            tools.display().showReal(player, entry.world, positions);
        }
        if (!entry.entities.isEmpty()) {
            tools.display().remove(player, List.copyOf(entry.entities.keySet()));
        }
    }

    private boolean holds(UUID player, OverlayEntity<L> entity) {
        return tools.display().holdsChunk(player, new ChunkKey(entity.chunkX(), entity.chunkZ()));
    }

    private Entry<L> editable(UUID id) {
        Entry<L> entry = overlays.get(id);
        return entry == null || entry.closing ? null : entry;
    }

    private void publish(Entry<L> entry) {
        entry.snapshot = entry.build();
        for (UUID viewer : entry.viewers) {
            tools.views().open(viewer).publish(entry.snapshot);
        }
    }

    private static final class Entry<L> {

        private final UUID id;
        private final String owner;
        private final UUID world;
        private final ChunkRange area;
        private final int minY;
        private final int maxY;
        private final TreeMap<Long, Integer> blocks = new TreeMap<>();
        private final Map<Integer, OverlayEntity<L>> entities = new LinkedHashMap<>();
        private final Set<UUID> viewers = new LinkedHashSet<>();
        private OverlaySnapshot<L> snapshot;
        private boolean closing;

        private Entry(UUID id, String owner, UUID world, ChunkRange area, int minY, int maxY) {
            this.id = id;
            this.owner = owner;
            this.world = world;
            this.area = area;
            this.minY = minY;
            this.maxY = maxY;
        }

        private boolean holds(BlockPosition position) {
            return area.containsBlock(position.x(), position.z()) && position.y() >= minY && position.y() <= maxY;
        }

        private boolean holds(Point at) {
            return at.isFinite() && area.containsBlock((int) Math.floor(at.x()), (int) Math.floor(at.z()))
                    && at.y() >= minY && at.y() < maxY + 1;
        }

        private OverlaySnapshot<L> build() {
            return new OverlaySnapshot<>(id, owner, world, area, OverlayBlockIndex.of(blocks), List.copyOf(entities.values()));
        }
    }
}
