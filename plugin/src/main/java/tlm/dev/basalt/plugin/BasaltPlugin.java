package tlm.dev.basalt.plugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tlm.dev.basalt.api.BasaltProvider;
import tlm.dev.basalt.core.entity.EntitySettings;
import tlm.dev.basalt.core.entity.EntityTools;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.interaction.InteractionService;
import tlm.dev.basalt.core.interaction.InteractionSettings;
import tlm.dev.basalt.core.interaction.InteractionTools;
import tlm.dev.basalt.core.platform.GameVersion;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.EntityMaskSettings;
import tlm.dev.basalt.core.visibility.EntityMasking;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.nms.MojangChunkViews;
import tlm.dev.basalt.nms.MojangEntities;
import tlm.dev.basalt.nms.MojangInteractions;
import tlm.dev.basalt.nms.platform.MojangGameVersionSource;
import tlm.dev.basalt.plugin.command.DebugChunkCommand;
import tlm.dev.basalt.plugin.command.DebugCommands;
import tlm.dev.basalt.plugin.command.DebugEntityCommand;
import tlm.dev.basalt.plugin.config.ConfigFile;
import tlm.dev.basalt.plugin.config.ConfigReader;
import tlm.dev.basalt.plugin.config.EntityMaskSettingsParser;
import tlm.dev.basalt.plugin.config.EntitySettingsParser;
import tlm.dev.basalt.plugin.config.InteractionSettingsParser;
import tlm.dev.basalt.plugin.config.MessageFiles;
import tlm.dev.basalt.plugin.config.MessageSettings;
import tlm.dev.basalt.plugin.config.YamlFiles;
import tlm.dev.basalt.plugin.entity.ChunkEntityViews;
import tlm.dev.basalt.plugin.entity.EntityExits;
import tlm.dev.basalt.plugin.entity.EntityResender;
import tlm.dev.basalt.plugin.entity.EntityTicker;
import tlm.dev.basalt.plugin.facade.BasaltFacade;
import tlm.dev.basalt.plugin.facade.ServiceRegistration;
import tlm.dev.basalt.plugin.interaction.InteractionQueues;
import tlm.dev.basalt.plugin.interaction.InteractionSessions;
import tlm.dev.basalt.plugin.interaction.InteractionTicker;
import tlm.dev.basalt.plugin.interaction.ViewBlocks;
import tlm.dev.basalt.plugin.interaction.ViewEntities;
import tlm.dev.basalt.plugin.lifecycle.LifecycleManager;
import tlm.dev.basalt.plugin.listener.FlyingKickListener;
import tlm.dev.basalt.plugin.listener.PlayerJoinListener;
import tlm.dev.basalt.plugin.listener.PlayerQuitListener;
import tlm.dev.basalt.plugin.listener.ViewSessions;
import tlm.dev.basalt.plugin.listener.WorldChangeListener;
import tlm.dev.basalt.plugin.masking.EntityScanner;
import tlm.dev.basalt.plugin.masking.EntityTrackListener;
import tlm.dev.basalt.plugin.masking.MaskedViewers;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class BasaltPlugin extends JavaPlugin {

    private LifecycleManager lifecycle;

    @Override
    public void onEnable() {
        YamlFiles files = new YamlFiles(this);
        MessageSettings messages = new MessageFiles(files, getLogger()).load();
        ConfigFile config = files.load(InteractionSettingsParser.FILE_NAME);
        ConfigReader reader = new ConfigReader(config.path(), config.server()::get, config.shipped()::get, getLogger()::warning);
        InteractionSettings settings = InteractionSettingsParser.parse(reader);
        EntityMaskSettings maskSettings = EntityMaskSettingsParser.parse(reader);
        EntitySettings entitySettings = EntitySettingsParser.parse(reader);
        InteractionQueues queues = new InteractionQueues(settings.maxActionsPerSecond());
        MaskedViewers masks = new MaskedViewers(this, new EntityMasking(maskSettings.maxEntitiesPerScan()));
        AtomicReference<ChunkViews> viewsOnceCreated = new AtomicReference<>();
        VirtualEntityService entities = new VirtualEntityService(new EntityTools(MojangEntities.ids(), MojangEntities.display(getServer()),
                new ChunkEntityViews(viewsOnceCreated::get)), entitySettings);
        ChunkViews views = MojangChunkViews.create(getServer(), queues, ViewChanges.both(masks, new EntityExits(entities)));
        viewsOnceCreated.set(views);
        InteractionService interactions = new InteractionService(new InteractionTools(MojangInteractions.world(getServer()),
                new ViewBlocks(views), new ViewEntities(entities, MojangEntities.shapes()), MojangInteractions.replies(getServer())), settings);
        BasaltFacade basalt = new BasaltFacade(getPluginMeta().getVersion());
        FlyingKickListener flyingKicks = new FlyingKickListener(views, getLogger());
        lifecycle = new LifecycleManager(List.of(
                new ServiceRegistration(getServer().getServicesManager(), this, basalt),
                new EntityTicker(this, entities),
                new ViewSessions(views, getServer(), getLogger()),
                flyingKicks,
                new InteractionTicker(this, queues, interactions),
                new EntityScanner(this, masks, maskSettings.scanIntervalTicks())), getLogger());
        lifecycle.enable();
        registerListeners(views);
        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(flyingKicks, this);
        plugins.registerEvents(new InteractionSessions(queues, interactions), this);
        plugins.registerEvents(new EntityTrackListener(masks), this);
        plugins.registerEvents(new EntityResender(views, entities), this);
        DebugCommands commands = new DebugCommands(new DebugChunkCommand(views, getServer(), messages),
                new DebugEntityCommand(entities, entitySettings, getServer(), messages));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(commands.node()));

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
