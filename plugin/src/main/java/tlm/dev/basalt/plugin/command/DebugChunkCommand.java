package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.HideResult;
import tlm.dev.basalt.core.visibility.ShowResult;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

public final class DebugChunkCommand {

    public static final String PERMISSION = "basalt.admin";

    private static final String PLAYER = "player";
    private static final String CHUNK_X = "cx";
    private static final String CHUNK_Z = "cz";

    private final ChunkViews views;
    private final Server server;
    private final MessageSettings messages;

    public DebugChunkCommand(ChunkViews views, Server server, MessageSettings messages) {
        this.views = views;
        this.server = server;
        this.messages = messages;
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("basalt")
                .requires(source -> source.getSender().hasPermission(PERMISSION))
                .then(Commands.literal("debug")
                        .then(Commands.literal("chunk")
                                .then(Commands.argument(PLAYER, StringArgumentType.word())
                                        .then(Commands.literal("on")
                                                .then(Commands.argument(CHUNK_X, coordinate())
                                                        .then(patterns(Commands.argument(CHUNK_Z, coordinate())))))
                                        .then(Commands.literal("off").executes(this::hide)))))
                .build();
    }

    private RequiredArgumentBuilder<CommandSourceStack, Integer> patterns(RequiredArgumentBuilder<CommandSourceStack, Integer> chunkZ) {
        chunkZ.executes(context -> show(context, DebugPattern.FLAT));
        for (DebugPattern pattern : DebugPattern.values()) {
            chunkZ.then(Commands.literal(pattern.label()).executes(context -> show(context, pattern)));
        }
        return chunkZ;
    }

    private int show(CommandContext<CommandSourceStack> context, DebugPattern pattern) {
        CommandSender sender = context.getSource().getSender();
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        ChunkKey chunk = new ChunkKey(IntegerArgumentType.getInteger(context, CHUNK_X), IntegerArgumentType.getInteger(context, CHUNK_Z));
        ShowResult result = views.show(player.getUniqueId(), chunk, pattern);
        MessageKey message = switch (result) {
            case SHOWN -> MessageKey.DEBUG_CHUNK_SHOWN;
            case ALREADY_VIRTUAL -> MessageKey.DEBUG_CHUNK_ALREADY_VIRTUAL;
            case NOT_TRACKED -> MessageKey.DEBUG_CHUNK_NOT_TRACKED;
        };
        sender.sendMessage(messages.render(message,
                Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("x", Integer.toString(chunk.x())),
                Placeholder.unparsed("z", Integer.toString(chunk.z())),
                Placeholder.unparsed("pattern", pattern.label())));
        return result == ShowResult.SHOWN ? Command.SINGLE_SUCCESS : 0;
    }

    private int hide(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        HideResult result = views.hide(player.getUniqueId(), ExitCause.COMMAND);
        MessageKey message = result == HideResult.RESTORED ? MessageKey.DEBUG_CHUNK_RESTORED : MessageKey.DEBUG_CHUNK_NOT_VIRTUAL;
        sender.sendMessage(messages.render(message, Placeholder.unparsed(PLAYER, player.getName())));
        return result == HideResult.RESTORED ? Command.SINGLE_SUCCESS : 0;
    }

    private Player onlinePlayer(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, PLAYER);
        Player player = server.getPlayerExact(name);
        if (player == null) {
            context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_CHUNK_PLAYER_OFFLINE, Placeholder.unparsed(PLAYER, name)));
        }
        return player;
    }

    private static IntegerArgumentType coordinate() {
        return IntegerArgumentType.integer(-ChunkKey.MAX_COORDINATE, ChunkKey.MAX_COORDINATE);
    }
}
