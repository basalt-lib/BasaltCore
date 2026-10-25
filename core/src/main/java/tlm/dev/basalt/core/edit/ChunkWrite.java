package tlm.dev.basalt.core.edit;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ChunkWrite(ChunkKey chunk, List<SectionWrite> sections) {

    public ChunkWrite {
        if (chunk == null || sections == null) {
            throw new IllegalArgumentException("a chunk write needs its chunk and its sections");
        }
        sections = List.copyOf(sections);
        Set<Integer> seen = new HashSet<>();
        for (SectionWrite section : sections) {
            if (!seen.add(section.sectionIndex())) {
                throw new IllegalArgumentException("section " + section.sectionIndex() + " written twice in chunk " + chunk);
            }
        }
    }
}
