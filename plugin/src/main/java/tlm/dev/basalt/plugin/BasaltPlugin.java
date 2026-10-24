package tlm.dev.basalt.plugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tlm.dev.basalt.api.BasaltProvider;
import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.entity.EntitySettings;
import tlm.dev.basalt.core.entity.EntityTools;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.interaction.InteractionReplies;
import tlm.dev.basalt.core.interaction.InteractionService;
import tlm.dev.basalt.core.interaction.InteractionSettings;
import tlm.dev.basalt.core.interaction.InteractionTools;
import tlm.dev.basalt.core.platform.GameVersion;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.EntityMaskSettings;
import tlm.dev.basalt.core.visibility.EntityMasking;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldChunks;
import tlm.dev.basalt.core.world.WorldListener;
import tlm.dev.basalt.core.world.WorldSettings;
import tlm.dev.basalt.core.world.WorldTools;
import tlm.dev.basalt.nms.MojangChunkViews;
import tlm.dev.basalt.nms.MojangEntities;
import tlm.dev.basalt.nms.MojangInteractions;
import tlm.dev.basalt.nms.MojangWorlds;
import tlm.dev.basalt.nms.platform.MojangBlockStates;
import tlm.dev.basalt.nms.platform.MojangGameVersionSource;
import tlm.dev.basalt.nms.port.WorldSource;
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
import tlm.dev.basalt.plugin.config.WorldSettingsParser;
import tlm.dev.basalt.plugin.config.YamlFiles;
import tlm.dev.basalt.plugin.entity.ChunkEntityViews;
import tlm.dev.basalt.plugin.entity.EntityExits;
import tlm.dev.basalt.plugin.entity.EntityTicker;
import tlm.dev.basalt.plugin.facade.BasaltFacade;
import tlm.dev.basalt.plugin.facade.FacadeTools;
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
import tlm.dev.basalt.plugin.world.ChunkEncoders;
import tlm.dev.basalt.plugin.world.ViewSender;
import tlm.dev.basalt.plugin.world.ViewWorldViewers;
import tlm.dev.basalt.plugin.world.WorldExits;
import tlm.dev.basalt.plugin.world.WorldSessions;

import java.util.List;
import java.util.UUID;
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
        WorldSettings worldSettings = WorldSettingsParser.parse(reader);
        InteractionQueues queues = new InteractionQueues(settings.maxActionsPerSecond());
        MaskedViewers masks = new MaskedViewers(this, new EntityMasking(maskSettings.maxEntitiesPerScan()));
        AtomicReference<ChunkViews> viewsOnceCreated = new AtomicReference<>();
        AtomicReference<VirtualWorlds<?>> worldsOnceCreated = new AtomicReference<>();
        InteractionReplies replies = MojangInteractions.replies(getServer());
        ChunkEncoders encoders = new ChunkEncoders(WorldSettingsParser.encoderThreads(reader), getLogger());
        VirtualEntityService entities = new VirtualEntityService(new EntityTools(MojangEntities.ids(), MojangEntities.display(getServer()),
                new ChunkEntityViews(viewsOnceCreated::get, worldsOnceCreated::get)), entitySettings);
        EntityExits entityExits = new EntityExits(entities);
        WorldExits worldExits = new WorldExits(worldsOnceCreated::get, viewsOnceCreated::get);
        WorldChunks<VirtualChunk> chunks = MojangWorlds.chunks(getServer());
        VirtualWorlds<VirtualChunk> worlds = new VirtualWorlds<>(new WorldTools<>(chunks, MojangWorlds.realChunks(getServer()),
                new ViewWorldViewers(viewsOnceCreated::get, replies), WorldListener.both(worldExits, entityExits)), worldSettings, UUID::randomUUID);
        worldsOnceCreated.set(worlds);
        ChunkViews views = MojangChunkViews.create(getServer(), queues,
                ViewChanges.both(masks, ViewChanges.both(entityExits, worldExits)), new WorldSource(worlds, chunks, encoders));
        viewsOnceCreated.set(views);
        InteractionService interactions = new InteractionService(new InteractionTools(MojangInteractions.world(getServer()),
                new ViewBlocks(worlds), new ViewEntities(entities, MojangEntities.shapes()), replies), settings);
        BasaltFacade basalt = new BasaltFacade(getPluginMeta().getVersion(), new FacadeTools(getServer(), worlds, new MojangBlockStates(), worldSettings));
        FlyingKickListener flyingKicks = new FlyingKickListener(views, getLogger());
        lifecycle = new LifecycleManager(List.of(
                new ServiceRegistration(getServer().getServicesManager(), this, basalt),
                encoders,
                new EntityTicker(this, entities),
                new WorldSessions(worlds, getLogger()),
                new ViewSessions(views, getServer(), getLogger()),
                flyingKicks,
                new ViewSender(this, views, WorldSettingsParser.chunkSendsPerTick(reader)),
                new InteractionTicker(this, queues, interactions),
                new EntityScanner(this, masks, maskSettings.scanIntervalTicks())), getLogger());
        lifecycle.enable();
        registerListeners(views);
        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(flyingKicks, this);
        plugins.registerEvents(new InteractionSessions(queues, interactions), this);
        plugins.registerEvents(new EntityTrackListener(masks), this);
        DebugCommands commands = new DebugCommands(new DebugChunkCommand(worlds, getServer(), messages, new MojangBlockStates()),
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
