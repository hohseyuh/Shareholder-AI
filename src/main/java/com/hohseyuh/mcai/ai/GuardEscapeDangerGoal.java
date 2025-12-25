package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

// ============================================================================
// GuardEscapeDangerGoal.java - SMART DANGER AVOIDANCE
// ============================================================================

/**
 * Modified EscapeDangerGoal that doesn't trigger during combat
 * Replace the standard EscapeDangerGoal with this
 */
public class GuardEscapeDangerGoal extends Goal {
    private final SimpleNpcEntity npc;
    private final double speed;

    public GuardEscapeDangerGoal(SimpleNpcEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // DON'T FLEE if we're a guard in combat!
        if (npc.getJob() == NpcJob.GUARD && npc.getTarget() != null) {
            return false; // Stand and fight!
        }

        // For other jobs, flee from danger normally
        if (npc.isOnFire() || npc.getRecentDamageSource() != null) {
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldContinue() {
        // Stop fleeing if we became a guard
        if (npc.getJob() == NpcJob.GUARD && npc.getTarget() != null) {
            return false;
        }

        return npc.isOnFire();
    }

    @Override
    public void start() {
        // Simple flee logic - move away from danger
        double x = npc.getX() + (npc.getRandom().nextDouble() - 0.5) * 10;
        double z = npc.getZ() + (npc.getRandom().nextDouble() - 0.5) * 10;
        npc.getNavigation().startMovingTo(x, npc.getY(), z, speed);
    }
}