package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class FakeDisplay implements ViewDisplay<String> {

    final List<String> events = new ArrayList<>();
    final Map<UUID, PlayerView<String>> attached = new HashMap<>();
    final Set<ChunkKey> clientChunks = new HashSet<>();
    DebugPattern builtPattern;

    @Override
    public void attach(UUID player, PlayerView<String> view) {
        attached.put(player, view);
        events.add("attach");
    }

    @Override
    public void detach(UUID player) {
        attached.remove(player);
        events.add("detach");
    }

    @Override
    public boolean hasChunk(UUID player, ChunkKey chunk) {
        return clientChunks.contains(chunk);
    }

    @Override
    public String buildVirtual(UUID player, ChunkKey chunk, DebugPattern pattern) {
        events.add("build " + chunk.x() + " " + chunk.z());
        builtPattern = pattern;
        return "V" + chunk.x() + chunk.z();
    }

    @Override
    public void sendVirtual(UUID player, String payload) {
        events.add("send " + payload + " while " + state(player));
    }

    @Override
    public void resendReal(UUID player, ChunkKey chunk) {
        events.add("resend " + chunk.x() + " " + chunk.z() + " while " + state(player));
    }

    private String state(UUID player) {
        PlayerView<String> view = attached.get(player);
        return view == null ? "detached" : view.snapshot().isVirtual() ? "virtual" : "real";
    }
}
