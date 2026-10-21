package tlm.dev.basalt.nms.port;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;

public interface ChunkPackets {

    ClientboundLevelChunkWithLightPacket virtualChunk(Player player, ChunkKey chunk, DebugPattern pattern);

    Optional<ClientboundLevelChunkWithLightPacket> realChunk(Player player, ChunkKey chunk);
}
