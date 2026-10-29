package com.synkra.cropsgtm.fallback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.CropManagerCycle;
import com.synkra.cropsgtm.farming.CropManagerSettings;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.items.ItemSprayCan;
import com.synkra.cropsgtm.items.ItemWeedEx;
import com.synkra.cropsgtm.reference.Constants;

import javax.annotation.Nullable;

import static com.synkra.cropsgtm.farming.CropManagerCycle.FERTILIZER_CAP;
import static com.synkra.cropsgtm.farming.CropManagerCycle.FERTILIZER_THRESHOLD;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WATER_CAP;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WATER_THRESHOLD;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WEEDEX_CAP;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WEEDEX_COST;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WEEDEX_THRESHOLD;

/**
 * 备用作物管理器的方块实体。
 * <p>
 * 它使用 Forge 能量运行，并把水、肥料与除草剂保存在内部储罐中。储罐刻意不暴露为流体能力：界面是唯一接口，输入槽把物品转换为储罐内容，并把空容器放回输出槽。
 * </p>
 * <p>
 * 工作周期、范围与效力阈值与 GregTech 作物管理器一致，因此两个版本行为相同，只有能量来源与存储表示不同。
 * </p>
 */
public class BlockEntityCropManagerFE extends BlockEntity implements MenuProvider, IEnergyStorage, CropManagerSettings,
                                      CropManagerCycle.Host {

    public static final int INPUT_SLOTS = 6;
    /** 用于回收输入物品空容器的专用输出槽：一个放桶、一个放瓶。 */
    public static final int CONTAINER_SLOTS = 2;
    public static final int OUTPUT_SLOTS = 15;

    public static final int ENERGY_CAPACITY = 300_000;
    public static final int MAX_ENERGY_RECEIVE = 1024;
    public static final int ENERGY_PER_REDSTONE = 3200;
    /** 单次收获的费用，是 MV 级 GregTech 作物管理器 16 EU 的四倍。 */
    public static final int HARVEST_COST = 64;
    /** 单次浇水、施肥或施用除草剂的费用，是 MV 级管理器 4 EU 的四倍。 */
    public static final int SECONDARY_COST = 16;

    public static final int HORIZONTAL_RADIUS = 7;
    public static final int VERTICAL_RADIUS = 1;

    public static final int WATER_CAPACITY = 16_000;
    public static final int FERTILIZER_CAPACITY = 16_000;
    public static final int WEED_EX_CAPACITY = 16_000;

    /** 对应储罐流体的每 mB 效力值。 */
    public static final int WATER_POTENCY_PER_MB = 1;
    public static final int FERTILIZER_POTENCY_PER_MB = 1;
    public static final int WEED_EX_POTENCY_PER_MB = Constants.WEEDEX_POTENCY_PER_MB;

    private static final int WATER_PER_BUCKET = 1000;
    private static final int WATER_PER_BOTTLE = 250;
    /** 未安装 GregTech 时，一个骨粉价值 25 点肥料。 */
    private static final int FERTILIZER_PER_BONE_MEAL = 25;
    private static final int WEED_EX_PER_POTION = 250;
    /** 除草剂罐的一次用量等同于它原本会被灌入的流体量。 */
    private static final int MB_PER_WEED_EX_USE = Constants.WEEDEX_POTENCY_PER_USE / WEED_EX_POTENCY_PER_MB;
    private static final int AUTO_OUTPUT_INTERVAL = 10;

    private static final int CYCLE_INTERVAL = 50;
    private static final double HARVEST_BONUS = 0.10d;

    private final ItemStackHandler input = new InputHandler();
    private final ItemStackHandler containerOutput = new ContainerOutputHandler();
    private final ItemStackHandler output = new OutputHandler();
    private final ItemStackHandler energySlot = new EnergySlotHandler();

    private final CropManagerCycle cycle = new CropManagerCycle(this);

    private int waterMb;
    private int fertilizerMb;
    private int weedExMb;
    private int energyStored;
    private int tickCounter;
    private boolean autoOutput;
    private boolean workingEnabled = true;
    private boolean harvestEnabled = true;
    private boolean waterEnabled = true;
    private boolean weedExEnabled = true;
    private boolean fertilizerEnabled = true;

    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> this);

    /** 把内部水箱暴露为仅输出的流体处理器。 */
    private final IFluidHandler waterHandler = new WaterFluidHandler();
    private LazyOptional<IFluidHandler> waterCapability = LazyOptional.of(() -> this.waterHandler);

    public BlockEntityCropManagerFE(BlockPos pos, BlockState state) {
        super(FallbackContent.cropManagerTE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityCropManagerFE machine) {
        if (level.isClientSide) return;
        machine.serverTick();
    }

    private void serverTick() {
        boolean changed = chargeFromSlot();
        changed |= convertInputs();
        if (this.autoOutput && this.tickCounter % AUTO_OUTPUT_INTERVAL == 0) {
            changed |= pushOutputs();
        }
        if (++this.tickCounter >= CYCLE_INTERVAL) {
            this.tickCounter = 0;
            changed |= runCycle();
        }
        if (changed) setChanged();
    }

    private boolean pushOutputs() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return false;
        BlockPos pos = getBlockPos();
        boolean changed = false;
        for (Direction side : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
            if (neighbor == null) continue;
            IItemHandler target = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite())
                    .orElse(null);
            if (target == null) continue;
            changed |= pushItems(this.output, target);
            changed |= pushItems(this.containerOutput, target);
        }
        return changed;
    }

    private static boolean pushItems(ItemStackHandler source, IItemHandler target) {
        boolean changed = false;
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
            if (remainder.getCount() == stack.getCount()) continue;
            source.setStackInSlot(slot, remainder.isEmpty() ? ItemStack.EMPTY : remainder);
            changed = true;
        }
        return changed;
    }

    // ---- 能量 ----

    /**
     * 把电池槽的能量转移到内部缓存。红石一次消耗一个，每个价值 {@link #ENERGY_PER_REDSTONE} FE。
     */
    private boolean chargeFromSlot() {
        ItemStack stack = this.energySlot.getStackInSlot(0);
        if (stack.isEmpty() || this.energyStored >= ENERGY_CAPACITY) return false;
        if (stack.is(Items.REDSTONE)) {
            if (ENERGY_CAPACITY - this.energyStored < ENERGY_PER_REDSTONE) return false;
            stack.shrink(1);
            this.energyStored += ENERGY_PER_REDSTONE;
            return true;
        }
        int[] drained = { 0 };
        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> drained[0] = storage
                .extractEnergy(Math.min(MAX_ENERGY_RECEIVE, ENERGY_CAPACITY - this.energyStored), false));
        if (drained[0] <= 0) return false;
        this.energyStored += drained[0];
        return true;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int accepted = Math.min(Math.min(maxReceive, MAX_ENERGY_RECEIVE), ENERGY_CAPACITY - this.energyStored);
        if (accepted <= 0) return 0;
        if (!simulate) {
            this.energyStored += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return this.energyStored;
    }

    @Override
    public int getMaxEnergyStored() {
        return ENERGY_CAPACITY;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    // ---- 物品转换 ----

    /** @return 该物品能转换为内部储罐之一时返回 true。 */
    public static boolean isConvertible(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.WATER_BUCKET) || stack.is(Items.BONE_MEAL)) return true;
        if (stack.is(CropsGTMItems.poisonPowder.get())) return true;
        if (stack.getItem() instanceof ItemWeedEx) return true;
        if (stack.is(Items.POTION)) {
            Potion potion = PotionUtils.getPotion(stack);
            return potion == Potions.WATER || isPoison(potion);
        }
        return false;
    }

    /** @return 该物品能放入电池槽时返回 true。 */
    public static boolean isEnergyItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.REDSTONE) || stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
    }

    private static boolean isPoison(Potion potion) {
        return potion == Potions.POISON || potion == Potions.LONG_POISON || potion == Potions.STRONG_POISON;
    }

    private @Nullable Conversion getConversion(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            return new Conversion(WATER_PER_BUCKET, 0, 0, new ItemStack(Items.BUCKET));
        }
        if (stack.is(Items.BONE_MEAL)) {
            return new Conversion(0, FERTILIZER_PER_BONE_MEAL, 0, ItemStack.EMPTY);
        }
        if (stack.is(CropsGTMItems.poisonPowder.get())) {
            return new Conversion(0, 0, WEED_EX_PER_POTION, ItemStack.EMPTY);
        }
        if (stack.is(Items.POTION)) {
            Potion potion = PotionUtils.getPotion(stack);
            if (potion == Potions.WATER) {
                return new Conversion(WATER_PER_BOTTLE, 0, 0, new ItemStack(Items.GLASS_BOTTLE));
            }
            if (isPoison(potion)) {
                return new Conversion(0, 0, WEED_EX_PER_POTION, new ItemStack(Items.GLASS_BOTTLE));
            }
            return null;
        }
        return null;
    }

    private boolean convertInputs() {
        boolean changed = false;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = this.input.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof ItemWeedEx) {
                changed |= convertWeedExCan(slot, stack);
                continue;
            }
            Conversion conversion = getConversion(stack);
            if (conversion == null) continue;
            if (!hasTankSpace(conversion)) continue;
            if (!conversion.container().isEmpty() && !canStoreContainer(conversion.container())) {
                continue;
            }
            stack.shrink(1);
            this.waterMb += conversion.water();
            this.fertilizerMb += conversion.fertilizer();
            this.weedExMb += conversion.weedEx();
            if (!conversion.container().isEmpty()) {
                insertContainer(conversion.container());
            }
            this.input.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            changed = true;
        }
        return changed;
    }

    /**
     * 从除草剂罐中抽一次用量注回储罐。最后一次完全排空后把空桶放回容器输出槽，避免罐子堵塞输入槽。
     */
    private boolean convertWeedExCan(int slot, ItemStack stack) {
        if (this.weedExMb + MB_PER_WEED_EX_USE > WEED_EX_CAPACITY) return false;
        int uses = ((ItemWeedEx) stack.getItem()).getUsesLeft(stack);
        if (uses <= 1) {
            ItemStack bucket = new ItemStack(Items.BUCKET);
            if (!canStoreContainer(bucket)) return false;
            this.weedExMb += MB_PER_WEED_EX_USE;
            insertContainer(bucket);
            this.input.setStackInSlot(slot, ItemStack.EMPTY);
            return true;
        }
        this.weedExMb += MB_PER_WEED_EX_USE;
        ItemStack copy = stack.copy();
        ItemSprayCan.setUsesLeft(copy, uses - 1);
        this.input.setStackInSlot(slot, copy);
        return true;
    }

    private boolean hasTankSpace(Conversion conversion) {
        return this.waterMb + conversion.water() <= WATER_CAPACITY &&
                this.fertilizerMb + conversion.fertilizer() <= FERTILIZER_CAPACITY &&
                this.weedExMb + conversion.weedEx() <= WEED_EX_CAPACITY;
    }

    private boolean canStoreContainer(ItemStack container) {
        for (int slot = 0; slot < CONTAINER_SLOTS; slot++) {
            ItemStack existing = this.containerOutput.getStackInSlot(slot);
            if (existing.isEmpty()) return true;
            if (ItemStack.isSameItemSameTags(existing, container) && existing.getCount() <
                    Math.min(existing.getMaxStackSize(), this.containerOutput.getSlotLimit(slot))) {
                return true;
            }
        }
        return false;
    }

    private void insertContainer(ItemStack container) {
        int amount = container.getCount();
        for (int slot = 0; slot < CONTAINER_SLOTS && amount > 0; slot++) {
            ItemStack existing = this.containerOutput.getStackInSlot(slot);
            if (existing.isEmpty()) {
                this.containerOutput.setStackInSlot(slot, container.copyWithCount(amount));
                amount = 0;
            } else if (ItemStack.isSameItemSameTags(existing, container)) {
                int space = Math.min(existing.getMaxStackSize(), this.containerOutput.getSlotLimit(slot)) -
                        existing.getCount();
                int toAdd = Math.min(space, amount);
                this.containerOutput.setStackInSlot(slot, existing.copyWithCount(existing.getCount() + toAdd));
                amount -= toAdd;
            }
        }
    }

    // ---- 工作周期 ----

    private boolean runCycle() {
        this.cycle.refreshCache();
        return this.cycle.run();
    }

    // ---- 工作周期 ----

    @Override
    public Level level() {
        return getLevel();
    }

    @Override
    public BlockPos pos() {
        return getBlockPos();
    }

    @Override
    public CropManagerSettings settings() {
        return this;
    }

    @Override
    public int horizontalRadius() {
        return HORIZONTAL_RADIUS;
    }

    @Override
    public int verticalRadius() {
        return VERTICAL_RADIUS;
    }

    @Override
    public boolean canRunCycle() {
        return this.energyStored >= HARVEST_COST;
    }

    @Override
    public boolean hasEnergy(long amount) {
        return this.energyStored >= amount;
    }

    @Override
    public void consumeEnergy(long amount) {
        this.energyStored -= (int) amount;
    }

    @Override
    public long harvestEnergyCost() {
        return HARVEST_COST;
    }

    @Override
    public long secondaryEnergyCost() {
        return SECONDARY_COST;
    }

    @Override
    public double harvestBonus() {
        return HARVEST_BONUS;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public boolean applyWater(ICropStickTile crop) {
        if (this.waterMb <= 0) return false;
        if (crop.getWaterStorage() > WATER_THRESHOLD) return false;
        int potency = CropsGTMConfig.scalePotency(WATER_POTENCY_PER_MB, CropsGTMConfig.waterPotencyMultiplier);
        int units = CropsGTMConfig.scaleConsumption(1, CropsGTMConfig.waterConsumptionMultiplier);
        int applications = CropManagerCycle.getApplications(this.waterMb, potency, units,
                WATER_CAP - crop.getWaterStorage());
        if (applications <= 0) return false;
        int amount = applications * potency;
        if (!crop.addWater(amount, WATER_THRESHOLD, WATER_CAP, true)) return false;
        if (!crop.addWater(amount, WATER_THRESHOLD, WATER_CAP, false)) return false;
        this.waterMb -= applications * units;
        return true;
    }

    @Override
    public boolean applyWeedEx(ICropStickTile crop) {
        if (this.weedExMb <= 0) return false;
        if (crop.getWeedExStorage() > WEEDEX_THRESHOLD) return false;
        int potency = CropsGTMConfig.scalePotency(WEED_EX_POTENCY_PER_MB, CropsGTMConfig.weedExPotencyMultiplier);
        int units = CropsGTMConfig.scaleConsumption(1, CropsGTMConfig.weedExConsumptionMultiplier);
        int applications = CropManagerCycle.getApplications(this.weedExMb, potency, units,
                WEEDEX_CAP - crop.getWeedExStorage());
        if (applications <= 0) return false;
        int amount = Math.min(applications * potency, WEEDEX_COST);
        if (!crop.addWeedEx(amount, WEEDEX_THRESHOLD, WEEDEX_CAP, true)) return false;
        if (!crop.addWeedEx(amount, WEEDEX_THRESHOLD, WEEDEX_CAP, false)) return false;
        this.weedExMb -= applications * units;
        return true;
    }

    @Override
    public boolean applyFertilizer(ICropStickTile crop) {
        if (this.fertilizerMb <= 0) return false;
        if (crop.getFertilizerStorage() > FERTILIZER_THRESHOLD) return false;
        int potency = CropsGTMConfig.scalePotency(FERTILIZER_POTENCY_PER_MB,
                CropsGTMConfig.fertilizerPotencyMultiplier);
        int units = CropsGTMConfig.scaleConsumption(1, CropsGTMConfig.fertilizerConsumptionMultiplier);
        int applications = CropManagerCycle.getApplications(this.fertilizerMb, potency, units,
                FERTILIZER_CAP - crop.getFertilizerStorage());
        if (applications <= 0) return false;
        int amount = applications * potency;
        if (!crop.addFertilizer(amount, FERTILIZER_THRESHOLD, FERTILIZER_CAP, true)) return false;
        if (!crop.addFertilizer(amount, FERTILIZER_THRESHOLD, FERTILIZER_CAP, false)) return false;
        this.fertilizerMb -= applications * units;
        return true;
    }

    @Override
    public boolean hasOutputSpace() {
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack stack = this.output.getStackInSlot(slot);
            if (stack.isEmpty()) return true;
            if (stack.getCount() < Math.min(stack.getMaxStackSize(), this.output.getSlotLimit(slot))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 没能放进输出槽的物品数量。
     */
    @Override
    public int insertOutput(ItemStack stack) {
        int amount = stack.getCount();
        for (int slot = 0; slot < OUTPUT_SLOTS && amount > 0; slot++) {
            ItemStack existing = this.output.getStackInSlot(slot);
            int limit = Math.min(stack.getMaxStackSize(), this.output.getSlotLimit(slot));
            if (existing.isEmpty()) {
                int toAdd = Math.min(limit, amount);
                this.output.setStackInSlot(slot, stack.copyWithCount(toAdd));
                amount -= toAdd;
            } else if (ItemStack.isSameItemSameTags(existing, stack)) {
                int space = limit - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, amount);
                    this.output.setStackInSlot(slot, existing.copyWithCount(existing.getCount() + toAdd));
                    amount -= toAdd;
                }
            }
        }
        return amount;
    }

    // ---- 内部维护 ----

    public ItemStackHandler getInput() {
        return this.input;
    }

    public ItemStackHandler getContainerOutput() {
        return this.containerOutput;
    }

    public ItemStackHandler getOutput() {
        return this.output;
    }

    public ItemStackHandler getEnergySlot() {
        return this.energySlot;
    }

    public int getWaterMb() {
        return this.waterMb;
    }

    public int getFertilizerMb() {
        return this.fertilizerMb;
    }

    public int getWeedExMb() {
        return this.weedExMb;
    }

    public boolean isAutoOutput() {
        return this.autoOutput;
    }

    public void toggleAutoOutput() {
        this.autoOutput = !this.autoOutput;
        setChanged();
    }

    @Override
    public boolean isWorkingEnabled() {
        return this.workingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean value) {
        this.workingEnabled = value;
    }

    @Override
    public boolean isHarvestEnabled() {
        return this.harvestEnabled;
    }

    @Override
    public void setHarvestEnabled(boolean value) {
        this.harvestEnabled = value;
    }

    @Override
    public boolean isWaterEnabled() {
        return this.waterEnabled;
    }

    @Override
    public void setWaterEnabled(boolean value) {
        this.waterEnabled = value;
    }

    @Override
    public boolean isWeedExEnabled() {
        return this.weedExEnabled;
    }

    @Override
    public void setWeedExEnabled(boolean value) {
        this.weedExEnabled = value;
    }

    @Override
    public boolean isFertilizerEnabled() {
        return this.fertilizerEnabled;
    }

    @Override
    public void setFertilizerEnabled(boolean value) {
        this.fertilizerEnabled = value;
    }

    @Override
    public boolean isAutoOutputEnabled() {
        return this.autoOutput;
    }

    @Override
    public void setAutoOutputEnabled(boolean value) {
        this.autoOutput = value;
    }

    /** 切换机器的某项功能，id 即容器界面的按钮 id。 */
    @Override
    public void toggleFeature(int id) {
        CropManagerSettings.super.toggleFeature(id);
        setChanged();
    }

    public void dropContents() {
        Level level = getLevel();
        if (level == null) return;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            dropStack(level, this.input.getStackInSlot(slot));
            this.input.setStackInSlot(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < CONTAINER_SLOTS; slot++) {
            dropStack(level, this.containerOutput.getStackInSlot(slot));
            this.containerOutput.setStackInSlot(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            dropStack(level, this.output.getStackInSlot(slot));
            this.output.setStackInSlot(slot, ItemStack.EMPTY);
        }
        dropStack(level, this.energySlot.getStackInSlot(0));
        this.energySlot.setStackInSlot(0, ItemStack.EMPTY);
    }

    private void dropStack(Level level, ItemStack stack) {
        if (!stack.isEmpty()) {
            Block.popResource(level, getBlockPos(), stack.copy());
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cropsgtm.crop_manager_fe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
        return new CropManagerFEMenu(windowId, inventory, this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            return this.energyCapability.cast();
        }
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            return this.waterCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.energyCapability.invalidate();
        this.waterCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.energyCapability = LazyOptional.of(() -> this);
        this.waterCapability = LazyOptional.of(() -> this.waterHandler);
    }

    private class WaterFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return BlockEntityCropManagerFE.this.waterMb <= 0 ? FluidStack.EMPTY :
                    new FluidStack(Fluids.WATER, BlockEntityCropManagerFE.this.waterMb);
        }

        @Override
        public int getTankCapacity(int tank) {
            return WATER_CAPACITY;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return !stack.isEmpty() && stack.getFluid().isSame(Fluids.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource)) return 0;
            int amount = Math.min(resource.getAmount(), WATER_CAPACITY - BlockEntityCropManagerFE.this.waterMb);
            if (amount <= 0) return 0;
            if (action.execute()) {
                BlockEntityCropManagerFE.this.waterMb += amount;
                setChanged();
            }
            return amount;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.getFluid().isSame(Fluids.WATER)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            int amount = Math.min(maxDrain, BlockEntityCropManagerFE.this.waterMb);
            if (amount <= 0) return FluidStack.EMPTY;
            if (action.execute()) {
                BlockEntityCropManagerFE.this.waterMb -= amount;
                setChanged();
            }
            return new FluidStack(Fluids.WATER, amount);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Water", this.waterMb);
        tag.putInt("Fertilizer", this.fertilizerMb);
        tag.putInt("WeedEx", this.weedExMb);
        tag.putInt("Energy", this.energyStored);
        tag.putBoolean(NBT_AUTO_OUTPUT, this.autoOutput);
        tag.putBoolean(NBT_WORKING, this.workingEnabled);
        tag.putBoolean(NBT_HARVEST, this.harvestEnabled);
        tag.putBoolean(NBT_WATER, this.waterEnabled);
        tag.putBoolean(NBT_WEED_EX, this.weedExEnabled);
        tag.putBoolean(NBT_FERTILIZER, this.fertilizerEnabled);
        tag.put("Input", this.input.serializeNBT());
        tag.put("ContainerOutput", this.containerOutput.serializeNBT());
        tag.put("Output", this.output.serializeNBT());
        tag.put("EnergySlot", this.energySlot.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.waterMb = tag.getInt("Water");
        this.fertilizerMb = tag.getInt("Fertilizer");
        this.weedExMb = tag.getInt("WeedEx");
        this.energyStored = tag.getInt("Energy");
        this.autoOutput = tag.getBoolean(NBT_AUTO_OUTPUT);
        this.workingEnabled = !tag.contains(NBT_WORKING) || tag.getBoolean(NBT_WORKING);
        this.harvestEnabled = !tag.contains(NBT_HARVEST) || tag.getBoolean(NBT_HARVEST);
        this.waterEnabled = !tag.contains(NBT_WATER) || tag.getBoolean(NBT_WATER);
        this.weedExEnabled = !tag.contains(NBT_WEED_EX) || tag.getBoolean(NBT_WEED_EX);
        this.fertilizerEnabled = !tag.contains(NBT_FERTILIZER) || tag.getBoolean(NBT_FERTILIZER);
        this.input.deserializeNBT(tag.getCompound("Input"));
        this.containerOutput.deserializeNBT(tag.getCompound("ContainerOutput"));
        this.output.deserializeNBT(tag.getCompound("Output"));
        this.energySlot.deserializeNBT(tag.getCompound("EnergySlot"));
    }

    private record Conversion(int water, int fertilizer, int weedEx, ItemStack container) {}

    private class InputHandler extends ItemStackHandler {

        InputHandler() {
            super(INPUT_SLOTS);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isConvertible(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }

    private class ContainerOutputHandler extends ItemStackHandler {

        ContainerOutputHandler() {
            super(CONTAINER_SLOTS);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }

    private class OutputHandler extends ItemStackHandler {

        OutputHandler() {
            super(OUTPUT_SLOTS);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }

    private class EnergySlotHandler extends ItemStackHandler {

        EnergySlotHandler() {
            super(1);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isEnergyItem(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    }
}
