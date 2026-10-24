package tlm.dev.basalt.core.chunk;

public record PatternBox(LocalBox box, String block) {

    public PatternBox {
        if (box == null || block == null || block.isBlank()) {
            throw new IllegalArgumentException("a pattern box needs its box and its block id");
        }
    }
}
