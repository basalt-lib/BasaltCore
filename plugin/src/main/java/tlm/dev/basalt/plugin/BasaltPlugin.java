package tlm.dev.basalt.plugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.BasaltProvider;
import tlm.dev.basalt.core.blueprint.PlanService;
import tlm.dev.basalt.core.blueprint.PlanSettings;
import tlm.dev.basalt.core.chunk.VirtualChunk;
import tlm.dev.basalt.core.entity.EntitySettings;
import tlm.dev.basalt.core.entity.EntityShapes;
import tlm.dev.basalt.core.entity.EntityTools;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.interaction.InteractionReplies;
import tlm.dev.basalt.core.interaction.InteractionService;
import tlm.dev.basalt.core.interaction.InteractionSettings;
import tlm.dev.basalt.core.interaction.InteractionTools;
import tlm.dev.basalt.core.interaction.InteractionWorld;
import tlm.dev.basalt.core.platform.GameVersion;
import tlm.dev.basalt.core.platform.SupportedVersion;
import tlm.dev.basalt.core.overlay.OverlaySettings;
import tlm.dev.basalt.core.overlay.OverlayTools;
import tlm.dev.basalt.core.overlay.OverlayViews;
import tlm.dev.basalt.core.overlay.Overlays;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.EntityMaskSettings;
import tlm.dev.basalt.core.visibility.EntityMasking;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldChunks;
import tlm.dev.basalt.core.world.WorldListener;
import tlm.dev.basalt.core.world.WorldSettings;
import tlm.dev.basalt.core.world.WorldTools;
import tlm.dev.basalt.nms.MojangBlueprints;
import tlm.dev.basalt.nms.MojangChunkViews;
import tlm.dev.basalt.nms.MojangEntities;
import tlm.dev.basalt.nms.MojangInteractions;
import tlm.dev.basalt.nms.MojangOverlays;
import tlm.dev.basalt.nms.MojangWorlds;
import tlm.dev.basalt.nms.platform.MojangBlockStates;
import tlm.dev.basalt.nms.platform.MojangGameVersionSource;
import tlm.dev.basalt.nms.platform.MojangLook;
import tlm.dev.basalt.nms.platform.MojangLooks;
import tlm.dev.basalt.nms.port.MojangVersion;
import tlm.dev.basalt.nms.port.ViewInputs;
import tlm.dev.basalt.nms.port.WorldSource;
import tlm.dev.basalt.plugin.command.BlueprintDebugTools;
import tlm.dev.basalt.plugin.command.DebugBlueprintCommand;
import tlm.dev.basalt.plugin.command.DebugChunkCommand;
import tlm.dev.basalt.plugin.command.DebugCommands;
import tlm.dev.basalt.plugin.command.DebugEventWatch;
import tlm.dev.basalt.plugin.command.DebugEventsCommand;
import tlm.dev.basalt.plugin.command.DebugWorldCommand;
import tlm.dev.basalt.plugin.command.DebugWorlds;
import tlm.dev.basalt.plugin.command.DebugEntityCommand;
import tlm.dev.basalt.plugin.config.BlueprintSettingsParser;
import tlm.dev.basalt.plugin.config.ConfigFile;
import tlm.dev.basalt.plugin.config.ConfigReader;
import tlm.dev.basalt.plugin.config.EntityMaskSettingsParser;
import tlm.dev.basalt.plugin.config.EntitySettingsParser;
import tlm.dev.basalt.plugin.config.InteractionSettingsParser;
import tlm.dev.basalt.plugin.config.MessageFiles;
import tlm.dev.basalt.plugin.config.MessageSettings;
import tlm.dev.basalt.plugin.config.OverlaySettingsParser;
import tlm.dev.basalt.plugin.config.WorldSettingsParser;
import tlm.dev.basalt.plugin.config.YamlFiles;
import tlm.dev.basalt.plugin.entity.BukkitEntityEvents;
import tlm.dev.basalt.plugin.entity.ChunkEntityViews;
import tlm.dev.basalt.plugin.entity.EntityExits;
import tlm.dev.basalt.plugin.entity.EntityTicker;
import tlm.dev.basalt.plugin.facade.BasaltFacade;
import tlm.dev.basalt.plugin.facade.ContentTools;
import tlm.dev.basalt.plugin.facade.FacadeTools;
import tlm.dev.basalt.plugin.facade.ServiceRegistration;
import tlm.dev.basalt.plugin.facade.SessionTools;
import tlm.dev.basalt.plugin.interaction.BukkitInteractionEvents;
import tlm.dev.basalt.plugin.interaction.BukkitOverlayEvents;
import tlm.dev.basalt.plugin.interaction.InteractionQueues;
import tlm.dev.basalt.plugin.interaction.InteractionSessions;
import tlm.dev.basalt.plugin.interaction.InteractionTicker;
import tlm.dev.basalt.plugin.interaction.OverlayBlocks;
import tlm.dev.basalt.plugin.interaction.OverlayEntityTargets;
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
import tlm.dev.basalt.plugin.masking.PlayerSeparations;
import tlm.dev.basalt.plugin.masking.SeparationJoinListener;
import tlm.dev.basalt.plugin.masking.SeparationQuitListener;
import tlm.dev.basalt.plugin.overlay.OverlayOwnerListener;
import tlm.dev.basalt.plugin.overlay.OverlayQuitListener;
import tlm.dev.basalt.plugin.overlay.OverlaySessions;
import tlm.dev.basalt.plugin.overlay.OverlayWorldListener;
import tlm.dev.basalt.plugin.world.ChunkEncoders;
import tlm.dev.basalt.plugin.world.OwnerListener;
import tlm.dev.basalt.plugin.world.ViewSender;
import tlm.dev.basalt.plugin.world.ViewWorldViewers;
import tlm.dev.basalt.plugin.world.ViewerEvents;
import tlm.dev.basalt.plugin.world.WorldExits;
import tlm.dev.basalt.plugin.world.WorldSessions;
import tlm.dev.basalt.v1_21_11.Mojang1_21_11;
import tlm.dev.basalt.v26_2.Mojang26_2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class BasaltPlugin extends JavaPlugin {

    private LifecycleManager lifecycle;

    @Override
    public void onEnable() {
        GameVersion game = new MojangGameVersionSource().current();
        Optional<SupportedVersion> supported = SupportedVersion.of(game);
        if (supported.isEmpty()) {
            getLogger().severe("Basalt " + getPluginMeta().getVersion() + " runs on Minecraft " + SupportedVersion.names()
                    + ", not on " + game.name() + " (protocol " + game.protocol() + "): disabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        MojangVersion version = switch (supported.get()) {
            case V1_21_11 -> new Mojang1_21_11();
            case V26_2 -> new Mojang26_2();
        };
        YamlFiles files = new YamlFiles(this);
        MessageSettings messages = new MessageFiles(files, getLogger()).load();
        ConfigFile config = files.load(InteractionSettingsParser.FILE_NAME);
        ConfigReader reader = new ConfigReader(config.path(), config.server()::get, config.shipped()::get, getLogger()::warning);
        InteractionSettings settings = InteractionSettingsParser.parse(reader);
        EntityMaskSettings maskSettings = EntityMaskSettingsParser.parse(reader);
        EntitySettings entitySettings = EntitySettingsParser.parse(reader);
        WorldSettings worldSettings = WorldSettingsParser.parse(reader);
        PlanSettings planSettings = BlueprintSettingsParser.parse(reader);
        OverlaySettings overlaySettings = OverlaySettingsParser.parse(reader);
        InteractionQueues queues = new InteractionQueues(settings.maxActionsPerSecond());
        MaskedViewers masks = new MaskedViewers(this, new EntityMasking(maskSettings.maxEntitiesPerScan()));
        AtomicReference<ChunkViews> viewsOnceCreated = new AtomicReference<>();
        AtomicReference<VirtualWorlds<?>> worldsOnceCreated = new AtomicReference<>();
        AtomicReference<Basalt> basaltOnceCreated = new AtomicReference<>();
        InteractionReplies replies = MojangInteractions.replies(getServer());
        ChunkEncoders encoders = new ChunkEncoders(WorldSettingsParser.encoderThreads(reader), getLogger());
        EntityShapes shapes = MojangEntities.shapes(getServer());
        VirtualEntityService entities = new VirtualEntityService(new EntityTools(MojangEntities.ids(getServer(), version), MojangEntities.display(getServer()),
                new ChunkEntityViews(getServer(), viewsOnceCreated::get, worldsOnceCreated::get),
                new BukkitEntityEvents(basaltOnceCreated::get, getServer())), entitySettings);
        EntityExits entityExits = new EntityExits(entities);
        WorldExits worldExits = new WorldExits(worldsOnceCreated::get, viewsOnceCreated::get);
        PlayerSeparations separations = new PlayerSeparations(this, worldsOnceCreated::get);
        WorldChunks<VirtualChunk> chunks = MojangWorlds.chunks(getServer(), version);
        VirtualWorlds<VirtualChunk> worlds = new VirtualWorlds<>(new WorldTools<>(chunks, MojangWorlds.realChunks(getServer()),
                new ViewWorldViewers(viewsOnceCreated::get, replies), WorldListener.both(WorldListener.both(worldExits, entityExits), separations)), worldSettings, UUID::randomUUID);
        worldsOnceCreated.set(worlds);
        OverlayViews<MojangLook> overlayViews = new OverlayViews<>();
        InteractionQueues overlayQueues = new InteractionQueues(settings.maxActionsPerSecond());
        Overlays<MojangLook> overlays = new Overlays<>(new OverlayTools<>(MojangOverlays.display(getServer()), overlayViews,
                UUID::randomUUID, version.entityIds(getServer())), overlaySettings);
        ChunkViews views = MojangChunkViews.create(getServer(), version, new ViewInputs(queues,
                ViewChanges.both(masks, ViewChanges.both(entityExits, ViewChanges.both(worldExits, ViewChanges.both(separations,
                        new ViewerEvents(basaltOnceCreated::get, getServer()))))), new WorldSource(worlds, chunks, encoders),
                MojangOverlays.channel(overlayViews, overlayQueues, version)));
        viewsOnceCreated.set(views);
        MojangBlockStates blockStates = new MojangBlockStates();
        PlanService plans = new PlanService(MojangBlueprints.names(), MojangBlueprints.realBlocks(getServer()), planSettings);
        BasaltFacade basalt = new BasaltFacade(getPluginMeta().getVersion(), new FacadeTools(getServer(),
                new SessionTools(worlds, entities, overlays), new ContentTools(blockStates, shapes, plans, new MojangLooks())));
        basaltOnceCreated.set(basalt);
        InteractionWorld interactionWorld = MojangInteractions.world(getServer());
        InteractionService interactions = new InteractionService(new InteractionTools(interactionWorld,
                new ViewBlocks(worlds), new ViewEntities(entities, shapes), replies), new BukkitInteractionEvents(basalt, getServer(), blockStates), settings);
        InteractionService overlayInteractions = new InteractionService(new InteractionTools(interactionWorld,
                new OverlayBlocks(overlays, interactionWorld.air()), new OverlayEntityTargets(overlays, shapes), MojangOverlays.replies(getServer(), overlayViews)),
                new BukkitOverlayEvents(basalt, getServer(), blockStates), settings);
        FlyingKickListener flyingKicks = new FlyingKickListener(views, getLogger());
        lifecycle = new LifecycleManager(List.of(
                new ServiceRegistration(getServer().getServicesManager(), this, basalt),
                encoders,
                new WorldSessions(worlds, entities::held, getLogger()),
                separations,
                new ViewSessions(views, getServer(), getLogger()),
                new OverlaySessions(overlays, getLogger()),
                flyingKicks,
                new ViewSender(this, views, WorldSettingsParser.chunkSendsPerTick(reader)),
                new EntityTicker(this, entities),
                new InteractionTicker(this, queues, interactions),
                new InteractionTicker(this, overlayQueues, overlayInteractions),
                new EntityScanner(this, masks, maskSettings.scanIntervalTicks())), getLogger());
        lifecycle.enable();
        registerListeners(views);
        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(flyingKicks, this);
        plugins.registerEvents(new InteractionSessions(queues, interactions), this);
        plugins.registerEvents(new InteractionSessions(overlayQueues, overlayInteractions), this);
        plugins.registerEvents(new OverlayQuitListener(overlays), this);
        plugins.registerEvents(new OverlayWorldListener(overlays), this);
        plugins.registerEvents(new OverlayOwnerListener(this, overlays), this);
        plugins.registerEvents(new EntityTrackListener(masks), this);
        plugins.registerEvents(new OwnerListener(this, worlds), this);
        plugins.registerEvents(new SeparationJoinListener(separations), this);
        plugins.registerEvents(new SeparationQuitListener(separations), this);
        DebugEventWatch eventWatch = new DebugEventWatch(this);
        eventWatch.listen();
        DebugWorlds debugWorlds = new DebugWorlds(views::memory);
        Path blueprintFolder = blueprintFolder();
        BlueprintDebugTools blueprintTools = new BlueprintDebugTools(blueprintFolder,
                (world, file, x, y, z) -> MojangBlueprints.placeReal(world, file, planSettings.maxFileBytes(), x, y, z), basalt::bytesOf);
        DebugCommands commands = new DebugCommands(List.of(
                new DebugChunkCommand(basalt, area -> basalt.createWorldOverRealBlocks(this, area), getServer(), messages),
                new DebugEntityCommand(basalt, entities, getServer(), messages),
                new DebugWorldCommand(basalt, this, messages, debugWorlds),
                new DebugEventsCommand(eventWatch, getServer(), messages),
                new DebugBlueprintCommand(basalt, debugWorlds, blueprintTools, messages)));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(commands.node()));

        getLogger().info("Basalt " + BasaltProvider.get().version()
                + " on Minecraft " + game.name() + " (protocol " + game.protocol() + ")");
    }

    @Override
    public void onDisable() {
        if (lifecycle != null) {
            lifecycle.disable();
        }
    }

    private Path blueprintFolder() {
        Path folder = getDataFolder().toPath().resolve("blueprints");
        try {
            Files.createDirectories(folder);
        } catch (IOException unwritable) {
            getLogger().warning("Cannot create the blueprints folder " + folder + ": " + unwritable.getMessage());
        }
        return folder;
    }

    private void registerListeners(ChunkViews views) {
        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new PlayerJoinListener(views), this);
        plugins.registerEvents(new PlayerQuitListener(views), this);
        plugins.registerEvents(new WorldChangeListener(views), this);
    }
}
