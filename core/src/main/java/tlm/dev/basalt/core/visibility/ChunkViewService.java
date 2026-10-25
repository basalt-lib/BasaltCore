package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ChunkViewService implements ChunkViews {

    private final PlayerViews views = new PlayerViews();
    private final Map<UUID, Set<ChunkKey>> pending = new HashMap<>();
    private final ViewDisplay display;
    private final ViewChanges changes;
    private final ChunkRequests requests;

    public ChunkViewService(ViewDisplay display, ViewChanges changes, ChunkRequests requests) {
        this.display = display;
        this.changes = changes;
        this.requests = requests;
    }

    @Override
    public void track(UUID player) {
        if (views.find(player).isEmpty()) {
            display.attach(player, views.open(player));
        }
    }

    @Override
    public ShowResult show(UUID player, UUID world, ChunkRange area) {
        Optional<PlayerView> found = views.find(player);
        if (found.isEmpty()) {
            return ShowResult.NOT_TRACKED;
        }
        if (!found.get().enter(world, area)) {
            return ShowResult.ALREADY_VIRTUAL;
        }
        pending.put(player, new LinkedHashSet<>(display.chunksHeld(player, area)));
        changes.entered(player, world, area);
        return ShowResult.SHOWN;
    }

    @Override
    public HideResult hide(UUID player, ExitCause cause) {
        Optional<PlayerView> found = views.find(player);
        if (found.isEmpty()) {
            return HideResult.NOT_VIRTUAL;
        }
        Optional<ViewSnapshot> left = found.get().exit();
        pending.remove(player);
        left.ifPresent(previous -> restore(player, previous, cause));
        if (cause.endsTracking()) {
            views.close(player);
            display.detach(player);
        }
        left.ifPresent(previous -> changes.exited(player, previous.world(), cause));
        return left.isPresent() ? HideResult.RESTORED : HideResult.NOT_VIRTUAL;
    }

    @Override
    public int hideAll(ExitCause cause) {
        int restored = 0;
        for (UUID player : views.players()) {
            if (hide(player, cause) == HideResult.RESTORED) {
                restored++;
            }
        }
        return restored;
    }

    @Override
    public Optional<UUID> world(UUID player) {
        return snapshot(player).map(ViewSnapshot::world);
    }

    @Override
    public Optional<ChunkRange> area(UUID player) {
        return snapshot(player).map(ViewSnapshot::area);
    }

    @Override
    public boolean publishEntities(UUID player, Set<Integer> ids) {
        return views.find(player).map(view -> view.publishEntities(ids)).orElse(false);
    }

    @Override
    public void request(UUID player, ChunkKey chunk) {
        requests.offer(player, chunk);
    }

    @Override
    public boolean awaits(UUID player, ChunkKey chunk) {
        return pending.getOrDefault(player, Set.of()).contains(chunk);
    }

    @Override
    public int sendRequested(int budget) {
        requests.drain((player, chunk) -> snapshot(player).filter(snapshot -> snapshot.hides(chunk))
                .ifPresent(snapshot -> pending.computeIfAbsent(player, id -> new LinkedHashSet<>()).add(chunk)));
        int sent = 0;
        boolean progress = true;
        while (sent < budget && progress) {
            progress = false;
            for (UUID player : List.copyOf(pending.keySet())) {
                if (sent >= budget) {
                    break;
                }
                if (sendNext(player)) {
                    sent++;
                    progress = true;
                }
            }
        }
        pending.values().removeIf(Set::isEmpty);
        return sent;
    }

    @Override
    public void release(UUID world) {
        display.release(world);
    }

    @Override
    public PacketMemory memory(UUID world) {
        return display.memory(world);
    }

    private boolean sendNext(UUID player) {
        Optional<ViewSnapshot> snapshot = snapshot(player);
        Set<ChunkKey> chunks = pending.get(player);
        if (snapshot.isEmpty()) {
            chunks.clear();
            return false;
        }
        UUID world = snapshot.get().world();
        Iterator<ChunkKey> next = chunks.iterator();
        while (next.hasNext()) {
            ChunkKey chunk = next.next();
            ChunkSend result = display.sendVirtual(player, world, chunk);
            if (result == ChunkSend.PENDING) {
                continue;
            }
            next.remove();
            if (result == ChunkSend.SENT) {
                changes.sent(player, world, chunk);
                return true;
            }
        }
        return false;
    }

    private void restore(UUID player, ViewSnapshot previous, ExitCause cause) {
        display.restartFlyingCheck(player);
        if (cause.clientKeepsChunks()) {
            for (ChunkKey chunk : display.chunksHeld(player, previous.area())) {
                display.resendReal(player, chunk);
            }
        }
        changes.left(player, previous.world(), cause);
    }

    private Optional<ViewSnapshot> snapshot(UUID player) {
        return views.find(player).map(PlayerView::snapshot).filter(ViewSnapshot::isVirtual);
    }
}
