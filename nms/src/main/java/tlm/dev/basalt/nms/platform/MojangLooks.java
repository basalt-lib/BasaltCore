package tlm.dev.basalt.nms.platform;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class MojangLooks {

    public static final String ITEM = "minecraft:item";
    public static final String ITEM_DISPLAY = "minecraft:item_display";

    public boolean showsItem(String type) {
        return ITEM.equals(type) || ITEM_DISPLAY.equals(type);
    }

    public Optional<MojangLook> look(World world, String type, Optional<ItemStack> item, Optional<Component> name) {
        ServerLevel level = ((CraftWorld) world).getHandle();
        Optional<Entity> template = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(type))
                .map(found -> found.create(level, EntitySpawnReason.COMMAND));
        return template.map(entity -> {
            entity.setNoGravity(true);
            entity.setSilent(true);
            entity.setCustomName(name.map(PaperAdventure::asVanilla).orElse(null));
            entity.setCustomNameVisible(name.isPresent());
            net.minecraft.world.item.ItemStack stack = item.map(CraftItemStack::asNMSCopy).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            if (entity instanceof ItemEntity itemEntity) {
                itemEntity.setItem(stack);
            }
            if (entity instanceof Display.ItemDisplay display) {
                display.setItemStack(stack);
            }
            return new MojangLook(entity.getType(), entity.getEntityData().packAll(), item.map(ItemStack::clone).orElse(null), name.orElse(null));
        });
    }
}
