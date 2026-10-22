package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class InteractionService {

    private final InteractionTools tools;
    private final InteractionSettings settings;
    private final Map<UUID, PlayerInteractions> players = new HashMap<>();

    public InteractionService(InteractionTools tools, InteractionSettings settings) {
        this.tools = tools;
        this.settings = settings;
    }

    public InteractionOutcome handle(UUID player, InteractionRequest request) {
        Optional<InteractionContext> context = tools.world().context(player, request.hand());
        if (context.isEmpty()) {
            return InteractionOutcome.ignored(Refusal.PLAYER_GONE);
        }
        PlayerInteractions state = players.computeIfAbsent(player, id -> new PlayerInteractions(settings.maxActionsPerSecond()));
        Optional<Refusal> malformed = InteractionValidator.screen(request, state.lastSequence());
        if (malformed.isPresent()) {
            return InteractionOutcome.ignored(malformed.get());
        }
        if (!state.budget().take(context.get().tick())) {
            return InteractionOutcome.ignored(Refusal.OVER_BUDGET);
        }
        if (request.kind().predictsBlocks()) {
            state.sequenced(request.sequence());
        }
        InteractionOutcome outcome = switch (request.kind()) {
            case START_BREAK -> startBreak(player, request, context.get(), state.breaks());
            case STOP_BREAK -> stopBreak(player, request, context.get(), state.breaks());
            case ABORT_BREAK -> abortBreak(request, context.get(), state.breaks());
            case USE_ON -> useOn(player, request, context.get());
        };
        send(player, outcome);
        return outcome;
    }

    public void tickDelayedBreaks() {
        for (Map.Entry<UUID, PlayerInteractions> entry : players.entrySet()) {
            BreakTracker breaks = entry.getValue().breaks();
            breaks.delayed().ifPresent(position -> tickDelayed(entry.getKey(), breaks, position));
        }
    }

    public void forget(UUID player) {
        players.remove(player);
    }

    int playersTracked() {
        return players.size();
    }

    private InteractionOutcome startBreak(UUID player, InteractionRequest request, InteractionContext context, BreakTracker breaks) {
        BlockPosition position = request.position();
        OptionalInt current = tools.blocks().stateAt(player, position);
        if (current.isEmpty()) {
            return InteractionOutcome.refused(Refusal.NOT_VIRTUAL, List.of(), false, request.sequence());
        }
        if (!inReach(context, position)) {
            return InteractionOutcome.refused(Refusal.OUT_OF_REACH, List.of(), false, request.sequence());
        }
        if (!context.mode().changesBlocks()) {
            return InteractionOutcome.refused(Refusal.GAME_MODE, List.of(new BlockChange(position, current.getAsInt())), false, request.sequence());
        }
        if (tools.world().isAir(current.getAsInt())) {
            return InteractionOutcome.refused(Refusal.AIR, List.of(), false, request.sequence());
        }
        if (context.mode() == GameMode.CREATIVE
                || InteractionValidator.breaksAtOnce(progress(player, position, current.getAsInt(), context))) {
            return breakNow(player, position, request.sequence());
        }
        List<BlockChange> updates = breaks.start(position, context.tick())
                .flatMap(previous -> virtualState(player, previous))
                .map(List::of)
                .orElse(List.of());
        return InteractionOutcome.accepted(List.of(), updates, false, request.sequence());
    }

    private InteractionOutcome stopBreak(UUID player, InteractionRequest request, InteractionContext context, BreakTracker breaks) {
        BlockPosition position = request.position();
        OptionalInt current = tools.blocks().stateAt(player, position);
        if (current.isEmpty()) {
            return InteractionOutcome.refused(Refusal.NOT_VIRTUAL, List.of(), false, request.sequence());
        }
        if (!inReach(context, position)) {
            return InteractionOutcome.refused(Refusal.OUT_OF_REACH, List.of(), false, request.sequence());
        }
        if (tools.world().isAir(current.getAsInt())) {
            return InteractionOutcome.refused(Refusal.AIR, List.of(), false, request.sequence());
        }
        float progress = progress(player, position, current.getAsInt(), context);
        return switch (breaks.stop(position, context.tick(), progress)) {
            case BROKEN -> breakNow(player, position, request.sequence());
            case DELAYED -> InteractionOutcome.refused(Refusal.TOO_EARLY, List.of(), false, request.sequence());
            case NOT_BREAKING -> InteractionOutcome.refused(Refusal.NOT_BREAKING, List.of(), false, request.sequence());
        };
    }

    private InteractionOutcome abortBreak(InteractionRequest request, InteractionContext context, BreakTracker breaks) {
        if (!inReach(context, request.position())) {
            return InteractionOutcome.refused(Refusal.OUT_OF_REACH, List.of(), false, request.sequence());
        }
        breaks.abort();
        return InteractionOutcome.accepted(List.of(), List.of(), false, request.sequence());
    }

    private InteractionOutcome useOn(UUID player, InteractionRequest request, InteractionContext context) {
        BlockPosition clicked = request.position();
        Point hit = request.hit().orElseThrow();
        if (!InteractionValidator.hitInsideBlock(hit, clicked)) {
            return InteractionOutcome.refused(Refusal.HIT_OUTSIDE_BLOCK, List.of(), false, request.sequence());
        }
        if (!inReach(context, clicked)) {
            return InteractionOutcome.refused(Refusal.OUT_OF_REACH, List.of(), false, request.sequence());
        }
        BlockPosition neighbour = clicked.relative(request.face());
        Placement placement = checkPlacement(player, clicked, neighbour, context);
        if (placement.refusal() == null) {
            tools.blocks().set(player, placement.target(), context.heldBlock().getAsInt());
        }
        List<BlockChange> updates = new ArrayList<>();
        virtualState(player, clicked).ifPresent(updates::add);
        virtualState(player, neighbour).ifPresent(updates::add);
        if (placement.refusal() != null) {
            return InteractionOutcome.refused(placement.refusal(), updates, true, request.sequence());
        }
        BlockChange placed = new BlockChange(placement.target(), context.heldBlock().getAsInt());
        return InteractionOutcome.accepted(List.of(placed), updates, true, request.sequence());
    }

    private Placement checkPlacement(UUID player, BlockPosition clicked, BlockPosition neighbour, InteractionContext context) {
        OptionalInt clickedState = tools.blocks().stateAt(player, clicked);
        if (clickedState.isEmpty()) {
            return Placement.refused(Refusal.NOT_VIRTUAL);
        }
        if (context.heldBlock().isEmpty()) {
            return Placement.refused(Refusal.NO_EFFECT);
        }
        if (!context.mode().changesBlocks()) {
            return Placement.refused(Refusal.GAME_MODE);
        }
        BlockPosition target = tools.world().replaceable(clickedState.getAsInt()) ? clicked : neighbour;
        OptionalInt targetState = tools.blocks().stateAt(player, target);
        if (targetState.isEmpty()) {
            return Placement.refused(Refusal.TARGET_NOT_VIRTUAL);
        }
        if (!tools.world().replaceable(targetState.getAsInt())) {
            return Placement.refused(Refusal.NOT_REPLACEABLE);
        }
        if (!tools.world().unobstructed(player, target, context.heldBlock().getAsInt())) {
            return Placement.refused(Refusal.OBSTRUCTED);
        }
        return new Placement(target, null);
    }

    private InteractionOutcome breakNow(UUID player, BlockPosition position, int sequence) {
        int air = tools.world().air();
        tools.blocks().set(player, position, air);
        BlockChange change = new BlockChange(position, air);
        return InteractionOutcome.accepted(List.of(change), List.of(change), false, sequence);
    }

    private void tickDelayed(UUID player, BreakTracker breaks, BlockPosition position) {
        Optional<InteractionContext> context = tools.world().context(player, Hand.MAIN);
        OptionalInt current = tools.blocks().stateAt(player, position);
        if (context.isEmpty() || current.isEmpty() || tools.world().isAir(current.getAsInt())) {
            breaks.clearDelayed();
            return;
        }
        float progress = progress(player, position, current.getAsInt(), context.get());
        if (InteractionValidator.breaksWhenDelayed(progress, context.get().tick() - breaks.delayedSince())) {
            breaks.clearDelayed();
            int air = tools.world().air();
            tools.blocks().set(player, position, air);
            tools.replies().block(player, position, air);
        }
    }

    private boolean inReach(InteractionContext context, BlockPosition position) {
        return InteractionValidator.withinReach(context.stance().eyes(), position, context.stance().reach(), settings.reachMargin());
    }

    private float progress(UUID player, BlockPosition position, int state, InteractionContext context) {
        return tools.world().destroyProgress(player, position, state, context.stance().onGround() || standsOnCopy(player, context.stance()));
    }

    private boolean standsOnCopy(UUID player, PlayerStance stance) {
        Point feet = stance.feet();
        int below = (int) Math.floor(feet.y() - PlayerStance.GROUND_PROBE);
        for (double dx : new double[] {-PlayerStance.HALF_WIDTH, PlayerStance.HALF_WIDTH}) {
            for (double dz : new double[] {-PlayerStance.HALF_WIDTH, PlayerStance.HALF_WIDTH}) {
                BlockPosition support = new BlockPosition((int) Math.floor(feet.x() + dx), below, (int) Math.floor(feet.z() + dz));
                OptionalInt state = tools.blocks().stateAt(player, support);
                if (state.isPresent() && tools.world().supports(state.getAsInt())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Optional<BlockChange> virtualState(UUID player, BlockPosition position) {
        OptionalInt state = tools.blocks().stateAt(player, position);
        return state.isPresent() ? Optional.of(new BlockChange(position, state.getAsInt())) : Optional.empty();
    }

    private void send(UUID player, InteractionOutcome outcome) {
        for (BlockChange update : outcome.updates()) {
            tools.replies().block(player, update.position(), update.state());
        }
        if (outcome.resyncInventory()) {
            tools.replies().resyncInventory(player);
        }
        outcome.ack().ifPresent(sequence -> tools.replies().acknowledge(player, sequence));
    }

    private record Placement(BlockPosition target, Refusal refusal) {

        private static Placement refused(Refusal refusal) {
            return new Placement(null, refusal);
        }
    }
}
