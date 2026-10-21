package tlm.dev.basalt.plugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tlm.dev.basalt.api.BasaltProvider;
import tlm.dev.basalt.core.platform.GameVersion;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.nms.MojangChunkViews;
import tlm.dev.basalt.nms.platform.MojangGameVersionSource;
import tlm.dev.basalt.plugin.command.DebugChunkCommand;
import tlm.dev.basalt.plugin.config.MessageFiles;
import tlm.dev.basalt.plugin.config.MessageSettings;
import tlm.dev.basalt.plugin.facade.BasaltFacade;
import tlm.dev.basalt.plugin.facade.ServiceRegistration;
import tlm.dev.basalt.plugin.lifecycle.LifecycleManager;
import tlm.dev.basalt.plugin.listener.PlayerJoinListener;
import tlm.dev.basalt.plugin.listener.PlayerQuitListener;
import tlm.dev.basalt.plugin.listener.ViewSessions;
import tlm.dev.basalt.plugin.listener.WorldChangeListener;

import java.util.List;

public final class BasaltPlugin extends JavaPlugin {

    private LifecycleManager lifecycle;

    @Override
    public void onEnable() {
        MessageSettings messages = new MessageFiles(this).load();
        ChunkViews views = MojangChunkViews.create(getServer());
        BasaltFacade basalt = new BasaltFacade(getPluginMeta().getVersion());
        lifecycle = new LifecycleManager(List.of(
                new ServiceRegistration(getServer().getServicesManager(), this, basalt),
                new ViewSessions(views, getServer(), getLogger())), getLogger());
        lifecycle.enable();
        registerListeners(views);
        DebugChunkCommand command = new DebugChunkCommand(views, getServer(), messages);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(command.node()));

        GameVersion game = new MojangGameVersionSource().current();
        getLogger().info("Basalt " + BasaltProvider.get().version()
                + " on Minecraft " + game.name() + " (protocol " + game.protocol() + ")");
    }

    @Override
    public void onDisable() {
        if (lifecycle != null) {
            lifecycle.disable();
        }
    }

    private void registerListeners(ChunkViews views) {
        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new PlayerJoinListener(views), this);
        plugins.registerEvents(new PlayerQuitListener(views), this);
        plugins.registerEvents(new WorldChangeListener(views), this);
    }
}
