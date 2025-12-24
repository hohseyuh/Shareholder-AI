package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;

public class MiningGoal extends Goal {
    private final SimpleNpcEntity npc;
    private BlockPos targetPos;
    private float breakingProgress;
    private int ticker;

    public MiningGoal(SimpleNpcEntity npc) {
        this.npc = npc;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        if (npc.getJob() != NpcJob.MINER)
            return false;

        // Don't mine if we are requesting a tool (prevents spamming air)
        if (npc.isRequestingTool())
            return false;

        // Don't mine if player is too far (Leash)
        if (npc.getFollowTarget() != null && npc.squaredDistanceTo(npc.getFollowTarget()) > 256.0)
            return false;

        BlockPos bestOre = OreTargeting.findBestMineableOre(npc, 8);
        if (bestOre != null) {
            this.targetPos = bestOre;
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldContinue() {
        if (npc.getJob() != NpcJob.MINER)
            return false;
        if (npc.getFollowTarget() != null && npc.squaredDistanceTo(npc.getFollowTarget()) > 256.0)
            return false;

        return targetPos != null
                && npc.getWorld().getBlockState(targetPos).isSolid()
                && npc.squaredDistanceTo(targetPos.getX(), targetPos.getY(), targetPos.getZ()) < 225.0;
    }

    @Override
    public void start() {
        this.breakingProgress = 0;
        this.ticker = 0;
        npc.setMining(true);
    }

    @Override
    public void stop() {
        npc.setMining(false);
        npc.getNavigation().stop();
        if (targetPos != null)
            npc.getWorld().setBlockBreakingInfo(npc.getId(), targetPos, -1);
        this.targetPos = null;
        this.breakingProgress = 0;
    }

    @Override
    public void tick() {
        if (targetPos == null)
            return;

        double x = targetPos.getX() + 0.5;
        double y = targetPos.getY() + 0.5;
        double z = targetPos.getZ() + 0.5;
        double distSq = npc.squaredDistanceTo(x, y, z);

        npc.getLookControl().lookAt(x, y, z);

        if (distSq > 20.25) {
            if (this.ticker % 5 == 0)
                npc.getNavigation().startMovingTo(x, y, z, 1.0);
        } else {
            npc.getNavigation().stop();
            BlockState state = npc.getWorld().getBlockState(targetPos);
            ItemStack tool = npc.getMainHandStack();

            float hardness = state.getHardness(npc.getWorld(), targetPos);
            if (state.isAir() || hardness < 0) {
                findNextTargetOrStop();
                return;
            }

            float speed = tool.getMiningSpeedMultiplier(state);
            float damage = (hardness > 0) ? (speed / hardness / 30f) : 1.0f;

            breakingProgress += damage;
            npc.getWorld().setBlockBreakingInfo(npc.getId(), targetPos, (int) (breakingProgress * 10));

            if (breakingProgress >= 1.0f) {
                npc.getWorld().breakBlock(targetPos, true, npc);
                // CHAIN MINING: Immediately find next block
                findNextTargetOrStop();
            }
        }
        this.ticker++;
    }

    private void findNextTargetOrStop() {
        npc.getWorld().setBlockBreakingInfo(npc.getId(), targetPos, -1);
        this.breakingProgress = 0;

        BlockPos nextOre = OreTargeting.findBestMineableOre(npc, 8);
        if (nextOre != null) {
            this.targetPos = nextOre;
            npc.getLookControl().lookAt(nextOre.getX(), nextOre.getY(), nextOre.getZ());
        } else {
            this.targetPos = null;
        }
    }
}