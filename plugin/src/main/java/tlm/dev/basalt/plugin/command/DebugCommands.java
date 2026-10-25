package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class DebugCommands {

    public static final String PERMISSION = "basalt.admin";

    private final DebugChunkCommand chunk;
    private final DebugEntityCommand entity;
    private final DebugWorldCommand world;
    private final DebugEventsCommand events;

    public DebugCommands(DebugChunkCommand chunk, DebugEntityCommand entity, DebugWorldCommand world, DebugEventsCommand events) {
        this.chunk = chunk;
        this.entity = entity;
        this.world = world;
        this.events = events;
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("basalt")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("debug")
                        .then(chunk.branch())
                        .then(entity.branch())
                        .then(world.branch())
                        .then(events.branch()))
                .build();
    }
}
