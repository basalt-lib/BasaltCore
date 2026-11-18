package tlm.dev.basalt.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.ChunkArea;
import tlm.dev.basalt.api.Overlay;
import tlm.dev.basalt.api.OverlayEntity;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.plugin.config.MessageKey;
import tlm.dev.basalt.plugin.config.MessageSettings;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class DebugOverlayCommand implements DebugBranch {

    private static final String NAME = "name";
    private static final String PLAYER = "player";
    private static final String OVERLAY = "overlay";
    private static final String BLOCK = "block";
    private static final String ITEM = "item";
    private static final String REASON = "reason";
    private static final int LID_ACTION = 1;
    private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final List<String> CORNERS = List.of("cx1", "cz1", "cx2", "cz2");
    private static final List<String> AXES = List.of("x", "y", "z");

    private final Basalt basalt;
    private final Plugin plugin;
    private final MessageSettings messages;
    private final DebugOverlays named;

    public DebugOverlayCommand(Basalt basalt, Plugin plugin, MessageSettings messages, DebugOverlays named) {
        this.basalt = basalt;
        this.plugin = plugin;
        this.messages = messages;
        this.named = named;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> branch() {
        return Commands.literal("overlay")
                .then(Commands.literal("create").then(Commands.argument(NAME, StringArgumentType.word()).then(corners())))
                .then(Commands.literal(BLOCK).then(named(at(z -> z.then(Commands.argument(BLOCK, StringArgumentType.greedyString())
                        .executes(this::block))))))
                .then(Commands.literal("clear").then(named(at(z -> z.executes(this::clear)))))
                .then(Commands.literal("lid").then(named(at(z -> z
                        .then(Commands.literal("open").executes(context -> lid(context, true)))
                        .then(Commands.literal("close").executes(context -> lid(context, false)))))))
                .then(Commands.literal(ITEM).then(named(at(z -> z.then(Commands.argument(ITEM, StringArgumentType.word())
                        .executes(context -> item(context, EntityType.ITEM))
                        .then(Commands.literal("display").executes(context -> item(context, EntityType.ITEM_DISPLAY))))))))
                .then(Commands.literal("add").then(Commands.argument(NAME, StringArgumentType.word())
                        .then(Commands.argument(PLAYER, StringArgumentType.word()).executes(this::add))))
                .then(Commands.literal("remove").then(Commands.argument(PLAYER, StringArgumentType.word()).executes(this::remove)))
                .then(Commands.literal("breaks").then(Commands.argument(NAME, StringArgumentType.word())
                        .then(Commands.literal("keep").executes(context -> breaks(context, true)))
                        .then(Commands.literal("remove").executes(context -> breaks(context, false)))))
                .then(Commands.literal("close").then(Commands.argument(NAME, StringArgumentType.word()).executes(this::close)))
                .then(Commands.literal("info").then(Commands.argument(NAME, StringArgumentType.word()).executes(this::info)))
                .then(Commands.literal("list").executes(this::list));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> named(ArgumentBuilder<CommandSourceStack, ?> position) {
        return Commands.argument(NAME, StringArgumentType.word()).then(position);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> at(UnaryOperator<RequiredArgumentBuilder<CommandSourceStack, Integer>> last) {
        RequiredArgumentBuilder<CommandSourceStack, Integer> z = last.apply(Commands.argument(AXES.get(2), horizontal()));
        return Commands.argument(AXES.get(0), horizontal()).then(Commands.argument(AXES.get(1), vertical()).then(z));
    }

    private RequiredArgumentBuilder<CommandSourceStack, Integer> corners() {
        return Commands.argument(CORNERS.get(0), chunk())
                .then(Commands.argument(CORNERS.get(1), chunk())
                        .then(Commands.argument(CORNERS.get(2), chunk())
                                .then(Commands.argument(CORNERS.get(3), chunk()).executes(this::create))));
    }

    private int create(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        if (!VALID_NAME.matcher(name).matches()) {
            return reply(sender, MessageKey.DEBUG_OVERLAY_BAD_NAME, Placeholder.unparsed(OVERLAY, name));
        }
        if (named.live(name).isPresent()) {
            return reply(sender, MessageKey.DEBUG_OVERLAY_EXISTS, Placeholder.unparsed(OVERLAY, name));
        }
        int x1 = IntegerArgumentType.getInteger(context, CORNERS.get(0));
        int z1 = IntegerArgumentType.getInteger(context, CORNERS.get(1));
        int x2 = IntegerArgumentType.getInteger(context, CORNERS.get(2));
        int z2 = IntegerArgumentType.getInteger(context, CORNERS.get(3));
        World world = SourceWorld.of(context.getSource());
        ChunkArea area = new ChunkArea(world, Math.min(x1, x2), Math.min(z1, z2), Math.max(x1, x2), Math.max(z1, z2));
        Overlay overlay;
        try {
            overlay = basalt.createOverlay(plugin, area);
        } catch (IllegalArgumentException | IllegalStateException refused) {
            return refused(sender, name, refused);
        }
        named.put(name, overlay);
        sender.sendMessage(messages.render(MessageKey.DEBUG_OVERLAY_CREATED, Placeholder.unparsed(OVERLAY, name), area(area),
                Placeholder.unparsed("real", world.getName())));
        return Command.SINGLE_SUCCESS;
    }

    private int block(CommandContext<CommandSourceStack> context) {
        return onOverlay(context, overlay -> {
            BlockData data = plugin.getServer().createBlockData(StringArgumentType.getString(context, BLOCK));
            int[] at = position(context);
            overlay.setBlock(at[0], at[1], at[2], data);
            return messages.render(MessageKey.DEBUG_OVERLAY_BLOCK, Placeholder.unparsed(OVERLAY, nameOf(context)),
                    Placeholder.unparsed(BLOCK, data.getAsString()), position(at));
        });
    }

    private int clear(CommandContext<CommandSourceStack> context) {
        return onOverlay(context, overlay -> {
            int[] at = position(context);
            overlay.removeBlock(at[0], at[1], at[2]);
            return messages.render(MessageKey.DEBUG_OVERLAY_CLEARED, Placeholder.unparsed(OVERLAY, nameOf(context)), position(at));
        });
    }

    private int lid(CommandContext<CommandSourceStack> context, boolean open) {
        return onOverlay(context, overlay -> {
            int[] at = position(context);
            overlay.playBlockAction(at[0], at[1], at[2], LID_ACTION, open ? 1 : 0);
            return messages.render(MessageKey.DEBUG_OVERLAY_LID, Placeholder.unparsed(OVERLAY, nameOf(context)),
                    Placeholder.unparsed("state", open ? "open" : "close"), position(at));
        });
    }

    private int item(CommandContext<CommandSourceStack> context, EntityType type) {
        return onOverlay(context, overlay -> {
            String itemName = StringArgumentType.getString(context, ITEM);
            Material material = Material.matchMaterial(itemName);
            if (material == null || material.isAir()) {
                throw new IllegalArgumentException(itemName + " is not an item");
            }
            int[] at = position(context);
            OverlayEntity entity = overlay.spawnEntity(type, new Location(overlay.area().world(), at[0] + 0.5, at[1], at[2] + 0.5));
            entity.setItem(new ItemStack(material));
            return messages.render(MessageKey.DEBUG_OVERLAY_ITEM, Placeholder.unparsed(OVERLAY, nameOf(context)),
                    Placeholder.unparsed(ITEM, material.getKey().toString()), Placeholder.unparsed("id", Integer.toString(entity.id())),
                    Placeholder.unparsed("type", type.getKey().getKey()), position(at));
        });
    }

    private int add(CommandContext<CommandSourceStack> context) {
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        return onOverlay(context, overlay -> {
            overlay.addViewer(player);
            return messages.render(MessageKey.DEBUG_OVERLAY_ADDED, Placeholder.unparsed(OVERLAY, nameOf(context)),
                    Placeholder.unparsed(PLAYER, player.getName()));
        });
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Player player = onlinePlayer(context);
        if (player == null) {
            return 0;
        }
        Overlay overlay = basalt.overlayOf(player);
        if (overlay == null) {
            return reply(sender, MessageKey.DEBUG_OVERLAY_NOT_VIEWING, Placeholder.unparsed(PLAYER, player.getName()));
        }
        overlay.removeViewer(player);
        sender.sendMessage(messages.render(MessageKey.DEBUG_OVERLAY_REMOVED, Placeholder.unparsed(OVERLAY, named.nameOf(overlay)),
                Placeholder.unparsed(PLAYER, player.getName())));
        return Command.SINGLE_SUCCESS;
    }

    private int breaks(CommandContext<CommandSourceStack> context, boolean keep) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        Optional<DebugOverlays.Entry> entry = named.entry(name);
        if (entry.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_OVERLAY_UNKNOWN, Placeholder.unparsed(OVERLAY, name));
        }
        entry.get().keepBlocks(keep);
        sender.sendMessage(messages.render(MessageKey.DEBUG_OVERLAY_BREAKS, Placeholder.unparsed(OVERLAY, name),
                Placeholder.unparsed("mode", keep ? "keep" : "remove")));
        return Command.SINGLE_SUCCESS;
    }

    private int close(CommandContext<CommandSourceStack> context) {
        return onOverlay(context, overlay -> {
            overlay.close();
            named.remove(nameOf(context));
            return messages.render(MessageKey.DEBUG_OVERLAY_CLOSED, Placeholder.unparsed(OVERLAY, nameOf(context)));
        });
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String name = StringArgumentType.getString(context, NAME);
        Optional<DebugOverlays.Entry> entry = named.entry(name);
        if (entry.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_OVERLAY_UNKNOWN, Placeholder.unparsed(OVERLAY, name));
        }
        DebugOverlays.Entry found = entry.get();
        sender.sendMessage(messages.render(MessageKey.DEBUG_OVERLAY_INFO, describe(name, found.overlay()),
                Placeholder.unparsed("uses", Integer.toString(found.uses())),
                Placeholder.unparsed("breaks", Integer.toString(found.breaks())),
                Placeholder.unparsed("entityuses", Integer.toString(found.entityUses())),
                Placeholder.unparsed("mode", found.keepsBlocks() ? "keep" : "remove"),
                Placeholder.unparsed("last", found.last())));
        return Command.SINGLE_SUCCESS;
    }

    private int list(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        List<Overlay> open = basalt.overlays().stream()
                .sorted(Comparator.comparing((Overlay overlay) -> overlay.owner().getName()).thenComparing(overlay -> overlay.id().toString()))
                .toList();
        sender.sendMessage(messages.render(MessageKey.DEBUG_OVERLAY_LIST, Placeholder.unparsed("overlays", Integer.toString(open.size()))));
        open.forEach(overlay -> sender.sendMessage(messages.render(MessageKey.DEBUG_OVERLAY_LIST_ENTRY, describe(named.nameOf(overlay), overlay))));
        return Command.SINGLE_SUCCESS;
    }

    private TagResolver describe(String name, Overlay overlay) {
        List<String> viewers = overlay.viewers().stream().map(Player::getName).sorted().toList();
        return TagResolver.resolver(Placeholder.unparsed(OVERLAY, name),
                Placeholder.unparsed("id", overlay.id().toString()), area(overlay.area()),
                Placeholder.unparsed("blocks", Integer.toString(overlay.blockCount())),
                Placeholder.unparsed("entities", Integer.toString(overlay.entities().size())),
                Placeholder.unparsed("count", Integer.toString(viewers.size())),
                Placeholder.unparsed("viewers", viewers.isEmpty() ? "none" : String.join(",", viewers)),
                Placeholder.unparsed("owner", overlay.owner().getName()),
                Placeholder.unparsed("real", overlay.area().world().getName()));
    }

    private int onOverlay(CommandContext<CommandSourceStack> context, OverlayAction action) {
        CommandSender sender = context.getSource().getSender();
        String name = nameOf(context);
        Optional<Overlay> overlay = named.live(name);
        if (overlay.isEmpty()) {
            return reply(sender, MessageKey.DEBUG_OVERLAY_UNKNOWN, Placeholder.unparsed(OVERLAY, name));
        }
        try {
            sender.sendMessage(action.run(overlay.get()));
        } catch (IllegalArgumentException | IllegalStateException refused) {
            return refused(sender, name, refused);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int refused(CommandSender sender, String name, RuntimeException refused) {
        return reply(sender, MessageKey.DEBUG_OVERLAY_REFUSED, Placeholder.unparsed(OVERLAY, name),
                Placeholder.unparsed(REASON, String.valueOf(refused.getMessage())));
    }

    private Player onlinePlayer(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, PLAYER);
        Player player = plugin.getServer().getPlayerExact(name);
        if (player == null) {
            context.getSource().getSender().sendMessage(messages.render(MessageKey.DEBUG_CHUNK_PLAYER_OFFLINE, Placeholder.unparsed(PLAYER, name)));
        }
        return player;
    }

    private int reply(CommandSender sender, MessageKey key, TagResolver... resolvers) {
        sender.sendMessage(messages.render(key, resolvers));
        return 0;
    }

    private static String nameOf(CommandContext<CommandSourceStack> context) {
        return StringArgumentType.getString(context, NAME);
    }

    private static int[] position(CommandContext<CommandSourceStack> context) {
        return new int[] {IntegerArgumentType.getInteger(context, AXES.get(0)), IntegerArgumentType.getInteger(context, AXES.get(1)),
                IntegerArgumentType.getInteger(context, AXES.get(2))};
    }

    private static TagResolver position(int[] at) {
        return TagResolver.resolver(Placeholder.unparsed("x", Integer.toString(at[0])), Placeholder.unparsed("y", Integer.toString(at[1])),
                Placeholder.unparsed("z", Integer.toString(at[2])));
    }

    private static TagResolver area(ChunkArea area) {
        return TagResolver.resolver(
                Placeholder.unparsed("x1", Integer.toString(area.minChunkX())),
                Placeholder.unparsed("z1", Integer.toString(area.minChunkZ())),
                Placeholder.unparsed("x2", Integer.toString(area.maxChunkX())),
                Placeholder.unparsed("z2", Integer.toString(area.maxChunkZ())));
    }

    private static IntegerArgumentType chunk() {
        return IntegerArgumentType.integer(-ChunkArea.MAX_CHUNK_COORDINATE, ChunkArea.MAX_CHUNK_COORDINATE);
    }

    private static IntegerArgumentType horizontal() {
        return IntegerArgumentType.integer(-BlockPosition.MAX_HORIZONTAL, BlockPosition.MAX_HORIZONTAL);
    }

    private static IntegerArgumentType vertical() {
        return IntegerArgumentType.integer(-BlockPosition.MAX_VERTICAL, BlockPosition.MAX_VERTICAL);
    }

    private interface OverlayAction {

        Component run(Overlay overlay);
    }
}
