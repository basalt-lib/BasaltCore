package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class VirtualWorlds<C> {

    private final WorldTools<C> tools;
    private final WorldSettings settings;
    private final Supplier<UUID> ids;
    private final Map<UUID, WorldSnapshot<C>> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> viewing = new HashMap<>();

    public VirtualWorlds(WorldTools<C> tools, WorldSettings settings, Supplier<UUID> ids) {
        this.tools = tools;
        this.settings = settings;
        this.ids = ids;
    }

    public Creation create(HostArea host, HostRule rule, String owner) {
        if (worlds.size() >= settings.maxWorlds()) {
            return new Creation.Refused(CreationRefusal.TOO_MANY_WORLDS, Optional.empty());
        }
        if (host.chunks().chunkCount() > settings.maxChunks()) {
            return new Creation.Refused(CreationRefusal.TOO_MANY_CHUNKS, Optional.empty());
        }
        if (rule == HostRule.AIR_ONLY) {
            for (ChunkKey chunk : host.chunks().chunks()) {
                Optional<RealBlock> found = tools.real().firstBlock(host.world(), chunk);
                if (found.isPresent()) {
                    return new Creation.Refused(CreationRefusal.REAL_BLOCKS, found);
                }
            }
        }
        UUID id = ids.get();
        if (worlds.containsKey(id)) {
            throw new IllegalStateException("world id " + id + " is already taken");
        }
        worlds.put(id, WorldSnapshot.empty(new WorldLabel(id, owner, false), host));
        return new Creation.Created(id);
    }

    public Optional<WorldSnapshot<C>> snapshot(UUID world) {
        return Optional.ofNullable(worlds.get(world));
    }

    public List<UUID> open() {
        return List.copyOf(worlds.keySet());
    }

    public Optional<UUID> worldOf(UUID player) {
        return Optional.ofNullable(viewing.get(player));
    }

    public OptionalInt blockAt(UUID world, BlockPosition position) {
        WorldSnapshot<C> snapshot = worlds.get(world);
        if (snapshot == null || !snapshot.host().contains(position)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(snapshot.chunk(position.chunk())
                .map(chunk -> tools.chunks().stateAt(chunk, position))
                .orElse(tools.chunks().air()));
    }

    public OptionalInt blockSeenBy(UUID player, BlockPosition position) {
        return worldOf(player).map(world -> blockAt(world, position)).orElse(OptionalInt.empty());
    }

    public EditResult setBlock(UUID world, BlockPosition position, int state) {
        return edit(world, BlockBox.of(position), state, Optional.empty());
    }

    public EditResult setBlockBy(UUID player, BlockPosition position, int state) {
        return worldOf(player).map(world -> edit(world, BlockBox.of(position), state, Optional.of(player)))
                .orElse(EditResult.CLOSED);
    }

    public EditResult fill(UUID world, BlockBox box, int state) {
        return edit(world, box, state, Optional.empty());
    }

    public ViewerResult addViewer(UUID world, UUID player) {
        WorldSnapshot<C> snapshot = worlds.get(world);
        if (snapshot == null) {
            return ViewerResult.CLOSED;
        }
        if (viewing.containsKey(player)) {
            return ViewerResult.ALREADY_VIEWING;
        }
        viewing.put(player, world);
        worlds.put(world, snapshot.withViewer(player));
        if (!tools.viewers().enter(player, world, snapshot.host().chunks())) {
            forgetViewer(world, player);
            return ViewerResult.NO_VIEW;
        }
        return ViewerResult.ADDED;
    }

    public boolean removeViewer(UUID world, UUID player) {
        if (!world.equals(viewing.get(player))) {
            return false;
        }
        tools.viewers().exit(player, Departure.REMOVED);
        viewerLeft(world, player);
        return true;
    }

    public void viewerLeft(UUID world, UUID player) {
        if (!forgetViewer(world, player)) {
            return;
        }
        WorldSnapshot<C> snapshot = worlds.get(world);
        if (snapshot != null && snapshot.viewers().isEmpty()) {
            tools.listener().emptied(world);
        }
    }

    public boolean close(UUID world) {
        WorldSnapshot<C> snapshot = worlds.get(world);
        if (snapshot == null) {
            return false;
        }
        for (UUID viewer : snapshot.viewers()) {
            tools.viewers().exit(viewer, Departure.CLOSED);
            forgetViewer(world, viewer);
        }
        worlds.remove(world);
        tools.listener().closed(world);
        return true;
    }

    public boolean setOpenToEveryone(UUID world, boolean open) {
        WorldSnapshot<C> snapshot = worlds.get(world);
        if (snapshot == null) {
            return false;
        }
        if (snapshot.label().openToEveryone() != open) {
            worlds.put(world, snapshot.withLabel(snapshot.label().withOpenToEveryone(open)));
            tools.listener().policyChanged(world);
        }
        return true;
    }

    public int closeOwnedBy(String owner) {
        int closed = 0;
        for (UUID world : open()) {
            WorldSnapshot<C> snapshot = worlds.get(world);
            if (snapshot != null && snapshot.label().owner().equals(owner) && close(world)) {
                closed++;
            }
        }
        return closed;
    }

    public WorldSettings settings() {
        return settings;
    }

    public int closeAll() {
        int closed = 0;
        for (UUID world : open()) {
            if (close(world)) {
                closed++;
            }
        }
        return closed;
    }

    private boolean forgetViewer(UUID world, UUID player) {
        if (!viewing.remove(player, world)) {
            return false;
        }
        worlds.computeIfPresent(world, (id, snapshot) -> snapshot.withoutViewer(player));
        return true;
    }

    private EditResult edit(UUID world, BlockBox box, int state, Optional<UUID> actor) {
        WorldSnapshot<C> snapshot = worlds.get(world);
        if (snapshot == null) {
            return EditResult.CLOSED;
        }
        if (!snapshot.host().contains(box)) {
            return EditResult.OUTSIDE;
        }
        Map<ChunkKey, C> chunks = new HashMap<>(snapshot.chunks());
        List<ChunkKey> changed = new ArrayList<>();
        for (ChunkKey chunk : box.chunks().chunks()) {
            C before = snapshot.chunk(chunk).orElseGet(() -> tools.chunks().empty(chunk, snapshot.host()));
            C after = tools.chunks().fill(before, box.within(chunk).orElseThrow(), state);
            if (after != before) {
                chunks.put(chunk, after);
                changed.add(chunk);
            }
        }
        if (changed.isEmpty()) {
            return EditResult.UNCHANGED;
        }
        worlds.put(world, snapshot.withChunks(chunks));
        Set<UUID> audience = new HashSet<>(snapshot.viewers());
        actor.ifPresent(audience::remove);
        if (box.volume() == 1) {
            tools.viewers().blockChanged(Set.copyOf(audience), box.min(), state);
        } else {
            tools.viewers().chunksChanged(Set.copyOf(audience), List.copyOf(changed));
        }
        return EditResult.CHANGED;
    }
}
