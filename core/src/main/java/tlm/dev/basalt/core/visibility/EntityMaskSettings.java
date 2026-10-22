package tlm.dev.basalt.core.visibility;

public record EntityMaskSettings(int scanIntervalTicks, int maxEntitiesPerScan) {

    public static final int MAX_SCAN_INTERVAL = 200;
    public static final int MAX_ENTITIES_LIMIT = 10_000;

    public EntityMaskSettings {
        if (scanIntervalTicks < 1 || scanIntervalTicks > MAX_SCAN_INTERVAL) {
            throw new IllegalArgumentException("the entity scan interval must be from 1 to " + MAX_SCAN_INTERVAL + " ticks");
        }
        if (maxEntitiesPerScan < 1 || maxEntitiesPerScan > MAX_ENTITIES_LIMIT) {
            throw new IllegalArgumentException("the entities per scan must be from 1 to " + MAX_ENTITIES_LIMIT);
        }
    }
}
