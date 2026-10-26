package tlm.dev.basalt.plugin.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.World;

final class SourceWorld {

    private SourceWorld() {
    }

    static World of(CommandSourceStack source) {
        return source.getLocation().getWorld();
    }
}
