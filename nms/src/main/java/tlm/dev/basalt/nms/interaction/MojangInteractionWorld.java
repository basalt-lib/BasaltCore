package tlm.dev.basalt.nms.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bukkit.Server;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.interaction.GameMode;
import tlm.dev.basalt.core.interaction.Hand;
import tlm.dev.basalt.core.interaction.HeldItem;
import tlm.dev.basalt.core.interaction.InteractionContext;
import tlm.dev.basalt.core.interaction.InteractionWorld;
import tlm.dev.basalt.core.interaction.PlayerStance;
import tlm.dev.basalt.core.interaction.Reach;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class MojangInteractionWorld implements InteractionWorld {

    private static final int AIR = Block.getId(Blocks.AIR.defaultBlockState());

    private final Server server;

    public MojangInteractionWorld(Server server) {
        this.server = server;
    }

    @Override
    public Optional<InteractionContext> context(UUID player, Hand hand) {
        return handle(player).map(online -> {
            Vec3 eyes = online.getEyePosition();
            GameMode mode = switch (online.gameMode.getGameModeForPlayer()) {
                case SURVIVAL -> GameMode.SURVIVAL;
                case CREATIVE -> GameMode.CREATIVE;
                case ADVENTURE -> GameMode.ADVENTURE;
                case SPECTATOR -> GameMode.SPECTATOR;
            };
            ItemStack held = online.getItemInHand(handOf(hand));
            PlayerStance stance = new PlayerStance(new Point(eyes.x, eyes.y, eyes.z),
                    new Point(online.getX(), online.getY(), online.getZ()), online.onGround(),
                    new Reach(online.blockInteractionRange(), online.entityInteractionRange()));
            HeldItem item = new HeldItem(placeableBlock(held), online.getAttributeValue(Attributes.ATTACK_DAMAGE));
            return new InteractionContext(online.level().getLagCompensationTick(), mode, stance, item);
        });
    }

    @Override
    public float destroyProgress(UUID player, BlockPosition position, int state, boolean standing) {
        return handle(player).map(online -> {
            boolean simulated = online.onGround;
            online.onGround = standing;
            try {
                return Block.stateById(state).getDestroyProgress(online, online.level(), blockPos(position));
            } finally {
                online.onGround = simulated;
            }
        }).orElse(0.0F);
    }

    @Override
    public void consume(UUID player, Hand hand) {
        handle(player).ifPresent(online -> online.getItemInHand(handOf(hand)).shrink(1));
    }

    @Override
    public int air() {
        return AIR;
    }

    @Override
    public boolean isAir(int state) {
        return Block.stateById(state).isAir();
    }

    @Override
    public boolean supports(int state) {
        return !Block.stateById(state).getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
    }

    @Override
    public boolean replaceable(int state) {
        return Block.stateById(state).canBeReplaced();
    }

    @Override
    public boolean unobstructed(UUID player, BlockPosition position, int state) {
        return handle(player).map(online -> online.level().checkEntityCollision(
                Block.stateById(state), online, CollisionContext.placementContext(online), blockPos(position), true)).orElse(false);
    }

    private Optional<ServerPlayer> handle(UUID player) {
        Player online = server.getPlayer(player);
        return online == null ? Optional.empty() : Optional.of(((CraftPlayer) online).getHandle());
    }

    private static OptionalInt placeableBlock(ItemStack held) {
        if (held.isEmpty() || !(held.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() instanceof EntityBlock) {
            return OptionalInt.empty();
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        if (!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Block.getId(state));
    }

    private static InteractionHand handOf(Hand hand) {
        return hand == Hand.MAIN ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static BlockPos blockPos(BlockPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }
}
