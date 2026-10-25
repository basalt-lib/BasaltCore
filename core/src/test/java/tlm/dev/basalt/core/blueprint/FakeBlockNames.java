package tlm.dev.basalt.core.blueprint;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

final class FakeBlockNames implements BlockNames {

    static final int AIR = 0;
    static final int STONE = 1;
    static final int STAIRS = 2;
    static final int GOLD = 3;
    static final int NUMBERED = 1000;

    private final Map<String, Integer> states = new HashMap<>(Map.of(
            "minecraft:air", AIR,
            "minecraft:stone", STONE,
            "minecraft:oak_stairs[facing=east,half=bottom]", STAIRS,
            "minecraft:gold_block", GOLD));

    @Override
    public OptionalInt stateOf(String blockState) {
        Integer state = states.get(blockState.contains(":") ? blockState : "minecraft:" + blockState);
        if (state != null) {
            return OptionalInt.of(state);
        }
        if (blockState.startsWith("minecraft:numbered_")) {
            return OptionalInt.of(NUMBERED + Integer.parseInt(blockState.substring("minecraft:numbered_".length())));
        }
        return OptionalInt.empty();
    }

    @Override
    public boolean isAir(int state) {
        return state == AIR;
    }
}
