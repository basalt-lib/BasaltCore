package tlm.dev.basalt.nms.port;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;

public interface ChunkPackets {

    ClientboundLevelChunkWithLightPacket virtualChunk(Player player, ChunkKey chunk);

    Optional<ClientboundLevelChunkWithLightPacket> realChunk(Player player, ChunkKey chunk);
}
