package tlm.dev.basalt.nms.entity;

import io.netty.buffer.Unpooled;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Server;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.entity.EntityDisplay;
import tlm.dev.basalt.core.entity.VirtualEntity;
import tlm.dev.basalt.nms.port.PlayerChannels;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MojangEntityDisplay implements EntityDisplay {

    private static final float VOLUME = 1.0F;
    private static final float PITCH_SPREAD = 0.2F;

    private final Server server;
    private final PlayerChannels channels;
    private final RandomSource random = RandomSource.create();

    public MojangEntityDisplay(Server server, PlayerChannels channels) {
        this.server = server;
        this.channels = channels;
    }

    @Override
    public void spawn(UUID player, VirtualEntity entity) {
        Player online = server.getPlayer(player);
        if (online == null) {
            return;
        }
        ServerPlayer handle = ((CraftPlayer) online).getHandle();
        Optional<Entity> template = template(handle, entity);
        if (template.isEmpty()) {
            return;
        }
        Vec3 at = position(entity);
        float yaw = entity.rotation().yaw();
        handle.connection.send(new ClientboundBundlePacket(List.of(
                new ClientboundAddEntityPacket(entity.id(), entity.uuid(), at.x, at.y, at.z,
                        entity.rotation().pitch(), yaw, template.get().getType(), 0, Vec3.ZERO, yaw),
                new ClientboundSetEntityDataPacket(entity.id(), template.get().getEntityData().packAll()))));
    }

    @Override
    public void move(UUID player, VirtualEntity entity) {
        send(player, ClientboundTeleportEntityPacket.teleport(entity.id(),
                new PositionMoveRotation(position(entity), Vec3.ZERO, entity.rotation().yaw(), entity.rotation().pitch()), Set.of(), false));
    }

    @Override
    public void update(UUID player, VirtualEntity entity) {
        Player online = server.getPlayer(player);
        if (online == null) {
            return;
        }
        template(((CraftPlayer) online).getHandle(), entity).ifPresent(template ->
                channels.writeDirect(online, new ClientboundSetEntityDataPacket(entity.id(), template.getEntityData().packAll())));
    }

    @Override
    public void hurt(UUID player, VirtualEntity entity, UUID attacker) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, damage(attackerOf(online, attacker), entity));
            sound(entity, "hurt").ifPresent(sound -> channels.writeDirect(online, sound));
        }
    }

    @Override
    public void die(UUID player, VirtualEntity entity, Optional<UUID> killer) {
        Player online = server.getPlayer(player);
        if (online != null) {
            killer.ifPresent(attacker -> channels.writeDirect(online, damage(attackerOf(online, attacker), entity)));
            channels.writeDirect(online, event(entity.id(), EntityEvent.DEATH));
            sound(entity, "death").ifPresent(sound -> channels.writeDirect(online, sound));
        }
    }

    @Override
    public void remove(UUID player, List<Integer> ids) {
        send(player, new ClientboundRemoveEntitiesPacket(ids.stream().mapToInt(Integer::intValue).toArray()));
    }

    @Override
    public void removeDead(UUID player, List<Integer> ids) {
        Player online = server.getPlayer(player);
        if (online != null) {
            ids.forEach(id -> channels.writeDirect(online, event(id, EntityEvent.POOF)));
            channels.writeDirect(online, new ClientboundRemoveEntitiesPacket(ids.stream().mapToInt(Integer::intValue).toArray()));
        }
    }

    private ServerPlayer attackerOf(Player viewer, UUID attacker) {
        Player hitter = server.getPlayer(attacker);
        return ((CraftPlayer) (hitter != null ? hitter : viewer)).getHandle();
    }

    private void send(UUID player, Packet<?> packet) {
        Player online = server.getPlayer(player);
        if (online != null) {
            channels.writeDirect(online, packet);
        }
    }

    private static Optional<Entity> template(ServerPlayer viewer, VirtualEntity entity) {
        Optional<Entity> created = EntityType.byString(entity.type())
                .map(type -> type.create(viewer.level(), EntitySpawnReason.COMMAND));
        created.ifPresent(template -> {
            template.setCustomName(entity.looks().name()
                    .map(json -> PaperAdventure.asVanilla(GsonComponentSerializer.gson().deserialize(json)))
                    .orElse(null));
            template.setCustomNameVisible(entity.looks().name().isPresent());
            template.setNoGravity(true);
            template.setSilent(true);
            if (template instanceof LivingEntity living) {
                AttributeInstance maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(entity.life().maxHealth());
                }
                living.setHealth((float) entity.health());
            }
        });
        return created;
    }

    private static ClientboundDamageEventPacket damage(ServerPlayer attacker, VirtualEntity entity) {
        return new ClientboundDamageEventPacket(entity.id(), attacker.damageSources().playerAttack(attacker).typeHolder(),
                attacker.getId(), attacker.getId(), Optional.empty());
    }

    private static ClientboundEntityEventPacket event(int id, byte event) {
        FriendlyByteBuf fields = new FriendlyByteBuf(Unpooled.buffer());
        fields.writeInt(id);
        fields.writeByte(event);
        return ClientboundEntityEventPacket.STREAM_CODEC.decode(fields);
    }

    private Optional<ClientboundSoundPacket> sound(VirtualEntity entity, String kind) {
        if (entity.looks().silent()) {
            return Optional.empty();
        }
        Optional<EntityType<?>> type = EntityType.byString(entity.type());
        Identifier typeKey = Identifier.tryParse(entity.type());
        if (type.isEmpty() || typeKey == null) {
            return Optional.empty();
        }
        Identifier soundKey = Identifier.fromNamespaceAndPath(typeKey.getNamespace(), "entity." + typeKey.getPath() + "." + kind);
        Optional<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getOptional(soundKey);
        SoundSource source = type.get().getCategory() == MobCategory.MONSTER ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
        Vec3 at = position(entity);
        float pitch = (random.nextFloat() - random.nextFloat()) * PITCH_SPREAD + 1.0F;
        return sound.map(event -> new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event), source,
                at.x, at.y, at.z, VOLUME, pitch, random.nextLong()));
    }

    private static Vec3 position(VirtualEntity entity) {
        return new Vec3(entity.position().x(), entity.position().y(), entity.position().z());
    }
}
