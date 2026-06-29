package net.fretux.adaptivemobs.ai;

import net.fretux.adaptivemobs.ai.goals.AdaptiveCreeperPressureGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveAirbornePositioningGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveAntiCheeseGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveBiomeTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveCreeperTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveEndermanTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveEndermiteTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveEvokerTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveEscapeDenialGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveFlankGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveGuardianTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveLowHealthRetreatGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveMeleePositioningGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveMoraleGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveRavagerTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveRangedAttackGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveRangedCoverGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveRetreatAndReengageGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveSkeletonShieldGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveSharedTargetGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveShulkerTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveSlimeTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveSoundInvestigationGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveSpiderDisengageGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveSpiderTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveSupportGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveTargetPriorityGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveThreatMemoryGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveWardenTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveWitchTacticsGoal;
import net.fretux.adaptivemobs.ai.goals.AdaptiveZombiePackTacticsGoal;
import net.fretux.adaptivemobs.config.AMConfig;
import net.fretux.adaptivemobs.difficulty.AdaptiveDifficultyManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.Zoglin;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.IntSupplier;

public final class AdaptiveGoalInjector {

    private static final Set<Mob> INJECTED = Collections.newSetFromMap(new WeakHashMap<>());

    private AdaptiveGoalInjector() {
    }

    public static void inject(Mob mob, ServerLevel level, int initialTier) {
        if (!AMConfig.AI_ENABLED.get() || initialTier <= 0 || isBoss(mob)) {
            return;
        }
        if (!INJECTED.add(mob)) {
            return;
        }
        PathfinderMob pathfinder = mob instanceof PathfinderMob pf ? pf : null;
        IntSupplier tier = () -> AdaptiveDifficultyManager.getMobTier(level, mob.getType());

        if (tier.getAsInt() >= 4) {
            mob.targetSelector.addGoal(2, new AdaptiveSharedTargetGoal(mob, tier, 4, 12.0D));
        }

        if (pathfinder != null && mob instanceof AbstractSkeleton && AMConfig.AI_SKELETON.get()) {
            mob.goalSelector.addGoal(3, new AdaptiveSkeletonShieldGoal(pathfinder, tier));
            mob.goalSelector.addGoal(4, new AdaptiveFlankGoal(pathfinder, tier, 2, 8.0D, 1.0D, true));
        } else if (pathfinder != null && mob instanceof Zombie && AMConfig.AI_ZOMBIE.get()) {
            mob.goalSelector.addGoal(3, new AdaptiveFlankGoal(pathfinder, tier, 2, 2.5D, 1.05D, true));
            mob.goalSelector.addGoal(2, new AdaptiveZombiePackTacticsGoal((Zombie) mob, tier));
        } else if (pathfinder != null && mob instanceof Creeper creeper && AMConfig.AI_CREEPER.get()) {
            mob.goalSelector.addGoal(2, new AdaptiveCreeperPressureGoal(creeper, tier));
            mob.goalSelector.addGoal(4, new AdaptiveFlankGoal(pathfinder, tier, 2, 3.0D, 1.0D, false));
        } else if (pathfinder != null && mob instanceof Spider && AMConfig.AI_SPIDER.get()) {
            mob.goalSelector.addGoal(4, new AdaptiveFlankGoal(pathfinder, tier, 2, 2.0D, 1.15D, false));
        } else if (pathfinder != null && ((mob instanceof Witch && AMConfig.AI_WITCH.get())
                || (mob instanceof Pillager && AMConfig.AI_PILLAGER.get()))) {
            mob.goalSelector.addGoal(4, new AdaptiveFlankGoal(pathfinder, tier, 2, 7.0D, 1.0D, false));
        }

        if (AMConfig.ENABLE_ADVANCED_AI.get()) {
            injectAdvanced(mob, pathfinder, tier);
            AdaptiveAIGoalUtils.debug(mob, tier, "injected adaptive goals");
        }
    }

