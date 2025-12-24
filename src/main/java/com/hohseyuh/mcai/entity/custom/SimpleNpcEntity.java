package com.hohseyuh.mcai.entity.custom;

import com.hohseyuh.mcai.ai.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

@SuppressWarnings("unused")
public class SimpleNpcEntity extends PathAwareEntity {

    // --- STATE ---
    private NpcJob currentJob = NpcJob.NONE;
    private MinerLevel minerClass = MinerLevel.APPRENTICE;

    // Logic Vars
    private PlayerEntity followTarget;
    private boolean isMining = false; // Helper for animation/status
    private boolean isRequestingTool = false; // Helper for chat
    private long lastMessageTime;

    // Queues
    private String queuedMessage;
    private int messageDelayTimer = 0;
    private PlayerEntity dropTargetPlayer; // For "give loot" command

    // Inventory
    private final SimpleInventory inventory = new SimpleInventory(27);

    private static final String[] RANDOM_NAMES = { "Cavid", "Elvin", "Ruslan" };

    public SimpleNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        String name = RANDOM_NAMES[world.random.nextInt(RANDOM_NAMES.length)];
        this.setCustomName(Text.literal(name));
        this.setCustomNameVisible(true);
        this.setCanPickUpLoot(true);
    }

    // =============================================================
    // 1. BRAIN (GOALS)
    // =============================================================
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));

        // Priority 1: SAFETY (Run from Fire/Lava/Cactus)
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.5));

        // Priority 2: SCAVENGING (Tools & Loot)
        // Checks Job internally. Overrides Mining.
        this.goalSelector.add(2, new PickupItemGoal(this));

        // Priority 3: WORK (Mining)
        // Checks Job internally.
        this.goalSelector.add(3, new MiningGoal(this));

        // Priority 4: FOLLOW (Loyalty)
        // If not working or safe, follow player.
        this.goalSelector.add(4, new FollowPlayerGoal(this, 1.0, 3.0f, 6.0f));

        // Priority 5: IDLE
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    // =============================================================
    // 2. SENSES (TICK LOGIC)
    // =============================================================
    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.getWorld().isClient)
            return;

        // A. Chat Queue
        if (messageDelayTimer > 0) {
            messageDelayTimer--;
            if (messageDelayTimer == 0 && queuedMessage != null) {
                this.sendMessage(queuedMessage);
                queuedMessage = null;
            }
        }

        validateFollowTarget();

        // B. High Priority Overrides (Commands)
        if (dropTargetPlayer != null) {
            tickDelivery();
            return;
        }

        // C. Passive Spotter (Only for Miner)
        if (currentJob == NpcJob.MINER) {
            if (this.age % 60 == 0)
                checkForUnmineableOres();
        }
    }

    // =============================================================
    // 3. ACTIONS & COMMANDS
    // =============================================================

    public void commandUnequip(PlayerEntity player) {
        this.dropTargetPlayer = player;
        this.getNavigation().stop();
        this.sendMessage("Bringing you my tool.");
    }

    public void commandDropLoot(PlayerEntity player) {
        this.dropTargetPlayer = player;
        this.getNavigation().stop();
        this.sendMessage("Emptying backpack.");
    }

    // Handles walking to player and dropping items (for both commands above)
    private void tickDelivery() {
        if (dropTargetPlayer == null || !dropTargetPlayer.isAlive()) {
            dropTargetPlayer = null;
            return;
        }

        double distSq = this.squaredDistanceTo(dropTargetPlayer);

        if (distSq > 9.0) {
            this.getNavigation().startMovingTo(dropTargetPlayer, 1.3);
        } else {
            this.getNavigation().stop();
            this.getLookControl().lookAt(dropTargetPlayer);

            boolean droppedSomething = false;

            // 1. Drop Backpack Contents
            for (int i = 0; i < this.inventory.size(); i++) {
                ItemStack stack = this.inventory.getStack(i);
                if (!stack.isEmpty()) {
                    dropItemToPlayer(stack);
                    this.inventory.setStack(i, ItemStack.EMPTY);
                    droppedSomething = true;
                }
            }

            // 2. Drop Hand Item (Only if explicitly unequip command, logic simplified here)
            if (!droppedSomething && !this.getMainHandStack().isEmpty()) {
                ItemStack hand = this.getMainHandStack();
                dropItemToPlayer(hand);
                this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                this.sendMessage("Here is my " + hand.getName().getString());
            } else {
                this.sendMessage(droppedSomething ? "There is my loot." : "I have nothing for you.");
            }

            this.dropTargetPlayer = null;
        }
    }

    private void dropItemToPlayer(ItemStack stack) {
        ItemEntity droppedItem = new ItemEntity(getWorld(), getX(), getEyeY() - 0.3, getZ(), stack);
        droppedItem.setPickupDelay(40);
        Vec3d vec = dropTargetPlayer.getEyePos().subtract(this.getPos()).normalize().multiply(0.3);
        droppedItem.setVelocity(vec);
        getWorld().spawnEntity(droppedItem);
    }

    private void checkForUnmineableOres() {
        if (this.getWorld().getTime() - lastMessageTime < 200)
            return;
        BlockPos teaser = OreTargeting.findBestUnmineableOre(this, 8);
        if (teaser != null) {
            BlockState state = this.getWorld().getBlockState(teaser);
            this.sendMessage("I see " + state.getBlock().getName().getString() + " but can't mine it!");
            this.getLookControl().lookAt(teaser.getX(), teaser.getY(), teaser.getZ());
            this.lastMessageTime = this.getWorld().getTime();
        }
    }

    // =============================================================
    // 4. GETTERS / SETTERS / BOILERPLATE
    // =============================================================

    public void setJob(NpcJob newJob) {
        this.currentJob = newJob;
        this.getNavigation().stop();
        this.sendMessage("Job changed to: " + newJob.name());
        this.isRequestingTool = false;
    }

    public void promote() {
        int current = this.minerClass.ordinal();
        if (current < MinerLevel.values().length - 1) {
            this.minerClass = MinerLevel.values()[current + 1];
            this.sendMessage("Promoted to " + this.minerClass.name() + "!");
        }
    }

    public void demote() {
        int current = this.minerClass.ordinal();
        if (current > 0) {
            this.minerClass = MinerLevel.values()[current - 1];
            this.sendMessage("Demoted to " + this.minerClass.name() + ".");
        }
    }

    public void reportClass() {
        this.sendMessage("Current rank: " + this.minerClass.name());
    }

    // Required for Goals
    public SimpleInventory getInventory() {
        return this.inventory;
    }

    public NpcJob getJob() {
        return currentJob;
    }

    public MinerLevel getMinerClass() {
        return minerClass;
    }

    public PlayerEntity getFollowTarget() {
        return followTarget;
    }

    public void setFollowTarget(PlayerEntity target) {
        this.followTarget = target;
    }

    public boolean isMining() {
        return isMining;
    }

    public void setMining(boolean mining) {
        this.isMining = mining;
    }

    public boolean isRequestingTool() {
        return isRequestingTool;
    }

    public void setRequestingTool(boolean requesting) {
        this.isRequestingTool = requesting;
    }

    public void sendMessage(String message) {
        if (!this.getWorld().isClient) {
            this.getWorld().getPlayers().forEach(player -> {
                if (player.squaredDistanceTo(this) < 400) {
                    player.sendMessage(Text.literal("<" + this.getCustomName().getString() + "> " + message), false);
                }
            });
        }
    }

    public void setDelayedMessage(String msg, int delay) {
        this.queuedMessage = msg;
        this.messageDelayTimer = delay;
    }

    private void validateFollowTarget() {
        if (this.followTarget != null) {
            if (this.followTarget.isRemoved() || !this.followTarget.isAlive()
                    || this.followTarget.getWorld() != this.getWorld()) {
                this.followTarget = null;
                this.getNavigation().stop();
            }
        }
    }

    public static DefaultAttributeContainer.Builder createNpcAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0); // Fixes crash
    }

    // =============================================================
    // 5. NBT DATA
    // =============================================================
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("MinerClass", this.minerClass.name());
        nbt.putString("CurrentJob", this.currentJob.name());
        nbt.putBoolean("IsRequestingTool", this.isRequestingTool);

        NbtList list = new NbtList();
        for (int i = 0; i < this.inventory.size(); ++i) {
            ItemStack stack = this.inventory.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound itemNbt = (NbtCompound) stack.encode(this.getRegistryManager());
                itemNbt.putByte("Slot", (byte) i);
                list.add(itemNbt);
            }
        }
        nbt.put("Inventory", list);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("CurrentJob")) {
            try {
                this.currentJob = NpcJob.valueOf(nbt.getString("CurrentJob"));
            } catch (Exception e) {
                this.currentJob = NpcJob.NONE;
            }
        }
        if (nbt.contains("MinerClass")) {
            try {
                this.minerClass = MinerLevel.valueOf(nbt.getString("MinerClass"));
            } catch (Exception e) {
                this.minerClass = MinerLevel.APPRENTICE;
            }
        }
        this.isRequestingTool = nbt.getBoolean("IsRequestingTool");

        NbtList list = nbt.getList("Inventory", 10);
        for (int i = 0; i < list.size(); ++i) {
            NbtCompound itemNbt = list.getCompound(i);
            int j = itemNbt.getByte("Slot") & 255;
            if (j < this.inventory.size()) {
                ItemStack stack = ItemStack.fromNbt(this.getRegistryManager(), itemNbt).orElse(ItemStack.EMPTY);
                this.inventory.setStack(j, stack);
            }
        }
    }
}