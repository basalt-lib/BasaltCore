package tlm.dev.basalt.plugin.facade;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

public final class ServiceRegistration implements Lifecycle {

    private final ServicesManager services;
    private final Plugin plugin;
    private final Basalt basalt;

    public ServiceRegistration(ServicesManager services, Plugin plugin, Basalt basalt) {
        this.services = services;
        this.plugin = plugin;
        this.basalt = basalt;
    }

    @Override
    public void onEnable() {
        services.register(Basalt.class, basalt, plugin, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        services.unregister(Basalt.class, basalt);
    }
}
