package com.hohseyuh.mcai.ai;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;

import java.util.EnumSet;
import java.util.List;

public class PickupItemGoal extends Goal {
    private final SimpleNpcEntity npc;
    private ItemEntity targetItem;

    public PickupItemGoal(SimpleNpcEntity npc) {
        this.npc = npc;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (npc.age % 10 != 0)
            return false; // Optimize scanning
        if (npc.getJob() == NpcJob.NONE)
            return false;

        this.targetItem = findTargetItem();
        return this.targetItem != null;
    }

    @Override
    public boolean shouldContinue() {
        return targetItem != null && targetItem.isAlive() && !targetItem.getStack().isEmpty();
    }

    @Override
    public void start() {
        npc.getNavigation().startMovingTo(targetItem, 1.2);
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
        targetItem = null;
    }

    @Override
    public void tick() {
        if (targetItem == null)
            return;
        npc.getLookControl().lookAt(targetItem);

        if (npc.age % 20 == 0)
            npc.getNavigation().startMovingTo(targetItem, 1.2);

        if (npc.squaredDistanceTo(targetItem) < 4.0) {
            pickUp();
        }
    }

    private ItemEntity findTargetItem() {
        Box box = npc.getBoundingBox().expand(10.0);
        List<ItemEntity> items = npc.getWorld().getEntitiesByClass(ItemEntity.class, box, i -> true);

        ItemEntity bestItem = null;
        double closestDistSq = Double.MAX_VALUE;

        for (ItemEntity item : items) {
            ItemStack stack = item.getStack();
            boolean isInteresting = false;

            // RULE: MINER JOB
            if (npc.getJob() == NpcJob.MINER) {
                // A. Pickaxes
                if (stack.getItem() instanceof net.minecraft.item.PickaxeItem) {
                    if (npc.getMinerClass().canUseTool(stack.getItem())) {
                        // Want if no tool, or if new tool is stronger
                        if (!hasPickaxe() || MinerLevel.isStronger(stack.getItem(), npc.getMainHandStack().getItem())) {
                            isInteresting = true;
                        }
                    }
                }
                // B. Loot
                else if (isMiningLoot(stack)) {
                    isInteresting = true;
                }
            }

            if (isInteresting) {
                double distSq = npc.squaredDistanceTo(item);
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    bestItem = item;
                }
            }
        }
        return bestItem;
    }

    private void pickUp() {
        ItemStack stack = targetItem.getStack();
        ItemStack currentHand = npc.getMainHandStack();
        boolean shouldEquip = false;

        // Auto-Equip Logic for Miner
        if (npc.getJob() == NpcJob.MINER && stack.getItem() instanceof net.minecraft.item.PickaxeItem) {
            shouldEquip = true;
        }

        if (shouldEquip) {
            if (!currentHand.isEmpty())
                npc.getInventory().addStack(currentHand);
            npc.equipStack(EquipmentSlot.MAINHAND, stack);
            npc.sendMessage("Equipping " + stack.getName().getString());
        } else {
            npc.getInventory().addStack(stack);
        }

        npc.sendPickup(targetItem, stack.getCount());
        targetItem.discard();
        stop();
    }

    private boolean hasPickaxe() {
        return npc.getMainHandStack().getItem() instanceof net.minecraft.item.PickaxeItem;
    }

    private boolean isMiningLoot(ItemStack stack) {
        String name = stack.getTranslationKey().toLowerCase();
        return name.contains("raw") || name.contains("ore") || name.contains("diamond")
                || name.contains("emerald") || name.contains("ancient") || name.contains("scrap")
                || name.contains("ingot") || name.contains("coal") || name.contains("flint")
                || name.contains("iron") || name.contains("gold") || name.contains("copper");
    }
}