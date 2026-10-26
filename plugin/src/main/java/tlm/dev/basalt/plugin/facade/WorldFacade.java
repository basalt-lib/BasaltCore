package tlm.dev.basalt.plugin.facade;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.Blueprint;
import tlm.dev.basalt.api.PlayerVisibility;
import tlm.dev.basalt.api.VirtualEntity;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.blueprint.BlockPlacer;
import tlm.dev.basalt.core.blueprint.BlockPlan;
import tlm.dev.basalt.core.entity.EntityResult;
import tlm.dev.basalt.core.entity.Rotation;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;
import tlm.dev.basalt.core.world.EditResult;
import tlm.dev.basalt.core.world.HostArea;
import tlm.dev.basalt.core.world.WorldSnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

final class WorldFacade implements VirtualWorld {

    private final UUID id;
    private final ChunkArea area;
    private final Plugin owner;
    private final FacadeTools tools;

    WorldFacade(UUID id, ChunkArea area, Plugin owner, FacadeTools tools) {
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
        return tools.worlds().snapshot(id).isEmpty();
    }

    @Override
    public BlockData getBlock(int x, int y, int z) {
        open();
        OptionalInt state = tools.worlds().blockAt(id, position(x, y, z));
        if (state.isEmpty()) {
            throw outside(x + " " + y + " " + z);
        }
        return tools.content().blocks().dataOf(state.getAsInt());
    }

    @Override
    public void setBlock(int x, int y, int z, BlockData data) {
        open();
        Objects.requireNonNull(data, "data");
        edited(tools.worlds().setBlock(id, position(x, y, z), tools.content().blocks().stateOf(data)), x + " " + y + " " + z);
    }

    @Override
    public void fill(BoundingBox box, BlockData data) {
        open();
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(data, "data");
        BlockBox blocks = BlockBoxes.of(box);
        edited(tools.worlds().fill(id, blocks, tools.content().blocks().stateOf(data)), blocks.min() + " to " + blocks.max());
    }

    @Override
    public void addViewer(Player player) {
        open();
        Objects.requireNonNull(player, "player");
        if (!player.isOnline() || !player.isConnected()) {
            throw new IllegalStateException(player.getName() + " is not online");
        }
        if (!player.getWorld().getUID().equals(area.world().getUID())) {
            throw new IllegalStateException(player.getName() + " is in " + player.getWorld().getName()
                    + ", not in " + area.world().getName());
        }
        switch (tools.worlds().addViewer(id, player.getUniqueId())) {
            case ADDED -> {
            }
            case CLOSED -> throw closed();
            case ALREADY_VIEWING -> throw new IllegalStateException(player.getName() + " already views a virtual world");
            case NO_VIEW -> throw new IllegalStateException(player.getName() + " has no view yet, try on the next tick");
        }
    }

    @Override
    public void removeViewer(Player player) {
        tools.checkThread();
        Objects.requireNonNull(player, "player");
        tools.worlds().removeViewer(id, player.getUniqueId());
    }

    @Override
    public boolean hasViewer(Player player) {
        tools.checkThread();
        Objects.requireNonNull(player, "player");
        return tools.worlds().worldOf(player.getUniqueId()).filter(id::equals).isPresent();
    }

    @Override
    public Set<Player> viewers() {
        tools.checkThread();
        Optional<? extends WorldSnapshot<?>> snapshot = tools.worlds().snapshot(id);
        if (snapshot.isEmpty()) {
            return Set.of();
        }
        Set<Player> viewers = new HashSet<>();
        for (UUID viewer : snapshot.get().viewers()) {
            Player online = tools.server().getPlayer(viewer);
            if (online != null) {
                viewers.add(online);
            }
        }
        return Set.copyOf(viewers);
    }

