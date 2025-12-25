package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

// ============================================================================
// GuardTargetGoal.java - TARGET SELECTOR
// ============================================================================

public class GuardTargetGoal extends Goal {
    private final SimpleNpcEntity npc;
    private final float guardRadius;
    private final int scanInterval;

    private int ticksSinceLastScan;

    public GuardTargetGoal(SimpleNpcEntity npc, float guardRadius, int scanInterval) {
        this.npc = npc;
        this.guardRadius = guardRadius;
        this.scanInterval = scanInterval;
        this.ticksSinceLastScan = 0;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (npc.getJob() != NpcJob.GUARD) {
            return false;
        }

        BlockPos post = npc.getGuardPost();
        if (post == null) {
            return false;
        }

        if (++ticksSinceLastScan < scanInterval) {
            return false;
        }
        ticksSinceLastScan = 0;

        if (npc.getTarget() != null && npc.getTarget().isAlive()) {
            return false;
        }

        LivingEntity threat = findNearestThreat(post);
        if (threat != null) {
            npc.setTarget(threat);
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = npc.getTarget();
        BlockPos post = npc.getGuardPost();

        if (npc.getJob() != NpcJob.GUARD || post == null) {
            return false;
        }

        if (target == null || !target.isAlive()) {
            return false;
        }

        double distToPost = target.squaredDistanceTo(post.getX(), post.getY(), post.getZ());
        if (distToPost > (guardRadius * guardRadius * 1.5)) {
            npc.sendMessage("Enemy escaped.");
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        LivingEntity target = npc.getTarget();
        if (target != null) {
            npc.sendMessage("Engaging " + target.getName().getString() + "!");
        }
    }

    @Override
    public void stop() {
        npc.setTarget(null);
    }

    private LivingEntity findNearestThreat(BlockPos guardPost) {
        Box searchBox = new Box(guardPost).expand(guardRadius);
        List<HostileEntity> hostiles = npc.getWorld().getEntitiesByClass(
                HostileEntity.class,
                searchBox,
                entity -> entity.isAlive() && !entity.isRemoved());

        if (hostiles.isEmpty()) {
            return null;
        }

        return hostiles.stream()
                .min(Comparator.comparingDouble(
                        entity -> entity.squaredDistanceTo(guardPost.getX(), guardPost.getY(), guardPost.getZ())))
                .orElse(null);
    }
}

// //
// ============================================================================
// // NpcMeleeAttackGoal.java - IMPROVED COMBAT
// //
// ============================================================================

// public class NpcMeleeAttackGoal extends Goal {
// private final SimpleNpcEntity npc;
// private final double speed;

// private int attackCooldown;
// private int ticksUntilNextPathRecalculation;
// private int failedPathAttempts; // Track pathfinding failures

// public NpcMeleeAttackGoal(SimpleNpcEntity npc, double speed, boolean
// pauseWhenMobIdle) {
// this.npc = npc;
// this.speed = speed;
// this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
// }

// @Override
// public boolean canStart() {
// LivingEntity target = npc.getTarget();
// if (target == null || !target.isAlive()) {
// return false;
// }
// return npc.getJob() == NpcJob.GUARD;
// }

// @Override
// public boolean shouldContinue() {
// LivingEntity target = npc.getTarget();
// if (target == null || !target.isAlive()) {
// return false;
// }
// if (npc.getJob() != NpcJob.GUARD) {
// return false;
// }
// // More aggressive - chase further
// return npc.squaredDistanceTo(target) < 400.0; // 20 blocks
// }

// @Override
// public void start() {
// npc.getNavigation().startMovingTo(npc.getTarget(), speed);
// this.ticksUntilNextPathRecalculation = 0;
// this.attackCooldown = 0;
// this.failedPathAttempts = 0;
// }

// @Override
// public void stop() {
// npc.getNavigation().stop();
// }

// @Override
// public void tick() {
// LivingEntity target = npc.getTarget();
// if (target == null) {
// return;
// }

// // ALWAYS look at target (improves hit accuracy)
// npc.getLookControl().lookAt(target, 30.0F, 30.0F);

// double distSq = npc.squaredDistanceTo(target);
// double attackReach = getAttackReachSq(target);

// // Pathfinding update
// this.ticksUntilNextPathRecalculation =
// Math.max(ticksUntilNextPathRecalculation - 1, 0);

// if (ticksUntilNextPathRecalculation <= 0) {
// // Update path more frequently during combat (every 0.2-0.5 seconds)
// ticksUntilNextPathRecalculation = 4 + npc.getRandom().nextInt(7);

// if (distSq > attackReach) {
// // AGGRESSIVE PURSUIT - Use higher speed multiplier
// boolean pathSuccess = npc.getNavigation().startMovingTo(target, speed * 1.3);

// if (!pathSuccess) {
// failedPathAttempts++;
// // If stuck, try to jump or move directly toward target
// if (failedPathAttempts > 3) {
// npc.getNavigation().startMovingTo(
// target.getX(),
// target.getY(),
// target.getZ(),
// speed * 1.5);
// failedPathAttempts = 0;
// }
// }
// } else {
// // In range - stop moving for accurate hits
// npc.getNavigation().stop();
// failedPathAttempts = 0;
// }
// }

// // Attack logic
// this.attackCooldown = Math.max(attackCooldown - 1, 0);

// if (distSq <= attackReach) {
// if (attackCooldown <= 0) {
// // IMPROVED ATTACK TIMING
// // Reset cooldown BEFORE attack (prevents double-hits)
// attackCooldown = 15; // Faster attacks (0.75 seconds instead of 1)

// // Face target precisely before hitting
// npc.getLookControl().lookAt(target, 30.0F, 30.0F);

// // Perform attack
// npc.tryAttack(target);

// // Optional: Add slight knockback to create spacing
// npc.takeKnockback(0.1, target.getX() - npc.getX(), target.getZ() -
// npc.getZ());
// }
// }
// }

// /**
// * Improved attack reach calculation
// */
// private double getAttackReachSq(LivingEntity target) {
// // More generous reach (2.5 blocks instead of vanilla's ~2)
// float reach = npc.getWidth() * 2.5F + target.getWidth();
// return reach * reach;
// }
// }

// //
// ============================================================================
// // GuardEscapeDangerGoal.java - SMART DANGER AVOIDANCE
// //
// ============================================================================

// /**
// * Modified EscapeDangerGoal that doesn't trigger during combat
// * Replace the standard EscapeDangerGoal with this
// */
// public class GuardEscapeDangerGoal extends Goal {
// private final SimpleNpcEntity npc;
// private final double speed;

// public GuardEscapeDangerGoal(SimpleNpcEntity npc, double speed) {
// this.npc = npc;
// this.speed = speed;
// this.setControls(EnumSet.of(Control.MOVE));
// }

// @Override
// public boolean canStart() {
// // DON'T FLEE if we're a guard in combat!
// if (npc.getJob() == NpcJob.GUARD && npc.getTarget() != null) {
// return false; // Stand and fight!
// }

// // For other jobs, flee from danger normally
// if (npc.isOnFire() || npc.getRecentDamageSource() != null) {
// return true;
// }

// return false;
// }

// @Override
// public boolean shouldContinue() {
// // Stop fleeing if we became a guard
// if (npc.getJob() == NpcJob.GUARD && npc.getTarget() != null) {
// return false;
// }

// return npc.isOnFire();
// }

// @Override
// public void start() {
// // Simple flee logic - move away from danger
// double x = npc.getX() + (npc.getRandom().nextDouble() - 0.5) * 10;
// double z = npc.getZ() + (npc.getRandom().nextDouble() - 0.5) * 10;
// npc.getNavigation().startMovingTo(x, npc.getY(), z, speed);
// }
// }