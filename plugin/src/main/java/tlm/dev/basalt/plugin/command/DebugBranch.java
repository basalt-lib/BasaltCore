package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public interface DebugBranch {

    LiteralArgumentBuilder<CommandSourceStack> branch();
}
