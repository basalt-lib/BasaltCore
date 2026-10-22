package tlm.dev.basalt.plugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tlm.dev.basalt.api.BasaltProvider;
import tlm.dev.basalt.core.interaction.InteractionService;
import tlm.dev.basalt.core.interaction.InteractionSettings;
import tlm.dev.basalt.core.interaction.InteractionTools;
import tlm.dev.basalt.core.platform.GameVersion;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.nms.MojangChunkViews;
import tlm.dev.basalt.nms.MojangInteractions;
import tlm.dev.basalt.nms.platform.MojangGameVersionSource;
import tlm.dev.basalt.plugin.command.DebugChunkCommand;
import tlm.dev.basalt.plugin.config.ConfigFile;
import tlm.dev.basalt.plugin.config.InteractionSettingsParser;
import tlm.dev.basalt.plugin.config.MessageFiles;
import tlm.dev.basalt.plugin.config.MessageSettings;
import tlm.dev.basalt.plugin.config.YamlFiles;
import tlm.dev.basalt.plugin.facade.BasaltFacade;
import tlm.dev.basalt.plugin.facade.ServiceRegistration;
import tlm.dev.basalt.plugin.interaction.InteractionQueues;
import tlm.dev.basalt.plugin.interaction.InteractionSessions;
import tlm.dev.basalt.plugin.interaction.InteractionTicker;
import tlm.dev.basalt.plugin.interaction.ViewBlocks;
import tlm.dev.basalt.plugin.lifecycle.LifecycleManager;
import tlm.dev.basalt.plugin.listener.FlyingKickListener;
import tlm.dev.basalt.plugin.listener.PlayerJoinListener;
import tlm.dev.basalt.plugin.listener.PlayerQuitListener;
import tlm.dev.basalt.plugin.listener.ViewSessions;
import tlm.dev.basalt.plugin.listener.WorldChangeListener;

import java.util.List;

public final class BasaltPlugin extends JavaPlugin {

    private LifecycleManager lifecycle;

    @Override
    public void onEnable() {
        YamlFiles files = new YamlFiles(this);
        MessageSettings messages = new MessageFiles(files, getLogger()).load();
        ConfigFile config = files.load(InteractionSettingsParser.FILE_NAME);
        InteractionSettings settings = InteractionSettingsParser.parse(config.path(), config.server()::get, config.shipped()::get, getLogger()::warning);
        InteractionQueues queues = new InteractionQueues(settings.maxActionsPerSecond());
        ChunkViews views = MojangChunkViews.create(getServer(), queues);
        InteractionService interactions = new InteractionService(new InteractionTools(
                MojangInteractions.world(getServer()), new ViewBlocks(views), MojangInteractions.replies(getServer())), settings);
        BasaltFacade basalt = new BasaltFacade(getPluginMeta().getVersion());
        FlyingKickListener flyingKicks = new FlyingKickListener(views, getLogger());
        lifecycle = new LifecycleManager(List.of(
                new ServiceRegistration(getServer().getServicesManager(), this, basalt),
                new ViewSessions(views, getServer(), getLogger()),
                flyingKicks,
                new InteractionTicker(this, queues, interactions)), getLogger());
        lifecycle.enable();
        registerListeners(views);
        getServer().getPluginManager().registerEvents(flyingKicks, this);
        getServer().getPluginManager().registerEvents(new InteractionSessions(queues, interactions), this);
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
