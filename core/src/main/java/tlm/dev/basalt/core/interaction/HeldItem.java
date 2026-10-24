package tlm.dev.basalt.core.interaction;

import java.util.OptionalInt;

public record HeldItem(OptionalInt block, double attackDamage) {

    public static final double MIN_DAMAGE = 1;
    public static final HeldItem BARE_HAND = new HeldItem(OptionalInt.empty(), MIN_DAMAGE);

    public HeldItem {
        if (block == null) {
            throw new IllegalArgumentException("a held item places a block or none");
        }
        if (!Double.isFinite(attackDamage) || attackDamage < 0) {
            throw new IllegalArgumentException("an attack damage is finite and 0 or more: " + attackDamage);
        }
    }

    public double damage() {
        return Math.max(MIN_DAMAGE, attackDamage);
    }
}
