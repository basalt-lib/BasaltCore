package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class DebugCommands {

    public static final String PERMISSION = "basalt.admin";

    private final DebugChunkCommand chunk;
    private final DebugEntityCommand entity;

    public DebugCommands(DebugChunkCommand chunk, DebugEntityCommand entity) {
        this.chunk = chunk;
        this.entity = entity;
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("basalt")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("debug")
                        .then(chunk.branch())
                        .then(entity.branch()))
                .build();
    }
}
