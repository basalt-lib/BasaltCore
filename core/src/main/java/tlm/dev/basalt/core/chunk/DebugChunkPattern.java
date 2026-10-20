package tlm.dev.basalt.core.chunk;

import java.util.function.ToIntFunction;

public final class DebugChunkPattern {

    public static final String BEDROCK = "minecraft:bedrock";
    public static final String BASALT = "minecraft:basalt";
    public static final String SMOOTH_BASALT = "minecraft:smooth_basalt";
    public static final String GOLD_BLOCK = "minecraft:gold_block";
    public static final String DIAMOND_BLOCK = "minecraft:diamond_block";

    private static final int PILLAR = 8;
    private static final int PILLAR_HEIGHT = 3;

    private DebugChunkPattern() {
    }

    public static void paint(VirtualChunkBuilder builder, ToIntFunction<String> stateOf) {
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
                if (x == 0 || x == 15 || z == 0 || z == 15) {
                    builder.set(x, bottom + 4, z, gold);
                }
            }
        }
        int diamond = stateOf.applyAsInt(DIAMOND_BLOCK);
        for (int level = 0; level < PILLAR_HEIGHT; level++) {
            builder.set(PILLAR, bottom + 4 + level, PILLAR, diamond);
        }
    }
}
