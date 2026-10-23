package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.entity.EntityRefusal;
import tlm.dev.basalt.core.entity.EntityResult;
import tlm.dev.basalt.core.entity.EntitySettings;
import tlm.dev.basalt.core.entity.EntityStatus;
import tlm.dev.basalt.core.entity.VirtualEntity;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

import java.util.Locale;
import java.util.UUID;
import java.util.function.BiFunction;

public final class DebugEntityCommand {

    private static final String PLAYER = "player";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String NONE = "none";

    private final VirtualEntityService entities;
    private final EntitySettings settings;
    private final Server server;
    private final MessageSettings messages;

    public DebugEntityCommand(VirtualEntityService entities, EntitySettings settings, Server server, MessageSettings messages) {
        this.entities = entities;
        this.settings = settings;
        this.server = server;
        this.messages = messages;
    }

    public LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("entity")
                .then(Commands.argument(PLAYER, StringArgumentType.word())
                        .then(Commands.literal("on").then(position(entities::spawn, MessageKey.DEBUG_ENTITY_SPAWNED)))
                        .then(Commands.literal("move").then(position(entities::move, MessageKey.DEBUG_ENTITY_MOVED)))
                        .then(Commands.literal("off").executes(this::removeAll))
                        .then(Commands.literal("status").executes(this::status)));
    }

    private RequiredArgumentBuilder<CommandSourceStack, Double> position(
            BiFunction<UUID, Point, EntityResult> action, MessageKey done) {
        return Commands.argument(X, horizontal())
                .then(Commands.argument(Y, vertical())
                        .then(Commands.argument(Z, horizontal()).executes(context -> place(context, action, done))));
    }

    private int place(CommandContext<CommandSourceStack> context, BiFunction<UUID, Point, EntityResult> action, MessageKey done) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        Point at = new Point(DoubleArgumentType.getDouble(context, X), DoubleArgumentType.getDouble(context, Y), DoubleArgumentType.getDouble(context, Z));
        EntityResult result = action.apply(player.getUniqueId(), at);
        MessageKey message = result.refusal().map(DebugEntityCommand::refusalMessage).orElse(done);
        int id = result.entity().map(VirtualEntity::id).orElse(0);
        context.getSource().getSender().sendMessage(messages.render(message,
                Placeholder.unparsed(PLAYER, player.getName()),
                Placeholder.unparsed("id", Integer.toString(id)),
                Placeholder.unparsed("max", Integer.toString(settings.maxPerPlayer())),
                number(X, at.x()), number(Y, at.y()), number(Z, at.z())));
        return result.entity().isPresent() ? Command.SINGLE_SUCCESS : 0;
    }

    private int removeAll(CommandContext<CommandSourceStack> context) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        int removed = entities.removeAll(player.getUniqueId());
        context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_ENTITY_REMOVED,
                Placeholder.unparsed(PLAYER, player.getName()), Placeholder.unparsed("count", Integer.toString(removed))));
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
                Placeholder.unparsed("health", status.last().map(entity -> Integer.toString(entity.health())).orElse(NONE)),
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

    private static MessageKey refusalMessage(EntityRefusal refusal) {
        return switch (refusal) {
            case NOT_VIRTUAL -> MessageKey.DEBUG_ENTITY_NOT_VIRTUAL;
            case OUTSIDE_VIRTUAL_CHUNK -> MessageKey.DEBUG_ENTITY_OUTSIDE;
            case TOO_MANY -> MessageKey.DEBUG_ENTITY_TOO_MANY;
            case NONE_TO_MOVE -> MessageKey.DEBUG_ENTITY_NONE;
        };
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
}
