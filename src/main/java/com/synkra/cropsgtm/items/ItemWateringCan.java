package com.synkra.cropsgtm.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.synkra.cropsgtm.api.ICropRightClickHandler;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 一种洒水壶，可存水并在使用时浇灌瞄准方块周围的作物架。
 * <p>
 * 廉价水壶覆盖 3x1x3 范围、容量 3600 mB；强化水壶覆盖 5x1x5 范围、容量 10000 mB。对水源右键可补水，且不会消耗水源。
 * </p>
 * <p>
 * 使用水壶类似拉弓：物品保持不动、玩家缓慢移动，每四分之一秒浇灌瞄准方块周围的作物架。范围内没有作物架时仍会消耗一次水量，相当于把水倒在地上。
 * </p>
 */
public class ItemWateringCan extends Item implements ICropRightClickHandler {

    /** 两次浇水刻之间的刻数。 */
    private static final int WATERING_INTERVAL = 5;
    /** 物品可使用的刻数，实际上为无限。 */
    private static final int USE_DURATION = 72000;

    /** 区域半径（方块）。浇水区域宽 {@code (2 * radius + 1)}、高一格。 */
    private final int radius;
    /** 该水壶可存储的最大水量。 */
    private final int capacity;
    /** 每次浇水刻给予每个作物的水量。 */
    private final int waterPerUse;
    /** 带此标志的洒水壶永远不需要补充。 */
    private final boolean infinite;

    public ItemWateringCan(int capacity, int radius) {
        this(capacity, radius, Constants.WATERING_CAN_WATER_PER_USE, false);
    }

