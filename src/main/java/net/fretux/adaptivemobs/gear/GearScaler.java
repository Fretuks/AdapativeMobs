package net.fretux.adaptivemobs.gear;

import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Upgrades a mob's vanilla equipment based on its tier. Only vanilla items are used, armor is
 * partial more often than full, existing/better gear is respected (never downgraded), and added
 * gear is given a 0% drop chance so it stays a challenge rather than a farm.
 */
public final class GearScaler {

    // Material ranks (low -> high) used both to pick new gear and to compare against existing gear.
    private static final Item[] HELMETS = {Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET};
    private static final Item[] CHESTS = {Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE};
    private static final Item[] LEGS = {Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS};
    private static final Item[] BOOTS = {Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS};
    private static final Item[] SWORDS = {Items.WOODEN_SWORD, Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD};

    private GearScaler() {
    }

    public static void applyGear(Mob mob, int tier) {
        if (!AMConfig.GEAR_SCALING_ENABLED.get() || tier <= 0) {
            return;
        }
        RandomSource random = mob.getRandom();

        if (mob instanceof AbstractSkeleton skeleton) {
            applyArmor(skeleton, tier, random);
            upgradeBow(skeleton, tier, random);
            if (!skeleton.getMainHandItem().is(Items.BOW)) {
                upgradeMeleeWeapon(skeleton, tier, random);
            }
        } else if (mob instanceof Zombie zombie) {
            // Covers zombies, husks, drowned and zombified piglins (all extend Zombie).
            applyArmor(zombie, tier, random);
            upgradeMeleeWeapon(zombie, tier, random);
        } else if (mob instanceof Pillager pillager) {
            upgradeCrossbow(pillager, tier, random);
        }
        // TODO: Piglin gear is intentionally left alone to avoid disturbing bartering behaviour.
    }

    // --- Armor -------------------------------------------------------------

    private static void applyArmor(Mob mob, int tier, RandomSource random) {
        // Partial armor more often than full: each slot rolled independently.
        double perSlotChance = tier >= 5 ? 0.78D : 0.16D + 0.11D * tier; // tier1 ~0.27 .. tier4 ~0.60
        tryArmorSlot(mob, EquipmentSlot.HEAD, HELMETS, tier, perSlotChance, random);
        tryArmorSlot(mob, EquipmentSlot.CHEST, CHESTS, tier, perSlotChance, random);
        tryArmorSlot(mob, EquipmentSlot.LEGS, LEGS, tier, perSlotChance, random);
        tryArmorSlot(mob, EquipmentSlot.FEET, BOOTS, tier, perSlotChance, random);
    }

    private static void tryArmorSlot(Mob mob, EquipmentSlot slot, Item[] ladder, int tier,
                                     double chance, RandomSource random) {
        if (random.nextDouble() > chance) {
            return;
        }
        int newLevel = rollMaterialLevel(tier, random);
        int existingLevel = materialLevel(mob.getItemBySlot(slot).getItem(), ladder);
        if (existingLevel >= newLevel) {
            return; // respect existing/better gear
        }
        ItemStack stack = new ItemStack(ladder[newLevel]);
        maybeEnchant(stack, Enchantments.ALL_DAMAGE_PROTECTION, tier, random);
        equip(mob, slot, stack);
    }

    // --- Weapons -----------------------------------------------------------

    private static void upgradeMeleeWeapon(Mob mob, int tier, RandomSource random) {
        ItemStack current = mob.getMainHandItem();
        int existingLevel = materialLevel(current.getItem(), SWORDS);
        if (!current.isEmpty() && existingLevel < 0) {
            return; // holding something we don't recognise (e.g. a trident) — leave it alone
        }
        // Zombies trend towards iron at high tiers; bias the ladder a little lower than armor.
        int newLevel = Math.max(0, rollMaterialLevel(tier, random) - (tier >= 4 ? 0 : 1));
        if (existingLevel >= newLevel && !current.isEmpty()) {
            return;
        }
        ItemStack sword = new ItemStack(SWORDS[newLevel]);
        maybeEnchant(sword, Enchantments.SHARPNESS, tier, random);
        if (tier >= 5 && random.nextDouble() < 0.18D) {
            sword.enchant(Enchantments.FIRE_ASPECT, 1);
        }
        equip(mob, EquipmentSlot.MAINHAND, sword);
    }

    private static void upgradeBow(AbstractSkeleton skeleton, int tier, RandomSource random) {
        ItemStack bow = skeleton.getMainHandItem();
        if (!bow.is(Items.BOW)) {
            return; // wither skeletons hold swords etc. — handled by melee logic if desired
        }
        // Don't replace the bow item (no custom items); just occasionally enchant it at higher tiers.
        if (tier >= 3) {
            maybeEnchant(bow, Enchantments.POWER_ARROWS, tier, random);
        }
        if (tier >= 5 && random.nextDouble() < 0.35D) {
            bow.enchant(Enchantments.PUNCH_ARROWS, 1);
        }
        if (tier >= 5 && random.nextDouble() < 0.22D) {
            bow.enchant(Enchantments.FLAMING_ARROWS, 1);
        }
    }

    private static void upgradeCrossbow(Pillager pillager, int tier, RandomSource random) {
        ItemStack crossbow = pillager.getMainHandItem();
        if (!crossbow.is(Items.CROSSBOW)) {
            return;
        }
        if (tier >= 3) {
            maybeEnchant(crossbow, Enchantments.QUICK_CHARGE, tier, random);
        }
        if (tier >= 5 && random.nextDouble() < 0.30D) {
            crossbow.enchant(Enchantments.MULTISHOT, 1);
        } else if (tier >= 5 && random.nextDouble() < 0.35D) {
            crossbow.enchant(Enchantments.PIERCING, 2);
        }
    }

    // --- Helpers -----------------------------------------------------------

    /** Picks a material rank (0=leather/wood .. 4=diamond) appropriate for the tier. */
    private static int rollMaterialLevel(int tier, RandomSource random) {
        double r = random.nextDouble();
        return switch (tier) {
            case 1 -> r < 0.70 ? 0 : 1;                       // leather / gold
            case 2 -> r < 0.40 ? 0 : (r < 0.80 ? 2 : 1);      // leather / chain / gold
            case 3 -> r < 0.30 ? 0 : (r < 0.65 ? 2 : 3);      // leather / chain / iron
            case 4 -> r < 0.12 ? 4 : (r < 0.72 ? 3 : 2);      // diamond(rare) / iron / chain
            case 5 -> r < 0.35 ? 4 : (r < 0.88 ? 3 : 2);      // diamond / iron / chain
            default -> 0;
        };
    }

    /** Returns the rank of an item within a ladder, or -1 if it isn't in the ladder. */
    private static int materialLevel(Item item, Item[] ladder) {
        for (int i = 0; i < ladder.length; i++) {
            if (ladder[i] == item) {
                return i;
            }
        }
        return -1;
    }

    private static void maybeEnchant(ItemStack stack, Enchantment enchantment, int tier, RandomSource random) {
        double chance = AMConfig.GEAR_ENCHANT_CHANCE_PER_TIER.get() * tier;
        if (random.nextDouble() < chance) {
            int level = enchantment == Enchantments.QUICK_CHARGE && tier >= 5
                    ? 2 + random.nextInt(2)
                    : (tier >= 5 && random.nextBoolean() ? 2 : 1);
            int existing = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
            if (existing >= level) {
                return;
            }
            stack.enchant(enchantment, level);
        }
    }

    private static void equip(Mob mob, EquipmentSlot slot, ItemStack stack) {
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.0F); // added gear shouldn't be farmable
    }
}
