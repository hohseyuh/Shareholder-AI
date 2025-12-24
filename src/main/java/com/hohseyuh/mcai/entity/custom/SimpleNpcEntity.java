package com.hohseyuh.mcai.entity.custom;

import com.hohseyuh.mcai.ai.OreTargeting;

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
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class SimpleNpcEntity extends PathAwareEntity {

    private PlayerEntity followTarget;
    private boolean isMining;
    private BlockPos miningTarget;
    private float miningProgress;
    private long lastMessageTime;

    // Inventory & State
    private final SimpleInventory inventory = new SimpleInventory(27);
    private ItemEntity targetItemEntity;
    private boolean isRequestingTool = false;

    private static final String[] RANDOM_NAMES = { "Cavid", "Elvin", "Ruslan" };

    public SimpleNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        String name = RANDOM_NAMES[world.random.nextInt(RANDOM_NAMES.length)];
        this.setCustomName(Text.literal(name));
        this.setCustomNameVisible(true);
        this.setCanPickUpLoot(true);
    }

    public SimpleInventory getInventory() {
        return this.inventory;
    }

    @Override
    protected void mobTick() {
        super.mobTick();

        // 1. CRITICAL FIX: Validation
        // If we don't do this, the game crashes when the player leaves or changes
        // dimension
        validateFollowTarget();

        // 2. Priority: Item Pickup (Tools/Drops)
        if (targetItemEntity != null) {
            tickItemPickup();
            return;
        }

        if (this.age % 60 == 0) {
            checkForUnmineableOres();
        }

        // 3. Priority: Smart Leash
        if (followTarget != null) {
            // Safe to calculate distance now because we validated above
            double distSq = this.squaredDistanceTo(followTarget);

            boolean isActivelyBreaking = (miningTarget != null && miningProgress > 0);
            double allowedDistance = isActivelyBreaking ? 225.0 : 64.0; // 15 blocks vs 8 blocks

            if (distSq > allowedDistance) {
                // Too far - abandon mining to catch up
                if (miningTarget != null && miningProgress == 0) {
                    cleanupMining(); // Use helper to clean up cleanly
                }

                tickFollow();
                return;
            }
        }

        // 4. Priority: Mining
        if (isMining) {
            tickMining();
            return;
        }

        // 5. Priority: Casual Follow
        if (followTarget != null) {
            tickFollow();
        }
    }

    private void checkForUnmineableOres() {
        // Only complain if we haven't said anything recently (e.g. 10 seconds)
        if (this.getWorld().getTime() - lastMessageTime < 200)
            return;

        // Scan for things we see but CAN'T mine (Radius 8)
        BlockPos teaser = OreTargeting.findBestUnmineableOre(this, 8);

        if (teaser != null) {
            BlockState state = this.getWorld().getBlockState(teaser);

            // Determine what tool is needed
            String toolName = "better Pickaxe";
            if (state.isIn(BlockTags.NEEDS_IRON_TOOL))
                toolName = "Iron Pickaxe";
            if (state.isIn(BlockTags.NEEDS_DIAMOND_TOOL))
                toolName = "Diamond Pickaxe";

            this.sendMessage("I see " + state.getBlock().getName().getString()
                    + " over there, but I need an " + toolName + "!");

            // Look at it to show the player what we mean
            this.getLookControl().lookAt(teaser.getX(), teaser.getY(), teaser.getZ());

            // Update timer so we don't spam
            this.lastMessageTime = this.getWorld().getTime();
        }
    }

    // --- NEW HELPER METHOD TO PREVENT CRASHES ---
    private void validateFollowTarget() {
        if (this.followTarget != null) {
            // Check 1: Is player removed? (Disconnected)
            // Check 2: Is player dead?
            // Check 3: Is player in a different dimension? (The #1 cause of crashes)
            if (this.followTarget.isRemoved() || !this.followTarget.isAlive()
                    || this.followTarget.getWorld() != this.getWorld()) {
                this.followTarget = null;
                this.getNavigation().stop();
            }
        }
    }

    // ... [The rest of your methods remain exactly the same below] ...

    private void tickItemPickup() {
        if (targetItemEntity == null || !targetItemEntity.isAlive() || targetItemEntity.getStack().isEmpty()) {
            this.targetItemEntity = null;
            this.getNavigation().stop();
            return;
        }

        if (this.age % 10 == 0) {
            this.getNavigation().startMovingTo(targetItemEntity, 1.2);
        }

        if (this.squaredDistanceTo(targetItemEntity) < 4.0) {
            ItemStack stack = targetItemEntity.getStack();

            if (stack.getItem() instanceof ToolItem) {
                boolean needTool = isMining && miningTarget != null;
                boolean isBetter = false;

                if (needTool) {
                    BlockState targetState = this.getWorld().getBlockState(miningTarget);
                    if (stack.isSuitableFor(targetState)) {
                        isBetter = true;
                    }
                }

                if (isBetter) {
                    this.equipStack(EquipmentSlot.MAINHAND, stack);
                    this.isRequestingTool = false;
                    this.sendMessage("Thanks! This tool is perfect.");
                } else {
                    this.inventory.addStack(stack);
                    if (needTool) {
                        this.sendMessage("I can't use this tool for this ore.");
                    }
                }
            } else {
                this.inventory.addStack(stack);
            }

            targetItemEntity.discard();
            targetItemEntity = null;
            this.getNavigation().stop();
        }
    }

    private boolean findNearbyToolOnGround(BlockState state) {
        Box box = this.getBoundingBox().expand(10.0);
        List<ItemEntity> items = this.getWorld().getEntitiesByClass(ItemEntity.class, box, item -> true);

        for (ItemEntity item : items) {
            ItemStack stack = item.getStack();
            if (stack.isSuitableFor(state)) {
                this.targetItemEntity = item;
                this.getNavigation().startMovingTo(item, 1.2);
                this.sendMessage("I see a valid tool! Going to get it.");
                return true;
            }
        }
        return false;
    }

    private void tickMining() {
        if (miningTarget == null) {
            // TASK 1: Only find things we can ACTUALLY break (Coal/Iron)
            this.miningTarget = OreTargeting.findBestMineableOre(this, 7);
            return;
        }

        BlockState state = this.getWorld().getBlockState(miningTarget);
        if (state.isAir() || !isOre(state)) {
            cleanupMining();
            return;
        }

        ItemStack mainHand = this.getMainHandStack();
        boolean isToolValid = mainHand.isSuitableFor(state);

        if (!isToolValid) {
            if (swapToolFromInventory(state)) {
                this.isRequestingTool = false;
                return;
            }
            if (findNearbyToolOnGround(state)) {
                this.isRequestingTool = false;
                return;
            }
            requestTool(state);
            return;
        }

        this.isRequestingTool = false;

        double x = miningTarget.getX() + 0.5;
        double y = miningTarget.getY() + 0.5;
        double z = miningTarget.getZ() + 0.5;

        // UPDATED REACH DISTANCE
        double distSq = this.squaredDistanceTo(x, y, z);
        double reach = 20.25; // 4.5 blocks

        if (distSq > reach) {
            this.getNavigation().startMovingTo(x, y, z, 1.0);
            this.miningProgress = 0;
        } else {
            this.getNavigation().stop();
            this.getLookControl().lookAt(x, y, z);

            float hardness = state.getHardness(this.getWorld(), miningTarget);
            float speed = mainHand.getMiningSpeedMultiplier(state);

            if (hardness == -1.0f) {
                cleanupMining();
                return;
            }

            float damage = (hardness > 0) ? (speed / hardness / 30f) : 1.0f;
            this.miningProgress += damage;
            this.getWorld().setBlockBreakingInfo(this.getId(), miningTarget, (int) (this.miningProgress * 10));

            if (this.miningProgress >= 1.0f) {
                this.getWorld().breakBlock(miningTarget, true, this);
                cleanupMining();
                findNearbyDroppedOre();
            }
        }
    }

    private void requestTool(BlockState state) {
        long time = this.getWorld().getTime();
        if (!isRequestingTool || (time - lastMessageTime > 200)) {
            String msg = "I need a tool for " + state.getBlock().getName().getString() + "!";
            this.sendMessage(msg);
            lastMessageTime = time;
            isRequestingTool = true;
        }
        if (followTarget != null) {
            this.getLookControl().lookAt(followTarget);
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("IsMining", this.isMining);
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
        this.isMining = nbt.getBoolean("IsMining");
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

    private boolean swapToolFromInventory(BlockState state) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isSuitableFor(state)) {
                ItemStack currentHand = this.getMainHandStack();
                this.equipStack(EquipmentSlot.MAINHAND, stack);
                inventory.setStack(i, currentHand);
                return true;
            }
        }
        return false;
    }

    private void findNearbyDroppedOre() {
        Box box = this.getBoundingBox().expand(5.0);
        List<ItemEntity> items = this.getWorld().getEntitiesByClass(ItemEntity.class, box, item -> true);
        if (!items.isEmpty()) {
            this.targetItemEntity = items.get(0);
        }
    }

    private void sendMessage(String message) {
        if (!this.getWorld().isClient) {
            this.getWorld().getPlayers().forEach(player -> {
                if (player.squaredDistanceTo(this) < 400) {
                    player.sendMessage(Text.literal("<" + this.getCustomName().getString() + "> " + message), false);
                }
            });
        }
    }

    private void tickFollow() {
        if (followTarget == null)
            return; // Extra Safety

        double distanceSq = this.squaredDistanceTo(followTarget);
        if (distanceSq > 100.0) {
            this.getNavigation().startMovingTo(followTarget, 1.3);
        } else if (distanceSq > 9.0) {
            this.getNavigation().startMovingTo(followTarget, 1.0);
        } else {
            this.getNavigation().stop();
        }
    }

    private void cleanupMining() {
        if (miningTarget != null)
            this.getWorld().setBlockBreakingInfo(this.getId(), miningTarget, -1);
        this.miningTarget = null;
        this.miningProgress = 0;
    }

    // 1. UPDATED: Find Ore with Visibility Check
    private void findNearestOre() {
        BlockPos pos = this.getBlockPos();
        BlockPos nearest = null;
        double minDistanceSq = Double.MAX_VALUE;

        // Iterate all blocks nearby
        for (BlockPos p : BlockPos.iterate(pos.add(-7, -7, -7), pos.add(7, 7, 7))) {
            if (isOre(this.getWorld().getBlockState(p))) {

                // 1. Check Distance first (Optimization)
                double distSq = p.getSquaredDistance(pos);
                if (distSq < minDistanceSq) {

                    // 2. CRITICAL FIX: Raycast check
                    // Only target this ore if we have a direct line of sight to it
                    if (canSeeBlock(p)) {
                        minDistanceSq = distSq;
                        nearest = p.toImmutable();
                    }
                }
            }
        }
        this.miningTarget = nearest;
    }

    // 2. NEW HELPER: Raycast Logic
    private boolean canSeeBlock(BlockPos targetPos) {
        // Start ray from NPC's eyes
        net.minecraft.util.math.Vec3d start = this.getEyePos();
        // End ray at the center of the target block
        net.minecraft.util.math.Vec3d end = net.minecraft.util.math.Vec3d.ofCenter(targetPos);

        // Cast the ray
        net.minecraft.util.hit.BlockHitResult result = this.getWorld().raycast(new net.minecraft.world.RaycastContext(
                start,
                end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                this));

        // If the ray hits our target block, it's visible.
        // If it hits a stone wall first, the result will NOT equal targetPos.
        return result.getBlockPos().equals(targetPos);
    }

    private boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.GOLD_ORES);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.25));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    public void startMining() {
        this.isMining = true;
    }

    public void stopMining() {
        this.isMining = false;
        cleanupMining();
    }

    public void setFollowTarget(PlayerEntity target) {
        this.followTarget = target;
    }

    public static DefaultAttributeContainer.Builder createNpcAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0);
    }
}