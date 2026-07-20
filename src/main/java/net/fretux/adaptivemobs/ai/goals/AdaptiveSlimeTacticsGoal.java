package net.fretux.adaptivemobs.ai.goals;

import net.fretux.adaptivemobs.ai.AdaptiveAIGoalUtils;
import net.fretux.adaptivemobs.ai.AdaptivePositioningUtils;
import net.fretux.adaptivemobs.config.AMConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.IntSupplier;

public class AdaptiveSlimeTacticsGoal extends Goal {

    private static final String RECOMBINE_READY_AT_KEY = "am_recombine_ready_at";
    private static final String RECOMBINE_CLAIM_KEY = "am_recombine_claim";
    private static final String RECOMBINE_CLAIM_EXPIRES_KEY = "am_recombine_claim_expires";
    private static final int MAX_RECOMBINE_SIZE = 4;
    private static final double RECOMBINE_SEARCH_RADIUS = 12.0D;
    private static final double RECOMBINE_DISTANCE_SQR = 3.24D;
    private static final double MAX_CHASE_DISTANCE_SQR = 256.0D;
    private static final int MAX_CHASE_TICKS = 200;
    private static final int MAX_STUCK_TICKS = 40;

    private final Slime slime;
    private final IntSupplier tierSupplier;
    private int cooldown;
    private int recombineCheckCooldown;
    private Slime recombinePartner;
    private int chaseTicks;
    private int stuckTicks;
    private Vec3 lastChasePosition;

    public AdaptiveSlimeTacticsGoal(Slime slime, IntSupplier tierSupplier) {
        this.slime = slime;
        this.tierSupplier = tierSupplier;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        scheduleRecombine(200 + slime.getRandom().nextInt(101));
    }

    @Override
    public boolean canUse() {
        boolean magma = slime instanceof MagmaCube;
        if (!AMConfig.ENABLE_MELEE_AI.get()
                || !(magma ? AMConfig.ENABLE_MAGMA_CUBE_ADVANCED_AI.get() : AMConfig.ENABLE_SLIME_ADVANCED_AI.get())
                || !AMConfig.AI_SLIME.get() || tierSupplier.getAsInt() < 2) {
            return false;
        }
        recombinePartner = null;
        if (tierSupplier.getAsInt() >= 5 && slime.getSize() < MAX_RECOMBINE_SIZE
                && slime.level().getGameTime() >= slime.getPersistentData().getLong(RECOMBINE_READY_AT_KEY)
                && --recombineCheckCooldown <= 0) {
            recombineCheckCooldown = 10;
            recombinePartner = findRecombinePartner();
            if (recombinePartner != null && claim(recombinePartner)) {
                return true;
            }
            recombinePartner = null;
        }
        return AdaptiveAIGoalUtils.isValidAdaptiveTarget(slime.getTarget()) && --cooldown <= 0;
    }

    @Override
    public void start() {
        int tier = tierSupplier.getAsInt();
        if (recombinePartner != null && recombinePartner.isAlive()) {
            chaseTicks = 0;
            stuckTicks = 0;
            lastChasePosition = slime.position();
            if (tryRecombine(recombinePartner)) {
                recombinePartner = null;
            }
            return;
        }
        cooldown = Math.max(16, 52 - tier * 5) + slime.getRandom().nextInt(25);
        LivingEntity target = slime.getTarget();
        if (!AdaptiveAIGoalUtils.isValidAdaptiveTarget(target)) {
            return;
        }
        double distance = Math.sqrt(slime.distanceToSqr(target));
        double side = slime.getId() % 2 == 0 ? 1.0D : -1.0D;
        Vec3 dest = AdaptivePositioningUtils.sidePosition(slime, target, Math.max(2.0D, distance * 0.55D), side, 0.0D);
        if (!AdaptivePositioningUtils.isReasonableGround(slime, dest)) {
            dest = target.position();
        }
        double speed = tier >= 5 && slime.getSize() >= 3 ? 1.25D : 1.0D;
        slime.getLookControl().setLookAt(target, 30.0F, 30.0F);
        slime.getMoveControl().setWantedPosition(dest.x, dest.y, dest.z, speed);
        if (slime.onGround() && distance < 9.0D) {
            Vec3 leap = target.position().subtract(slime.position()).normalize().scale(tier >= 5 && slime.getSize() >= 3 ? 0.34D : 0.24D);
            slime.setDeltaMovement(slime.getDeltaMovement().add(leap.x, 0.28D, leap.z));
        }
        AdaptiveAIGoalUtils.debug(slime, tierSupplier, "slime angled jump pressure");
    }

    @Override
    public boolean canContinueToUse() {
        return validPartner(recombinePartner, true) && chaseTicks < MAX_CHASE_TICKS
                && stuckTicks < MAX_STUCK_TICKS
                && slime.distanceToSqr(recombinePartner) <= MAX_CHASE_DISTANCE_SQR;
    }

