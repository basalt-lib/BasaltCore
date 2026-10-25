package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.DoubleUnaryOperator;

final class FakeInteractionEvents implements InteractionEvents {

    final List<String> fired = new ArrayList<>();
    boolean allowBreaks = true;
    boolean allowUses = true;
    boolean allowEntityUses = true;
    boolean cancelDamage;
    PlaceDecision place = new PlaceDecision(true, false);
    DoubleUnaryOperator damage = value -> value;
    Runnable duringBreak = () -> {
    };
    Runnable duringPlace = () -> {
    };

    @Override
    public boolean breaks(UUID player, BlockPosition position, int state) {
        fired.add("break " + at(position) + " = " + state);
        duringBreak.run();
        return allowBreaks;
    }

    @Override
    public PlaceDecision places(UUID player, BlockPosition position, int state, Hand hand) {
        fired.add("place " + at(position) + " = " + state + " " + hand);
        duringPlace.run();
        return place;
    }

    @Override
    public boolean uses(UUID player, BlockPosition position, int state, BlockFace face, Hand hand) {
        fired.add("use " + at(position) + " = " + state + " " + face + " " + hand);
        return allowUses;
    }

    @Override
    public OptionalDouble damages(UUID player, int entity, double damage) {
        fired.add("damage #" + entity + " " + damage);
        return cancelDamage ? OptionalDouble.empty() : OptionalDouble.of(this.damage.applyAsDouble(damage));
    }

    @Override
    public boolean usesEntity(UUID player, int entity, Hand hand) {
        fired.add("use #" + entity + " " + hand);
        return allowEntityUses;
    }

    private static String at(BlockPosition position) {
        return position.x() + " " + position.y() + " " + position.z();
    }
}
