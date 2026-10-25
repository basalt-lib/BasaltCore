package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class FakeDisplay implements ViewDisplay {

    final List<String> events = new ArrayList<>();
    final Map<UUID, PlayerView> attached = new HashMap<>();
    final Set<ChunkKey> clientChunks = new HashSet<>();
    final Set<ChunkKey> encoding = new HashSet<>();

    @Override
    public void attach(UUID player, PlayerView view) {
        attached.put(player, view);
        events.add("attach");
    }

    @Override
    public void detach(UUID player) {
        attached.remove(player);
        events.add("detach");
    }

    @Override
    public List<ChunkKey> chunksHeld(UUID player, ChunkRange area) {
        return area.chunks().stream().filter(clientChunks::contains).toList();
    }

    @Override
    public ChunkSend sendVirtual(UUID player, UUID world, ChunkKey chunk) {
        if (!clientChunks.contains(chunk)) {
            return ChunkSend.GONE;
        }
        if (encoding.contains(chunk)) {
            return ChunkSend.PENDING;
        }
        events.add("send " + chunk.x() + " " + chunk.z() + " while " + state(player));
        return ChunkSend.SENT;
    }

    @Override
    public void resendReal(UUID player, ChunkKey chunk) {
        events.add("resend " + chunk.x() + " " + chunk.z() + " while " + state(player));
    }

    @Override
    public void restartFlyingCheck(UUID player) {
        events.add("restart flying check while " + state(player));
    }

    @Override
    public void release(UUID world) {
        events.add("release");
    }

    @Override
    public PacketMemory memory(UUID world) {
        return new PacketMemory(1, 2048);
    }

    private String state(UUID player) {
        PlayerView view = attached.get(player);
        return view == null ? "detached" : view.snapshot().isVirtual() ? "virtual" : "real";
    }
}
