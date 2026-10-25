package tlm.dev.basalt.core.blueprint;

public record PlanSettings(int maxBlocks, int maxFileBytes) {

    public static final int MAX_BLOCKS_LIMIT = 16_777_216;
    public static final int MIN_FILE_BYTES = 1024;
    public static final int MAX_FILE_BYTES_LIMIT = 268_435_456;

    public PlanSettings {
        if (maxBlocks < 1 || maxBlocks > MAX_BLOCKS_LIMIT) {
            throw new IllegalArgumentException("blocks per plan must be from 1 to " + MAX_BLOCKS_LIMIT + ": " + maxBlocks);
        }
        if (maxFileBytes < MIN_FILE_BYTES || maxFileBytes > MAX_FILE_BYTES_LIMIT) {
            throw new IllegalArgumentException("file bytes must be from " + MIN_FILE_BYTES + " to " + MAX_FILE_BYTES_LIMIT + ": " + maxFileBytes);
        }
    }
}
