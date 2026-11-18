package tlm.dev.basalt.nms;

import org.bukkit.Server;
import tlm.dev.basalt.core.interaction.InteractionInbox;
import tlm.dev.basalt.core.interaction.InteractionReplies;
import tlm.dev.basalt.core.overlay.OverlayDisplay;
import tlm.dev.basalt.core.overlay.OverlayView;
import tlm.dev.basalt.core.overlay.OverlayViews;
import tlm.dev.basalt.nms.channel.NettyPlayerChannels;
import tlm.dev.basalt.nms.interaction.MojangInteractionReplies;
import tlm.dev.basalt.nms.overlay.MojangOverlayDisplay;
import tlm.dev.basalt.nms.overlay.MojangOverlayReplies;
import tlm.dev.basalt.nms.overlay.OverlayPacketFilter;
import tlm.dev.basalt.nms.overlay.OverlayPacketReader;
import tlm.dev.basalt.nms.platform.MojangLook;
import tlm.dev.basalt.nms.port.ChannelFilters;
import tlm.dev.basalt.nms.port.MojangVersion;
import tlm.dev.basalt.nms.port.OverlayChannel;

public final class MojangOverlays {

    private MojangOverlays() {
    }

    public static OverlayDisplay<MojangLook> display(Server server) {
        return new MojangOverlayDisplay(server, new NettyPlayerChannels());
    }

    public static InteractionReplies replies(Server server, OverlayViews<MojangLook> views) {
        NettyPlayerChannels channels = new NettyPlayerChannels();
        return new MojangOverlayReplies(new MojangInteractionReplies(server, channels), new MojangOverlayDisplay(server, channels), server, views);
    }

    public static OverlayChannel channel(OverlayViews<MojangLook> views, InteractionInbox inbox, MojangVersion version) {
        return player -> {
            OverlayView<MojangLook> view = views.open(player);
            return new ChannelFilters(new OverlayPacketFilter(view), new OverlayPacketReader(player, view, inbox, version));
        };
    }
}
