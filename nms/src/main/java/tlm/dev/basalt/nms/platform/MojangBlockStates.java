package tlm.dev.basalt.nms.platform;

import net.minecraft.world.level.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;

public final class MojangBlockStates {

    public int stateOf(BlockData data) {
        if (!(data instanceof CraftBlockData craft)) {
            throw new IllegalArgumentException("block data does not come from this server: " + data);
        }
        return Block.getId(craft.getState());
    }

    public BlockData dataOf(int state) {
        if (state < 0 || state >= Block.BLOCK_STATE_REGISTRY.size()) {
            throw new IllegalArgumentException("unknown block state " + state);
        }
        return CraftBlockData.fromData(Block.stateById(state));
    }
}
