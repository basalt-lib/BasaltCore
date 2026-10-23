package tlm.dev.basalt.core.interaction;

public record Reach(double blocks, double entities) {

    public Reach {
        if (!Double.isFinite(blocks) || blocks < 0 || !Double.isFinite(entities) || entities < 0) {
            throw new IllegalArgumentException("a reach is a distance of 0 or more: " + blocks + ", " + entities);
        }
    }
}
