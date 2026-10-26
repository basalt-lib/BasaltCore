package tlm.dev.basalt.plugin.command;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

final class WorldQuery {

    static final int MIN_PREFIX = 8;
    private static final Pattern ID_PART = Pattern.compile("[0-9a-f-]{" + MIN_PREFIX + ",36}");

    private WorldQuery() {
    }

    static Pick pick(String query, Map<String, UUID> named, List<UUID> open) {
        UUID byName = named.get(query);
        if (byName != null && open.contains(byName)) {
            return new Pick.Found(byName);
        }
        String lowered = query.toLowerCase(Locale.ROOT);
        if (!ID_PART.matcher(lowered).matches()) {
            return new Pick.UnknownName();
        }
        List<UUID> matching = open.stream().filter(id -> id.toString().startsWith(lowered)).toList();
        return switch (matching.size()) {
            case 0 -> new Pick.UnknownId();
            case 1 -> new Pick.Found(matching.getFirst());
            default -> new Pick.Ambiguous(matching.size());
        };
    }

    static boolean ownedBy(String owner, Optional<String> wanted) {
        return wanted.map(name -> name.equalsIgnoreCase(owner)).orElse(true);
    }

    sealed interface Pick {

        record Found(UUID id) implements Pick {
        }

        record UnknownName() implements Pick {
        }

        record UnknownId() implements Pick {
        }

        record Ambiguous(int count) implements Pick {
        }
    }
}
