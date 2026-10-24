package tlm.dev.basalt.nms.view;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.bukkit.Server;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.visibility.ChunkSend;
import tlm.dev.basalt.core.visibility.PlayerView;
import tlm.dev.basalt.core.visibility.ViewDisplay;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MojangViewDisplay implements ViewDisplay {

    private final Server server;
    private final ViewTools tools;

    public MojangViewDisplay(Server server, ViewTools tools) {
        this.server = server;
        this.tools = tools;
    }

    @Override
    public void attach(UUID player, PlayerView view) {
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
    public List<ChunkKey> chunksHeld(UUID player, ChunkRange area) {
        Player online = server.getPlayer(player);
        if (online == null) {
            return List.of();
        }
        return area.chunks().stream().filter(chunk -> online.isChunkSent(chunk.packed())).toList();
    }

    @Override
    public ChunkSend sendVirtual(UUID player, UUID world, ChunkKey chunk) {
        Player online = server.getPlayer(player);
        if (online == null || !online.isChunkSent(chunk.packed())) {
            return ChunkSend.GONE;
        }
        Optional<CompletableFuture<ClientboundLevelChunkWithLightPacket>> packet = tools.cache().packet(world, chunk);
        if (packet.isEmpty() || packet.get().isCompletedExceptionally()) {
            return ChunkSend.GONE;
        }
        if (!packet.get().isDone()) {
            return ChunkSend.PENDING;
        }
        tools.channels().writeDirect(online, packet.get().join());
        return ChunkSend.SENT;
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

    @Override
    public void release(UUID world) {
        tools.cache().release(world);
    }

    private Player online(UUID player) {
        Player online = server.getPlayer(player);
        if (online == null) {
            throw new IllegalStateException("player " + player + " is not online");
        }
        return online;
    }
}
