package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {
    private final SimpleNpcEntity npc;
    private final double speed;
    private final float minDistance; // Stop moving when this close (e.g. 3 blocks)
    private final float maxDistance; // Start moving when this far (e.g. 10 blocks)

    private LivingEntity target;

    public FollowPlayerGoal(SimpleNpcEntity npc, double speed, float minDistance, float maxDistance) {
        this.npc = npc;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // 1. Do we have a leader?
        LivingEntity leader = npc.getFollowTarget();
        if (leader == null)
            return false;

        // 2. Are they too far?
        if (npc.squaredDistanceTo(leader) < (maxDistance * maxDistance)) {
            return false; // We are close enough
        }

        this.target = leader;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        // Keep following until we are close enough or leader is lost
        return this.target != null
                && this.target.isAlive()
                && npc.squaredDistanceTo(this.target) > (minDistance * minDistance);
    }

    @Override
    public void start() {
        this.npc.getNavigation().startMovingTo(this.target, this.speed);
    }

    @Override
    public void stop() {
        this.npc.getNavigation().stop();
        this.target = null;
    }

    @Override
    public void tick() {
        // Update path every 10 ticks to avoid lag, or if target moves significantly
        if (this.npc.age % 10 == 0) {
            this.npc.getNavigation().startMovingTo(this.target, this.speed);
        }
        this.npc.getLookControl().lookAt(this.target, 10.0F, this.npc.getMaxLookPitchChange());
    }
}