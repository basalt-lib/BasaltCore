package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.List;
import java.util.UUID;

public interface ViewDisplay {

    void attach(UUID player, PlayerView view);

    void detach(UUID player);

    List<ChunkKey> chunksHeld(UUID player, ChunkRange area);

    ChunkSend sendVirtual(UUID player, UUID world, ChunkKey chunk);

    void resendReal(UUID player, ChunkKey chunk);

    void restartFlyingCheck(UUID player);

    void release(UUID world);

    PacketMemory memory(UUID world);
}
