package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

// ============================================================================
// NpcMeleeAttackGoal.java - IMPROVED COMBAT
// ============================================================================

public class NpcMeleeAttackGoal extends Goal {
    private final SimpleNpcEntity npc;
    private final double speed;

    private int attackCooldown;
    private int ticksUntilNextPathRecalculation;
    private int failedPathAttempts; // Track pathfinding failures

    public NpcMeleeAttackGoal(SimpleNpcEntity npc, double speed, boolean pauseWhenMobIdle) {
        this.npc = npc;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return npc.getJob() == NpcJob.GUARD;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = npc.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (npc.getJob() != NpcJob.GUARD) {
            return false;
        }
        // More aggressive - chase further
        return npc.squaredDistanceTo(target) < 400.0; // 20 blocks
    }

    @Override
    public void start() {
        npc.getNavigation().startMovingTo(npc.getTarget(), speed);
        this.ticksUntilNextPathRecalculation = 0;
        this.attackCooldown = 0;
        this.failedPathAttempts = 0;
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = npc.getTarget();
        if (target == null) {
            return;
        }

        // ALWAYS look at target (improves hit accuracy)
        npc.getLookControl().lookAt(target, 30.0F, 30.0F);

        double distSq = npc.squaredDistanceTo(target);
        double attackReach = getAttackReachSq(target);

        // Pathfinding update
        this.ticksUntilNextPathRecalculation = Math.max(ticksUntilNextPathRecalculation - 1, 0);

        if (ticksUntilNextPathRecalculation <= 0) {
            // Update path more frequently during combat (every 0.2-0.5 seconds)
            ticksUntilNextPathRecalculation = 4 + npc.getRandom().nextInt(7);

            if (distSq > attackReach) {
                // AGGRESSIVE PURSUIT - Use higher speed multiplier
                boolean pathSuccess = npc.getNavigation().startMovingTo(target, speed * 1.3);

                if (!pathSuccess) {
                    failedPathAttempts++;
                    // If stuck, try to jump or move directly toward target
                    if (failedPathAttempts > 3) {
                        npc.getNavigation().startMovingTo(
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                speed * 1.5);
                        failedPathAttempts = 0;
                    }
                }
            } else {
                // In range - stop moving for accurate hits
                npc.getNavigation().stop();
                failedPathAttempts = 0;
            }
        }

        // Attack logic
        this.attackCooldown = Math.max(attackCooldown - 1, 0);

        if (distSq <= attackReach) {
            if (attackCooldown <= 0) {
                // IMPROVED ATTACK TIMING
                // Reset cooldown BEFORE attack (prevents double-hits)
                attackCooldown = 15; // Faster attacks (0.75 seconds instead of 1)

                // Face target precisely before hitting
                npc.getLookControl().lookAt(target, 30.0F, 30.0F);

                // Perform attack
                npc.tryAttack(target);

                // Optional: Add slight knockback to create spacing
                npc.takeKnockback(0.1, target.getX() - npc.getX(), target.getZ() - npc.getZ());
            }
        }
    }

    /**
     * Improved attack reach calculation
     */
    private double getAttackReachSq(LivingEntity target) {
        // More generous reach (2.5 blocks instead of vanilla's ~2)
        float reach = npc.getWidth() * 2.5F + target.getWidth();
        return reach * reach;
    }
}