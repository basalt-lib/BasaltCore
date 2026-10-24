package tlm.dev.basalt.core.chunk;

import tlm.dev.basalt.core.debug.DebugPattern;

import java.util.List;

public final class DebugChunkPattern {

    public static final String BEDROCK = "minecraft:bedrock";
    public static final String BASALT = "minecraft:basalt";
    public static final String SMOOTH_BASALT = "minecraft:smooth_basalt";
    public static final String GOLD_BLOCK = "minecraft:gold_block";
    public static final String DIAMOND_BLOCK = "minecraft:diamond_block";

    private static final int PILLAR = 8;
    private static final int PILLAR_HEIGHT = 3;
    private static final int PLATFORM_LEVEL = 10;
    private static final int OPENING_X = 15;
    private static final int OPENING_FIRST_Z = 6;
    private static final int OPENING_LAST_Z = 9;

    private DebugChunkPattern() {
    }

    public static List<PatternBox> boxes(DebugPattern pattern, int bottom) {
        return switch (pattern) {
            case FLAT -> flat(bottom);
            case RAISED -> raised(bottom);
        };
    }

    private static List<PatternBox> flat(int bottom) {
        int rim = bottom + 4;
        return List.of(
                layer(bottom, bottom, BEDROCK),
                layer(bottom + 1, bottom + 2, BASALT),
                layer(bottom + 3, bottom + 3, SMOOTH_BASALT),
                new PatternBox(new LocalBox(0, rim, 0, 15, rim, 0), GOLD_BLOCK),
                new PatternBox(new LocalBox(0, rim, 15, 15, rim, 15), GOLD_BLOCK),
                new PatternBox(new LocalBox(0, rim, 1, 0, rim, 14), GOLD_BLOCK),
                new PatternBox(new LocalBox(15, rim, 1, 15, rim, 14), GOLD_BLOCK),
                new PatternBox(new LocalBox(PILLAR, rim, PILLAR, PILLAR, rim + PILLAR_HEIGHT - 1, PILLAR), DIAMOND_BLOCK));
    }

    private static List<PatternBox> raised(int bottom) {
        int platform = bottom + PLATFORM_LEVEL;
        int rim = platform + 1;
        return List.of(
                layer(platform, platform, SMOOTH_BASALT),
                new PatternBox(new LocalBox(0, rim, 0, 15, rim, 0), GOLD_BLOCK),
                new PatternBox(new LocalBox(0, rim, 15, 15, rim, 15), GOLD_BLOCK),
                new PatternBox(new LocalBox(0, rim, 1, 0, rim, 14), GOLD_BLOCK),
                new PatternBox(new LocalBox(OPENING_X, rim, 1, OPENING_X, rim, OPENING_FIRST_Z - 1), GOLD_BLOCK),
                new PatternBox(new LocalBox(OPENING_X, rim, OPENING_LAST_Z + 1, OPENING_X, rim, 14), GOLD_BLOCK),
                new PatternBox(new LocalBox(PILLAR, rim, PILLAR, PILLAR, rim, PILLAR), DIAMOND_BLOCK));
    }

    private static PatternBox layer(int fromY, int toY, String block) {
        return new PatternBox(new LocalBox(0, fromY, 0, 15, toY, 15), block);
    }
}
