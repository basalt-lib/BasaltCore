package tlm.dev.basalt.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class FakeEntityDisplay implements EntityDisplay {

    final List<String> events = new ArrayList<>();

    @Override
    public void spawn(UUID player, VirtualEntity entity) {
        events.add("spawn #" + entity.id() + " " + entity.type() + " health " + entity.health());
    }

    @Override
    public void move(UUID player, VirtualEntity entity) {
        events.add("move #" + entity.id() + " to " + entity.position().x() + " " + entity.position().z());
    }

    @Override
    public void hurt(UUID player, VirtualEntity entity) {
        events.add("hurt #" + entity.id() + " health " + entity.health());
    }

    @Override
    public void die(UUID player, VirtualEntity entity) {
        events.add("die #" + entity.id());
    }

    @Override
    public void remove(UUID player, List<Integer> ids) {
        events.add("remove " + ids);
    }
}
