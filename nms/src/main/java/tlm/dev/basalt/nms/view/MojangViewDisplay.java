package tlm.dev.basalt.nms.view;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.bukkit.Server;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.visibility.PlayerView;
import tlm.dev.basalt.core.visibility.ViewDisplay;

import java.util.UUID;

public final class MojangViewDisplay implements ViewDisplay<ClientboundLevelChunkWithLightPacket> {

    private final Server server;
    private final ViewTools tools;

    public MojangViewDisplay(Server server, ViewTools tools) {
        this.server = server;
        this.tools = tools;
    }

    @Override
    public void attach(UUID player, PlayerView<ClientboundLevelChunkWithLightPacket> view) {
        Player online = server.getPlayer(player);
        if (online != null) {
            tools.channels().attach(online, tools.filters().forView(view));
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
    public ClientboundLevelChunkWithLightPacket buildVirtual(UUID player, ChunkKey chunk, DebugPattern pattern) {
        return tools.packets().virtualChunk(online(player), chunk, pattern);
    }

    @Override
    public void sendVirtual(UUID player, ClientboundLevelChunkWithLightPacket payload) {
        tools.channels().writeDirect(online(player), payload);
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
