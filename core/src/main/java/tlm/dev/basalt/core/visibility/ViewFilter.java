package tlm.dev.basalt.core.visibility;

import java.util.ArrayList;
import java.util.List;

public final class ViewFilter {

    private ViewFilter() {
    }

    public static Verdict decide(PacketFacts facts, ViewSnapshot snapshot) {
        if (!snapshot.hides(facts.chunk())) {
            return Verdict.PASS;
        }
        return switch (facts.kind()) {
            case CHUNK -> Verdict.REPLACE_WITH_VIRTUAL;
            case BLOCK, SECTION, BLOCK_ENTITY, LIGHT, BIOMES,
                 SOUND, PARTICLES, LEVEL_EVENT, BLOCK_EVENT, BLOCK_DESTRUCTION, EXPLOSION -> Verdict.DROP;
            case CHUNK_FORGET, OTHER -> Verdict.PASS;
        };
    }

    public static List<Verdict> decideEach(List<PacketFacts> contents, ViewSnapshot snapshot) {
        List<Verdict> verdicts = new ArrayList<>(contents.size());
        for (PacketFacts facts : contents) {
            verdicts.add(decide(facts, snapshot));
        }
        return List.copyOf(verdicts);
    }
}
