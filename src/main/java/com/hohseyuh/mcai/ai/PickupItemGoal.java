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

            // RULE 2: GUARD JOB (New!)
            else if (npc.getJob() == NpcJob.GUARD) {
                // A. Swords
                if (stack.getItem() instanceof net.minecraft.item.SwordItem) {
                    // Grab if we have no sword, or this one is stronger (using MinerLevel logic as
                    // a proxy for tier)
                    ItemStack current = npc.getMainHandStack();
                    if (current.isEmpty() || !(current.getItem() instanceof net.minecraft.item.SwordItem)
                            || MinerLevel.isStronger(stack.getItem(), current.getItem())) {
                        isInteresting = true;
                    }
                }
                // B. Armor (Simple check: is it an armor item?)
                else if (stack.getItem() instanceof net.minecraft.item.ArmorItem) {
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

        // Auto-Equip Logic for Guard
        if (npc.getJob() == NpcJob.GUARD && stack.getItem() instanceof net.minecraft.item.SwordItem) {
            shouldEquip = true;
        }

        if (shouldEquip) {
            if (!currentHand.isEmpty())
                npc.getInventory().addStack(currentHand);
            npc.equipStack(EquipmentSlot.MAINHAND, stack);
            npc.sendMessage("Equipping " + stack.getName().getString());
        }
        // Auto-Equip Armor
        else if (stack.getItem() instanceof net.minecraft.item.ArmorItem armor) {
            EquipmentSlot slot = armor.getSlotType();
            ItemStack currentArmor = npc.getEquippedStack(slot);
            if (currentArmor.isEmpty()) {
                npc.equipStack(slot, stack);
                npc.sendMessage("Putting on armor.");
            } else {
                npc.getInventory().addStack(stack);
            }
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