package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

public final class DebugEventsCommand implements DebugBranch {

    private static final String PLAYER = "player";
    private static final String OFF = "off";

    private final DebugEventWatch watch;
    private final Server server;
    private final MessageSettings messages;

    public DebugEventsCommand(DebugEventWatch watch, Server server, MessageSettings messages) {
        this.watch = watch;
        this.server = server;
        this.messages = messages;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> branch() {
        RequiredArgumentBuilder<CommandSourceStack, String> player = Commands.argument(PLAYER, StringArgumentType.word());
        for (DebugEventMode mode : DebugEventMode.values()) {
            player.then(Commands.literal(mode.label()).executes(context -> watch(context, mode)));
        }
        return Commands.literal("events").then(player
                .then(Commands.literal(OFF).executes(this::stop))
                .then(Commands.literal("status").executes(this::status)));
    }

    private int watch(CommandContext<CommandSourceStack> context, DebugEventMode mode) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        watch.watch(player.getUniqueId(), mode);
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_EVENTS_WATCHING,
                Placeholder.unparsed(PLAYER, player.getName()), Placeholder.unparsed("mode", mode.label())));
        return Command.SINGLE_SUCCESS;
    }

    private int stop(CommandContext<CommandSourceStack> context) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        watch.stop(player.getUniqueId());
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_EVENTS_OFF, Placeholder.unparsed(PLAYER, player.getName())));
        return Command.SINGLE_SUCCESS;
    }

    private int status(CommandContext<CommandSourceStack> context) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_EVENTS_STATUS,
                Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("mode", watch.mode(player.getUniqueId()).map(DebugEventMode::label).orElse(OFF)),
                Placeholder.unparsed("counts", watch.counts(player.getUniqueId())),
                Placeholder.unparsed("last", watch.last(player.getUniqueId()))));
        return Command.SINGLE_SUCCESS;
    }

    private Player onlinePlayer(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, PLAYER);
        Player player = server.getPlayerExact(name);
        if (player == null) {
            context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_CHUNK_PLAYER_OFFLINE, Placeholder.unparsed(PLAYER, name)));
        }
        return player;
    }
}