    @Override
    public void place(Blueprint blueprint, int x, int y, int z) {
        open();
        Objects.requireNonNull(blueprint, "blueprint");
        if (!(blueprint instanceof BlueprintFacade facade)) {
            throw new IllegalArgumentException("the blueprint does not come from Basalt");
        }
        BlockPlan plan = facade.plan();
        BlockBox volume = new BlockBox(position(x, y, z), position(x + plan.size().sizeX() - 1, y + plan.size().sizeY() - 1, z + plan.size().sizeZ() - 1));
        HostArea host = tools.worlds().snapshot(id).orElseThrow(this::closed).host();
        if (!host.contains(volume)) {
            throw outside(volume.min() + " to " + volume.max());
        }
        edited(tools.worlds().write(id, BlockPlacer.writes(plan, volume.min(), host.minY(), host.height() / 16)), volume.min() + " to " + volume.max());
    }

    @Override
    public VirtualEntity spawnEntity(EntityType type, Location location) {
        open();
        Objects.requireNonNull(type, "type");
        if (type == EntityType.UNKNOWN || !tools.content().entities().creatable(type.getKey().toString())) {
            throw new IllegalArgumentException(type + " cannot be summoned");
        }
        Point at = pointOf(location);
        EntityResult result = tools.entities().spawn(id, type.getKey().toString(), at, new Rotation(location.getYaw(), location.getPitch()));
        result.refusal().ifPresent(refusal -> {
            throw switch (refusal) {
                case TOO_MANY -> new IllegalStateException("a virtual world holds at most "
                        + tools.entities().settings().maxPerWorld() + " entities (entity.max-per-world)");
                case OUTSIDE_AREA -> outside(at.x() + " " + at.y() + " " + at.z());
                case NO_WORLD, GONE, DYING -> closed();
            };
        });
        return new EntityFacade(this, result.entity().orElseThrow(), tools);
    }

    @Override
    public List<VirtualEntity> entities() {
        tools.checkThread();
        return tools.entities().entities(id).stream().<VirtualEntity>map(entity -> new EntityFacade(this, entity, tools)).toList();
    }

    @Override
    public PlayerVisibility playerVisibility() {
        open();
        boolean everyone = tools.worlds().snapshot(id).map(snapshot -> snapshot.label().openToEveryone()).orElse(false);
        return everyone ? PlayerVisibility.EVERYONE : PlayerVisibility.WORLD_ONLY;
    }

    @Override
    public void setPlayerVisibility(PlayerVisibility visibility) {
        open();
        Objects.requireNonNull(visibility, "visibility");
        tools.worlds().setOpenToEveryone(id, visibility == PlayerVisibility.EVERYONE);
    }

    @Override
    public void close() {
        tools.checkThread();
        tools.worlds().close(id);
    }

    Point pointOf(Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null || !location.getWorld().getUID().equals(area.world().getUID())) {
            throw new IllegalArgumentException("the location is not in " + area.world().getName());
        }
        Point at = new Point(location.getX(), location.getY(), location.getZ());
        if (!at.isFinite() || at.y() < area.world().getMinHeight() || at.y() >= area.world().getMaxHeight()) {
            throw outside(at.x() + " " + at.y() + " " + at.z());
        }
        return at;
    }

    private void open() {
        tools.checkThread();
        if (tools.worlds().snapshot(id).isEmpty()) {
            throw closed();
        }
    }

    private void edited(EditResult result, String where) {
        switch (result) {
            case CHANGED, UNCHANGED -> {
            }
            case OUTSIDE -> throw outside(where);
            case CLOSED -> throw closed();
        }
    }

    private IllegalArgumentException outside(String where) {
        return new IllegalArgumentException(where + " is outside the area or the height of the virtual world " + id);
    }

    private IllegalStateException closed() {
        return new IllegalStateException("the virtual world " + id + " is closed");
    }

    private static BlockPosition position(int x, int y, int z) {
        try {
            return new BlockPosition(x, y, z);
        } catch (IllegalArgumentException outsideTheWorld) {
            throw new IllegalArgumentException(x + " " + y + " " + z + " is outside the world", outsideTheWorld);
        }
    }
}
