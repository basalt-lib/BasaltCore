package tlm.dev.basalt.nms.blueprint;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import tlm.dev.basalt.core.blueprint.BlockNames;

import java.util.OptionalInt;

public final class MojangBlockNames implements BlockNames {

    @Override
    public OptionalInt stateOf(String blockState) {
        try {
            return OptionalInt.of(Block.getId(BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, blockState, false).blockState()));
        } catch (CommandSyntaxException unknown) {
            return OptionalInt.empty();
        }
    }

    @Override
    public boolean isAir(int state) {
        return Block.stateById(state).isAir();
    }
}
