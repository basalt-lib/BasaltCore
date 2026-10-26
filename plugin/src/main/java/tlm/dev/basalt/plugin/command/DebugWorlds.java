package tlm.dev.basalt.plugin.command;

import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.visibility.PacketMemory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class DebugWorlds {

    private final Map<String, VirtualWorld> named = new LinkedHashMap<>();
    private final Function<UUID, PacketMemory> memory;

    public DebugWorlds(Function<UUID, PacketMemory> memory) {
        this.memory = memory;
    }

    Optional<VirtualWorld> live(String name) {
        VirtualWorld world = named.get(name);
        if (world != null && world.isClosed()) {
            named.remove(name);
            return Optional.empty();
        }
        return Optional.ofNullable(world);
    }

    void put(String name, VirtualWorld world) {
        named.put(name, world);
    }

    void remove(String name) {
        named.remove(name);
    }

    String nameOf(VirtualWorld world) {
        return named.entrySet().stream().filter(entry -> entry.getValue().id().equals(world.id()))
                .map(Map.Entry::getKey).findFirst().orElse(world.id().toString());
    }

    PacketMemory memory(VirtualWorld world) {
        return memory.apply(world.id());
    }
}