    public ItemWateringCan(int capacity, int radius, int waterPerUse, boolean infinite) {
        super(new Item.Properties().stacksTo(1));
        this.capacity = capacity;
        this.radius = radius;
        this.waterPerUse = waterPerUse;
        this.infinite = infinite;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getRadius() {
        return this.radius;
    }

    public int getWaterPerUse() {
        return this.waterPerUse;
    }

    public boolean isInfinite() {
        return this.infinite;
    }

    public static int getWater(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return 0;
        return Math.max(0, Math.min(getCapacityOf(stack), tag.getInt(Names.NBT.water)));
    }

    public static int getCapacityOf(ItemStack stack) {
        return stack.getItem() instanceof ItemWateringCan can ? can.capacity : Constants.WATERING_CAN_MAX_WATER;
    }

    public static int getWaterPerUse(ItemStack stack) {
        return stack.getItem() instanceof ItemWateringCan can ? can.waterPerUse : Constants.WATERING_CAN_WATER_PER_USE;
    }

    public static boolean isInfinite(ItemStack stack) {
        return stack.getItem() instanceof ItemWateringCan can && can.infinite;
    }

    /** @return 水壶能浇水时返回 true，无限水壶无需补充。 */
    public static boolean hasWater(ItemStack stack) {
        return isInfinite(stack) || getWater(stack) > 0;
    }

    public static void setWater(ItemStack stack, int water) {
        if (isInfinite(stack)) return;
        stack.getOrCreateTag().putInt(Names.NBT.water, Math.max(0, Math.min(getCapacityOf(stack), water)));
    }

    // ---- 浇水 ----

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (fluidHit.getType() == HitResult.Type.BLOCK && isWaterSource(level, fluidHit.getBlockPos())) {
            InteractionResult result = refill(level, fluidHit.getBlockPos(), stack);
            return result.consumesAction() ? InteractionResultHolder.success(stack) :
                    InteractionResultHolder.pass(stack);
        }
        if (!hasWater(stack)) {
            sendEmptyMessage(level, player);
            // 空罐不会浇水，因此也不应播放使用动画
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        // 方块射线只命中水面以下的方块，因此改为追踪流体本身
        BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (fluidHit.getType() == HitResult.Type.BLOCK && isWaterSource(level, fluidHit.getBlockPos())) {
            return refill(level, fluidHit.getBlockPos(), stack);
        }
        if (isWaterSource(level, pos)) {
            return refill(level, pos, stack);
        }
        // 水壶作用于玩家周围的作物架，而不是被点击的方块
        if (!hasWater(stack)) {
            sendEmptyMessage(level, player);
            return InteractionResult.CONSUME;
        }
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    /**
     * 作物架会先由方块处理再轮到物品，因此必须在此处开启使用流程，否则作物会被收获而不是浇水。
     */
    @Override
    public boolean onRightClick(ICropStickTile tile, Player player, ItemStack heldItem) {
        // 始终消耗，使玩家手持洒水壶时作物不会被收获
        if (!hasWater(heldItem)) {
            sendEmptyMessage(player.level(), player);
            return true;
        }
        player.startUsingItem(InteractionHand.MAIN_HAND);
        return true;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!(living instanceof Player player)) return;
        if (!hasWater(stack)) {
            // 罐子一旦见底就停止，不再产生粒子或减速
            player.stopUsingItem();
            return;
        }
        int used = getUseDuration(stack) - remainingUseDuration;
        // 水量由服务端持有，因此粒子也由服务端生成
        if (used % 2 == 0) spawnParticles(level, player);
        if (level.isClientSide) return;
        if (used % WATERING_INTERVAL != 0) return;
        waterArea(level, player, stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // 使用期间物品保持不动，不做挥手动画
        return UseAnim.NONE;
    }

    /**
     * 已存水量在每次浇水刻都会写入物品，而默认实现会把每次标签变化当成物品替换，反复播放装备动画。
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    /**
     * 浇灌瞄准方块周围区域内的每个作物架，每个作物消耗一单位水量。范围内没有作物架时仍会消耗一次区域水量，相当于把水倒在地面。
     */
    private void waterArea(Level level, Player player, ItemStack stack) {
        boolean infinite = isInfinite(stack);
        int waterPerUse = getWaterPerUse(stack);
        int potency = CropsGTMConfig.scalePotency(waterPerUse, CropsGTMConfig.waterPotencyMultiplier);
        int drainPerUse = CropsGTMConfig.scaleConsumption(waterPerUse, CropsGTMConfig.waterConsumptionMultiplier);
        int water = getWater(stack);
        int watered = 0;
        BlockPos center = getAimPos(level, player);
        for (BlockPos target : BlockPos.betweenClosed(center.offset(-this.radius, 0, -this.radius),
                center.offset(this.radius, 0, this.radius))) {
            if (!infinite && water < drainPerUse) break;
            if (!(level.getBlockEntity(target) instanceof ICropStickTile tile)) continue;
            if (tile.getWaterStorage() >= Constants.MAX_WATER_STORAGE) continue;
            if (!tile.addWater(potency, Constants.MAX_WATER_STORAGE - 1, Constants.MAX_WATER_STORAGE, false)) {
                continue;
            }
            if (!infinite) water -= drainPerUse;
            watered++;
        }
        if (watered > 0) {
            if (!infinite) {
                setWater(stack, water);
                // 客户端物品在使用期间不可靠，因此在此停止
                if (water <= 0) player.stopUsingItem();
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, center, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.3f, 1.0f);
            }
        } else if (!infinite) {
            int remaining = Math.max(0, water - Constants.WATERING_CAN_IDLE_DRAIN);
            setWater(stack, remaining);
            if (remaining <= 0) player.stopUsingItem();
        }
    }

    /**
     * 玩家当前瞄准的方块，准星所指即为浇水的目标。
     */
    private static BlockPos getAimPos(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : BlockPos.containing(hit.getLocation());
    }

    private void spawnParticles(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        Vec3 target = hit.getLocation();
        double spread = this.radius * 2 + 1;
        serverLevel.sendParticles(ParticleTypes.SPLASH, target.x, target.y + 0.05d, target.z, 4, spread * 0.5d, 0.0d,
                spread * 0.5d, 0.0d);
    }

    private static boolean isWaterSource(Level level, BlockPos pos) {
        return level.getBlockState(pos).getFluidState().is(FluidTags.WATER);
    }

    private static InteractionResult refill(Level level, BlockPos pos, ItemStack stack) {
        if (isInfinite(stack) || getWater(stack) >= getCapacityOf(stack)) return InteractionResult.SUCCESS;
        if (!level.isClientSide) {
            setWater(stack, getCapacityOf(stack));
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void sendEmptyMessage(Level level, Player player) {
        if (!level.isClientSide && player != null) {
            player.displayClientMessage(Component.translatable(Names.MOD_ID + "_tooltip.wateringCan.empty"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isInfinite(stack)) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.wateringCan.infinite")
                    .withStyle(ChatFormatting.BLUE));
        } else {
            tooltip.add(Component
                    .translatable(Names.MOD_ID + "_tooltip.wateringCan.water", getWater(stack), getCapacityOf(stack))
                    .withStyle(ChatFormatting.BLUE));
        }
        int speedBonus = this.getSpeedBonusPercent();
        if (speedBonus > 0) {
            tooltip.add(Component
                    .translatable(Names.MOD_ID + "_tooltip.wateringCan.speed",
                            Component.literal("+" + speedBonus + "%").withStyle(ChatFormatting.GOLD))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.wateringCan.area", this.radius * 2 + 1)
                .withStyle(ChatFormatting.GRAY));
    }

    /** @return 该水壶相对基础水壶的浇水速度加成，单位为百分比。 */
    private int getSpeedBonusPercent() {
        return (this.waterPerUse - Constants.WATERING_CAN_WATER_PER_USE) * 100 / Constants.WATERING_CAN_WATER_PER_USE;
    }
}