    private static void injectAdvanced(Mob mob, PathfinderMob pathfinder, IntSupplier tier) {
        if (AMConfig.ENABLE_TARGET_PRIORITY_AI.get()) {
            mob.targetSelector.addGoal(3, new AdaptiveTargetPriorityGoal(mob, tier));
        }
        if (pathfinder != null && AMConfig.ENABLE_MORALE_AI.get() && !(mob instanceof Warden)) {
            mob.goalSelector.addGoal(8, new AdaptiveMoraleGoal(pathfinder, tier));
        }
        if (pathfinder != null && AMConfig.ENABLE_THREAT_MEMORY_AI.get()) {
            mob.goalSelector.addGoal(2, new AdaptiveThreatMemoryGoal(pathfinder, tier));
        }
        if (pathfinder != null && AMConfig.ENABLE_SOUND_INVESTIGATION_AI.get()) {
            mob.goalSelector.addGoal(9, new AdaptiveSoundInvestigationGoal(pathfinder, tier));
        }
        if (pathfinder != null && AMConfig.ENABLE_SUPPORT_AI.get() && !(mob instanceof Warden)) {
            mob.goalSelector.addGoal(8, new AdaptiveSupportGoal(pathfinder, tier));
        }
        if (pathfinder != null && AMConfig.ENABLE_ESCAPE_DENIAL_AI.get()
                && (mob instanceof Zombie || mob instanceof Spider)) {
            mob.goalSelector.addGoal(6, new AdaptiveEscapeDenialGoal(pathfinder, tier));
        }
        if (pathfinder != null && AMConfig.ENABLE_BIOME_TACTICS_AI.get()) {
            mob.goalSelector.addGoal(8, new AdaptiveBiomeTacticsGoal(pathfinder, tier));
        }
        if (pathfinder != null && AMConfig.ENABLE_ANTI_CHEESE_AI.get()) {
            mob.goalSelector.addGoal(mob instanceof Zombie ? 1 : 7, new AdaptiveAntiCheeseGoal(pathfinder, tier));
        }
        if (pathfinder != null && isRangedMob(mob) && rangedEnabled(mob)) {
            mob.goalSelector.addGoal(3, new AdaptiveRangedAttackGoal(pathfinder, tier, rangedOptimalRange(mob), 1.0D));
            if (AMConfig.ENABLE_RANGED_COVER_AI.get()
                    && (mob instanceof AbstractSkeleton || mob instanceof Pillager || mob instanceof Witch || mob instanceof Evoker)) {
                mob.goalSelector.addGoal(6, new AdaptiveRangedCoverGoal(pathfinder, tier));
            }
        }
        if (pathfinder != null && isMeleeMob(mob) && meleeEnabled(mob)) {
            mob.goalSelector.addGoal(5, new AdaptiveMeleePositioningGoal(pathfinder, tier, meleeSpeed(mob), conservativeMelee(mob)));
        }
        if (mob instanceof Creeper creeper && AMConfig.ENABLE_CREEPER_AI.get() && AMConfig.AI_CREEPER.get()) {
            mob.goalSelector.addGoal(3, new AdaptiveCreeperTacticsGoal(creeper, tier));
        }
        if (pathfinder != null && mob instanceof Spider && AMConfig.ENABLE_SPIDER_AI.get() && AMConfig.AI_SPIDER.get()) {
            mob.goalSelector.addGoal(3, new AdaptiveSpiderTacticsGoal(pathfinder, tier, mob instanceof CaveSpider));
            mob.goalSelector.addGoal(2, new AdaptiveSpiderDisengageGoal(pathfinder, tier, mob instanceof CaveSpider ? 3 : 4, 0.14D, 1.3D));
            mob.goalSelector.addGoal(5, new AdaptiveLowHealthRetreatGoal(pathfinder, tier, 4, 0.38D, 1.15D));
        }
        if (mob instanceof Witch witch && AMConfig.ENABLE_WITCH_AI.get() && AMConfig.AI_WITCH.get()) {
            mob.goalSelector.addGoal(3, new AdaptiveWitchTacticsGoal(witch, tier));
            mob.goalSelector.addGoal(7, new AdaptiveLowHealthRetreatGoal(witch, tier, 3, 0.35D, 1.0D));
        }
        if (mob instanceof EnderMan enderman && AMConfig.ENABLE_ENDERMAN_AI.get() && AMConfig.AI_ENDERMAN.get()) {
            mob.goalSelector.addGoal(2, new AdaptiveEndermanTacticsGoal(enderman, tier));
        }
        if (pathfinder != null && mob instanceof Endermite && AMConfig.ENABLE_ENDERMITE_AI.get() && AMConfig.AI_SMALL_SWARM.get()) {
            mob.goalSelector.addGoal(4, new AdaptiveEndermiteTacticsGoal(pathfinder, tier));
        }
        if (mob instanceof Evoker evoker && AMConfig.ENABLE_RAID_MOB_AI.get()
                && AMConfig.ENABLE_EVOKER_AI.get() && AMConfig.AI_EVOKER.get()) {
            mob.goalSelector.addGoal(3, new AdaptiveEvokerTacticsGoal(evoker, tier));
        }
        if (mob instanceof Guardian guardian && AMConfig.ENABLE_AQUATIC_HOSTILE_AI.get()
                && AMConfig.ENABLE_GUARDIAN_ADVANCED_AI.get() && AMConfig.AI_GUARDIAN.get()) {
            mob.goalSelector.addGoal(4, new AdaptiveGuardianTacticsGoal(guardian, tier));
        }
        if (mob instanceof Shulker shulker && AMConfig.ENABLE_END_MOB_AI.get()
                && AMConfig.ENABLE_SHULKER_ADVANCED_AI.get() && AMConfig.AI_SHULKER.get()) {
            mob.goalSelector.addGoal(4, new AdaptiveShulkerTacticsGoal(shulker, tier));
        }
        if (mob instanceof Slime slime && AMConfig.AI_SLIME.get()
                && ((mob instanceof MagmaCube && AMConfig.ENABLE_MAGMA_CUBE_ADVANCED_AI.get())
                || (!(mob instanceof MagmaCube) && AMConfig.ENABLE_SLIME_ADVANCED_AI.get()))) {
            mob.goalSelector.addGoal(4, new AdaptiveSlimeTacticsGoal(slime, tier));
        }
        if (mob instanceof Ravager ravager && AMConfig.ENABLE_RAID_MOB_AI.get()
                && AMConfig.ENABLE_RAVAGER_ADVANCED_AI.get() && AMConfig.AI_RAVAGER.get()) {
            mob.goalSelector.addGoal(4, new AdaptiveRavagerTacticsGoal(ravager, tier));
        }
        if (isAirborneMob(mob) && airborneEnabled(mob)) {
            mob.goalSelector.addGoal(4, new AdaptiveAirbornePositioningGoal(mob, tier, airborneRange(mob), airborneVerticalOffset(mob)));
        }
        if (pathfinder != null && mob instanceof Vex && AMConfig.ENABLE_RAID_MOB_AI.get() && AMConfig.AI_VEX.get()) {
            mob.goalSelector.addGoal(4, new AdaptiveRetreatAndReengageGoal(pathfinder, tier, 5, 0.10D, 1.2D));
            mob.goalSelector.addGoal(7, new AdaptiveLowHealthRetreatGoal(pathfinder, tier, 4, 0.30D, 1.2D));
        }
        if (pathfinder != null && mob instanceof AbstractSkeleton && AMConfig.AI_SKELETON.get()) {
            mob.goalSelector.addGoal(7, new AdaptiveLowHealthRetreatGoal(pathfinder, tier, 4, 0.25D, 1.0D));
        }
        if (pathfinder != null && mob instanceof Pillager && AMConfig.AI_PILLAGER.get()) {
            mob.goalSelector.addGoal(7, new AdaptiveLowHealthRetreatGoal(pathfinder, tier, 4, 0.28D, 1.0D));
        }
        if (pathfinder != null && mob instanceof Warden && AMConfig.ENABLE_WARDEN_AI.get() && AMConfig.AI_WARDEN.get()) {
            mob.goalSelector.addGoal(7, new AdaptiveMeleePositioningGoal(pathfinder, tier, 0.9D, true));
            if (AMConfig.ENABLE_WARDEN_ADVANCED_AI.get()) {
                mob.goalSelector.addGoal(8, new AdaptiveWardenTacticsGoal((Warden) mob, tier));
            }
        }
    }

