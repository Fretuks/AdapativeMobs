package net.fretux.adaptivemobs.ai;

import net.fretux.adaptivemobs.config.AMConfig;
import net.fretux.adaptivemobs.difficulty.AdaptiveDifficultyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class AdaptiveSpecialAbilities {

    private static final String COOLDOWN_PREFIX = "am_special_cd_";
    private static final String SPLIT_FIREBALL_KEY = "am_split_fireball";
    private static final List<TemporaryBlock> TEMPORARY_BLOCKS = new ArrayList<>();
    private static final List<DelayedReappear> DELAYED_REAPPEARS = new ArrayList<>();

    private AdaptiveSpecialAbilities() {
    }

    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (!adaptiveAbilitiesEnabled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Projectile projectile) || !(projectile.getOwner() instanceof Mob mob)) {
            return;
        }
        int tier = tier(level, mob);
        if (tier >= 5 && mob instanceof Ghast ghast && projectile instanceof LargeFireball fireball) {
            splitGhastFireball(level, ghast, fireball);
            return;
        }
        if (tier < 4 || !(projectile instanceof Arrow arrow)) {
            return;
        }
        LivingEntity target = mob.getTarget();
        if (mob instanceof Stray) {
            arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, tier >= 5 ? 100 : 70, tier >= 5 ? 1 : 0));
            if (tier >= 5 && mob.getRandom().nextFloat() < 0.22F) {
                arrow.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 70, 0));
            }
            return;
        }
        if (mob instanceof AbstractSkeleton) {
            float poisonChance = tier >= 5 ? 0.16F : 0.09F;
            if (mob.getRandom().nextFloat() < poisonChance) {
                arrow.addEffect(new MobEffectInstance(MobEffects.POISON, 80 + mob.getRandom().nextInt(41), 0));
            }
            if (tier >= 5 && AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)
                    && AdaptiveAIGoalUtils.isBlockingShield(target) && mob.getRandom().nextFloat() < 0.18F) {
                arrow.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            } else if (tier >= 5 && AdaptiveAIGoalUtils.isValidAdaptiveTarget(target) && target.isSprinting()
                    && mob.getRandom().nextFloat() < 0.18F) {
                arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
            }
            return;
        }
        if (mob instanceof Pillager && tier >= 5 && mob.getRandom().nextFloat() < 0.18F) {
            arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            arrow.getPersistentData().putBoolean("am_pinning_bolt", true);
        }
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (!adaptiveAbilitiesEnabled() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();

        if (victim instanceof Mob damagedMob && sourceEntity instanceof LivingEntity attacker) {
            onMobDamaged(level, damagedMob, attacker, directEntity);
        }
        if (victim instanceof ServerPlayer player && sourceEntity instanceof Mob attackerMob) {
            onPlayerHurt(level, player, attackerMob, directEntity, event);
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        Iterator<TemporaryBlock> blockIterator = TEMPORARY_BLOCKS.iterator();
        while (blockIterator.hasNext()) {
            TemporaryBlock block = blockIterator.next();
            ServerLevel level = block.server == event.getServer() ? event.getServer().getLevel(block.dimension) : null;
            if (level == null || level.getGameTime() < block.expireTick) {
                continue;
            }
            if (level.getBlockState(block.pos).is(Blocks.COBWEB)) {
                level.removeBlock(block.pos, false);
            }
            blockIterator.remove();
        }

        Iterator<DelayedReappear> reappearIterator = DELAYED_REAPPEARS.iterator();
        while (reappearIterator.hasNext()) {
            DelayedReappear pending = reappearIterator.next();
            ServerLevel level = pending.server == event.getServer() ? event.getServer().getLevel(pending.dimension) : null;
            if (level == null || level.getGameTime() < pending.tick) {
                continue;
            }
            Entity mobEntity = level.getEntity(pending.mobId);
            Entity targetEntity = level.getEntity(pending.targetId);
            if (mobEntity instanceof Silverfish mob && targetEntity instanceof LivingEntity target
                    && mob.isAlive() && AdaptiveAIGoalUtils.isValidAdaptiveTarget(target) && mob.canAttack(target)) {
                Vec3 side = AdaptivePositioningUtils.sidePosition(mob, target, 1.6D,
                        mob.getRandom().nextBoolean() ? 1.0D : -1.0D, 0.0D);
                BlockPos destination = BlockPos.containing(side);
                Vec3 delta = side.subtract(mob.position());
                if (level.hasChunkAt(destination) && level.getWorldBorder().isWithinBounds(destination)
                        && level.noCollision(mob, mob.getBoundingBox().move(delta))) {
                    mob.moveTo(side.x, side.y, side.z, mob.getYRot(), mob.getXRot());
                    mob.setTarget(target);
                    AdaptiveAIGoalUtils.debug(mob, () -> pending.tier, "silverfish burrow burst");
                }
                mob.removeEffect(MobEffects.INVISIBILITY);
            }
            reappearIterator.remove();
        }
    }

    private static void onMobDamaged(ServerLevel level, Mob mob, LivingEntity attacker, Entity directEntity) {
        int tier = tier(level, mob);
        if (tier <= 0 || !AdaptiveAIGoalUtils.isValidAdaptiveTarget(attacker) || !mob.canAttack(attacker)) {
            return;
        }
        if (tier >= 5 && mob instanceof ZombifiedPiglin && ready(mob, "blood_call", 160)) {
            for (Mob ally : AdaptiveTargetingUtils.nearbyHostiles(mob, 12.0D, other -> other instanceof ZombifiedPiglin)) {
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
                ally.setTarget(attacker);
            }
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "zombified piglin blood call");
        }
        if (tier >= 4 && mob instanceof Spider && ready(mob, "web_retreat", 600)) {
            placeTemporaryCobweb(level, attacker.blockPosition(), level.getGameTime() + 100 + mob.getRandom().nextInt(61));
        }
        if (tier >= 4 && mob instanceof EnderMan enderman && directEntity == attacker && ready(mob, "backstep_ambush", tier >= 5 ? 80 : 130)) {
            Vec3 behind = attacker.position().subtract(attacker.getLookAngle().normalize().scale(2.2D));
            if (enderman.randomTeleport(behind.x, behind.y, behind.z, true)) {
                enderman.setTarget(attacker);
                AdaptiveAIGoalUtils.debug(mob, () -> tier, "enderman backstep ambush");
            }
        }
        if (tier >= 4 && mob instanceof Silverfish silverfish
                && AdaptiveAIGoalUtils.healthFraction(silverfish) < 0.45D
                && ready(mob, "burrow_burst", 220)) {
            silverfish.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 35, 0));
            silverfish.getNavigation().stop();
            DELAYED_REAPPEARS.add(new DelayedReappear(level.getServer(), level.dimension(), level.getGameTime() + 24,
                    silverfish.getUUID(), attacker.getUUID(), tier));
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "silverfish burrow vanish");
        }
        if (tier >= 4 && mob instanceof Evoker evoker && ready(mob, "vex_relay", 120)) {
            for (Mob vex : AdaptiveTargetingUtils.nearbyHostiles(evoker, 14.0D, other -> other instanceof Vex)) {
                vex.setTarget(attacker);
            }
            Vec3 retreat = AdaptivePositioningUtils.retreatPosition(evoker, attacker, 5.0D);
            evoker.getNavigation().moveTo(retreat.x, retreat.y, retreat.z, 1.0D);
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "evoker vex relay");
        }
        if (tier >= 5 && mob instanceof Blaze && eventWasRanged(directEntity) && ready(mob, "overheat", 120)) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0));
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "blaze overheat volley");
        }
        if (tier >= 5 && mob instanceof Zoglin && AdaptiveAIGoalUtils.healthFraction(mob) < 0.50D && ready(mob, "rampage", 180)) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "zoglin frenzied rampage");
        }
        if (tier >= 5 && mob instanceof Warden && eventWasRanged(directEntity) && attacker instanceof ServerPlayer player) {
            AdaptiveMemory.rememberThreat(mob, player, 20 * 30);
            mob.getPersistentData().putLong(COOLDOWN_PREFIX + "echo_mark", level.getGameTime() + 20 * 30L);
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "warden echo mark");
        }
    }

    private static void onPlayerHurt(ServerLevel level, ServerPlayer player, Mob mob, Entity directEntity, LivingHurtEvent event) {
        int tier = tier(level, mob);
        if (tier <= 0 || !AdaptiveAIGoalUtils.isValidAdaptiveTarget(player)) {
            return;
        }
        if (tier >= 4 && mob instanceof Drowned && mob.isInWater() && player.isInWater() && ready(mob, "dragging_grip", 160)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            player.setDeltaMovement(player.getDeltaMovement().add(0.0D, -0.22D, 0.0D));
            AdaptiveAIGoalUtils.debug(mob, () -> tier, "drowned dragging grip");
        }
        if (mob instanceof WitherSkeleton && tier >= 4 && AdaptiveAIGoalUtils.healthFraction(player) < 0.40D) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, tier >= 5 ? 140 : 100, 0));
            shove(player, mob.position(), tier >= 5 ? 0.55D : 0.35D, 0.08D);
        }
        if (mob instanceof Spider && tier >= 4 && ready(mob, "web_hit", mob instanceof CaveSpider ? 400 : 600)) {
            placeTemporaryCobweb(level, player.blockPosition(), level.getGameTime() + 100 + mob.getRandom().nextInt(61));
        }
        if (mob instanceof CaveSpider && tier >= 3
                && (AdaptiveAIGoalUtils.isUsingRangedWeapon(player) || player.isUsingItem())) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        }
        if (mob instanceof Pillager && tier >= 5 && directEntity instanceof AbstractArrow arrow
                && arrow.getPersistentData().getBoolean("am_pinning_bolt")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            disableShield(player);
        }
        if ((mob instanceof Vindicator || mob instanceof PiglinBrute) && tier >= 5) {
            event.setAmount(event.getAmount() + 2.0F);
            disableShield(player);
        }
        if ((mob instanceof Ravager || mob instanceof Hoglin) && tier >= 4 && ready(mob, "crushing_charge", 120)) {
            shove(player, mob.position(), mob instanceof Ravager ? 1.15D : 0.85D, 0.35D);
            if (player.horizontalCollision) {
                event.setAmount(event.getAmount() + 3.0F);
            }
        }
        if (mob instanceof Guardian && tier >= 4) {
            int focused = AdaptiveTargetingUtils.nearbyHostiles(mob, 12.0D,
                    other -> other instanceof Guardian && other.getTarget() == player).size();
            if (focused >= 2) {
                event.setAmount(event.getAmount() * (tier >= 5 ? 1.25F : 1.15F));
            }
            if (mob instanceof ElderGuardian) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
                player.setDeltaMovement(player.getDeltaMovement().add(0.0D, -0.12D, 0.0D));
            }
        }
        if (mob instanceof Shulker && tier >= 4 && player.hasEffect(MobEffects.LEVITATION)) {
            player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0));
        }
        if (mob instanceof Phantom && tier >= 5) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 0));
        }
        if (mob instanceof Vex && tier >= 5 && mob.noPhysics) {
            event.setAmount(event.getAmount() + 1.5F);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
        }
        if (mob instanceof Endermite && tier >= 3) {
            for (Mob ally : AdaptiveTargetingUtils.nearbyHostiles(mob, 10.0D,
                    other -> other instanceof Endermite || other instanceof EnderMan)) {
                ally.setTarget(player);
            }
        }
        if (mob.getPersistentData().getLong(COOLDOWN_PREFIX + "echo_mark") > level.getGameTime()
                && event.getSource().is(DamageTypes.SONIC_BOOM)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
            shove(player, mob.position(), 0.65D, 0.10D);
        }
    }

    private static int tier(ServerLevel level, Mob mob) {
        if (!adaptiveAbilitiesEnabled() || !AdaptiveGoalInjector.isAIEnabledFor(mob)) {
            return 0;
        }
        return AdaptiveDifficultyManager.getMobTier(level, mob.getType());
    }

    private static boolean ready(Mob mob, String key, long cooldownTicks) {
        long now = mob.level().getGameTime();
        String fullKey = COOLDOWN_PREFIX + key;
        if (mob.getPersistentData().getLong(fullKey) > now) {
            return false;
        }
        mob.getPersistentData().putLong(fullKey, now + cooldownTicks);
        return true;
    }

    private static void disableShield(ServerPlayer player) {
        if (AdaptiveAIGoalUtils.isBlockingShield(player)) {
            player.disableShield(true);
        }
    }

    private static void shove(LivingEntity target, Vec3 from, double horizontal, double vertical) {
        Vec3 away = target.position().subtract(from);
        double len = Math.max(0.001D, Math.sqrt(away.x * away.x + away.z * away.z));
        target.setDeltaMovement(target.getDeltaMovement().add(away.x / len * horizontal, vertical, away.z / len * horizontal));
        target.hurtMarked = true;
    }

    private static void placeTemporaryCobweb(ServerLevel level, BlockPos requestedPos, long expireTick) {
        BlockPos pos = level.getFluidState(requestedPos).is(FluidTags.WATER) ? requestedPos.above() : requestedPos;
        if (!level.getBlockState(pos).isAir() || !level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }
        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
        TEMPORARY_BLOCKS.add(new TemporaryBlock(level.getServer(), level.dimension(), pos.immutable(), expireTick));
        level.playSound(null, pos, SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 0.6F, 1.25F);
    }

    private static boolean eventWasRanged(Entity directEntity) {
        return directEntity instanceof Projectile;
    }

    private static void splitGhastFireball(ServerLevel level, Ghast ghast, LargeFireball original) {
        if (original.getPersistentData().getBoolean(SPLIT_FIREBALL_KEY)) {
            return;
        }
        Vec3 direction = original.getDeltaMovement();
        if (direction.lengthSqr() < 1.0E-4D && AdaptiveAIGoalUtils.isValidAdaptiveTarget(ghast.getTarget())) {
            direction = ghast.getTarget().getEyePosition().subtract(original.position());
        }
        if (direction.lengthSqr() < 1.0E-4D) {
            return;
        }
        direction = direction.normalize();
        Vec3 side = new Vec3(-direction.z, 0.0D, direction.x);
        if (side.lengthSqr() < 1.0E-4D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }
        spawnSplitFireball(level, ghast, original, direction.add(side.scale(0.18D)).normalize());
        spawnSplitFireball(level, ghast, original, direction.subtract(side.scale(0.18D)).normalize());
        AdaptiveAIGoalUtils.debug(ghast, () -> 5, "ghast splitting fireball");
    }

    private static void spawnSplitFireball(ServerLevel level, Ghast ghast, LargeFireball original, Vec3 direction) {
        LargeFireball split = new LargeFireball(level, ghast, direction.x, direction.y, direction.z,
                ghast.getExplosionPower());
        split.getPersistentData().putBoolean(SPLIT_FIREBALL_KEY, true);
        split.setPos(original.getX(), original.getY(), original.getZ());
        level.addFreshEntity(split);
    }

    public static void invalidateTemporaryBlock(ServerLevel level, BlockPos pos) {
        TEMPORARY_BLOCKS.removeIf(block -> block.server == level.getServer()
                && block.dimension == level.dimension() && block.pos.equals(pos));
    }

    public static void clear(ServerLevel level) {
        TEMPORARY_BLOCKS.removeIf(block -> {
            if (block.server != level.getServer() || block.dimension != level.dimension()) {
                return false;
            }
            if (level.getBlockState(block.pos).is(Blocks.COBWEB)) {
                level.removeBlock(block.pos, false);
            }
            return true;
        });
        DELAYED_REAPPEARS.removeIf(pending -> pending.server == level.getServer() && pending.dimension == level.dimension());
    }

    public static void clear(net.minecraft.server.MinecraftServer server) {
        TEMPORARY_BLOCKS.removeIf(block -> {
            if (block.server != server) {
                return false;
            }
            ServerLevel level = server.getLevel(block.dimension);
            if (level != null && level.getBlockState(block.pos).is(Blocks.COBWEB)) {
                level.removeBlock(block.pos, false);
            }
            return true;
        });
        DELAYED_REAPPEARS.removeIf(pending -> pending.server == server);
    }

    private static boolean adaptiveAbilitiesEnabled() {
        return AMConfig.enabled && AMConfig.AI_ENABLED.get() && AMConfig.ENABLE_ADVANCED_AI.get();
    }

    private record TemporaryBlock(net.minecraft.server.MinecraftServer server,
                                  net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                  BlockPos pos, long expireTick) {
    }

    private record DelayedReappear(net.minecraft.server.MinecraftServer server,
                                   net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                                   long tick, UUID mobId, UUID targetId, int tier) {
    }
}
