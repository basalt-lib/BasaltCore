package tlm.dev.basalt.core.overlay;

public record OverlaySettings(int maxOverlays, int maxBlocks, int maxEntities) {

    public OverlaySettings {
        if (maxOverlays < 1 || maxBlocks < 1 || maxEntities < 1) {
            throw new IllegalArgumentException("overlay limits must be positive: " + maxOverlays + ", " + maxBlocks + ", " + maxEntities);
        }
    }
}
