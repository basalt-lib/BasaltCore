package tlm.dev.basalt.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class FakeEntityDisplay implements EntityDisplay {

    final List<String> events = new ArrayList<>();

    @Override
    public void spawn(UUID viewer, VirtualEntity entity) {
        events.add(name(viewer) + ": spawn #" + entity.id() + " " + describe(entity));
    }

    @Override
    public void move(UUID viewer, VirtualEntity entity) {
        events.add(name(viewer) + ": move #" + entity.id() + " to " + entity.position().x() + " " + entity.position().z());
    }

    @Override
    public void update(UUID viewer, VirtualEntity entity) {
        events.add(name(viewer) + ": update #" + entity.id() + " " + describe(entity));
    }

    @Override
    public void hurt(UUID viewer, VirtualEntity entity, UUID attacker) {
        events.add(name(viewer) + ": hurt #" + entity.id() + " health " + entity.health() + " by " + name(attacker));
    }

    @Override
    public void die(UUID viewer, VirtualEntity entity, Optional<UUID> killer) {
        events.add(name(viewer) + ": die #" + entity.id() + " by " + killer.map(FakeEntityDisplay::name).orElse("none"));
    }

    @Override
    public void remove(UUID viewer, List<Integer> ids) {
        events.add(name(viewer) + ": remove " + ids);
    }

    @Override
    public void removeDead(UUID viewer, List<Integer> ids) {
        events.add(name(viewer) + ": remove dead " + ids);
    }

    static String describe(VirtualEntity entity) {
        return entity.type() + " health " + entity.health() + "/" + entity.life().maxHealth()
                + entity.looks().name().map(name -> " named " + name).orElse("")
                + (entity.looks().silent() ? " silent" : "");
    }

    static String name(UUID player) {
        return switch ((int) player.getLeastSignificantBits()) {
            case 0xA -> "A";
            case 0xB -> "B";
            default -> "C";
        };
    }
}
