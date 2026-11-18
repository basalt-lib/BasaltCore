package tlm.dev.basalt.core.overlay;

public record OverlaySettings(int maxOverlays, int maxBlocks, int maxEntities) {

    public static final int MAX_OVERLAYS_LIMIT = 65536;
    public static final int MAX_BLOCKS_LIMIT = 65536;
    public static final int MAX_ENTITIES_LIMIT = 4096;

    public OverlaySettings {
        check("overlays", maxOverlays, MAX_OVERLAYS_LIMIT);
        check("blocks per overlay", maxBlocks, MAX_BLOCKS_LIMIT);
        check("entities per overlay", maxEntities, MAX_ENTITIES_LIMIT);
    }

    private static void check(String what, int value, int limit) {
        if (value < 1 || value > limit) {
            throw new IllegalArgumentException(what + " must be from 1 to " + limit + ": " + value);
        }
    }
}
