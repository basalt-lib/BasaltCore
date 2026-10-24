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
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.HostAreaException;
import tlm.dev.basalt.api.PlayerVisibility;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class DebugWorldCommand {

    private static final String NAME = "name";
    private static final String PLAYER = "player";
    private static final String WORLD = "world";
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final List<String> CORNERS = List.of("cx1", "cz1", "cx2", "cz2");

    private final Basalt basalt;
    private final Plugin plugin;
    private final Server server;
    private final MessageSettings messages;
    private final Map<String, VirtualWorld> named = new LinkedHashMap<>();

    public DebugWorldCommand(Basalt basalt, Plugin plugin, MessageSettings messages) {
        this.basalt = basalt;
        this.plugin = plugin;
        this.server = plugin.getServer();
        this.messages = messages;
    }

    public LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("world")
                .then(Commands.literal("create").then(Commands.argument(NAME, StringArgumentType.word()).then(corners())))
                .then(Commands.literal("add").then(Commands.argument(NAME, StringArgumentType.word())
                        .then(Commands.argument(PLAYER, StringArgumentType.word()).executes(this::add))))
                .then(Commands.literal("remove").then(Commands.argument(PLAYER, StringArgumentType.word()).executes(this::remove)))
                .then(Commands.literal("close").then(Commands.argument(NAME, StringArgumentType.word()).executes(this::close)))
                .then(Commands.literal("info").then(Commands.argument(NAME, StringArgumentType.word()).executes(this::info)))
                .then(Commands.literal("visibility").then(visibilities(Commands.argument(NAME, StringArgumentType.word()))));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> visibilities(RequiredArgumentBuilder<CommandSourceStack, String> name) {
        for (PlayerVisibility visibility : PlayerVisibility.values()) {
            name.then(Commands.literal(label(visibility)).executes(context -> visibility(context, visibility)));
        }
        return name;
    }

    private int visibility(CommandContext<CommandSourceStack> context, PlayerVisibility visibility) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        Optional<VirtualWorld> world = live(name);
        if (world.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_WORLD_UNKNOWN, Placeholder.unparsed(WORLD, name));
        }
        world.get().setPlayerVisibility(visibility);
        sender.sendMessage(messages.render(MessageKey.DEBUG_WORLD_VISIBILITY, Placeholder.unparsed(WORLD, name),
                Placeholder.unparsed("visibility", label(visibility))));
        return Command.SINGLE_SUCCESS;
    }

    private RequiredArgumentBuilder<CommandSourceStack, Integer> corners() {
        RequiredArgumentBuilder<CommandSourceStack, Integer> last = Commands.argument(CORNERS.get(3), coordinate());
        last.executes(context -> create(context, Optional.empty()));
        for (DebugPattern pattern : DebugPattern.values()) {
            last.then(Commands.literal(pattern.label()).executes(context -> create(context, Optional.of(pattern))));
        }
        return Commands.argument(CORNERS.get(0), coordinate())
                .then(Commands.argument(CORNERS.get(1), coordinate())
                        .then(Commands.argument(CORNERS.get(2), coordinate()).then(last)));
    }

    private int create(CommandContext<CommandSourceStack> context, Optional<DebugPattern> pattern) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        if (!VALID_NAME.matcher(name).matches()) {
            return reply(sender, MessageKey.DEBUG_WORLD_BAD_NAME, Placeholder.unparsed(WORLD, name));
        }
        if (live(name).isPresent()) {
            return reply(sender, MessageKey.DEBUG_WORLD_EXISTS, Placeholder.unparsed(WORLD, name));
        }
        int x1 = IntegerArgumentType.getInteger(context, CORNERS.get(0));
        int z1 = IntegerArgumentType.getInteger(context, CORNERS.get(1));
        int x2 = IntegerArgumentType.getInteger(context, CORNERS.get(2));
        int z2 = IntegerArgumentType.getInteger(context, CORNERS.get(3));
        World world = sender instanceof Player player ? player.getWorld() : server.getWorlds().getFirst();
        ChunkArea area = new ChunkArea(world, Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2));
        VirtualWorld created;
        try {
            created = basalt.createWorld(plugin, area);
        } catch (HostAreaException | IllegalArgumentException | IllegalStateException refused) {
            return reply(sender, MessageKey.DEBUG_WORLD_REFUSED, Placeholder.unparsed(WORLD, name),
                    Placeholder.unparsed("reason", String.valueOf(refused.getMessage())));
        }
        pattern.ifPresent(painted -> DebugPainter.paint(created, server, painted));
        named.put(name, created);
        sender.sendMessage(messages.render(MessageKey.DEBUG_WORLD_CREATED, Placeholder.unparsed(WORLD, name), area(area),
                Placeholder.unparsed("pattern", pattern.map(DebugPattern::label).orElse("empty"))));
        return Command.SINGLE_SUCCESS;
    }

    private int add(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        Optional<VirtualWorld> world = live(name);
        if (world.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_WORLD_UNKNOWN, Placeholder.unparsed(WORLD, name));
        }
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        try {
            world.get().addViewer(player);
        } catch (IllegalStateException refused) {
            return reply(sender, MessageKey.DEBUG_WORLD_VIEWER_REFUSED, Placeholder.unparsed(WORLD, name),
                    Placeholder.unparsed(PLAYER, player.getName()), Placeholder.unparsed("reason", String.valueOf(refused.getMessage())));
        }
        sender.sendMessage(messages.render(MessageKey.DEBUG_WORLD_ADDED, Placeholder.unparsed(WORLD, name),
                Placeholder.unparsed(PLAYER, player.getName())));
        return Command.SINGLE_SUCCESS;
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        VirtualWorld world = basalt.worldOf(player);
        if (world == null) {
            return reply(sender, MessageKey.DEBUG_WORLD_NOT_VIEWING, Placeholder.unparsed(PLAYER, player.getName()));
        }
        world.removeViewer(player);
        sender.sendMessage(messages.render(MessageKey.DEBUG_WORLD_REMOVED, Placeholder.unparsed(WORLD, nameOf(world)),
                Placeholder.unparsed(PLAYER, player.getName())));
        return Command.SINGLE_SUCCESS;
    }

    private int close(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        Optional<VirtualWorld> world = live(name);
        if (world.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_WORLD_UNKNOWN, Placeholder.unparsed(WORLD, name));
        }
        world.get().close();
        named.remove(name);
        sender.sendMessage(messages.render(MessageKey.DEBUG_WORLD_CLOSED, Placeholder.unparsed(WORLD, name)));
        return Command.SINGLE_SUCCESS;
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        Optional<VirtualWorld> world = live(name);
        if (world.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_WORLD_UNKNOWN, Placeholder.unparsed(WORLD, name));
        }
        List<String> viewers = world.get().viewers().stream().map(Player::getName).sorted().toList();
        sender.sendMessage(messages.render(MessageKey.DEBUG_WORLD_INFO, Placeholder.unparsed(WORLD, name),
                Placeholder.unparsed("id", world.get().id().toString()), area(world.get().area()),
                Placeholder.unparsed("chunks", Long.toString(world.get().area().chunkCount())),
                Placeholder.unparsed("count", Integer.toString(viewers.size())),
                Placeholder.unparsed("viewers", viewers.isEmpty() ? "none" : String.join(",", viewers)),
                Placeholder.unparsed("visibility", label(world.get().playerVisibility()))));
        return Command.SINGLE_SUCCESS;
    }

    private Optional<VirtualWorld> live(String name) {
        VirtualWorld world = named.get(name);
        if (world != null && world.isClosed()) {
            named.remove(name);
            return Optional.empty();
        }
        return Optional.ofNullable(world);
    }

    private String nameOf(VirtualWorld world) {
        return named.entrySet().stream().filter(entry -> entry.getValue().id().equals(world.id()))
                .map(Map.Entry::getKey).findFirst().orElse(world.id().toString());
    }

    private Player onlinePlayer(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, PLAYER);
        Player player = server.getPlayerExact(name);
        if (player == null) {
            context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_CHUNK_PLAYER_OFFLINE, Placeholder.unparsed(PLAYER, name)));
        }
        return player;
    }

    private int reply(CommandSender sender, MessageKey key, TagResolver... resolvers) {
        sender.sendMessage(messages.render(key, resolvers));
        return 0;
    }

    private static TagResolver area(ChunkArea area) {
        return TagResolver.resolver(
                Placeholder.unparsed("x1", Integer.toString(area.minChunkX())),
                Placeholder.unparsed("z1", Integer.toString(area.minChunkZ())),
                Placeholder.unparsed("x2", Integer.toString(area.maxChunkX())),
                Placeholder.unparsed("z2", Integer.toString(area.maxChunkZ())));
    }

    private static String label(PlayerVisibility visibility) {
        return visibility.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static IntegerArgumentType coordinate() {
        return IntegerArgumentType.integer(-ChunkArea.MAX_CHUNK_COORDINATE, ChunkArea.MAX_CHUNK_COORDINATE);
    }
}
