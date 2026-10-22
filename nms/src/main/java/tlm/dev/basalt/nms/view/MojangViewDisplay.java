package tlm.dev.basalt.nms.view;

import org.bukkit.Server;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.visibility.PlayerView;
import tlm.dev.basalt.core.visibility.ViewDisplay;
import tlm.dev.basalt.nms.port.VirtualCopy;

import java.util.OptionalInt;
import java.util.UUID;

public final class MojangViewDisplay implements ViewDisplay<VirtualCopy> {

    private final Server server;
    private final ViewTools tools;

    public MojangViewDisplay(Server server, ViewTools tools) {
        this.server = server;
        this.tools = tools;
    }

    @Override
    public void attach(UUID player, PlayerView<VirtualCopy> view) {
        Player online = server.getPlayer(player);
        if (online != null) {
            tools.channels().attach(online, tools.filters().forView(player, view));
        }
    }

    @Override
    public void detach(UUID player) {
        Player online = server.getPlayer(player);
        if (online != null) {
            tools.channels().detach(online);
        }
    }

    @Override
    public boolean hasChunk(UUID player, ChunkKey chunk) {
        Player online = server.getPlayer(player);
        return online != null && online.isChunkSent(chunk.packed());
    }

    @Override
    public VirtualCopy buildVirtual(UUID player, ChunkKey chunk, DebugPattern pattern) {
        return tools.packets().virtualChunk(online(player), chunk, pattern);
    }

    @Override
    public OptionalInt blockAt(VirtualCopy copy, BlockPosition position) {
        if (!copy.chunk().key().equals(position.chunk()) || !copy.chunk().holds(position.localX(), position.y(), position.localZ())) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(copy.chunk().stateAt(position.localX(), position.y(), position.localZ()));
    }

    @Override
    public VirtualCopy withBlock(UUID player, VirtualCopy copy, BlockPosition position, int state) {
        return tools.packets().withBlock(online(player), copy, position, state);
    }

    @Override
    public void sendVirtual(UUID player, VirtualCopy payload) {
        tools.channels().writeDirect(online(player), payload.packet());
    }

    @Override
    public void resendReal(UUID player, ChunkKey chunk) {
        Player online = online(player);
        tools.packets().realChunk(online, chunk).ifPresent(packet -> ((CraftPlayer) online).getHandle().connection.send(packet));
    }

    @Override
    public void restartFlyingCheck(UUID player) {
        Player online = server.getPlayer(player);
        if (online != null) {
            ((CraftPlayer) online).getHandle().connection.resetFlyingTicks();
        }
    }

    private Player online(UUID player) {
        Player online = server.getPlayer(player);
        if (online == null) {
            throw new IllegalStateException("player " + player + " is not online");
        }
        return online;
    }
}
