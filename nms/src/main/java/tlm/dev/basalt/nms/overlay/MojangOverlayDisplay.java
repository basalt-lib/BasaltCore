package tlm.dev.basalt.nms.overlay;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.overlay.OverlayBlock;
import tlm.dev.basalt.core.overlay.OverlayDisplay;
import tlm.dev.basalt.core.overlay.OverlayEntity;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.nms.platform.MojangLook;
import tlm.dev.basalt.nms.port.PlayerChannels;

import java.util.List;
import java.util.UUID;

public final class MojangOverlayDisplay implements OverlayDisplay<MojangLook> {

    private final Server server;
    private final PlayerChannels channels;

    public MojangOverlayDisplay(Server server, PlayerChannels channels) {
        this.server = server;
        this.channels = channels;
    }

    @Override
    public boolean holdsChunk(UUID player, ChunkKey chunk) {
        Player online = server.getPlayer(player);
        return online != null && online.isChunkSent(chunk.packed());
    }

    @Override
    public void show(UUID player, List<OverlayBlock> blocks) {
        Player online = server.getPlayer(player);
        if (online == null) {
            return;
        }
        for (OverlayBlock block : blocks) {
            BlockPosition at = block.position();
            channels.writeDirect(online, OverlayPackets.block(at.x(), at.y(), at.z(), block.state()));
        }
    }

    @Override
    public void showReal(UUID player, UUID world, List<BlockPosition> positions) {
        Player online = server.getPlayer(player);
        World real = server.getWorld(world);
        if (online == null || real == null) {
            return;
        }
        ServerLevel level = ((CraftWorld) real).getHandle();
        for (BlockPosition position : positions) {
            LevelChunk chunk = level.getChunkIfLoaded(position.x() >> 4, position.z() >> 4);
            if (chunk == null) {
                continue;
            }
            BlockPos at = new BlockPos(position.x(), position.y(), position.z());
            channels.writeDirect(online, new ClientboundBlockUpdatePacket(chunk, at));
            BlockEntity blockEntity = chunk.getBlockEntity(at);
            Packet<?> data = blockEntity == null ? null : blockEntity.getUpdatePacket();
            if (data != null) {
                channels.writeDirect(online, data);
            }
        }
    }

    @Override
    public void blockAction(UUID player, OverlayBlock block, int action, int parameter) {
        Player online = server.getPlayer(player);
        if (online != null) {
            BlockPosition at = block.position();
            channels.writeDirect(online, new ClientboundBlockEventPacket(new BlockPos(at.x(), at.y(), at.z()),
                    Block.stateById(block.state()).getBlock(), action, parameter));
        }
    }

    @Override
    public void spawn(UUID player, OverlayEntity<MojangLook> entity) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, OverlayPackets.add(entity));
            channels.writeDirect(online, OverlayPackets.data(entity));
        }
    }

    @Override
    public void move(UUID player, OverlayEntity<MojangLook> entity) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, OverlayPackets.move(entity));
        }
    }

    @Override
    public void update(UUID player, OverlayEntity<MojangLook> entity) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, OverlayPackets.data(entity));
        }
    }

    @Override
    public void remove(UUID player, List<Integer> entities) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, new ClientboundRemoveEntitiesPacket(entities.stream().mapToInt(Integer::intValue).toArray()));
        }
    }
}