    private static boolean isBoss(Mob mob) {
        return !AMConfig.ENABLE_BOSS_AI.get()
                && (mob.getType() == EntityType.ENDER_DRAGON || mob.getType() == EntityType.WITHER);
    }

    private static boolean isRangedMob(Mob mob) {
        return mob instanceof AbstractSkeleton || mob instanceof Pillager || mob instanceof Piglin
                || mob instanceof Witch || mob instanceof Ghast || mob instanceof Blaze
                || mob instanceof Guardian || mob instanceof Shulker;
    }

    private static boolean rangedEnabled(Mob mob) {
        if (!AMConfig.ENABLE_RANGED_AI.get()) {
            return false;
        }
        if (mob instanceof Pillager) {
            return AMConfig.ENABLE_PILLAGER_AI.get() && AMConfig.AI_PILLAGER.get();
        }
        if (mob instanceof AbstractSkeleton) {
            return AMConfig.AI_SKELETON.get();
        }
        if (mob instanceof Witch) {
            return AMConfig.ENABLE_WITCH_AI.get() && AMConfig.AI_WITCH.get();
        }
        if (mob instanceof Guardian) {
            return AMConfig.ENABLE_AQUATIC_HOSTILE_AI.get() && AMConfig.AI_GUARDIAN.get();
        }
        if (mob instanceof Shulker) {
            return AMConfig.ENABLE_END_MOB_AI.get() && AMConfig.AI_SHULKER.get();
        }
        return !(mob instanceof Piglin) || (AMConfig.ENABLE_NETHER_MOB_AI.get() && AMConfig.AI_PIGLIN_FAMILY.get());
    }

