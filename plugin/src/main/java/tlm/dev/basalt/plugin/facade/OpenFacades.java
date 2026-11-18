package tlm.dev.basalt.plugin.facade;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class OpenFacades<F> {

    private final Map<UUID, F> facades = new LinkedHashMap<>();

    void add(UUID id, F facade, Collection<UUID> open) {
        prune(open);
        facades.put(id, facade);
    }

    Optional<F> find(UUID id) {
        return Optional.ofNullable(facades.get(id));
    }

    List<F> all(Collection<UUID> open) {
        prune(open);
        return List.copyOf(facades.values());
    }

    int size() {
        return facades.size();
    }

    private void prune(Collection<UUID> open) {
        facades.keySet().retainAll(Set.copyOf(open));
    }
}
