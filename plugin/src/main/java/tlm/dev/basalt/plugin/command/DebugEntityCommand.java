package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.VirtualEntity;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.core.entity.EntityStatus;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

import java.util.List;
import java.util.Locale;

public final class DebugEntityCommand {

    private static final String PLAYER = "player";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String NONE = "none";
    private static final String TYPE = "type";
    private static final String HEALTH_ARGUMENT = "health";
    private static final String NAME = "Basalt";
    private static final double HEALTH = 5;
    private static final double MAX_HEALTH = 1024;

    private final Basalt basalt;
    private final VirtualEntityService entities;
    private final Server server;
    private final MessageSettings messages;

    public DebugEntityCommand(Basalt basalt, VirtualEntityService entities, Server server, MessageSettings messages) {
        this.basalt = basalt;
        this.entities = entities;
        this.server = server;
        this.messages = messages;
    }

    public LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("entity")
                .then(Commands.argument(PLAYER, StringArgumentType.word())
                        .then(Commands.literal("on").then(position(at(this::spawn)
                                .then(Commands.argument(TYPE, StringArgumentType.word()).executes(this::spawnType)))))
                        .then(Commands.literal("move").then(position(at(this::move))))
                        .then(Commands.literal("health").then(Commands.argument(HEALTH_ARGUMENT,
                                DoubleArgumentType.doubleArg(0, MAX_HEALTH)).executes(this::health)))
                        .then(Commands.literal("off").executes(this::removeAll))
                        .then(Commands.literal("status").executes(this::status)));
    }

    private RequiredArgumentBuilder<CommandSourceStack, Double> position(RequiredArgumentBuilder<CommandSourceStack, Double> z) {
        return Commands.argument(X, horizontal()).then(Commands.argument(Y, vertical()).then(z));
    }

    private RequiredArgumentBuilder<CommandSourceStack, Double> at(Placement placement) {
        return Commands.argument(Z, horizontal()).executes(context -> place(context, placement));
    }

    private int spawnType(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, TYPE).toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(name);
        EntityType type = key == null ? null : Registry.ENTITY_TYPE.get(key);
        return place(context, (player, world, at) -> spawnPlain(world, at, type));
    }

    private int place(CommandContext<CommandSourceStack> context, Placement placement) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        double x = DoubleArgumentType.getDouble(context, X);
        double y = DoubleArgumentType.getDouble(context, Y);
        double z = DoubleArgumentType.getDouble(context, Z);
        VirtualWorld world = basalt.worldOf(player);
        Placed placed = placement.place(player, world, world == null ? null : new Location(world.area().world(), x, y, z));
        context.getSource().getSender().sendMessage(messages.render(placed.message(),
                Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("id", Integer.toString(placed.id())),
                number(X, x), number(Y, y), number(Z, z)));
        return placed.id() > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private Placed spawn(Player player, VirtualWorld world, Location at) {
        if (world == null) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_NOT_VIRTUAL);
        }
        try {
            VirtualEntity entity = world.spawnEntity(EntityType.ZOMBIE, at);
            entity.setCustomName(Component.text(NAME));
            entity.setMaxHealth(HEALTH);
            return new Placed(MessageKey.DEBUG_ENTITY_SPAWNED, entity.id());
        } catch (IllegalArgumentException outside) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_OUTSIDE);
        } catch (IllegalStateException full) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_TOO_MANY);
        }
    }

    private Placed spawnPlain(VirtualWorld world, Location at, EntityType type) {
        if (world == null) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_NOT_VIRTUAL);
        }
        if (!inside(world, at)) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_OUTSIDE);
        }
        if (type == null) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_TYPE_REFUSED);
        }
        try {
            return new Placed(MessageKey.DEBUG_ENTITY_SPAWNED, world.spawnEntity(type, at).id());
        } catch (IllegalArgumentException refused) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_TYPE_REFUSED);
        } catch (IllegalStateException full) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_TOO_MANY);
        }
    }

    private Placed move(Player player, VirtualWorld world, Location to) {
        List<VirtualEntity> living = world == null ? List.of() : world.entities().stream().filter(entity -> entity.health() > 0).toList();
        if (living.isEmpty()) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_NONE);
        }
        VirtualEntity newest = living.getLast();
        try {
            newest.teleport(to);
            return new Placed(MessageKey.DEBUG_ENTITY_MOVED, newest.id());
        } catch (IllegalArgumentException outside) {
            return Placed.refused(MessageKey.DEBUG_ENTITY_OUTSIDE);
        }
    }

    private int health(CommandContext<CommandSourceStack> context) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        VirtualWorld world = basalt.worldOf(player);
        List<VirtualEntity> living = world == null ? List.of() : world.entities().stream().filter(entity -> entity.health() > 0).toList();
        double health = DoubleArgumentType.getDouble(context, HEALTH_ARGUMENT);
        if (living.isEmpty()) {
            return reply(context, MessageKey.DEBUG_ENTITY_NONE, player, 0, health);
        }
        VirtualEntity newest = living.getLast();
        newest.setHealth(Math.min(health, newest.maxHealth()));
        return reply(context, MessageKey.DEBUG_ENTITY_HEALTH, player, newest.id(), newest.health());
    }

    private int reply(CommandContext<CommandSourceStack> context, MessageKey message, Player player, int id, double health) {
        context.getSource().getSender().sendMessage(messages.render(message, Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("id", Integer.toString(id)), Placeholder.unparsed("health", health(health))));
        return id > 0 ? Command.SINGLE_SUCCESS : 0;
    }

    private static boolean inside(VirtualWorld world, Location at) {
        int chunkX = Math.floorDiv(at.getBlockX(), 16);
        int chunkZ = Math.floorDiv(at.getBlockZ(), 16);
        return chunkX >= world.area().minChunkX() && chunkX <= world.area().maxChunkX()
                && chunkZ >= world.area().minChunkZ() && chunkZ <= world.area().maxChunkZ();
    }

    private int removeAll(CommandContext<CommandSourceStack> context) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        VirtualWorld world = basalt.worldOf(player);
        List<VirtualEntity> removed = world == null ? List.of() : world.entities();
        removed.forEach(VirtualEntity::remove);
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_ENTITY_REMOVED,
                Placeholder.unparsed(PLAYER, player.getName()), Placeholder.unparsed("count", Integer.toString(removed.size()))));
        return Command.SINGLE_SUCCESS;
    }

    private int status(CommandContext<CommandSourceStack> context) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        EntityStatus status = entities.status(player.getUniqueId());
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_ENTITY_STATUS,
                Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("entities", Integer.toString(status.entities())),
                Placeholder.unparsed("last", status.last().map(entity -> Integer.toString(entity.id())).orElse(NONE)),
                Placeholder.unparsed("health", status.last().map(entity -> health(entity.health())).orElse(NONE)),
                Placeholder.unparsed("hits", Integer.toString(status.hits())),
                Placeholder.unparsed("uses", Integer.toString(status.uses()))));
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

    static String health(double health) {
        return health == Math.rint(health) ? Long.toString((long) health) : Double.toString(health);
    }

    private static TagResolver number(String name, double value) {
        return Placeholder.unparsed(name, String.format(Locale.ROOT, "%.2f", value));
    }

    private static DoubleArgumentType horizontal() {
        return DoubleArgumentType.doubleArg(-BlockPosition.MAX_HORIZONTAL, BlockPosition.MAX_HORIZONTAL);
    }

    private static DoubleArgumentType vertical() {
        return DoubleArgumentType.doubleArg(-BlockPosition.MAX_VERTICAL, BlockPosition.MAX_VERTICAL);
    }

    @FunctionalInterface
    private interface Placement {

        Placed place(Player player, VirtualWorld world, Location at);
    }

    private record Placed(MessageKey message, int id) {

        private static Placed refused(MessageKey message) {
            return new Placed(message, 0);
        }
    }
}
