package tlm.dev.basalt.plugin.facade;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.Overlay;
import tlm.dev.basalt.api.OverlayEntity;
import tlm.dev.basalt.core.overlay.OverlayEdit;
import tlm.dev.basalt.core.overlay.OverlayExit;
import tlm.dev.basalt.core.overlay.OverlaySpawn;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;
import tlm.dev.basalt.nms.platform.MojangLook;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

final class OverlayFacade implements Overlay {

    private static final int BYTE_LIMIT = 255;

    private final UUID id;
    private final ChunkArea area;
    private final Plugin owner;
    private final FacadeTools tools;

    OverlayFacade(UUID id, ChunkArea area, Plugin owner, FacadeTools tools) {
        this.id = id;
        this.area = area;
        this.owner = owner;
        this.tools = tools;
    }

    @Override
    public UUID id() {
        tools.checkThread();
        return id;
    }

    @Override
    public ChunkArea area() {
        tools.checkThread();
        return area;
    }

    @Override
    public Plugin owner() {
        tools.checkThread();
        return owner;
    }

    @Override
    public boolean isClosed() {
        tools.checkThread();
        return !tools.overlays().isOpen(id);
    }

    @Override
    public BlockData getBlock(int x, int y, int z) {
        open();
        OptionalInt state = tools.overlays().blockAt(id, position(x, y, z));
        return state.isEmpty() ? null : tools.content().blocks().dataOf(state.getAsInt());
    }

    @Override
    public void setBlock(int x, int y, int z, BlockData data) {
        open();
        Objects.requireNonNull(data, "data");
        edited(tools.overlays().setBlock(id, position(x, y, z), tools.content().blocks().stateOf(data)), x + " " + y + " " + z);
    }

    @Override
    public void removeBlock(int x, int y, int z) {
        open();
        edited(tools.overlays().removeBlock(id, position(x, y, z)), x + " " + y + " " + z);
    }

    @Override
    public int blockCount() {
        open();
        return tools.overlays().snapshot(id).map(snapshot -> snapshot.blocks().size()).orElse(0);
    }

    @Override
    public void playBlockAction(int x, int y, int z, int action, int parameter) {
        open();
        if (action < 0 || action > BYTE_LIMIT || parameter < 0 || parameter > BYTE_LIMIT) {
            throw new IllegalArgumentException("a block action and its parameter are from 0 to " + BYTE_LIMIT + ": " + action + ", " + parameter);
        }
        if (tools.overlays().blockAction(id, position(x, y, z), action, parameter) == OverlayEdit.NOT_FOUND) {
            throw new IllegalArgumentException("no block of the overlay " + id + " stands at " + x + " " + y + " " + z);
        }
    }

    @Override
    public OverlayEntity spawnEntity(EntityType type, Location location) {
        open();
        Objects.requireNonNull(type, "type");
        if (type == EntityType.UNKNOWN || !tools.content().entities().creatable(type.getKey().toString())) {
            throw new IllegalArgumentException(type + " cannot be summoned");
        }
        Point at = pointOf(location);
        String key = type.getKey().toString();
        MojangLook look = tools.content().looks().look(area.world(), key, Optional.empty(), Optional.empty())
                .orElseThrow(() -> new IllegalArgumentException(type + " cannot be summoned"));
        OverlaySpawn<MojangLook> spawn = tools.overlays().spawn(id, key, at, location.getYaw(), location.getPitch(), look);
        return switch (spawn.result()) {
            case CHANGED -> new OverlayEntityFacade(this, spawn.entity().orElseThrow(), tools);
            case TOO_MANY -> throw new IllegalStateException("an overlay holds at most " + tools.overlays().settings().maxEntities()
                    + " entities (overlay.max-entities)");
            case OUTSIDE -> throw outside(at.x() + " " + at.y() + " " + at.z());
            case UNCHANGED, NOT_FOUND, CLOSED -> throw closed();
        };
    }

    @Override
    public List<OverlayEntity> entities() {
        tools.checkThread();
        return tools.overlays().entities(id).stream().<OverlayEntity>map(entity -> new OverlayEntityFacade(this, entity, tools)).toList();
    }

    @Override
    public void addViewer(Player player) {
        open();
        Objects.requireNonNull(player, "player");
        if (!player.isOnline() || !player.isConnected()) {
            throw new IllegalStateException(player.getName() + " is not online");
        }
        if (!player.getWorld().getUID().equals(area.world().getUID())) {
            throw new IllegalStateException(player.getName() + " is in " + player.getWorld().getName() + ", not in " + area.world().getName());
        }
        if (tools.worlds().worldOf(player.getUniqueId()).isPresent()) {
            throw new IllegalStateException(player.getName() + " already views a virtual world");
        }
        switch (tools.overlays().addViewer(id, player.getUniqueId())) {
            case ADDED -> {
            }
            case ALREADY_VIEWING -> throw new IllegalStateException(player.getName() + " already views an overlay");
            case CLOSED -> throw closed();
        }
    }

    @Override
    public void removeViewer(Player player) {
        tools.checkThread();
        Objects.requireNonNull(player, "player");
        if (hasViewer(player)) {
            tools.overlays().removeViewer(player.getUniqueId(), OverlayExit.REQUESTED);
        }
    }

    @Override
    public boolean hasViewer(Player player) {
        tools.checkThread();
        Objects.requireNonNull(player, "player");
        return tools.overlays().overlayOf(player.getUniqueId()).filter(id::equals).isPresent();
    }

    @Override
    public Set<Player> viewers() {
        tools.checkThread();
        Set<Player> viewers = new HashSet<>();
        for (UUID viewer : tools.overlays().viewers(id)) {
            Player online = tools.server().getPlayer(viewer);
            if (online != null) {
                viewers.add(online);
            }
        }
        return Set.copyOf(viewers);
    }

    @Override
    public void close() {
        tools.checkThread();
        tools.overlays().close(id, OverlayExit.CLOSED);
    }

    Point pointOf(Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null || !location.getWorld().getUID().equals(area.world().getUID())) {
            throw new IllegalArgumentException("the location is not in " + area.world().getName());
        }
        Point at = new Point(location.getX(), location.getY(), location.getZ());
        if (!at.isFinite() || !Float.isFinite(location.getYaw()) || !Float.isFinite(location.getPitch())) {
            throw new IllegalArgumentException("the location " + at.x() + " " + at.y() + " " + at.z() + " is not finite");
        }
        return at;
    }

    void edited(OverlayEdit result, String where) {
        switch (result) {
            case CHANGED, UNCHANGED, NOT_FOUND -> {
            }
            case OUTSIDE -> throw outside(where);
            case TOO_MANY -> throw new IllegalStateException("an overlay holds at most " + tools.overlays().settings().maxBlocks()
                    + " blocks (overlay.max-blocks)");
            case CLOSED -> throw closed();
        }
    }

    IllegalArgumentException outside(String where) {
        return new IllegalArgumentException(where + " is outside the area or the height of the overlay " + id);
    }

    IllegalStateException closed() {
        return new IllegalStateException("the overlay " + id + " is closed");
    }

    private void open() {
        tools.checkThread();
        if (!tools.overlays().isOpen(id)) {
            throw closed();
        }
    }

    private static BlockPosition position(int x, int y, int z) {
        try {
            return new BlockPosition(x, y, z);
        } catch (IllegalArgumentException outsideTheWorld) {
            throw new IllegalArgumentException(x + " " + y + " " + z + " is outside the world", outsideTheWorld);
        }
    }
}
