package net.fretux.adaptivemobs.scaling;

import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Applies moderate, attribute-based stat scaling to a mob for a given tier. Scaling is applied
 * exactly once per mob instance and recorded in {@link AdaptiveMobData} so reloads don't stack it.
 */
public final class MobStatScaler {

    // Stable UUIDs so a given modifier is only ever added once per attribute instance.
    private static final UUID HEALTH_UUID = UUID.fromString("9f1d0a5e-1f2a-4c3b-8a11-000000000001");
    private static final UUID DAMAGE_UUID = UUID.fromString("9f1d0a5e-1f2a-4c3b-8a11-000000000002");
    private static final UUID SPEED_UUID = UUID.fromString("9f1d0a5e-1f2a-4c3b-8a11-000000000003");
    private static final UUID FOLLOW_UUID = UUID.fromString("9f1d0a5e-1f2a-4c3b-8a11-000000000004");
    private static final UUID ARMOR_UUID = UUID.fromString("9f1d0a5e-1f2a-4c3b-8a11-000000000005");
    private static final UUID KNOCKBACK_UUID = UUID.fromString("9f1d0a5e-1f2a-4c3b-8a11-000000000006");

    private MobStatScaler() {
    }

    /**
     * Applies tier scaling to the mob and heals it to its new maximum. No-op if scaling is
     * disabled, the tier is 0, or the mob has already been scaled.
     */
    public static void applyScaling(Mob mob, int tier) {
        if (!AMConfig.STAT_SCALING_ENABLED.get() || tier <= 0) {
            AdaptiveMobData.markScaled(mob, Math.max(tier, 0));
            return;
        }
        if (AdaptiveMobData.isScaled(mob)) {
            return;
        }

        addMultiplier(mob, Attributes.MAX_HEALTH, HEALTH_UUID, "am_health",
                AMConfig.tierValue(AMConfig.HEALTH_SCALE.get(), tier));
        addMultiplier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_UUID, "am_damage",
                AMConfig.tierValue(AMConfig.DAMAGE_SCALE.get(), tier));
        addMultiplier(mob, Attributes.MOVEMENT_SPEED, SPEED_UUID, "am_speed",
                AMConfig.tierValue(AMConfig.SPEED_SCALE.get(), tier));
        addMultiplier(mob, Attributes.FOLLOW_RANGE, FOLLOW_UUID, "am_follow",
                AMConfig.tierValue(AMConfig.FOLLOW_RANGE_SCALE.get(), tier));
        addAddition(mob, Attributes.ARMOR, ARMOR_UUID, "am_armor",
                AMConfig.tierValue(AMConfig.ARMOR_BONUS.get(), tier));
        addAddition(mob, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_UUID, "am_kb",
                AMConfig.tierValue(AMConfig.KNOCKBACK_RESIST.get(), tier));

        // Heal to the new maximum so the health bonus is actually felt.
        mob.setHealth(mob.getMaxHealth());

        AdaptiveMobData.markScaled(mob, tier);
    }

    private static void addMultiplier(Mob mob, Attribute attribute, UUID uuid, String name, double value) {
        if (value <= 0.0) {
            return;
        }
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || instance.getModifier(uuid) != null) {
            return;
        }
        instance.addPermanentModifier(
                new AttributeModifier(uuid, name, value, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void addAddition(Mob mob, Attribute attribute, UUID uuid, String name, double value) {
        if (value <= 0.0) {
            return;
        }
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null || instance.getModifier(uuid) != null) {
            return;
        }
        instance.addPermanentModifier(
                new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
    }
}
