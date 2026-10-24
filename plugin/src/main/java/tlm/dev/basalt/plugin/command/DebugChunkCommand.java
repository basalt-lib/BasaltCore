package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class DebugChunkCommand {

    private static final String PLAYER = "player";
    private static final String CHUNK_X = "cx";
    private static final String CHUNK_Z = "cz";

    private final Basalt basalt;
    private final Function<ChunkArea, VirtualWorld> overRealBlocks;
    private final Server server;
    private final MessageSettings messages;
    private final Map<UUID, VirtualWorld> debugWorlds = new HashMap<>();

    public DebugChunkCommand(Basalt basalt, Function<ChunkArea, VirtualWorld> overRealBlocks, Server server, MessageSettings messages) {
        this.basalt = basalt;
        this.overRealBlocks = overRealBlocks;
        this.server = server;
        this.messages = messages;
    }

    public LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("chunk")
                .then(Commands.argument(PLAYER, StringArgumentType.word())
                        .then(Commands.literal("on")
                                .then(Commands.argument(CHUNK_X, coordinate())
                                        .then(patterns(Commands.argument(CHUNK_Z, coordinate())))))
                        .then(Commands.literal("off").executes(this::hide)));
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
        int chunkX = IntegerArgumentType.getInteger(context, CHUNK_X);
        int chunkZ = IntegerArgumentType.getInteger(context, CHUNK_Z);
        MessageKey message = showIn(player, new ChunkArea(player.getWorld(), chunkX, chunkZ, chunkX, chunkZ), pattern);
        sender.sendMessage(messages.render(message,
                Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("x", Integer.toString(chunkX)),
                Placeholder.unparsed("z", Integer.toString(chunkZ)),
                Placeholder.unparsed("pattern", pattern.label())));
        return message == MessageKey.DEBUG_CHUNK_SHOWN ? Command.SINGLE_SUCCESS : 0;
    }

    private MessageKey showIn(Player player, ChunkArea area, DebugPattern pattern) {
        if (basalt.worldOf(player) != null) {
            return MessageKey.DEBUG_CHUNK_ALREADY_VIRTUAL;
        }
        VirtualWorld previous = debugWorlds.remove(player.getUniqueId());
        if (previous != null) {
            previous.close();
        }
        VirtualWorld world;
        try {
            world = overRealBlocks.apply(area);
        } catch (IllegalStateException tooMany) {
            return MessageKey.DEBUG_CHUNK_REFUSED;
        }
        DebugPainter.paint(world, server, pattern);
        try {
            world.addViewer(player);
        } catch (IllegalStateException noView) {
            world.close();
            return MessageKey.DEBUG_CHUNK_NOT_TRACKED;
        }
        debugWorlds.put(player.getUniqueId(), world);
        return MessageKey.DEBUG_CHUNK_SHOWN;
    }

    private int hide(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        VirtualWorld world = basalt.worldOf(player);
        if (world != null) {
            world.close();
        }
        debugWorlds.remove(player.getUniqueId());
        MessageKey message = world != null ? MessageKey.DEBUG_CHUNK_RESTORED : MessageKey.DEBUG_CHUNK_NOT_VIRTUAL;
        sender.sendMessage(messages.render(message, Placeholder.unparsed(PLAYER, player.getName())));
        return world != null ? Command.SINGLE_SUCCESS : 0;
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
        return IntegerArgumentType.integer(-ChunkArea.MAX_CHUNK_COORDINATE, ChunkArea.MAX_CHUNK_COORDINATE);
    }
}
