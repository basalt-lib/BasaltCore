package tlm.dev.basalt.core.interaction;

public sealed interface InteractionRequest permits BlockRequest, EntityRequest {

    Hand hand();
}
