package tlm.dev.basalt.core.interaction;

public record PlaceDecision(boolean allowed, boolean consume) {

    public static final PlaceDecision REFUSED = new PlaceDecision(false, false);
}
