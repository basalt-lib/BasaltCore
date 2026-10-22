package tlm.dev.basalt.nms;

import org.bukkit.Server;
import tlm.dev.basalt.core.interaction.InteractionReplies;
import tlm.dev.basalt.core.interaction.InteractionWorld;
import tlm.dev.basalt.nms.channel.NettyPlayerChannels;
import tlm.dev.basalt.nms.interaction.MojangInteractionReplies;
import tlm.dev.basalt.nms.interaction.MojangInteractionWorld;

public final class MojangInteractions {

    private MojangInteractions() {
    }

    public static InteractionWorld world(Server server) {
        return new MojangInteractionWorld(server);
    }

    public static InteractionReplies replies(Server server) {
        return new MojangInteractionReplies(server, new NettyPlayerChannels());
    }
}
