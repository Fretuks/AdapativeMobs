package net.fretux.adaptivemobs.config;

import net.fretux.adaptivemobs.AdaptiveMobs;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraft.world.entity.EntityType;

import java.util.List;

/**
 * Common (server-side gameplay) configuration for Adaptive Mobs.
 *
 * <p>All tier-indexed lists are 5 entries long and represent tiers 1..5. Tier 0 always means
 * "vanilla / no change", so the accessors below return a neutral value for tier 0.</p>
 */
@Mod.EventBusSubscriber(modid = AdaptiveMobs.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AMConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // --- General -----------------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADAPTIVE_MOBS = BUILDER
            .comment("Master switch. When false the mod does nothing to mobs.")
            .define("general.enableAdaptiveMobs", true);

    public static final ForgeConfigSpec.IntValue MAX_TIER = BUILDER
            .comment("Highest adaptive tier that can ever be reached (0-5).")
            .defineInRange("general.maxTier", 5, 0, 5);

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Verbose debug logging for progression, scaling and spawns.")
            .define("general.debugLogging", false);

    // --- Per-mob toggles ---------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue ENABLE_ZOMBIES = BUILDER.define("mobs.zombies", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_SKELETONS = BUILDER.define("mobs.skeletons", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_CREEPERS = BUILDER.define("mobs.creepers", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPIDERS = BUILDER.define("mobs.spiders", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_WITCHES = BUILDER.define("mobs.witches", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_PILLAGERS = BUILDER.define("mobs.pillagers", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_OTHER_HOSTILES = BUILDER
            .comment("Apply generic progression/scaling/spawn pressure to hostile mobs without a specific toggle.")
            .define("mobs.otherHostiles", true);

    // --- Progression thresholds -------------------------------------------
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> DAY_THRESHOLDS = BUILDER
            .comment("World day required to reach tiers 1..5 (whichever of day/kills hits first wins).")
            .defineList("progression.dayThresholds", List.of(5, 15, 30, 60, 100),
                    o -> o instanceof Integer i && i >= 0);

    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> KILL_THRESHOLDS = BUILDER
            .comment("Total hostile kills required to reach tiers 1..5.")
            .defineList("progression.killThresholds", List.of(25, 100, 250, 600, 1200),
                    o -> o instanceof Integer i && i >= 0);

    public static final ForgeConfigSpec.IntValue MOB_TYPE_KILL_BONUS_THRESHOLD = BUILDER
            .comment("Per-mob-type kills that grant +1 tier to that mob type (set 0 to disable per-type scaling).")
            .defineInRange("progression.perTypeKillsPerTier", 150, 0, Integer.MAX_VALUE);

    // --- Stat scaling ------------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue STAT_SCALING_ENABLED = BUILDER
            .comment("Enable attribute (health/damage/speed/etc.) scaling.")
            .define("scaling.enabled", true);

    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> HEALTH_SCALE = BUILDER
            .comment("Extra max-health fraction per tier 1..5 (0.10 = +10%).")
            .defineList("scaling.healthPerTier", List.of(0.10, 0.22, 0.40, 0.60, 0.95),
                    o -> isNumberInRange(o, 0.0, Double.MAX_VALUE));

    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> DAMAGE_SCALE = BUILDER
            .comment("Extra attack-damage fraction per tier 1..5.")
            .defineList("scaling.damagePerTier", List.of(0.06, 0.12, 0.24, 0.38, 0.60),
                    o -> isNumberInRange(o, 0.0, Double.MAX_VALUE));

    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> SPEED_SCALE = BUILDER
            .comment("Extra movement-speed fraction per tier 1..5 (kept small on purpose).")
            .defineList("scaling.speedPerTier", List.of(0.0, 0.0, 0.01, 0.04, 0.07),
                    o -> isNumberInRange(o, 0.0, Double.MAX_VALUE));

    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> FOLLOW_RANGE_SCALE = BUILDER
            .comment("Extra follow-range fraction per tier 1..5.")
            .defineList("scaling.followRangePerTier", List.of(0.0, 0.10, 0.10, 0.15, 0.25),
                    o -> isNumberInRange(o, 0.0, Double.MAX_VALUE));

    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> ARMOR_BONUS = BUILDER
            .comment("Flat armor points added per tier 1..5.")
            .defineList("scaling.armorPerTier", List.of(0.0, 1.0, 2.5, 4.0, 6.0),
                    o -> isNumberInRange(o, 0.0, Double.MAX_VALUE));

    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> KNOCKBACK_RESIST = BUILDER
            .comment("Flat knockback-resistance added per tier 1..5 (0.0-1.0 scale).")
            .defineList("scaling.knockbackResistPerTier", List.of(0.0, 0.0, 0.02, 0.08, 0.18),
                    o -> isNumberInRange(o, 0.0, Double.MAX_VALUE));

    // --- Gear scaling ------------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue GEAR_SCALING_ENABLED = BUILDER
            .comment("Enable upgrading vanilla gear on eligible mobs (zombies, skeletons, etc.).")
            .define("gear.enabled", true);

    public static final ForgeConfigSpec.DoubleValue GEAR_ENCHANT_CHANCE_PER_TIER = BUILDER
            .comment("Base enchant chance multiplier applied per tier when gear scaling rolls (kept low).")
            .defineInRange("gear.enchantChancePerTier", 0.08, 0.0, 1.0);

    // --- Spawn pressure ----------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue SPAWN_PRESSURE_ENABLED = BUILDER
            .comment("Enable occasional extra natural spawns of the same mob type.")
            .define("spawnPressure.enabled", true);

    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> EXTRA_SPAWN_CHANCE = BUILDER
            .comment("Chance to spawn ONE extra mob on a natural spawn, per tier 1..5.")
            .defineList("spawnPressure.extraChancePerTier", List.of(0.05, 0.10, 0.17, 0.25, 0.40),
                    o -> isNumberInRange(o, 0.0, 1.0));

    public static final ForgeConfigSpec.IntValue SPAWN_DENSITY_RADIUS = BUILDER
            .comment("Radius (blocks) checked for nearby mobs before adding an extra spawn.")
            .defineInRange("spawnPressure.densityRadius", 12, 2, 64);

    public static final ForgeConfigSpec.IntValue SPAWN_DENSITY_CAP = BUILDER
            .comment("If at least this many mobs are already within the density radius, skip the extra spawn.")
            .defineInRange("spawnPressure.densityCap", 6, 1, 64);

    // --- AI ----------------------------------------------------------------
    public static final ForgeConfigSpec.BooleanValue AI_ENABLED = BUILDER
            .comment("Master switch for all adaptive AI behaviour.")
            .define("ai.enabled", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_ADVANCED_AI = BUILDER
            .comment("Enable expanded adaptive AI goals beyond basic flanking/pressure behaviour.")
            .define("ai.enableAdvancedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_RANGED_AI = BUILDER.define("ai.enableRangedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_MELEE_AI = BUILDER.define("ai.enableMeleeAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_CREEPER_AI = BUILDER.define("ai.enableCreeperAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_SPIDER_AI = BUILDER.define("ai.enableSpiderAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_WITCH_AI = BUILDER.define("ai.enableWitchAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_PILLAGER_AI = BUILDER.define("ai.enablePillagerAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDERMAN_AI = BUILDER.define("ai.enableEndermanAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_NETHER_MOB_AI = BUILDER.define("ai.enableNetherMobAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_AQUATIC_HOSTILE_AI = BUILDER.define("ai.enableAquaticHostileAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_RAID_MOB_AI = BUILDER.define("ai.enableRaidMobAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_END_MOB_AI = BUILDER.define("ai.enableEndMobAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_WARDEN_AI = BUILDER.define("ai.enableWardenAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENDERMITE_AI = BUILDER.define("ai.enableEndermiteAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_EVOKER_AI = BUILDER.define("ai.enableEvokerAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_WITCH_ADVANCED_POTION_AI = BUILDER.define("ai.enableWitchAdvancedPotionAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_GUARDIAN_ADVANCED_AI = BUILDER.define("ai.enableGuardianAdvancedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_SHULKER_ADVANCED_AI = BUILDER.define("ai.enableShulkerAdvancedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLIME_ADVANCED_AI = BUILDER.define("ai.enableSlimeAdvancedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_MAGMA_CUBE_ADVANCED_AI = BUILDER.define("ai.enableMagmaCubeAdvancedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_RAVAGER_ADVANCED_AI = BUILDER.define("ai.enableRavagerAdvancedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_WARDEN_ADVANCED_AI = BUILDER
            .comment("Very conservative Warden behaviour tweaks only; does not change default speed, damage or sonic boom frequency.")
            .define("ai.enableWardenAdvancedAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_MORALE_AI = BUILDER
            .comment("Allow small group morale adjustments: bolder groups, defensive low-health isolated mobs.")
            .define("ai.enableMoraleAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_TARGET_PRIORITY_AI = BUILDER
            .comment("Allow mobs to occasionally retarget vulnerable or committed players.")
            .define("ai.enableTargetPriorityAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_ANTI_CHEESE_AI = BUILDER
            .comment("Allow small tactical responses to common cheese patterns without hard counters.")
            .define("ai.enableAntiCheeseAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_DARK_APPROACH_AI = BUILDER
            .comment("Prefer darker approach positions when an adaptive movement goal has comparable options.")
            .define("ai.enableDarkApproachAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_THREAT_MEMORY_AI = BUILDER
            .comment("Remember recent player attackers briefly to reduce trivial line-of-sight aggro resets.")
            .define("ai.enableThreatMemoryAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_SOUND_INVESTIGATION_AI = BUILDER
            .comment("Tier 2+ mobs can investigate nearby recent block breaking, eating, bow shots and explosions.")
            .define("ai.enableSoundInvestigationAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUPPORT_AI = BUILDER
            .comment("Allow small ally support behaviours such as peeling and ranged mobs holding behind melee pressure.")
            .define("ai.enableSupportAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_ESCAPE_DENIAL_AI = BUILDER
            .comment("Allow zombies and spiders to imperfectly bias toward likely player escape routes.")
            .define("ai.enableEscapeDenialAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_BIOME_TACTICS_AI = BUILDER
            .comment("Allow small dimension/terrain-specific tactics for Nether mobs, drowned, spiders and skeletons.")
            .define("ai.enableBiomeTacticsAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_RANGED_COVER_AI = BUILDER
            .comment("Allow ranged mobs to sometimes move toward imperfect cover after attacking.")
            .define("ai.enableRangedCoverAI", true);
    public static final ForgeConfigSpec.BooleanValue ENABLE_BOSS_AI = BUILDER
            .comment("Future boss AI support. Ender Dragon and Wither are not altered unless this is enabled.")
            .define("ai.enableBossAI", false);

    public static final ForgeConfigSpec.BooleanValue AI_SKELETON = BUILDER.define("ai.skeletons", true);
    public static final ForgeConfigSpec.BooleanValue AI_ZOMBIE = BUILDER.define("ai.zombies", true);
    public static final ForgeConfigSpec.BooleanValue AI_CREEPER = BUILDER.define("ai.creepers", true);
    public static final ForgeConfigSpec.BooleanValue AI_SPIDER = BUILDER.define("ai.spiders", true);
    public static final ForgeConfigSpec.BooleanValue AI_WITCH = BUILDER.define("ai.witches", true);
    public static final ForgeConfigSpec.BooleanValue AI_PILLAGER = BUILDER.define("ai.pillagers", true);
    public static final ForgeConfigSpec.BooleanValue AI_ENDERMAN = BUILDER.define("ai.endermen", true);
    public static final ForgeConfigSpec.BooleanValue AI_SLIME = BUILDER.define("ai.slimes", true);
    public static final ForgeConfigSpec.BooleanValue AI_GHAST = BUILDER.define("ai.ghasts", true);
    public static final ForgeConfigSpec.BooleanValue AI_BLAZE = BUILDER.define("ai.blazes", true);
    public static final ForgeConfigSpec.BooleanValue AI_GUARDIAN = BUILDER.define("ai.guardians", true);
    public static final ForgeConfigSpec.BooleanValue AI_PHANTOM = BUILDER.define("ai.phantoms", true);
    public static final ForgeConfigSpec.BooleanValue AI_RAVAGER = BUILDER.define("ai.ravagers", true);
    public static final ForgeConfigSpec.BooleanValue AI_EVOKER = BUILDER.define("ai.evokers", true);
    public static final ForgeConfigSpec.BooleanValue AI_VEX = BUILDER.define("ai.vexes", true);
    public static final ForgeConfigSpec.BooleanValue AI_PIGLIN_FAMILY = BUILDER.define("ai.piglinFamily", true);
    public static final ForgeConfigSpec.BooleanValue AI_SMALL_SWARM = BUILDER.define("ai.silverfishAndEndermites", true);
    public static final ForgeConfigSpec.BooleanValue AI_SHULKER = BUILDER.define("ai.shulkers", true);
    public static final ForgeConfigSpec.BooleanValue AI_WARDEN = BUILDER.define("ai.warden", true);

    public static final ForgeConfigSpec.IntValue AI_SCAN_COOLDOWN_MIN = BUILDER
            .comment("Minimum cooldown in ticks for adaptive AI scans.")
            .defineInRange("ai.scanCooldownMin", 20, 5, 200);
    public static final ForgeConfigSpec.IntValue AI_SCAN_COOLDOWN_MAX = BUILDER
            .comment("Maximum cooldown in ticks for adaptive AI scans.")
            .defineInRange("ai.scanCooldownMax", 60, 10, 400);
    public static final ForgeConfigSpec.IntValue AI_MAX_NEARBY_MOBS_SCANNED = BUILDER
            .comment("Maximum nearby mobs considered by each adaptive AI scan.")
            .defineInRange("ai.maxNearbyMobsScanned", 8, 1, 32);
    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> AI_ACCURACY_BONUS = BUILDER
            .comment("Projectile aim correction strength per tier 1..5. Small values preserve dodge counterplay.")
            .defineList("ai.accuracyBonusPerTier", List.of(0.0, 0.04, 0.08, 0.12, 0.16),
                    o -> isNumberInRange(o, 0.0, 0.5));
    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> AI_HEADSHOT_CHANCE = BUILDER
            .comment("Chance per tier 1..5 for skeleton-like archers to bias aim upward.")
            .defineList("ai.headshotChancePerTier", List.of(0.0, 0.0, 0.05, 0.18, 0.25),
                    o -> isNumberInRange(o, 0.0, 1.0));
    public static final ForgeConfigSpec.ConfigValue<List<? extends Number>> AI_INTERCEPTION_STRENGTH = BUILDER
            .comment("Movement prediction strength per tier 1..5.")
            .defineList("ai.interceptionStrengthPerTier", List.of(0.0, 0.0, 0.06, 0.14, 0.24),
                    o -> isNumberInRange(o, 0.0, 1.0));
    public static final ForgeConfigSpec.BooleanValue AI_RETREAT_ENABLED = BUILDER
            .comment("Allow brief retreat/re-engage behaviour for mobs that support it.")
            .define("ai.retreatBehaviorEnabled", true);
    public static final ForgeConfigSpec.BooleanValue DEBUG_AI_LOGGING = BUILDER
            .comment("Verbose debug logging for adaptive AI injection and special behaviour.")
            .define("ai.debugAILogging", false);

    public static final ForgeConfigSpec.BooleanValue ALLOW_ZOMBIE_BLOCK_BREAK = BUILDER
            .comment("If true, allow extra block-breaking behaviour for zombies (off by default; vanilla door behaviour is unaffected).")
            .define("ai.allowZombieBlockBreak", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // --- Cached values -----------------------------------------------------
    public static volatile boolean enabled = true;
    public static volatile int maxTier = 5;
    public static volatile boolean debug = false;

    private AMConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        enabled = ENABLE_ADAPTIVE_MOBS.get();
        maxTier = MAX_TIER.get();
        debug = DEBUG_LOGGING.get();
    }

    /** Reads tier-indexed list (tiers 1..5) returning neutral 0 for tier 0 or out-of-range. */
    public static double tierValue(List<? extends Number> list, int tier) {
        if (tier <= 0 || list == null || list.isEmpty()) {
            return 0.0;
        }
        int idx = Math.min(tier, list.size()) - 1;
        return list.get(idx).doubleValue();
    }

    public static int tierValueInt(List<? extends Integer> list, int tier1based) {
        if (list == null || tier1based <= 0 || tier1based > list.size()) {
            return Integer.MAX_VALUE;
        }
        return list.get(tier1based - 1);
    }

    public static int aiScanCooldown(net.minecraft.util.RandomSource random) {
        int min = AI_SCAN_COOLDOWN_MIN.get();
        int max = Math.max(min, AI_SCAN_COOLDOWN_MAX.get());
        return min + random.nextInt(max - min + 1);
    }

    public static boolean isMobEnabled(EntityType<?> type) {
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
            return ENABLE_BOSS_AI.get();
        }
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED
                || type == EntityType.ZOMBIE_VILLAGER || type == EntityType.ZOMBIFIED_PIGLIN) {
            return ENABLE_ZOMBIES.get();
        }
        if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.WITHER_SKELETON) {
            return ENABLE_SKELETONS.get();
        }
        if (type == EntityType.CREEPER) {
            return ENABLE_CREEPERS.get();
        }
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) {
            return ENABLE_SPIDERS.get();
        }
        if (type == EntityType.WITCH) {
            return ENABLE_WITCHES.get();
        }
        if (type == EntityType.PILLAGER) {
            return ENABLE_PILLAGERS.get();
        }
        return ENABLE_OTHER_HOSTILES.get();
    }

    private static boolean isNumberInRange(Object value, double min, double max) {
        if (!(value instanceof Number number)) {
            return false;
        }
        double asDouble = number.doubleValue();
        return Double.isFinite(asDouble) && asDouble >= min && asDouble <= max;
    }
}
