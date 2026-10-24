package tlm.dev.basalt.core.entity;

public record EntityLife(double health, double maxHealth) {

    public static final double DEFAULT_MAX_HEALTH = 20;
    public static final double MAX_HEALTH_LIMIT = 1024;

    public EntityLife {
        if (!Double.isFinite(maxHealth) || maxHealth <= 0 || maxHealth > MAX_HEALTH_LIMIT) {
            throw new IllegalArgumentException("max health goes above 0 up to " + MAX_HEALTH_LIMIT + ": " + maxHealth);
        }
        if (!Double.isFinite(health) || health < 0 || health > maxHealth) {
            throw new IllegalArgumentException("health goes from 0 to the max health " + maxHealth + ": " + health);
        }
    }

    public static EntityLife full(double maxHealth) {
        return new EntityLife(maxHealth, maxHealth);
    }

    public EntityLife withHealth(double changed) {
        return new EntityLife(changed, maxHealth);
    }

    public EntityLife withMaxHealth(double changed) {
        return new EntityLife(Math.min(health, changed), changed);
    }

    public EntityLife hurt(double damage) {
        if (!Double.isFinite(damage) || damage < 0) {
            throw new IllegalArgumentException("damage is 0 or more: " + damage);
        }
        return new EntityLife(Math.max(0, health - damage), maxHealth);
    }

    public boolean isDead() {
        return health == 0;
    }
}
