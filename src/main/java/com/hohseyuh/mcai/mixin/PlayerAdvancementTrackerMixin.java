package com.hohseyuh.mcai.mixin;

import com.hohseyuh.mcai.entity.custom.SimpleNpcEntity;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;

    @Shadow
    public abstract AdvancementProgress getProgress(AdvancementEntry advancement);

    // CHANGED: Inject at "RETURN" instead of "INVOKE".
    // This is safer and prevents crashes if internal mappings change.
    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void onCriterionGranted(AdvancementEntry advancement, String criterionName,
            CallbackInfoReturnable<Boolean> cir) {
        // Safety Check 1: Ensure the grant was actually successful (returned true)
        if (cir.getReturnValue()) {
            AdvancementProgress progress = this.getProgress(advancement);

            // Safety Check 2: Ensure progress is done and it's a visible advancement (not a
            // recipe)
            if (progress != null && progress.isDone() && advancement.value().display().isPresent()) {

                // Wrap in try-catch to prevent "Invalid Player Data" kick if logic fails
                try {
                    checkAndNotifyNpcs(advancement);
                } catch (Exception e) {
                    // Log error instead of crashing the player connection
                    System.err.println("Error in NPC Advancement Mixin: " + e.getMessage());
                }
            }
        }
    }

    // Extracted logic to keep the Mixin clean
    private void checkAndNotifyNpcs(AdvancementEntry advancement) {
        if (this.owner == null || this.owner.getWorld() == null)
            return;

        // Optimized: 100 blocks is very large. Reduced to 50 for performance.
        // If 100 is strict requirement, ensure you don't lag the server.
        Box box = this.owner.getBoundingBox().expand(50.0);

        List<SimpleNpcEntity> npcs = this.owner.getWorld().getEntitiesByClass(
                SimpleNpcEntity.class,
                box,
                entity -> true);

        for (SimpleNpcEntity npc : npcs) {
            String npcName = npc.getCustomName() != null ? npc.getCustomName().getString() : "NPC";

            // Send the message
            this.owner.sendMessage(Text.literal("<" + npcName + "> gamemode'u bagla"), false);
        }
    }
}