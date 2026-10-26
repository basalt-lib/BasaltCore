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
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.util.BoundingBox;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.Blueprint;
import tlm.dev.basalt.api.BlueprintException;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class DebugBlueprintCommand implements DebugBranch {

    private static final String NAME = "name";
    private static final String FILE = "file";
    private static final String WORLD = "world";
    private static final String BLUEPRINT = "blueprint";
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Pattern VALID_FILE = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
    private static final String CAPTURED = "capture";

    private final Basalt basalt;
    private final DebugWorlds worlds;
    private final BlueprintDebugTools tools;
    private final MessageSettings messages;
    private final Map<String, Named> blueprints = new LinkedHashMap<>();

    public DebugBlueprintCommand(Basalt basalt, DebugWorlds worlds, BlueprintDebugTools tools, MessageSettings messages) {
        this.basalt = basalt;
        this.worlds = worlds;
        this.tools = tools;
        this.messages = messages;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("blueprint")
                .then(Commands.literal("capture").then(name().then(corner("x1", "y1", "z1", corner("x2", "y2", "z2", null, this::capture), null))))
                .then(Commands.literal("read").then(name().then(Commands.argument(FILE, StringArgumentType.word()).executes(this::read))))
                .then(Commands.literal("place").then(Commands.argument(WORLD, StringArgumentType.word()).then(name()
                        .then(corner("x", "y", "z", null, this::place)))))
                .then(Commands.literal("place-real").then(name().then(corner("x", "y", "z", null, this::placeReal))))
                .then(Commands.literal("list").executes(this::list))
                .then(Commands.literal("drop").then(name().executes(this::drop)))
                .then(Commands.literal("info").then(name().executes(this::info)));
    }

    private RequiredArgumentBuilder<CommandSourceStack, String> name() {
        return Commands.argument(NAME, StringArgumentType.word());
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> corner(String x, String y, String z,
                                                                              RequiredArgumentBuilder<CommandSourceStack, Integer> next,
                                                                              Command<CommandSourceStack> action) {
        RequiredArgumentBuilder<CommandSourceStack, Integer> last = Commands.argument(z, horizontal());
        if (next != null) {
            last.then(next);
        } else {
            last.executes(action);
        }
        return Commands.argument(x, horizontal()).then(Commands.argument(y, vertical()).then(last));
    }

    private int capture(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        if (!VALID_NAME.matcher(name).matches()) {
            return reply(sender, MessageKey.DEBUG_BLUEPRINT_BAD_NAME, Placeholder.unparsed(BLUEPRINT, name));
        }
        World world = SourceWorld.of(context.getSource());
        BlockPosition first = position(context, "x1", "y1", "z1");
        BlockPosition second = position(context, "x2", "y2", "z2");
        BoundingBox box = new BoundingBox(Math.min(first.x(), second.x()), Math.min(first.y(), second.y()), Math.min(first.z(), second.z()),
                Math.max(first.x(), second.x()) + 1, Math.max(first.y(), second.y()) + 1, Math.max(first.z(), second.z()) + 1);
        long start = System.nanoTime();
        Blueprint blueprint;
        try {
            blueprint = basalt.captureBlueprint(world, box);
        } catch (IllegalArgumentException | IllegalStateException refused) {
            return refused(sender, name, refused);
        }
        long millis = millisSince(start);
        blueprints.put(name, new Named(blueprint, Optional.empty()));
        sender.sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_CAPTURED, Placeholder.unparsed(BLUEPRINT, name),
                Placeholder.unparsed(WORLD, world.getName()), size(blueprint), millis(millis)));
        return Command.SINGLE_SUCCESS;
    }

    private int read(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        String file = StringArgumentType.getString(context, FILE);
        if (!VALID_NAME.matcher(name).matches()) {
            return reply(sender, MessageKey.DEBUG_BLUEPRINT_BAD_NAME, Placeholder.unparsed(BLUEPRINT, name));
        }
        Optional<Path> path = inFolder(file);
        if (path.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_BLUEPRINT_BAD_FILE, Placeholder.unparsed(BLUEPRINT, name), Placeholder.unparsed(FILE, file));
        }
        long start = System.nanoTime();
        Blueprint blueprint;
        try {
            blueprint = basalt.readBlueprint(path.get());
        } catch (IOException unreadable) {
            return unreadable(sender, name, file, unreadable);
        } catch (BlueprintException refused) {
            return refused(sender, name, refused);
        }
        long millis = millisSince(start);
        blueprints.put(name, new Named(blueprint, path));
        sender.sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_READ, Placeholder.unparsed(BLUEPRINT, name),
                Placeholder.unparsed(FILE, file), size(blueprint), millis(millis)));
        return Command.SINGLE_SUCCESS;
    }

    private int place(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String worldName = StringArgumentType.getString(context, WORLD);
        Optional<VirtualWorld> world = worlds.live(worldName);
        if (world.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_WORLD_UNKNOWN, Placeholder.unparsed(WORLD, worldName));
        }
        Optional<Named> named = known(context);
        if (named.isEmpty()) {
            return 0;
        }
        BlockPosition at = position(context, "x", "y", "z");
        String name = StringArgumentType.getString(context, NAME);
        long start = System.nanoTime();
        try {
            world.get().place(named.get().blueprint(), at.x(), at.y(), at.z());
        } catch (IllegalArgumentException | IllegalStateException refused) {
            return refused(sender, name, refused);
        }
        sender.sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_PLACED, Placeholder.unparsed(BLUEPRINT, name),
                Placeholder.unparsed(WORLD, worldName), at(at), millis(millisSince(start))));
        return Command.SINGLE_SUCCESS;
    }

    private int placeReal(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Optional<Named> named = known(context);
        if (named.isEmpty()) {
            return 0;
        }
        String name = StringArgumentType.getString(context, NAME);
        if (named.get().file().isEmpty()) {
            return reply(sender, MessageKey.DEBUG_BLUEPRINT_NO_FILE, Placeholder.unparsed(BLUEPRINT, name));
        }
        World world = SourceWorld.of(context.getSource());
        BlockPosition at = position(context, "x", "y", "z");
        long start = System.nanoTime();
        Path file = named.get().file().get();
        boolean placed;
        try {
            placed = tools.real().place(world, file, at.x(), at.y(), at.z());
        } catch (IOException unreadable) {
            return unreadable(sender, name, file.getFileName().toString(), unreadable);
        } catch (RuntimeException refused) {
            return refused(sender, name, refused);
        }
        if (!placed) {
            return reply(sender, MessageKey.DEBUG_BLUEPRINT_REAL_EMPTY, Placeholder.unparsed(BLUEPRINT, name));
        }
        sender.sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_PLACED_REAL, Placeholder.unparsed(BLUEPRINT, name),
                Placeholder.unparsed(WORLD, world.getName()), at(at), millis(millisSince(start))));
        return Command.SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> context) {
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_LIST,
                Placeholder.unparsed(BLUEPRINT, blueprints.isEmpty() ? "none" : String.join(",", blueprints.keySet()))));
        return Command.SINGLE_SUCCESS;
    }

    private int drop(CommandContext<CommandSourceStack> context) {
        Optional<Named> named = known(context);
        if (named.isEmpty()) {
            return 0;
        }
        String name = StringArgumentType.getString(context, NAME);
        blueprints.remove(name);
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_DROPPED, Placeholder.unparsed(BLUEPRINT, name)));
        return Command.SINGLE_SUCCESS;
    }

    private int info(CommandContext<CommandSourceStack> context) {
        Optional<Named> named = known(context);
        if (named.isEmpty()) {
            return 0;
        }
        String name = StringArgumentType.getString(context, NAME);
        Blueprint blueprint = named.get().blueprint();
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_INFO, Placeholder.unparsed(BLUEPRINT, name),
                size(blueprint), Placeholder.unparsed("bytes", Long.toString(tools.bytes().applyAsLong(blueprint))),
                Placeholder.unparsed("source", named.get().file().map(file -> file.getFileName().toString()).orElse(CAPTURED))));
        return Command.SINGLE_SUCCESS;
    }

    private Optional<Named> known(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, NAME);
        Named named = blueprints.get(name);
        if (named == null) {
            context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_BLUEPRINT_UNKNOWN, Placeholder.unparsed(BLUEPRINT, name)));
        }
        return Optional.ofNullable(named);
    }

    private Optional<Path> inFolder(String file) {
        if (!VALID_FILE.matcher(file).matches() || file.equals(".") || file.equals("..")) {
            return Optional.empty();
        }
        Path folder = tools.folder().toAbsolutePath().normalize();
        Path path = folder.resolve(file).normalize();
        return path.getParent() != null && path.getParent().equals(folder) ? Optional.of(path) : Optional.empty();
    }

    private int refused(CommandSender sender, String name, Exception refused) {
        return reply(sender, MessageKey.DEBUG_BLUEPRINT_REFUSED, Placeholder.unparsed(BLUEPRINT, name),
                Placeholder.unparsed("reason", String.valueOf(refused.getMessage())));
    }

    private int unreadable(CommandSender sender, String name, String file, IOException unreadable) {
        return reply(sender, MessageKey.DEBUG_BLUEPRINT_UNREADABLE, Placeholder.unparsed(BLUEPRINT, name),
                Placeholder.unparsed(FILE, file), Placeholder.unparsed("cause", unreadable.getClass().getSimpleName()));
    }

    private int reply(CommandSender sender, MessageKey key, TagResolver... resolvers) {
        sender.sendMessage(messages.render(key, resolvers));
        return 0;
    }

    private static BlockPosition position(CommandContext<CommandSourceStack> context, String x, String y, String z) {
        return new BlockPosition(IntegerArgumentType.getInteger(context, x), IntegerArgumentType.getInteger(context, y),
                IntegerArgumentType.getInteger(context, z));
    }

    private static TagResolver size(Blueprint blueprint) {
        return TagResolver.resolver(Placeholder.unparsed("size", blueprint.sizeX() + " " + blueprint.sizeY() + " " + blueprint.sizeZ()),
                Placeholder.unparsed("blocks", Integer.toString(blueprint.blockCount())));
    }

    private static TagResolver at(BlockPosition at) {
        return Placeholder.unparsed("at", at.x() + " " + at.y() + " " + at.z());
    }

    private static TagResolver millis(long millis) {
        return Placeholder.unparsed("millis", Long.toString(millis));
    }

    private static long millisSince(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static IntegerArgumentType horizontal() {
        return IntegerArgumentType.integer(-BlockPosition.MAX_HORIZONTAL, BlockPosition.MAX_HORIZONTAL);
    }

    private static IntegerArgumentType vertical() {
        return IntegerArgumentType.integer(-BlockPosition.MAX_VERTICAL, BlockPosition.MAX_VERTICAL);
    }

    private record Named(Blueprint blueprint, Optional<Path> file) {
    }
}
