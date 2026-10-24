package tlm.dev.basalt.nms;

import net.minecraft.world.entity.Entity;
import org.bukkit.Server;
import tlm.dev.basalt.core.entity.EntityDisplay;
import tlm.dev.basalt.core.entity.EntityShapes;
import tlm.dev.basalt.core.entity.VirtualEntityIds;
import tlm.dev.basalt.nms.channel.NettyPlayerChannels;
import tlm.dev.basalt.nms.entity.MojangEntityDisplay;
import tlm.dev.basalt.nms.entity.MojangEntityShapes;

public final class MojangEntities {

    private MojangEntities() {
    }

    public static EntityDisplay display(Server server) {
        return new MojangEntityDisplay(server, new NettyPlayerChannels());
    }

    public static EntityShapes shapes(Server server) {
        return new MojangEntityShapes(server);
    }

    public static VirtualEntityIds ids() {
        return new VirtualEntityIds(Entity::nextEntityId);
    }
}
