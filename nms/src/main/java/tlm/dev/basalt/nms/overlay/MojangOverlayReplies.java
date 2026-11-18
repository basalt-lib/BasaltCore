package tlm.dev.basalt.nms.overlay;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.interaction.InteractionReplies;
import tlm.dev.basalt.core.overlay.OverlayBlockIndex;
import tlm.dev.basalt.core.overlay.OverlayViews;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.nms.platform.MojangLook;

import java.util.List;
import java.util.UUID;

public final class MojangOverlayReplies implements InteractionReplies {

    private final InteractionReplies plain;
    private final MojangOverlayDisplay display;
    private final Server server;
    private final OverlayViews<MojangLook> views;

    public MojangOverlayReplies(InteractionReplies plain, MojangOverlayDisplay display, Server server, OverlayViews<MojangLook> views) {
        this.plain = plain;
        this.display = display;
        this.server = server;
        this.views = views;
    }

    @Override
    public void block(UUID player, BlockPosition position, int state) {
        int seen = views.current(player).stateAt(position.x(), position.y(), position.z());
        if (seen != OverlayBlockIndex.NONE) {
            plain.block(player, position, seen);
            return;
        }
        Player online = server.getPlayer(player);
        if (online != null) {
            display.showReal(player, online.getWorld().getUID(), List.of(position));
        }
    }

    @Override
    public void resyncInventory(UUID player) {
        plain.resyncInventory(player);
    }

    @Override
    public void acknowledge(UUID player, int sequence) {
        plain.acknowledge(player, sequence);
    }
}
