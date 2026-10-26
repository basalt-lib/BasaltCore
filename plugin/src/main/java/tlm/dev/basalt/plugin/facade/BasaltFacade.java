package tlm.dev.basalt.plugin.facade;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.Blueprint;
import tlm.dev.basalt.api.BlueprintException;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.HostAreaException;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.blueprint.PlanException;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.world.Creation;
import tlm.dev.basalt.core.world.HostArea;
import tlm.dev.basalt.core.world.HostRule;
import tlm.dev.basalt.core.world.RealBlock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Override
    public Blueprint captureBlueprint(World world, BoundingBox box) {
        tools.checkThread();
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(box, "box");
        if (tools.server().getWorld(world.getUID()) == null) {
            throw new IllegalStateException("world " + world.getName() + " is not loaded");
        }
        BlockBox blocks = BlockBoxes.of(box);
        if (blocks.min().y() < world.getMinHeight() || blocks.max().y() >= world.getMaxHeight()) {
            throw new IllegalArgumentException("the box " + blocks.min().y() + " to " + blocks.max().y() + " leaves the height "
                    + world.getMinHeight() + " to " + (world.getMaxHeight() - 1) + " of " + world.getName());
        }
        if (blocks.volume() > tools.content().plans().settings().maxBlocks()) {
            throw new IllegalArgumentException("the box holds " + blocks.volume() + " blocks, more than "
                    + tools.content().plans().settings().maxBlocks() + " (blueprint.max-blocks)");
        }
        return new BlueprintFacade(tools.content().plans().capture(world.getUID(), blocks));
    }

    @Override
    public Blueprint readBlueprint(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        try (InputStream in = Files.newInputStream(file)) {
            return readBlueprint(in);
        }
    }

    @Override
    public Blueprint readBlueprint(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        try {
            return new BlueprintFacade(tools.content().plans().read(in));
        } catch (PlanException refused) {
            throw new BlueprintException(refused.getMessage(), refused);
        }
    }

    public long bytesOf(Blueprint blueprint) {
        return blueprint instanceof BlueprintFacade facade ? facade.plan().bytes() : 0;
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
                case TOO_MANY_WORLDS -> new IllegalStateException("at most " + tools.worlds().settings().maxWorlds()
                        + " virtual worlds can be open at once (world.max-worlds)");
                case TOO_MANY_CHUNKS -> new IllegalArgumentException("the area holds " + area.chunkCount()
                        + " chunks, a virtual world holds at most " + tools.worlds().settings().maxChunks() + " (world.max-chunks)");
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
