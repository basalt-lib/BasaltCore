package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.Point;

import java.util.Optional;

public record EntityRequest(int entity, EntityAction action, Hand hand, Optional<Point> at) implements InteractionRequest {

    public EntityRequest {
        if (action == null || hand == null || at == null) {
            throw new IllegalArgumentException("an entity request needs an action, a hand and its point or none");
        }
        if ((action == EntityAction.INTERACT_AT) != at.isPresent()) {
            throw new IllegalArgumentException(action == EntityAction.INTERACT_AT ? "an interaction at a point needs the point" : action + " has no point");
        }
    }

    public static EntityRequest attack(int entity) {
        return new EntityRequest(entity, EntityAction.ATTACK, Hand.MAIN, Optional.empty());
    }

    public static EntityRequest interact(int entity, Hand hand) {
        return new EntityRequest(entity, EntityAction.INTERACT, hand, Optional.empty());
    }

    public static EntityRequest interactAt(int entity, Hand hand, Point at) {
        return new EntityRequest(entity, EntityAction.INTERACT_AT, hand, Optional.of(at));
    }
}
