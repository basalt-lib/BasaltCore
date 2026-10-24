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
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.chunk.DebugChunkPattern;
import tlm.dev.basalt.core.chunk.LocalBox;
import tlm.dev.basalt.core.chunk.PatternBox;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.world.Creation;
import tlm.dev.basalt.core.world.HostArea;
import tlm.dev.basalt.core.world.HostRule;
import tlm.dev.basalt.core.world.ViewerResult;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.nms.platform.MojangBlockStates;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DebugChunkCommand {

    private static final String PLAYER = "player";
    private static final String CHUNK_X = "cx";
    private static final String CHUNK_Z = "cz";

    private final VirtualWorlds<?> worlds;
    private final Server server;
    private final MessageSettings messages;
    private final MojangBlockStates blocks;
    private final Map<UUID, UUID> debugWorlds = new HashMap<>();

    public DebugChunkCommand(VirtualWorlds<?> worlds, Server server, MessageSettings messages, MojangBlockStates blocks) {
        this.worlds = worlds;
        this.server = server;
        this.messages = messages;
        this.blocks = blocks;
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
        ChunkKey chunk = new ChunkKey(IntegerArgumentType.getInteger(context, CHUNK_X), IntegerArgumentType.getInteger(context, CHUNK_Z));
        MessageKey message = showIn(player, chunk, pattern);
        sender.sendMessage(messages.render(message,
                Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("x", Integer.toString(chunk.x())),
                Placeholder.unparsed("z", Integer.toString(chunk.z())),
                Placeholder.unparsed("pattern", pattern.label())));
        return message == MessageKey.DEBUG_CHUNK_SHOWN ? Command.SINGLE_SUCCESS : 0;
    }

    private MessageKey showIn(Player player, ChunkKey chunk, DebugPattern pattern) {
        UUID id = player.getUniqueId();
        if (worlds.worldOf(id).isPresent()) {
            return MessageKey.DEBUG_CHUNK_ALREADY_VIRTUAL;
        }
        Optional.ofNullable(debugWorlds.remove(id)).ifPresent(worlds::close);
        World world = player.getWorld();
        HostArea host = new HostArea(world.getUID(), ChunkRange.of(chunk), world.getMinHeight(), world.getMaxHeight() - world.getMinHeight());
        if (!(worlds.create(host, HostRule.ANY_BLOCKS) instanceof Creation.Created created)) {
            return MessageKey.DEBUG_CHUNK_REFUSED;
        }
        paint(created.world(), chunk, pattern, world.getMinHeight());
        if (worlds.addViewer(created.world(), id) != ViewerResult.ADDED) {
            worlds.close(created.world());
            return MessageKey.DEBUG_CHUNK_NOT_TRACKED;
        }
        debugWorlds.put(id, created.world());
        return MessageKey.DEBUG_CHUNK_SHOWN;
    }

    private void paint(UUID world, ChunkKey chunk, DebugPattern pattern, int bottom) {
        for (PatternBox painted : DebugChunkPattern.boxes(pattern, bottom)) {
            LocalBox local = painted.box();
            BlockBox box = new BlockBox(
                    new BlockPosition((chunk.x() << 4) + local.minX(), local.minY(), (chunk.z() << 4) + local.minZ()),
                    new BlockPosition((chunk.x() << 4) + local.maxX(), local.maxY(), (chunk.z() << 4) + local.maxZ()));
            worlds.fill(world, box, blocks.stateOf(server.createBlockData(painted.block())));
        }
    }

    private int hide(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        Optional<UUID> world = worlds.worldOf(player.getUniqueId());
        world.ifPresent(worlds::close);
        debugWorlds.remove(player.getUniqueId());
        MessageKey message = world.isPresent() ? MessageKey.DEBUG_CHUNK_RESTORED : MessageKey.DEBUG_CHUNK_NOT_VIRTUAL;
        sender.sendMessage(messages.render(message, Placeholder.unparsed(PLAYER, player.getName())));
        return world.isPresent() ? Command.SINGLE_SUCCESS : 0;
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
