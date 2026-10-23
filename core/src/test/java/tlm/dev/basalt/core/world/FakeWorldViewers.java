package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

final class FakeWorldViewers implements WorldViewers, WorldListener {

    final List<String> events = new ArrayList<>();
    final Set<UUID> withoutView = new HashSet<>();
    Consumer<UUID> exitPath = player -> {
    };

    @Override
    public boolean enter(UUID player, UUID world, ChunkRange area) {
        events.add("enter " + name(player) + " " + area.minX() + " " + area.minZ() + " " + area.maxX() + " " + area.maxZ());
        return !withoutView.contains(player);
    }

    @Override
    public void exit(UUID player) {
        events.add("exit " + name(player));
        exitPath.accept(player);
    }

    @Override
    public void blockChanged(Set<UUID> viewers, BlockPosition position, int state) {
        events.add("block " + position.x() + " " + position.y() + " " + position.z() + " = " + state + " to " + names(viewers));
    }

    @Override
    public void chunksChanged(Set<UUID> viewers, List<ChunkKey> chunks) {
        List<String> keys = chunks.stream().map(chunk -> chunk.x() + " " + chunk.z()).sorted().toList();
        events.add("chunks " + keys + " to " + names(viewers));
    }

    @Override
    public void emptied(UUID world) {
        events.add("emptied");
    }

    @Override
    public void closed(UUID world) {
        events.add("closed");
    }

    static String name(UUID player) {
        return player.getLeastSignificantBits() == 0xA ? "A" : player.getLeastSignificantBits() == 0xB ? "B" : "C";
    }

    private static String names(Set<UUID> players) {
        Set<String> sorted = new TreeSet<>();
        players.forEach(player -> sorted.add(name(player)));
        return sorted.toString();
    }
}
