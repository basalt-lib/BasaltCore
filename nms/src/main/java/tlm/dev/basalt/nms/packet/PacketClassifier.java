package tlm.dev.basalt.nms.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.visibility.PacketFacts;
import tlm.dev.basalt.core.visibility.PacketKind;

import java.util.concurrent.atomic.AtomicReference;

final class PacketClassifier {

    private PacketClassifier() {
    }

    static PacketFacts classify(Packet<?> packet) {
        return switch (packet) {
            case ClientboundLevelChunkWithLightPacket chunk -> PacketFacts.about(PacketKind.CHUNK, new ChunkKey(chunk.getX(), chunk.getZ()));
            case ClientboundForgetLevelChunkPacket forget -> PacketFacts.about(PacketKind.CHUNK_FORGET, key(forget.pos()));
            case ClientboundBlockUpdatePacket block -> PacketFacts.about(PacketKind.BLOCK, key(block.getPos()));
            case ClientboundSectionBlocksUpdatePacket section -> sectionFacts(section);
            case ClientboundBlockEntityDataPacket blockEntity -> PacketFacts.about(PacketKind.BLOCK_ENTITY, key(blockEntity.getPos()));
            case ClientboundLightUpdatePacket light -> PacketFacts.about(PacketKind.LIGHT, new ChunkKey(light.getX(), light.getZ()));
            default -> PacketFacts.unrelated();
        };
    }

    static ChunkKey key(ChunkPos pos) {
        return new ChunkKey(pos.x, pos.z);
    }

    private static ChunkKey key(BlockPos pos) {
        return ChunkKey.ofBlock(pos.getX(), pos.getZ());
    }

    private static PacketFacts sectionFacts(ClientboundSectionBlocksUpdatePacket section) {
        AtomicReference<ChunkKey> chunk = new AtomicReference<>();
        section.runUpdates((pos, state) -> chunk.compareAndSet(null, key(pos)));
        return chunk.get() == null ? PacketFacts.unrelated() : PacketFacts.about(PacketKind.SECTION, chunk.get());
    }
}
