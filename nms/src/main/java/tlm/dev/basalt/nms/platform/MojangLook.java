package tlm.dev.basalt.nms.platform;

import net.kyori.adventure.text.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public final class MojangLook {

    private final EntityType<?> type;
    private final List<SynchedEntityData.DataValue<?>> data;
    private final ItemStack item;
    private final Component name;

    MojangLook(EntityType<?> type, List<SynchedEntityData.DataValue<?>> data, ItemStack item, Component name) {
        this.type = type;
        this.data = List.copyOf(data);
        this.item = item;
        this.name = name;
    }

    public EntityType<?> type() {
        return type;
    }

    public List<SynchedEntityData.DataValue<?>> data() {
        return data;
    }

    public Optional<ItemStack> item() {
        return Optional.ofNullable(item).map(ItemStack::clone);
    }

    public Optional<Component> name() {
        return Optional.ofNullable(name);
    }
}
