package tlm.dev.basalt.core.chunk;

import tlm.dev.basalt.core.debug.DebugPattern;

import java.util.function.ToIntFunction;

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

    public static void paint(DebugPattern pattern, VirtualChunkBuilder builder, ToIntFunction<String> stateOf) {
        switch (pattern) {
            case FLAT -> paintFlat(builder, stateOf);
            case RAISED -> paintRaised(builder, stateOf);
        }
    }

    private static void paintFlat(VirtualChunkBuilder builder, ToIntFunction<String> stateOf) {
        int bottom = builder.shape().minY();
        int bedrock = stateOf.applyAsInt(BEDROCK);
        int basalt = stateOf.applyAsInt(BASALT);
        int smoothBasalt = stateOf.applyAsInt(SMOOTH_BASALT);
        int gold = stateOf.applyAsInt(GOLD_BLOCK);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                builder.set(x, bottom, z, bedrock);
                builder.set(x, bottom + 1, z, basalt);
                builder.set(x, bottom + 2, z, basalt);
                builder.set(x, bottom + 3, z, smoothBasalt);
                if (isRim(x, z)) {
                    builder.set(x, bottom + 4, z, gold);
                }
            }
        }
        int diamond = stateOf.applyAsInt(DIAMOND_BLOCK);
        for (int level = 0; level < PILLAR_HEIGHT; level++) {
            builder.set(PILLAR, bottom + 4 + level, PILLAR, diamond);
        }
    }

    private static void paintRaised(VirtualChunkBuilder builder, ToIntFunction<String> stateOf) {
        int platform = builder.shape().minY() + PLATFORM_LEVEL;
        int smoothBasalt = stateOf.applyAsInt(SMOOTH_BASALT);
        int gold = stateOf.applyAsInt(GOLD_BLOCK);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                builder.set(x, platform, z, smoothBasalt);
                if (isRim(x, z) && !isOpening(x, z)) {
                    builder.set(x, platform + 1, z, gold);
                }
            }
        }
        builder.set(PILLAR, platform + 1, PILLAR, stateOf.applyAsInt(DIAMOND_BLOCK));
    }

    private static boolean isRim(int x, int z) {
        return x == 0 || x == 15 || z == 0 || z == 15;
    }

    private static boolean isOpening(int x, int z) {
        return x == OPENING_X && z >= OPENING_FIRST_Z && z <= OPENING_LAST_Z;
    }
}
