package tlm.dev.basalt.nms.port;

import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;

public interface PlayerChannels {

    void attach(Player player, OutboundFilter filter);

    void detach(Player player);

    void writeDirect(Player player, Packet<?> packet);
}