    private static double rangedOptimalRange(Mob mob) {
        if (mob instanceof Pillager || mob instanceof Piglin) {
            return 10.0D;
        }
        if (mob instanceof Guardian || mob instanceof Shulker) {
            return 12.0D;
        }
        if (mob instanceof Ghast) {
            return 24.0D;
        }
        return 8.0D;
    }

    private static boolean isMeleeMob(Mob mob) {
        return mob instanceof Zombie || mob instanceof Husk || mob instanceof Drowned
                || mob instanceof ZombieVillager || mob instanceof ZombifiedPiglin
                || mob instanceof Piglin || mob instanceof PiglinBrute || mob instanceof Vindicator
                || mob instanceof WitherSkeleton || mob instanceof Hoglin || mob instanceof Zoglin
                || mob instanceof Silverfish || mob instanceof Endermite || mob instanceof Slime || mob instanceof MagmaCube
                || mob instanceof Ravager || mob instanceof Warden;
    }

    private static boolean meleeEnabled(Mob mob) {
        if (!AMConfig.ENABLE_MELEE_AI.get()) {
            return false;
        }
        if (mob instanceof Piglin || mob instanceof PiglinBrute || mob instanceof ZombifiedPiglin
                || mob instanceof Hoglin || mob instanceof Zoglin) {
            return AMConfig.ENABLE_NETHER_MOB_AI.get() && AMConfig.AI_PIGLIN_FAMILY.get();
        }
        if (mob instanceof Vindicator || mob instanceof Ravager) {
            return AMConfig.ENABLE_RAID_MOB_AI.get() && AMConfig.AI_RAVAGER.get();
        }
        if (mob instanceof Silverfish) {
            return AMConfig.AI_SMALL_SWARM.get();
        }
        if (mob instanceof Endermite) {
            return AMConfig.ENABLE_ENDERMITE_AI.get() && AMConfig.AI_SMALL_SWARM.get();
        }
        if (mob instanceof Warden) {
            return AMConfig.ENABLE_WARDEN_AI.get() && AMConfig.AI_WARDEN.get();
        }
        return AMConfig.AI_ZOMBIE.get() || mob instanceof WitherSkeleton || mob instanceof Slime || mob instanceof MagmaCube;
    }

    private static double meleeSpeed(Mob mob) {
        if (mob instanceof Silverfish) {
            return 1.15D;
        }
        if (mob instanceof Ravager || mob instanceof Warden) {
            return 0.9D;
        }
        return 1.05D;
    }

    private static boolean conservativeMelee(Mob mob) {
        return mob instanceof PiglinBrute || mob instanceof Ravager || mob instanceof Warden || mob instanceof ElderGuardian;
    }

    private static boolean isAirborneMob(Mob mob) {
        return mob instanceof Ghast || mob instanceof Blaze || mob instanceof Phantom || mob instanceof Vex;
    }

    private static boolean airborneEnabled(Mob mob) {
        if (mob instanceof Ghast) {
            return AMConfig.ENABLE_NETHER_MOB_AI.get() && AMConfig.AI_GHAST.get();
        }
        if (mob instanceof Blaze) {
            return AMConfig.ENABLE_NETHER_MOB_AI.get() && AMConfig.AI_BLAZE.get();
        }
        if (mob instanceof Phantom) {
            return AMConfig.AI_PHANTOM.get();
        }
        if (mob instanceof Vex) {
            return AMConfig.ENABLE_RAID_MOB_AI.get() && AMConfig.AI_VEX.get();
        }
        return true;
    }

    private static double airborneRange(Mob mob) {
        if (mob instanceof Ghast) {
            return 22.0D;
        }
        if (mob instanceof Phantom) {
            return 8.0D;
        }
        return 7.0D;
    }

    private static double airborneVerticalOffset(Mob mob) {
        if (mob instanceof Ghast) {
            return 6.0D;
        }
        if (mob instanceof Phantom) {
            return 5.0D;
        }
        return 2.0D;
    }
}
