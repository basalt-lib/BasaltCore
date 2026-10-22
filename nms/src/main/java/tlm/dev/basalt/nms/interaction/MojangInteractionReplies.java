package tlm.dev.basalt.nms.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.Block;
import org.bukkit.Server;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.interaction.InteractionReplies;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.nms.port.PlayerChannels;

import java.util.UUID;

public final class MojangInteractionReplies implements InteractionReplies {

    private final Server server;
    private final PlayerChannels channels;

    public MojangInteractionReplies(Server server, PlayerChannels channels) {
        this.server = server;
        this.channels = channels;
    }

    @Override
    public void block(UUID player, BlockPosition position, int state) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, new ClientboundBlockUpdatePacket(
                    new BlockPos(position.x(), position.y(), position.z()), Block.stateById(state)));
        }
    }

    @Override
    public void resyncInventory(UUID player) {
        Player online = server.getPlayer(player);
        if (online != null) {
            ((CraftPlayer) online).getHandle().containerMenu.sendAllDataToRemote();
        }
    }

    @Override
    public void acknowledge(UUID player, int sequence) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, new ClientboundBlockChangedAckPacket(sequence));
        }
    }
}
