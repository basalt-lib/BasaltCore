package tlm.dev.basalt.plugin;

import org.bukkit.plugin.java.JavaPlugin;
import tlm.dev.basalt.api.BasaltProvider;
import tlm.dev.basalt.core.platform.GameVersion;
import tlm.dev.basalt.nms.platform.MojangGameVersionSource;
import tlm.dev.basalt.plugin.facade.BasaltFacade;
import tlm.dev.basalt.plugin.facade.ServiceRegistration;
import tlm.dev.basalt.plugin.lifecycle.LifecycleManager;

import java.util.List;

public final class BasaltPlugin extends JavaPlugin {

    private LifecycleManager lifecycle;

    @Override
    public void onEnable() {
        BasaltFacade basalt = new BasaltFacade(getPluginMeta().getVersion());
        lifecycle = new LifecycleManager(
                List.of(new ServiceRegistration(getServer().getServicesManager(), this, basalt)),
                getLogger());
        lifecycle.enable();

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
}
