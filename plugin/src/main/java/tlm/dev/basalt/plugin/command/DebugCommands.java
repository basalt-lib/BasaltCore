package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

public final class DebugCommands {

    public static final String PERMISSION = "basalt.admin";

    private final List<DebugBranch> branches;

    public DebugCommands(List<DebugBranch> branches) {
        this.branches = List.copyOf(branches);
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        LiteralArgumentBuilder<CommandSourceStack> debug = Commands.literal("debug");
        branches.forEach(branch -> debug.then(branch.branch()));
        return Commands.literal("basalt")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(debug)
                .build();
    }
}
