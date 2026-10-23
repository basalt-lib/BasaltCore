package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InboundFilterTest {

    private static final ViewSnapshot<String> VIRTUAL = ViewSnapshot.virtual(new ChunkKey(64, 64), "copy");
    private static final BlockPosition INSIDE = new BlockPosition(1029, -58, 1029);
    private static final BlockPosition EAST_EDGE = new BlockPosition(1039, -58, 1029);
    private static final BlockPosition WEST_OUTSIDE = new BlockPosition(1023, -58, 1029);

    @ParameterizedTest
    @EnumSource(value = InboundKind.class, names = {"BLOCK_ACTION", "USE_ON"})
    void consumesABlockPacketInsideTheVirtualChunk(InboundKind kind) {
        assertEquals(InboundVerdict.CONSUME, InboundFilter.decide(InboundFacts.about(kind, INSIDE, BlockFace.UP), VIRTUAL));
    }

    @ParameterizedTest
    @EnumSource(value = InboundKind.class, names = {"BLOCK_ACTION", "USE_ON"})
    void passesEverythingOfAPlayerInTheRealWorld(InboundKind kind) {
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.about(kind, INSIDE, BlockFace.UP), ViewSnapshot.real()));
    }

    @Test
    void passesPacketsWithoutABlock() {
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.unrelated(), VIRTUAL));
    }

    @Test
    void consumesAUseOnARealBlockThatWouldPlaceInTheVirtualChunk() {
        assertEquals(InboundVerdict.CONSUME, InboundFilter.decide(InboundFacts.about(InboundKind.USE_ON, WEST_OUTSIDE, BlockFace.EAST), VIRTUAL));
        assertEquals(InboundVerdict.CONSUME, InboundFilter.decide(InboundFacts.about(InboundKind.USE_ON, EAST_EDGE, BlockFace.EAST), VIRTUAL));
    }

    @Test
    void passesAUseThatStaysInTheRealWorld() {
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.about(InboundKind.USE_ON, WEST_OUTSIDE, BlockFace.WEST), VIRTUAL));
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.about(InboundKind.USE_ON, WEST_OUTSIDE, BlockFace.UP), VIRTUAL));
    }

    @Test
    void passesABreakOfARealBlockBesideTheVirtualChunk() {
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.about(InboundKind.BLOCK_ACTION, WEST_OUTSIDE, BlockFace.EAST), VIRTUAL));
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(
                InboundFacts.about(InboundKind.BLOCK_ACTION, new BlockPosition(1029, -58, 1040), BlockFace.NORTH), VIRTUAL));
    }

    @Test
    void consumesOnlyAnInteractionWithAnEntityOfTheView() {
        ViewSnapshot<String> withZombie = VIRTUAL.withEntities(Set.of(1001));

        assertEquals(InboundVerdict.CONSUME, InboundFilter.decide(InboundFacts.entity(1001), withZombie));
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.entity(1002), withZombie));
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.entity(-1001), withZombie));
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.entity(1001), VIRTUAL));
        assertEquals(InboundVerdict.PASS, InboundFilter.decide(InboundFacts.entity(1001), ViewSnapshot.real()));
    }

    @Test
    void refusesFactsThatDoNotMatchTheirKind() {
        assertThrows(IllegalArgumentException.class, () -> InboundFacts.about(InboundKind.ENTITY, INSIDE, BlockFace.UP));
        assertThrows(IllegalArgumentException.class, () -> new InboundFacts(InboundKind.USE_ON, INSIDE, BlockFace.UP, 7));
        assertThrows(IllegalArgumentException.class, () -> InboundFacts.about(InboundKind.OTHER, INSIDE, BlockFace.UP));
        assertThrows(IllegalArgumentException.class, () -> InboundFacts.about(InboundKind.USE_ON, INSIDE, null));
        assertThrows(IllegalArgumentException.class, () -> InboundFacts.about(InboundKind.BLOCK_ACTION, null, BlockFace.UP));
    }
}
