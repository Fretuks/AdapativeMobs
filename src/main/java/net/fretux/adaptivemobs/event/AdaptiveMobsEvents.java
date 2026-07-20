package net.fretux.adaptivemobs.event;

import net.fretux.adaptivemobs.ai.AdaptiveGoalInjector;
import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptiveMemory;
import net.fretux.adaptivemobs.ai.AdaptiveMobHooks;
import net.fretux.adaptivemobs.ai.AdaptiveSpecialAbilities;
import net.fretux.adaptivemobs.commands.AdaptiveMobsCommands;
import net.fretux.adaptivemobs.config.AMConfig;
import net.fretux.adaptivemobs.difficulty.AdaptiveDifficultyManager;
import net.fretux.adaptivemobs.gear.GearScaler;
import net.fretux.adaptivemobs.scaling.MobStatScaler;
import net.fretux.adaptivemobs.spawn.SpawnPressureHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AdaptiveMobsEvents {

    private static final String FRIENDLY_FIRE_COUNT_KEY = "am_friendly_fire_count";
    private static final String FRIENDLY_FIRE_EXPIRES_KEY = "am_friendly_fire_expires";
    private static final String PENDING_SPAWN_PROCESSING_KEY = "am_pending_spawn_processing";
    private static final int FRIENDLY_FIRE_WINDOW_TICKS = 100;

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        AdaptiveMobsCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer directPlayer
                ? directPlayer : killed.getKillCredit() instanceof ServerPlayer creditedPlayer ? creditedPlayer : null;
        if (player == null) {
            return;
        }
        AdaptiveDifficultyManager.recordKill(player, killed);
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof Mob explosiveMob
                && event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && player.level() instanceof ServerLevel level
                && AMConfig.enabled && AMConfig.AI_ENABLED.get()
                && AdaptiveGoalInjector.isAIEnabledFor(explosiveMob)
                && AdaptiveDifficultyManager.getMobTier(level, explosiveMob.getType()) >= 5
                && AdaptiveAIGoalUtils.isBlockingShield(player)) {
            player.disableShield(true);
            AdaptiveAIGoalUtils.debug(explosiveMob, () -> 5, "mob explosion disabled shield");
        }
        if (!AMConfig.enabled || !AMConfig.AI_ENABLED.get()
                || !(event.getEntity() instanceof Mob victim) || !(victim instanceof Enemy)
                || !(event.getSource().getDirectEntity() instanceof Projectile)
                || !(event.getSource().getEntity() instanceof Mob attacker) || !(attacker instanceof Enemy)
                || attacker == victim || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        int tier = AdaptiveDifficultyManager.getMobTier(level, victim.getType());
        if (!net.fretux.adaptivemobs.ai.AdaptiveTargetingUtils.areCombatAllies(attacker, victim)) {
            return;
        }
        if (tier < 2 || !AMConfig.isMobEnabled(victim.getType())) {
            return;
        }

        long now = level.getGameTime();
        boolean sameVolley = victim.getPersistentData().getLong(FRIENDLY_FIRE_EXPIRES_KEY) >= now;
        int hits = sameVolley ? victim.getPersistentData().getInt(FRIENDLY_FIRE_COUNT_KEY) + 1 : 1;
        int toleratedHits = Math.min(4, tier - 1);
        if (hits <= toleratedHits) {
            victim.getPersistentData().putInt(FRIENDLY_FIRE_COUNT_KEY, hits);
            victim.getPersistentData().putLong(FRIENDLY_FIRE_EXPIRES_KEY, now + FRIENDLY_FIRE_WINDOW_TICKS);
            event.setCanceled(true);
            AdaptiveAIGoalUtils.debug(victim, () -> tier, "ignored allied projectile hit");
            return;
        }
        clearFriendlyFireTolerance(victim);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onLivingHurt(LivingHurtEvent event) {
        AdaptiveSpecialAbilities.onLivingHurt(event);
        if (!AMConfig.enabled || !AMConfig.ENABLE_THREAT_MEMORY_AI.get()) {
            return;
        }
        if (event.getEntity() instanceof Mob mob && mob instanceof Enemy
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            AdaptiveMemory.rememberThreat(mob, player, 20 * 12);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AdaptiveSpecialAbilities.invalidateTemporaryBlock(level, event.getPos());
        }
        if (AMConfig.enabled && AMConfig.ENABLE_SOUND_INVESTIGATION_AI.get()
                && event.getLevel() instanceof ServerLevel level) {
            if (event.getPlayer() instanceof ServerPlayer player
                    && !AdaptiveAIGoalUtils.isValidAdaptiveTarget(player)) {
                return;
            }
            AdaptiveMemory.rememberSound(level, event.getPos(), AdaptiveMemory.SoundKind.BLOCK_BREAK, 20 * 8);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AdaptiveSpecialAbilities.invalidateTemporaryBlock(level, event.getPos());
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (AMConfig.enabled && AMConfig.ENABLE_SOUND_INVESTIGATION_AI.get()
                && event.getEntity() instanceof ServerPlayer player && event.getItem().isEdible()
                && AdaptiveAIGoalUtils.isValidAdaptiveTarget(player)) {
            AdaptiveMemory.rememberSound(player.serverLevel(), player.blockPosition(), AdaptiveMemory.SoundKind.EATING, 20 * 6);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!AMConfig.enabled || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || !AMConfig.isMobEnabled(mob.getType())) {
            return;
        }

        mob.getPersistentData().putBoolean(PENDING_SPAWN_PROCESSING_KEY, true);
        int tier = AdaptiveDifficultyManager.getMobTier(level, mob.getType());
        SpawnPressureHelper.maybeSpawnExtra(level, mob, tier, event.getSpawnType());
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        AdaptiveMobHooks.onProjectileJoin(event);
        AdaptiveSpecialAbilities.onProjectileJoin(event);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (AMConfig.enabled && entity instanceof Mob mob && mob instanceof Enemy
                && AMConfig.isMobEnabled(mob.getType())) {
            mob.getPersistentData().remove(PENDING_SPAWN_PROCESSING_KEY);
            int tier = AdaptiveDifficultyManager.getMobTier(level, mob.getType());
            MobStatScaler.applyScaling(mob, tier);
            GearScaler.applyGear(mob, tier);
        }
        // Install dormant, config-gated goals on every supported hostile so a later config reload
        // can enable them without requiring the entity to unload and join again.
        if (entity instanceof Mob mob && mob instanceof Enemy) {
            AdaptiveGoalInjector.inject(mob, level, AdaptiveDifficultyManager.getMobTier(level, mob.getType()));
        }
        if (AMConfig.enabled && AMConfig.ENABLE_SOUND_INVESTIGATION_AI.get()
                && entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player
                && AdaptiveAIGoalUtils.isValidAdaptiveTarget(player)) {
            AdaptiveMemory.rememberSound(level, player.blockPosition(), AdaptiveMemory.SoundKind.BOW_SHOT, 20 * 6);
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AdaptiveMemory.clear(level);
            AdaptiveSpecialAbilities.clear(level);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        AdaptiveSpecialAbilities.protectSplitFireballVolley(event);
        if (AMConfig.enabled && AMConfig.ENABLE_SOUND_INVESTIGATION_AI.get()
                && event.getLevel() instanceof ServerLevel level) {
            AdaptiveMemory.rememberSound(level, event.getExplosion().getPosition(), AdaptiveMemory.SoundKind.EXPLOSION, 20 * 10);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        AdaptiveSpecialAbilities.onServerTick(event);
        if (event.phase == TickEvent.Phase.END && AMConfig.enabled
                && event.getServer().getTickCount() % 100 == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Mob mob && mob instanceof Enemy && AMConfig.isMobEnabled(mob.getType())) {
                        int tier = AdaptiveDifficultyManager.getMobTier(level, mob.getType());
                        MobStatScaler.applyScaling(mob, tier);
                        GearScaler.applyGear(mob, tier);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        AdaptiveSpecialAbilities.clear(event.getServer());
    }

    private static void clearFriendlyFireTolerance(Mob mob) {
        mob.getPersistentData().remove(FRIENDLY_FIRE_COUNT_KEY);
        mob.getPersistentData().remove(FRIENDLY_FIRE_EXPIRES_KEY);
    }
}
