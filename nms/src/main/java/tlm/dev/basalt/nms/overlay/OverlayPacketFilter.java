package tlm.dev.basalt.nms.overlay;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import tlm.dev.basalt.core.overlay.OverlayBlockIndex;
import tlm.dev.basalt.core.overlay.OverlayEntity;
import tlm.dev.basalt.core.overlay.OverlaySnapshot;
import tlm.dev.basalt.core.overlay.OverlayView;
import tlm.dev.basalt.nms.platform.MojangLook;
import tlm.dev.basalt.nms.port.OutboundFilter;
import tlm.dev.basalt.nms.port.PacketRun;

import java.util.ArrayList;
import java.util.List;

public final class OverlayPacketFilter implements OutboundFilter {

    private final OverlayView<MojangLook> view;

    public OverlayPacketFilter(OverlayView<MojangLook> view) {
        this.view = view;
    }

    @Override
    public Object filter(Object message) {
        OverlaySnapshot<MojangLook> snapshot = view.current();
        if (snapshot.isNone()) {
            return message;
        }
        return switch (message) {
            case ClientboundLevelChunkWithLightPacket chunk -> chunk(chunk, snapshot);
            case ClientboundBlockUpdatePacket block -> block(block, snapshot);
            case ClientboundSectionBlocksUpdatePacket section -> section(section, snapshot);
            case ClientboundBlockEntityDataPacket blockEntity -> covers(blockEntity.getPos(), snapshot) ? null : blockEntity;
            default -> message;
        };
    }

    private static Object chunk(ClientboundLevelChunkWithLightPacket chunk, OverlaySnapshot<MojangLook> snapshot) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        if (!snapshot.touches(chunkX, chunkZ)) {
            return chunk;
        }
        List<Object> run = new ArrayList<>();
        run.add(chunk);
        OverlayBlockIndex blocks = snapshot.blocks();
        for (int at = blocks.firstIn(chunkX, chunkZ); at < blocks.endIn(chunkX, chunkZ); at++) {
            run.add(OverlayPackets.block(blocks.x(at), blocks.y(at), blocks.z(at), blocks.state(at)));
        }
        for (OverlayEntity<MojangLook> entity : snapshot.entitiesIn(chunkX, chunkZ)) {
            run.add(OverlayPackets.add(entity));
            run.add(OverlayPackets.data(entity));
        }
        return new PacketRun(run);
    }

    private static Object block(ClientboundBlockUpdatePacket block, OverlaySnapshot<MojangLook> snapshot) {
        BlockPos pos = block.getPos();
        int state = snapshot.stateAt(pos.getX(), pos.getY(), pos.getZ());
        return state == OverlayBlockIndex.NONE ? block : OverlayPackets.block(pos.getX(), pos.getY(), pos.getZ(), state);
    }

    private static Object section(ClientboundSectionBlocksUpdatePacket section, OverlaySnapshot<MojangLook> snapshot) {
        int[] found = new int[4];
        section.runUpdates((pos, state) -> {
            if (found[3] == 0) {
                found[0] = pos.getX() >> 4;
                found[1] = pos.getY() >> 4;
                found[2] = pos.getZ() >> 4;
                found[3] = 1;
            }
        });
        if (found[3] == 0 || !snapshot.covers(found[0], found[2])) {
            return section;
        }
        OverlayBlockIndex blocks = snapshot.blocks();
        int from = blocks.firstInSection(found[0], found[1], found[2]);
        int to = blocks.endInSection(found[0], found[1], found[2]);
        if (from >= to) {
            return section;
        }
        List<Object> run = new ArrayList<>();
        run.add(section);
        for (int at = from; at < to; at++) {
            run.add(OverlayPackets.block(blocks.x(at), blocks.y(at), blocks.z(at), blocks.state(at)));
        }
        return new PacketRun(run);
    }

    private static boolean covers(BlockPos pos, OverlaySnapshot<MojangLook> snapshot) {
        return snapshot.stateAt(pos.getX(), pos.getY(), pos.getZ()) != OverlayBlockIndex.NONE;
    }
}
