package com.hohseyuh.mcai.ai;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;

public enum MinerLevel {
    NOVICE(0), // Wood/Gold
    APPRENTICE(1), // Stone
    JOURNEYMAN(2), // Iron
    EXPERT(3), // Diamond
    MASTER(4); // Netherite

    private final int rank;

    MinerLevel(int rank) {
        this.rank = rank;
    }

    public boolean canUseTool(Item item) {
        if (!(item instanceof ToolItem tool))
            return false;
        return getMaterialRank(tool.getMaterial()) <= this.rank;
    }

    public static boolean isStronger(Item newTool, Item currentTool) {
        if (currentTool == null || currentTool == Items.AIR)
            return true;
        if (!(newTool instanceof ToolItem newT))
            return false;
        if (!(currentTool instanceof ToolItem curT))
            return true;

        return getMaterialRank(newT.getMaterial()) > getMaterialRank(curT.getMaterial());
    }

    private static int getMaterialRank(ToolMaterial mat) {
        // Safe comparison for vanilla materials
        if (mat == ToolMaterials.WOOD || mat == ToolMaterials.GOLD)
            return 0;
        if (mat == ToolMaterials.STONE)
            return 1;
        if (mat == ToolMaterials.IRON)
            return 2;
        if (mat == ToolMaterials.DIAMOND)
            return 3;
        if (mat == ToolMaterials.NETHERITE)
            return 4;
        return 0; // Default to lowest
    }
}