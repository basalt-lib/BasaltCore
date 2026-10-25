package tlm.dev.basalt.nms.blueprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import java.io.IOException;
import java.nio.file.Path;

public final class MojangStructures {

    private MojangStructures() {
    }

    public static boolean placeReal(World world, Path file, long maxBytes, int x, int y, int z) throws IOException {
        ServerLevel level = ((CraftWorld) world).getHandle();
        CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.create(maxBytes));
        StructureTemplate template = new StructureTemplate();
        template.load(level.holderLookup(Registries.BLOCK), tag);
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
        BlockPos origin = new BlockPos(x, y, z);
        return template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
    }
}
