package tlm.dev.basalt.plugin.facade;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.HostAreaException;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.world.Creation;
import tlm.dev.basalt.core.world.HostArea;
import tlm.dev.basalt.core.world.HostRule;
import tlm.dev.basalt.core.world.RealBlock;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BasaltFacade implements Basalt {

    private final String version;
    private final FacadeTools tools;
    private final Map<UUID, WorldFacade> facades = new LinkedHashMap<>();

    public BasaltFacade(String version, FacadeTools tools) {
        this.version = version;
        this.tools = tools;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public VirtualWorld createWorld(Plugin owner, ChunkArea area) {
        return create(owner, area, HostRule.AIR_ONLY);
    }

    public VirtualWorld createWorldOverRealBlocks(Plugin owner, ChunkArea area) {
        return create(owner, area, HostRule.ANY_BLOCKS);
    }

    @Override
    public Collection<VirtualWorld> worlds() {
        tools.checkThread();
        List<UUID> open = tools.worlds().open();
        facades.keySet().retainAll(open);
        return List.copyOf(facades.values());
    }

    @Override
    public VirtualWorld worldOf(Player player) {
        tools.checkThread();
        Objects.requireNonNull(player, "player");
        return tools.worlds().worldOf(player.getUniqueId()).map(facades::get).orElse(null);
    }

    private VirtualWorld create(Plugin owner, ChunkArea area, HostRule rule) {
        tools.checkThread();
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(area, "area");
        if (!owner.isEnabled()) {
            throw new IllegalStateException("the owner " + owner.getName() + " is not enabled");
        }
        World world = area.world();
        if (tools.server().getWorld(world.getUID()) == null) {
            throw new IllegalStateException("world " + world.getName() + " is not loaded");
        }
        HostArea host = new HostArea(world.getUID(),
                new ChunkRange(area.minChunkX(), area.minChunkZ(), area.maxChunkX(), area.maxChunkZ()),
                world.getMinHeight(), world.getMaxHeight() - world.getMinHeight());
        return switch (tools.worlds().create(host, rule, owner.getName())) {
            case Creation.Created created -> {
                WorldFacade facade = new WorldFacade(created.world(), area, owner, tools);
                facades.put(created.world(), facade);
                yield facade;
            }
            case Creation.Refused refused -> throw switch (refused.reason()) {
                case TOO_MANY_WORLDS -> new IllegalStateException("at most " + tools.settings().maxWorlds()
                        + " virtual worlds can be open at once (world.max-worlds)");
                case TOO_MANY_CHUNKS -> new IllegalArgumentException("the area holds " + area.chunkCount()
                        + " chunks, a virtual world holds at most " + tools.settings().maxChunks() + " (world.max-chunks)");
                case REAL_BLOCKS -> hostArea(world, refused.cause().orElseThrow());
            };
        };
    }

    private static HostAreaException hostArea(World world, RealBlock block) {
        int chunkX = block.position().x() >> 4;
        int chunkZ = block.position().z() >> 4;
        String message = "chunk " + chunkX + " " + chunkZ + " of world " + world.getName() + " holds " + block.block()
                + " at " + block.position().x() + " " + block.position().y() + " " + block.position().z()
                + ", a virtual world stands where the real world is air";
        return new HostAreaException(message, world.getName(), chunkX, chunkZ);
    }
}
