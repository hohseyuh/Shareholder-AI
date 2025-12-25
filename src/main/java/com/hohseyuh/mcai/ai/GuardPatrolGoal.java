package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class GuardPatrolGoal extends Goal {
    private final SimpleNpcEntity npc;
    private final double speed;
    private final float maxRadius; // How far he can wander from post

    public GuardPatrolGoal(SimpleNpcEntity npc, double speed, float maxRadius) {
        this.npc = npc;
        this.speed = speed;
        this.maxRadius = maxRadius;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (npc.getJob() != NpcJob.GUARD)
            return false;

        BlockPos post = npc.getGuardPost();
        if (post == null)
            return false;

        // Start if we drifted too far from home
        return npc.squaredDistanceTo(post.getX(), post.getY(), post.getZ()) > (maxRadius * maxRadius);
    }

    @Override
    public void start() {
        BlockPos post = npc.getGuardPost();
        if (post != null) {
            npc.getNavigation().startMovingTo(post.getX(), post.getY(), post.getZ(), speed);
        }
    }

    @Override
    public boolean shouldContinue() {
        // Keep walking until we are close to the post
        BlockPos post = npc.getGuardPost();
        return post != null
                && npc.getJob() == NpcJob.GUARD
                && npc.squaredDistanceTo(post.getX(), post.getY(), post.getZ()) > 100.0; // Stop when within 10 blocks
    }
}