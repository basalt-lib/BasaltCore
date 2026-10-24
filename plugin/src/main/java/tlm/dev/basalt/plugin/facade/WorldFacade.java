package tlm.dev.basalt.plugin.facade;

import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.world.EditResult;
import tlm.dev.basalt.core.world.WorldSnapshot;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

final class WorldFacade implements VirtualWorld {

    private final UUID id;
    private final ChunkArea area;
    private final FacadeTools tools;

    WorldFacade(UUID id, ChunkArea area, FacadeTools tools) {
        this.id = id;
        this.area = area;
        this.tools = tools;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public ChunkArea area() {
        return area;
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
        return tools.blocks().dataOf(state.getAsInt());
    }

    @Override
    public void setBlock(int x, int y, int z, BlockData data) {
        open();
        Objects.requireNonNull(data, "data");
        edited(tools.worlds().setBlock(id, position(x, y, z), tools.blocks().stateOf(data)), x + " " + y + " " + z);
    }

    @Override
    public void fill(BoundingBox box, BlockData data) {
        open();
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(data, "data");
        BlockBox blocks = BlockBoxes.of(box);
        edited(tools.worlds().fill(id, blocks, tools.blocks().stateOf(data)), blocks.min() + " to " + blocks.max());
    }

    @Override
    public void addViewer(Player player) {
        open();
        Objects.requireNonNull(player, "player");
        if (!player.isOnline()) {
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
    public void close() {
        tools.checkThread();
        tools.worlds().close(id);
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
