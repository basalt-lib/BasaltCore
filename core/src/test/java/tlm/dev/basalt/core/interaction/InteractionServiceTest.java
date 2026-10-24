package tlm.dev.basalt.core.interaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionServiceTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final BlockPosition BLOCK = new BlockPosition(1029, -58, 1029);
    private static final BlockPosition ABOVE = new BlockPosition(1029, -57, 1029);
    private static final Point TOP = new Point(1029.5, -57.0, 1029.5);
    private static final float BASALT_WITH_WOODEN_PICKAXE = 2.0F / 1.25F / 30;
    private static final int ZOMBIE = 1001;
    private static final int FAR = 1002;
    private static final EntityTarget NEAR_ZOMBIE = new EntityTarget(new Point(1029.5, -60, 1029.5), 0.6, 1.95);

    private final FakeInteractionWorld world = new FakeInteractionWorld();
    private final FakePlayerBlocks blocks = new FakePlayerBlocks();
    private final FakeReplies replies = new FakeReplies();
    private final FakePlayerEntities entities = new FakePlayerEntities();
    private InteractionService service = new InteractionService(new InteractionTools(world, blocks, entities, replies), new InteractionSettings(20, 1.0, 3.0));

    @BeforeEach
    void placeTheBasalt() {
        blocks.put(A, BLOCK, FakeInteractionWorld.BASALT);
        blocks.put(B, BLOCK, FakeInteractionWorld.BASALT);
        world.progress.put(FakeInteractionWorld.BASALT, BASALT_WITH_WOODEN_PICKAXE);
    }

    @Test
    void creativeBreakChangesOnlyTheCopyOfThatPlayer() {
        world.stand(A, GameMode.CREATIVE, 100, OptionalInt.empty());

        InteractionOutcome outcome = service.handle(A, start(BLOCK, 1));

        assertEquals(List.of(new BlockChange(BLOCK, FakeInteractionWorld.AIR)), outcome.changes());
        assertEquals(List.of("block 1029 -58 1029 = 0", "ack 1"), replies.sent);
        assertEquals(OptionalInt.of(FakeInteractionWorld.AIR), blocks.stateAt(A, BLOCK));
        assertEquals(OptionalInt.of(FakeInteractionWorld.BASALT), blocks.stateAt(B, BLOCK));
    }

    @Test
    void survivalBreakFollowsTheThresholdsOfTheServer() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        service.handle(A, start(BLOCK, 1));
        world.at(A, 112);

        InteractionOutcome early = service.handle(A, stop(BLOCK, 2));
        world.at(A, 117);
        service.tickDelayedBreaks();

        assertEquals(Optional.of(Refusal.TOO_EARLY), early.refusal());
        assertEquals(List.of("ack 1", "ack 2"), replies.sent);
        assertEquals(OptionalInt.of(FakeInteractionWorld.BASALT), blocks.stateAt(A, BLOCK));

        world.at(A, 118);
        service.tickDelayedBreaks();
        assertEquals(List.of("ack 1", "ack 2", "block 1029 -58 1029 = 0"), replies.sent);
        assertEquals(OptionalInt.of(FakeInteractionWorld.AIR), blocks.stateAt(A, BLOCK));

        replies.sent.clear();
        blocks.put(A, BLOCK, FakeInteractionWorld.BASALT);
        world.at(A, 200);
        service.handle(A, start(BLOCK, 3));
        world.at(A, 213);
        InteractionOutcome inTime = service.handle(A, stop(BLOCK, 4));
        assertEquals(List.of(new BlockChange(BLOCK, FakeInteractionWorld.AIR)), inTime.changes());
        assertEquals(List.of("ack 3", "block 1029 -58 1029 = 0", "ack 4"), replies.sent);
    }

    @Test
    void countsAPlayerStandingOnTheCopyAsOnTheGround() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        BlockPosition floor = new BlockPosition(1030, -58, 1027);
        blocks.put(A, floor, FakeInteractionWorld.BASALT);
        blocks.put(A, new BlockPosition(1031, -58, 1027), FakeInteractionWorld.SHORT_GRASS);
        world.airborne(A, new Point(1030.75, -57.0, 1027.5));

        service.handle(A, start(BLOCK, 1));
        world.at(A, 113);

        assertEquals(List.of(new BlockChange(BLOCK, FakeInteractionWorld.AIR)), service.handle(A, stop(BLOCK, 2)).changes());
    }

    @Test
    void slowsAPlayerWithNothingUnderItsFeet() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        blocks.put(A, new BlockPosition(1031, -58, 1027), FakeInteractionWorld.SHORT_GRASS);
        world.airborne(A, new Point(1031.5, -57.0, 1027.5));

        service.handle(A, start(BLOCK, 1));
        world.at(A, 113);

        assertEquals(Optional.of(Refusal.TOO_EARLY), service.handle(A, stop(BLOCK, 2)).refusal());
    }

    @Test
    void breaksAtOnceWhenOneTickIsEnough() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        world.progress.put(FakeInteractionWorld.BASALT, 1.0F);

        service.handle(A, start(BLOCK, 1));

        assertEquals(List.of("block 1029 -58 1029 = 0", "ack 1"), replies.sent);
    }

    @Test
    void answersARefusedBreakWithTheAckOnly() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());

        assertEquals(Optional.of(Refusal.OUT_OF_REACH), service.handle(A, start(new BlockPosition(1029, -58, 1035), 1)).refusal());
        assertEquals(Optional.of(Refusal.AIR), service.handle(A, start(ABOVE, 2)).refusal());
        assertEquals(Optional.of(Refusal.NOT_BREAKING), service.handle(A, stop(BLOCK, 3)).refusal());
        assertEquals(Optional.of(Refusal.NOT_VIRTUAL), service.handle(A, start(new BlockPosition(1029, 320, 1029), 4)).refusal());
        assertEquals(List.of("ack 1", "ack 2", "ack 3", "ack 4"), replies.sent);
    }

    @Test
    void sendsTheBlockBackToASpectator() {
        world.stand(A, GameMode.SPECTATOR, 100, OptionalInt.empty());

        assertEquals(Optional.of(Refusal.GAME_MODE), service.handle(A, start(BLOCK, 1)).refusal());

        assertEquals(List.of("block 1029 -58 1029 = 2", "ack 1"), replies.sent);
    }

    @Test
    void placesTheHeldBlockOnTheClickedFaceAndResyncsTheInventory() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.of(FakeInteractionWorld.STONE));

        InteractionOutcome outcome = service.handle(A, use(BLOCK, BlockFace.UP, TOP, 1));

        assertEquals(List.of(new BlockChange(ABOVE, FakeInteractionWorld.STONE)), outcome.changes());
        assertEquals(List.of("block 1029 -58 1029 = 2", "block 1029 -57 1029 = 3", "inventory", "ack 1"), replies.sent);
        assertEquals(OptionalInt.of(FakeInteractionWorld.AIR), blocks.stateAt(B, ABOVE));
    }

    @Test
    void replacesAReplaceableClickedBlock() {
        world.stand(A, GameMode.CREATIVE, 100, OptionalInt.of(FakeInteractionWorld.STONE));
        blocks.put(A, ABOVE, FakeInteractionWorld.SHORT_GRASS);

        service.handle(A, use(ABOVE, BlockFace.UP, new Point(1029.5, -56.0, 1029.5), 1));

        assertEquals(OptionalInt.of(FakeInteractionWorld.STONE), blocks.stateAt(A, ABOVE));
        assertEquals(List.of("block 1029 -57 1029 = 3", "block 1029 -56 1029 = 0", "inventory", "ack 1"), replies.sent);
    }

    @Test
    void refusesAPlacementOutOfTheCopyIntoAPlayerOrIntoABlock() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.of(FakeInteractionWorld.STONE));
        world.eyes(A, new Point(1038.5, -58.38, 1029.5));
        BlockPosition eastEdge = new BlockPosition(1039, -58, 1029);
        blocks.put(A, eastEdge, FakeInteractionWorld.BASALT);

        assertEquals(Optional.of(Refusal.TARGET_NOT_VIRTUAL), service.handle(A, use(eastEdge, BlockFace.EAST, new Point(1040.0, -57.5, 1029.5), 1)).refusal());
        assertEquals(List.of("block 1039 -58 1029 = 2", "inventory", "ack 1"), replies.sent);

        replies.sent.clear();
        world.eyes(A, new Point(1024.5, -58.38, 1029.5));
        BlockPosition realWest = new BlockPosition(1023, -58, 1029);
        assertEquals(Optional.of(Refusal.NOT_VIRTUAL), service.handle(A, use(realWest, BlockFace.EAST, new Point(1024.0, -57.5, 1029.5), 2)).refusal());
        assertEquals(List.of("block 1024 -58 1029 = 0", "inventory", "ack 2"), replies.sent);

        world.eyes(A, new Point(1029.5, -58.38, 1027.5));
        world.occupied.add(ABOVE);
        assertEquals(Optional.of(Refusal.OBSTRUCTED), service.handle(A, use(BLOCK, BlockFace.UP, TOP, 3)).refusal());
        world.occupied.clear();
        blocks.put(A, ABOVE, FakeInteractionWorld.BASALT);
        assertEquals(Optional.of(Refusal.NOT_REPLACEABLE), service.handle(A, use(BLOCK, BlockFace.UP, TOP, 4)).refusal());
        assertEquals(OptionalInt.of(FakeInteractionWorld.BASALT), blocks.stateAt(A, ABOVE));
        assertEquals(OptionalInt.of(FakeInteractionWorld.AIR), blocks.stateAt(A, new BlockPosition(1024, -58, 1029)));
    }

    @Test
    void usingWithoutABlockOnlyAnswers() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());

        InteractionOutcome outcome = service.handle(A, use(BLOCK, BlockFace.UP, TOP, 1));

        assertEquals(Optional.of(Refusal.NO_EFFECT), outcome.refusal());
        assertEquals(List.of("block 1029 -58 1029 = 2", "block 1029 -57 1029 = 0", "inventory", "ack 1"), replies.sent);
    }

    @Test
    void answersAHitOutsideTheBlockWithTheAckOnly() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.of(FakeInteractionWorld.STONE));

        assertEquals(Optional.of(Refusal.HIT_OUTSIDE_BLOCK), service.handle(A, use(BLOCK, BlockFace.UP, new Point(1029.5, -55.0, 1029.5), 1)).refusal());

        assertEquals(List.of("ack 1"), replies.sent);
        assertEquals(OptionalInt.of(FakeInteractionWorld.AIR), blocks.stateAt(A, ABOVE));
    }

    @Test
    void ignoresMalformedRequestsWithoutAnyReply() {
        world.stand(A, GameMode.CREATIVE, 100, OptionalInt.empty());

        assertEquals(Optional.of(Refusal.MALFORMED), service.handle(A, start(BLOCK, -1)).refusal());
        assertEquals(Optional.of(Refusal.MALFORMED), service.handle(A, use(BLOCK, BlockFace.UP, new Point(Double.NaN, -57, 1029.5), 5)).refusal());
        service.handle(A, start(BLOCK, 5));
        replies.sent.clear();
        blocks.put(A, BLOCK, FakeInteractionWorld.BASALT);
        assertEquals(Optional.of(Refusal.MALFORMED), service.handle(A, start(BLOCK, 4)).refusal());
        assertEquals(Optional.of(Refusal.MALFORMED), service.handle(A, start(BLOCK, 5)).refusal());

        assertEquals(List.of(), replies.sent);
        assertEquals(OptionalInt.of(FakeInteractionWorld.BASALT), blocks.stateAt(A, BLOCK));
    }

    @Test
    void ignoresABurstOverTheBudget() {
        service = new InteractionService(new InteractionTools(world, blocks, entities, replies), new InteractionSettings(2, 1.0, 3.0));
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());

        service.handle(A, use(BLOCK, BlockFace.UP, TOP, 1));
        service.handle(A, use(BLOCK, BlockFace.UP, TOP, 2));
        InteractionOutcome third = service.handle(A, use(BLOCK, BlockFace.UP, TOP, 3));

        assertEquals(Optional.of(Refusal.OVER_BUDGET), third.refusal());
        assertEquals(2, replies.sent.stream().filter(line -> line.startsWith("ack")).count());
    }

    @Test
    void keepsNothingForARequestThatOutlivedItsPlayer() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        service.handle(A, start(BLOCK, 1));
        service.forget(A);
        world.contexts.remove(A);

        service.handle(A, stop(BLOCK, 2));

        assertEquals(0, service.playersTracked());
    }

    @Test
    void ignoresAPlayerWhoLeftAndForgetsBreaksInProgress() {
        assertEquals(Optional.of(Refusal.PLAYER_GONE), service.handle(A, start(BLOCK, 1)).refusal());

        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        service.handle(A, start(BLOCK, 1));
        service.forget(A);
        world.at(A, 200);

        assertEquals(Optional.of(Refusal.NOT_BREAKING), service.handle(A, stop(BLOCK, 2)).refusal());
    }

    @Test
    void attacksAndUsesAVirtualEntityInReachWithoutAnyReplyOfItsOwn() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        entities.targets.put(ZOMBIE, NEAR_ZOMBIE);

        assertEquals(Optional.empty(), service.handle(A, EntityRequest.attack(ZOMBIE)).refusal());
        assertEquals(Optional.empty(), service.handle(A, EntityRequest.interact(ZOMBIE, Hand.OFF)).refusal());
        assertEquals(Optional.empty(), service.handle(A, EntityRequest.interactAt(ZOMBIE, Hand.MAIN, new Point(0.1, 1.2, -0.3))).refusal());

        assertEquals(List.of("attack #1001 for 1.0", "use #1001", "use #1001"), entities.calls);
        assertEquals(List.of(), replies.sent);
    }

    @Test
    void refusesEntityInteractionsWithoutEffect() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        entities.targets.put(ZOMBIE, NEAR_ZOMBIE);
        entities.targets.put(FAR, new EntityTarget(new Point(1029.5, -60, 1040.5), 0.6, 1.95));

        assertEquals(Optional.of(Refusal.NO_ENTITY), service.handle(A, EntityRequest.attack(4242)).refusal());
        assertEquals(Optional.of(Refusal.OUT_OF_REACH), service.handle(A, EntityRequest.attack(FAR)).refusal());
        assertEquals(Optional.of(Refusal.MALFORMED), service.handle(A, EntityRequest.interactAt(ZOMBIE, Hand.MAIN, new Point(Double.NaN, 0, 0))).refusal());
        assertEquals(Optional.of(Refusal.MALFORMED), service.handle(A, EntityRequest.interactAt(ZOMBIE, Hand.MAIN, new Point(0, 65, 0))).refusal());
        entities.nextHit = EntityHit.TOO_SOON;
        assertEquals(Optional.of(Refusal.HIT_TOO_SOON), service.handle(A, EntityRequest.attack(ZOMBIE)).refusal());
        entities.nextHit = EntityHit.GONE;
        assertEquals(Optional.of(Refusal.NO_ENTITY), service.handle(A, EntityRequest.attack(ZOMBIE)).refusal());
        world.stand(A, GameMode.SPECTATOR, 100, OptionalInt.empty());
        assertEquals(Optional.of(Refusal.GAME_MODE), service.handle(A, EntityRequest.attack(ZOMBIE)).refusal());
        assertEquals(Optional.of(Refusal.PLAYER_GONE), service.handle(B, EntityRequest.attack(ZOMBIE)).refusal());

        assertEquals(List.of("attack #1001 for 1.0", "attack #1001 for 1.0"), entities.calls);
        assertEquals(List.of(), replies.sent);
    }

    @Test
    void hitsWithTheAttackDamageOfThePlayerAndAtLeastOne() {
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        entities.targets.put(ZOMBIE, NEAR_ZOMBIE);

        world.holding(A, 7.0);
        service.handle(A, EntityRequest.attack(ZOMBIE));
        world.holding(A, 0.25);
        service.handle(A, EntityRequest.attack(ZOMBIE));

        assertEquals(List.of("attack #1001 for 7.0", "attack #1001 for 1.0"), entities.calls);
    }

    @Test
    void sharesTheBudgetBetweenBlocksAndEntities() {
        service = new InteractionService(new InteractionTools(world, blocks, entities, replies), new InteractionSettings(2, 1.0, 3.0));
        world.stand(A, GameMode.SURVIVAL, 100, OptionalInt.empty());
        entities.targets.put(ZOMBIE, NEAR_ZOMBIE);

        service.handle(A, use(BLOCK, BlockFace.UP, TOP, 1));
        service.handle(A, EntityRequest.attack(ZOMBIE));

        assertEquals(Optional.of(Refusal.OVER_BUDGET), service.handle(A, EntityRequest.attack(ZOMBIE)).refusal());
        assertEquals(List.of("attack #1001 for 1.0"), entities.calls);
    }

    @Test
    void reachesAnEntityByItsBoxWithTheEntityRangeAndItsMargin() {
        EntityTarget zombie = new EntityTarget(new Point(0.5, 0, 0.5), 0.6, 1.95);

        assertEquals(0.0, zombie.distanceSquaredFrom(new Point(0.5, 1.62, 0.7)));
        assertEquals(4.0, zombie.distanceSquaredFrom(new Point(2.8, 1.0, 0.5)), 1e-9);
        assertTrue(InteractionValidator.withinEntityReach(new Point(6.7, 1.0, 0.5), zombie, 3.0, 3.0));
        assertFalse(InteractionValidator.withinEntityReach(new Point(6.81, 1.0, 0.5), zombie, 3.0, 3.0));
    }

    private static InteractionRequest start(BlockPosition position, int sequence) {
        return BlockRequest.breaking(InteractionKind.START_BREAK, position, BlockFace.NORTH, sequence);
    }

    private static InteractionRequest stop(BlockPosition position, int sequence) {
        return BlockRequest.breaking(InteractionKind.STOP_BREAK, position, BlockFace.NORTH, sequence);
    }

    private static InteractionRequest use(BlockPosition position, BlockFace face, Point hit, int sequence) {
        return BlockRequest.using(position, face, hit, Hand.MAIN, sequence);
    }
}
