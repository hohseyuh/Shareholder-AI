package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class OreTargeting {

    // Define Base Values for Ores (Heuristics)
    private static final double VALUE_NETHERITE = 100.0;
    private static final double VALUE_DIAMOND = 80.0;
    private static final double VALUE_EMERALD = 70.0;
    private static final double VALUE_GOLD = 50.0;
    private static final double VALUE_IRON = 40.0;
    private static final double VALUE_REDSTONE = 30.0;
    private static final double VALUE_LAPIS = 30.0;
    private static final double VALUE_COAL = 10.0;
    private static final double VALUE_COPPER = 10.0;
    private static final double VALUE_DEFAULT = 5.0;

    /**
     * Scans for the best ore based on Value vs. Distance.
     * 
     * @param entity The NPC doing the looking.
     * @param radius The radius to scan (e.g., 7 blocks).
     * @return The BlockPos of the best target, or null if none found.
     */

    public static BlockPos findBestMineableOre(SimpleNpcEntity npc, int radius) {
        return scanForOre(npc, radius, true); // true = require tool
    }

    public static BlockPos findBestUnmineableOre(SimpleNpcEntity npc, int radius) {
        return scanForOre(npc, radius, false); // false = find things we lack tools for
    }

    private static BlockPos scanForOre(SimpleNpcEntity npc, int radius, boolean lookingForMineable) {
        World world = npc.getWorld();
        BlockPos center = npc.getBlockPos();

        BlockPos bestPos = null;
        double bestScore = -1.0;

        for (BlockPos pos : BlockPos.iterate(center.add(-radius, -radius, -radius),
                center.add(radius, radius, radius))) {
            BlockState state = world.getBlockState(pos);
            if (!isOre(state))
                continue;

            // --- THE LOGIC SPLIT ---
            boolean hasTool = hasToolFor(npc, state);

            if (lookingForMineable) {
                // If we want to mine, but don't have the tool, SKIP THIS BLOCK
                if (!hasTool)
                    continue;
            } else {
                // If we want to complain, we only care about blocks we CAN'T mine
                // Also, ignore low value stuff (don't complain about not being able to mine
                // stone)
                if (hasTool || getOreValue(state) < VALUE_GOLD)
                    continue;
            }
            // -----------------------

            // Distance Check
            double distSq = center.getSquaredDistance(pos);
            if (distSq > (radius * radius))
                continue;

            // Score Calculation
            double value = getOreValue(state);
            double score = value / (Math.sqrt(distSq) + 1);

            if (score > bestScore) {
                if (canSeeBlock(npc, pos)) {
                    bestScore = score;
                    bestPos = pos.toImmutable();
                }
            }
        }
        return bestPos;
    }

    public static boolean hasToolFor(SimpleNpcEntity npc, BlockState state) {
        // 1. Hand
        if (npc.getMainHandStack().isSuitableFor(state))
            return true;
        // 2. Inventory
        for (int i = 0; i < npc.getInventory().size(); i++) {
            ItemStack stack = npc.getInventory().getStack(i);
            if (stack.isSuitableFor(state))
                return true;
        }
        return false;
    }

    private static double getOreValue(BlockState state) {
        if (state.isOf(Blocks.ANCIENT_DEBRIS))
            return VALUE_NETHERITE;
        if (state.isIn(BlockTags.DIAMOND_ORES))
            return VALUE_DIAMOND;
        if (state.isIn(BlockTags.EMERALD_ORES))
            return VALUE_EMERALD;
        if (state.isIn(BlockTags.GOLD_ORES))
            return VALUE_GOLD;
        if (state.isIn(BlockTags.IRON_ORES))
            return VALUE_IRON;
        if (state.isIn(BlockTags.REDSTONE_ORES))
            return VALUE_REDSTONE;
        if (state.isIn(BlockTags.LAPIS_ORES))
            return VALUE_LAPIS;
        if (state.isIn(BlockTags.COAL_ORES))
            return VALUE_COAL;
        if (state.isIn(BlockTags.COPPER_ORES))
            return VALUE_COPPER;
        return VALUE_DEFAULT;
    }

    private static boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.EMERALD_ORES) || state.isIn(BlockTags.COPPER_ORES)
                || state.isOf(Blocks.ANCIENT_DEBRIS) || state.isOf(Blocks.NETHER_QUARTZ_ORE)
                || state.isOf(Blocks.NETHER_GOLD_ORE);
    }

    private static boolean canSeeBlock(LivingEntity entity, BlockPos targetPos) {
        Vec3d start = entity.getEyePos();
        Vec3d end = Vec3d.ofCenter(targetPos);

        BlockHitResult result = entity.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                entity));

        return result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(targetPos);
    }
}