package tlm.dev.basalt.plugin.masking;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

public final class EntityScanner implements Lifecycle {

    private static final long NANOS_PER_MICRO = 1_000;

    private final Plugin plugin;
    private final MaskedViewers viewers;
    private final int intervalTicks;
    private BukkitTask task;
    private long passes;
    private long entitiesRead;
    private long spentNanos;
    private long longestNanos;

    public EntityScanner(Plugin plugin, MaskedViewers viewers, int intervalTicks) {
        this.plugin = plugin;
        this.viewers = viewers;
        this.intervalTicks = intervalTicks;
    }

    @Override
    public void onEnable() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::scan, intervalTicks, intervalTicks);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        viewers.releaseAll();
        plugin.getLogger().info(viewers.summary());
        long average = passes == 0 ? 0 : spentNanos / passes / NANOS_PER_MICRO;
        plugin.getLogger().info("Scanned virtual chunks " + passes + " times for " + entitiesRead + " entities, "
                + average + " us on average, " + longestNanos / NANOS_PER_MICRO + " us at most");
    }

    private void scan() {
        if (viewers.isEmpty()) {
            return;
        }
        long start = System.nanoTime();
        entitiesRead += viewers.scanAll();
        long spent = System.nanoTime() - start;
        passes++;
        spentNanos += spent;
        longestNanos = Math.max(longestNanos, spent);
    }
}
