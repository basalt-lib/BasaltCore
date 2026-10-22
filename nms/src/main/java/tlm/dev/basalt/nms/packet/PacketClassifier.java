package tlm.dev.basalt.nms.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
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
            case ClientboundSoundPacket sound -> positional(PacketKind.SOUND, sound.getX(), sound.getZ());
            case ClientboundLevelParticlesPacket particles -> positional(PacketKind.PARTICLES, particles.getX(), particles.getZ());
            case ClientboundLevelEventPacket event -> event.isGlobalEvent() ? PacketFacts.unrelated() : positional(PacketKind.LEVEL_EVENT, event.getPos());
            case ClientboundBlockEventPacket event -> positional(PacketKind.BLOCK_EVENT, event.getPos());
            case ClientboundBlockDestructionPacket destruction -> positional(PacketKind.BLOCK_DESTRUCTION, destruction.getPos());
            case ClientboundExplodePacket explosion -> positional(PacketKind.EXPLOSION, explosion.center().x(), explosion.center().z());
            default -> PacketFacts.unrelated();
        };
    }

    static ChunkKey key(ChunkPos pos) {
        return new ChunkKey(pos.x, pos.z);
    }

    private static ChunkKey key(BlockPos pos) {
        return ChunkKey.ofBlock(pos.getX(), pos.getZ());
    }

    private static PacketFacts positional(PacketKind kind, BlockPos pos) {
        return positional(kind, pos.getX(), pos.getZ());
    }

    private static PacketFacts positional(PacketKind kind, double x, double z) {
        return ChunkKey.containing(x, z).map(chunk -> PacketFacts.about(kind, chunk)).orElse(PacketFacts.unrelated());
    }

    private static PacketFacts sectionFacts(ClientboundSectionBlocksUpdatePacket section) {
        AtomicReference<ChunkKey> chunk = new AtomicReference<>();
        section.runUpdates((pos, state) -> chunk.compareAndSet(null, key(pos)));
        return chunk.get() == null ? PacketFacts.unrelated() : PacketFacts.about(PacketKind.SECTION, chunk.get());
    }
}
