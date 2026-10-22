package tlm.dev.basalt.core.interaction;

public record InteractionTools(InteractionWorld world, PlayerBlocks blocks, InteractionReplies replies) {

    public InteractionTools {
        if (world == null || blocks == null || replies == null) {
            throw new IllegalArgumentException("interaction tools need the world, the player blocks and the replies");
        }
    }
}
