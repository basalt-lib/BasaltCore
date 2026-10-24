package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ViewFilterTest {

    private static final ChunkKey HIDDEN = new ChunkKey(0, 0);
    private static final ChunkKey NEIGHBOUR = new ChunkKey(1, 0);
    private static final ViewSnapshot VIRTUAL = ViewSnapshot.of(new UUID(0, 1), ChunkRange.of(HIDDEN));

    @Test
    void replacesTheRealChunkWithTheVirtualOne() {
        assertEquals(Verdict.REPLACE_WITH_VIRTUAL, ViewFilter.decide(PacketFacts.about(PacketKind.CHUNK, HIDDEN), VIRTUAL));
    }

    @ParameterizedTest
    @EnumSource(value = PacketKind.class, names = {"BLOCK", "SECTION", "BLOCK_ENTITY", "LIGHT", "BIOMES"})
    void dropsRealChangesInsideTheHiddenChunk(PacketKind kind) {
        assertEquals(Verdict.DROP, ViewFilter.decide(PacketFacts.about(kind, HIDDEN), VIRTUAL));
    }

    @ParameterizedTest
    @EnumSource(value = PacketKind.class, names = {"SOUND", "PARTICLES", "LEVEL_EVENT", "BLOCK_EVENT", "BLOCK_DESTRUCTION", "EXPLOSION"})
    void dropsSoundsAndEffectsOfTheRealWorldInsideTheHiddenChunk(PacketKind kind) {
        assertEquals(Verdict.DROP, ViewFilter.decide(PacketFacts.about(kind, HIDDEN), VIRTUAL));
    }

    @Test
    void letsTheClientForgetTheHiddenChunk() {
        assertEquals(Verdict.PASS, ViewFilter.decide(PacketFacts.about(PacketKind.CHUNK_FORGET, HIDDEN), VIRTUAL));
    }

    @ParameterizedTest
    @EnumSource(value = PacketKind.class, names = "OTHER", mode = EnumSource.Mode.EXCLUDE)
    void passesEveryPacketAboutAnotherChunk(PacketKind kind) {
        assertEquals(Verdict.PASS, ViewFilter.decide(PacketFacts.about(kind, NEIGHBOUR), VIRTUAL));
    }

    @ParameterizedTest
    @EnumSource(value = PacketKind.class, names = "OTHER", mode = EnumSource.Mode.EXCLUDE)
    void passesEverythingInTheRealWorld(PacketKind kind) {
        assertEquals(Verdict.PASS, ViewFilter.decide(PacketFacts.about(kind, HIDDEN), ViewSnapshot.real()));
    }

    @Test
    void passesUnrelatedPackets() {
        assertEquals(Verdict.PASS, ViewFilter.decide(PacketFacts.unrelated(), VIRTUAL));
    }

    @Test
    void decidesEachPacketOfABundle() {
        List<PacketFacts> bundle = List.of(
                PacketFacts.unrelated(),
                PacketFacts.about(PacketKind.BLOCK, HIDDEN),
                PacketFacts.about(PacketKind.BLOCK, NEIGHBOUR),
                PacketFacts.about(PacketKind.CHUNK, HIDDEN));

        assertEquals(List.of(Verdict.PASS, Verdict.DROP, Verdict.PASS, Verdict.REPLACE_WITH_VIRTUAL), ViewFilter.decideEach(bundle, VIRTUAL));
    }

    @Test
    void decidesAnEmptyBundle() {
        assertEquals(List.of(), ViewFilter.decideEach(List.of(), VIRTUAL));
    }
}
