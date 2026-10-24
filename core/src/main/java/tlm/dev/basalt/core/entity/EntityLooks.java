package tlm.dev.basalt.core.entity;

import java.util.Optional;

public record EntityLooks(Optional<String> name, boolean silent) {

    public static final int MAX_NAME_LENGTH = 4096;
    public static final EntityLooks PLAIN = new EntityLooks(Optional.empty(), false);

    public EntityLooks {
        if (name == null) {
            throw new IllegalArgumentException("looks name an entity or not");
        }
        if (name.filter(text -> text.isEmpty() || text.length() > MAX_NAME_LENGTH).isPresent()) {
            throw new IllegalArgumentException("a name has from 1 to " + MAX_NAME_LENGTH + " characters");
        }
    }

    public EntityLooks withName(Optional<String> changed) {
        return new EntityLooks(changed, silent);
    }

    public EntityLooks withSilent(boolean changed) {
        return new EntityLooks(name, changed);
    }
}
