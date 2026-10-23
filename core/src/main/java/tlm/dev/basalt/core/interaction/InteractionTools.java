package tlm.dev.basalt.core.interaction;

public record InteractionTools(InteractionWorld world, PlayerBlocks blocks, PlayerEntities entities, InteractionReplies replies) {

    public InteractionTools {
        if (world == null || blocks == null || entities == null || replies == null) {
            throw new IllegalArgumentException("interaction tools need the world, the player blocks, the player entities and the replies");
        }
    }
}