    @Override
    public void tick() {
        chaseTicks++;
        if (lastChasePosition != null && slime.position().distanceToSqr(lastChasePosition) < 0.0025D) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastChasePosition = slime.position();
        }
        if (recombinePartner != null && tryRecombine(recombinePartner)) {
            recombinePartner = null;
        }
    }

    @Override
    public void stop() {
        releaseClaim(recombinePartner);
        recombinePartner = null;
    }

    private Slime findRecombinePartner() {
        boolean magma = slime instanceof MagmaCube;
        List<Slime> candidates = slime.level().getEntitiesOfClass(Slime.class,
                slime.getBoundingBox().inflate(RECOMBINE_SEARCH_RADIUS),
                other -> validPartner(other, false)
                        && (other instanceof MagmaCube) == magma
                        && claimAvailable(other));
        return candidates.stream().min(Comparator.comparingDouble(slime::distanceToSqr)).orElse(null);
    }

    private boolean tryRecombine(Slime partner) {
        if (!validPartner(partner, true) || slime.distanceToSqr(partner) > MAX_CHASE_DISTANCE_SQR) {
            releaseClaim(partner);
            return false;
        }
        if (slime.distanceToSqr(partner) > RECOMBINE_DISTANCE_SQR) {
            slime.getLookControl().setLookAt(partner, 30.0F, 30.0F);
            slime.getMoveControl().setWantedPosition(partner.getX(), partner.getY(), partner.getZ(), 1.2D);
            return false;
        }
        LivingEntity inheritedTarget = AdaptiveAIGoalUtils.isValidAdaptiveTarget(slime.getTarget())
                ? slime.getTarget() : partner.getTarget();
        float healthFraction = slime.getHealth() / slime.getMaxHealth();
        absorbPartnerState(partner);
        partner.discard();
        slime.setSize(Math.min(MAX_RECOMBINE_SIZE, slime.getSize() * 2), false);
        slime.setHealth(Math.max(1.0F, Math.min(slime.getMaxHealth(), slime.getMaxHealth() * healthFraction)));
        if (AdaptiveAIGoalUtils.isValidAdaptiveTarget(inheritedTarget)) {
            slime.setTarget(inheritedTarget);
        }
        scheduleRecombine(200 + slime.getRandom().nextInt(101));
        AdaptiveAIGoalUtils.debug(slime, tierSupplier,
                slime instanceof MagmaCube ? "magma cube recombined" : "slime recombined");
        return true;
    }

    private void absorbPartnerState(Slime partner) {
        if (!slime.hasCustomName() && partner.hasCustomName()) {
            slime.setCustomName(partner.getCustomName());
            slime.setCustomNameVisible(partner.isCustomNameVisible());
        }
        if (partner.isPersistenceRequired()) {
            slime.setPersistenceRequired();
        }
        partner.getActiveEffects().forEach(effect ->
                slime.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect)));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slime.getItemBySlot(slot).isEmpty() && !partner.getItemBySlot(slot).isEmpty()) {
                slime.setItemSlot(slot, partner.getItemBySlot(slot).copy());
            }
        }
        slime.getPersistentData().merge(partner.getPersistentData().copy());
        slime.getPersistentData().remove(RECOMBINE_CLAIM_KEY);
        slime.getPersistentData().remove(RECOMBINE_CLAIM_EXPIRES_KEY);
        if (partner.isLeashed() && partner.getLeashHolder() != null) {
            slime.setLeashedTo(partner.getLeashHolder(), true);
        }
    }

    private boolean validPartner(Slime partner, boolean requireClaim) {
        if (partner == null || partner == slime || !partner.isAlive() || !slime.isAlive()
                || tierSupplier.getAsInt() < 5 || slime.getSize() >= MAX_RECOMBINE_SIZE
                || partner.getSize() != slime.getSize()
                || (partner instanceof MagmaCube) != (slime instanceof MagmaCube)) {
            return false;
        }
        long now = slime.level().getGameTime();
        if (now < slime.getPersistentData().getLong(RECOMBINE_READY_AT_KEY)
                || now < partner.getPersistentData().getLong(RECOMBINE_READY_AT_KEY)) {
            return false;
        }
        return !requireClaim || (partner.getPersistentData().hasUUID(RECOMBINE_CLAIM_KEY)
                && partner.getPersistentData().getUUID(RECOMBINE_CLAIM_KEY).equals(slime.getUUID())
                && partner.getPersistentData().getLong(RECOMBINE_CLAIM_EXPIRES_KEY) >= now);
    }

    private boolean claimAvailable(Slime partner) {
        long now = slime.level().getGameTime();
        return !partner.getPersistentData().hasUUID(RECOMBINE_CLAIM_KEY)
                || partner.getPersistentData().getLong(RECOMBINE_CLAIM_EXPIRES_KEY) < now;
    }

    private boolean claim(Slime partner) {
        if (!validPartner(partner, false) || !claimAvailable(partner)) {
            return false;
        }
        partner.getPersistentData().putUUID(RECOMBINE_CLAIM_KEY, slime.getUUID());
        partner.getPersistentData().putLong(RECOMBINE_CLAIM_EXPIRES_KEY,
                slime.level().getGameTime() + MAX_CHASE_TICKS + 1L);
        return true;
    }

    private void releaseClaim(Slime partner) {
        if (partner != null && partner.getPersistentData().hasUUID(RECOMBINE_CLAIM_KEY)
                && partner.getPersistentData().getUUID(RECOMBINE_CLAIM_KEY).equals(slime.getUUID())) {
            partner.getPersistentData().remove(RECOMBINE_CLAIM_KEY);
            partner.getPersistentData().remove(RECOMBINE_CLAIM_EXPIRES_KEY);
        }
    }

    private void scheduleRecombine(int delayTicks) {
        if (slime.getPersistentData().getLong(RECOMBINE_READY_AT_KEY) == 0L) {
            slime.getPersistentData().putLong(RECOMBINE_READY_AT_KEY, slime.level().getGameTime() + delayTicks);
        }
    }
}
