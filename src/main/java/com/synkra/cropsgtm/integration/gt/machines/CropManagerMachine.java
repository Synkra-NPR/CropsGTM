package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputItem;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.registries.ForgeRegistries;

import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.CropManagerCycle;
import com.synkra.cropsgtm.farming.CropManagerSettings;
import com.synkra.cropsgtm.farming.PotencyData;
import com.synkra.cropsgtm.farming.registries.FertilizerRegistry;
import com.synkra.cropsgtm.farming.registries.HydrationRegistry;
import com.synkra.cropsgtm.farming.registries.WeedEXRegistry;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMaterials;
import com.synkra.cropsgtm.items.ItemSprayCan;
import com.synkra.cropsgtm.items.ItemWeedEx;
import com.synkra.cropsgtm.reference.Constants;
import org.jetbrains.annotations.Nullable;

import static com.synkra.cropsgtm.farming.CropManagerCycle.FERTILIZER_CAP;
import static com.synkra.cropsgtm.farming.CropManagerCycle.FERTILIZER_THRESHOLD;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WATER_CAP;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WATER_THRESHOLD;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WEEDEX_CAP;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WEEDEX_COST;
import static com.synkra.cropsgtm.farming.CropManagerCycle.WEEDEX_THRESHOLD;

/**
 * 电力作物管理器：收获并维护自身周围一定范围内的所有作物架。
 * <p>
 * 移植自原 CropNH 作物管理器。工作周期、扫描范围与效力数值保持一致。水、Weed-EX 与液态肥料存放在三个流体储罐中，而原机器使用效力计数；机器沿用 GT 标准的电池槽与自动输出配置。
 * </p>
 */
public class CropManagerMachine extends TieredEnergyMachine
                                implements IFancyUIMachine, IMachineLife, IControllable, IAutoOutputItem,
                                CropManagerSettings, CropManagerCycle.Host, BatteryAutoOutputSupport.Host {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CropManagerMachine.class,
            TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    public static final int WEEDEX_SLOT_COUNT = 2;
    public static final int FERTILIZER_SLOT_COUNT = 4;
    public static final int OUTPUT_SLOT_COUNT = 15;

    /** 两个工作周期之间的刻数。 */
    private static final int GLOBAL_UPDATE_RATE = 2 * 20 + 10;
    /** 未发现作物时使用的缓存刷新间隔。 */
    private static final int CACHE_REFRESH_EMPTY = GLOBAL_UPDATE_RATE * 2;
    /** 已发现作物时使用的缓存刷新间隔。 */
    private static final int CACHE_REFRESH_ANY = GLOBAL_UPDATE_RATE * 12;

    @Persisted
    @DescSynced
    private boolean workingEnabled = true;
    @Persisted
    @DescSynced
    private boolean harvestEnabled = true;
    @Persisted
    @DescSynced
    private boolean weedExEnabled = false;
    @Persisted
    @DescSynced
    private boolean waterEnabled = false;
    @Persisted
    @DescSynced
    private boolean fertilizerEnabled = false;

    /** 同步到客户端的储罐液位，使指示条保持同步。 */
    @DescSynced
    private int syncedWater = 0;
    @DescSynced
    private int syncedWeedEx = 0;
    @DescSynced
    private int syncedFertilizer = 0;

    @Persisted
    public final NotifiableItemStackHandler weedExInput;
    @Persisted
    public final NotifiableItemStackHandler fertilizerInput;
    @Persisted
    public final NotifiableItemStackHandler output;
    @Persisted
    public final NotifiableFluidTank waterTank;
    @Persisted
    public final NotifiableFluidTank weedExTank;
    /** 只装普通肥料，富集肥料会被转换成它。 */
    @Persisted
    public final NotifiableFluidTank fertilizerTank;

    @Persisted
    public final CustomItemStackHandler chargerInventory;

    @Persisted
    @DescSynced
    @RequireRerender
    private Direction outputFacingItems;
    @Persisted
    @DescSynced
    @RequireRerender
    private boolean autoOutputItems = false;
    @Persisted
    @DescSynced
    @RequireRerender
    private boolean allowInputFromOutputSideItems = false;

    @Nullable
    private TickableSubscription syncSubs;
    /** 电池与自动输出行为，与其他作物机器共用。 */
    private final BatteryAutoOutputSupport batteryAutoOutput = new BatteryAutoOutputSupport(this);

    /** 工作区域内的全部作物架，与备用作物管理器共用。 */
    private final CropManagerCycle cycle = new CropManagerCycle(this);

    public CropManagerMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier);
        this.outputFacingItems = getFrontFacing().getOpposite();

        // trait 在创建时会自行注册到机器，再次 attach 会
        // 会使每个处理器在机器 trait 中出现两次，因而在流体与
        // 物品能力，这会使配方查看器显示的内容翻倍
        this.weedExInput = new NotifiableItemStackHandler(this, WEEDEX_SLOT_COUNT, IO.IN)
                .setFilter(CropManagerMachine::isWeedExCan);
        this.fertilizerInput = new NotifiableItemStackHandler(this, FERTILIZER_SLOT_COUNT, IO.IN)
                .setFilter(CropManagerMachine::isFertilizerItem);
        this.output = new NotifiableItemStackHandler(this, OUTPUT_SLOT_COUNT, IO.BOTH, IO.OUT);

        this.waterTank = new NotifiableFluidTank(this, 1, getWaterCapacity(tier), IO.IN)
                .setFilter(CropManagerMachine::isWaterFluid);
        this.weedExTank = new NotifiableFluidTank(this, 1, getWeedExCapacity(tier), IO.IN)
                .setFilter(CropManagerMachine::isWeedExFluid);
        this.fertilizerTank = new FertilizerTank(tier);

        // 充电槽只接受单个电池，与 GregTech 机器保持一致
        this.chargerInventory = new CustomItemStackHandler(1) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        this.chargerInventory.setFilter(
                item -> com.gregtechceu.gtceu.api.capability.GTCapabilityHelper.getElectricItem(item) != null);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        return new NotifiableEnergyContainer(this, getEnergyCapacity(getTier()), GTValues.V[getTier()], 1L, 0L, 0L);
    }

    // ---- 参数 ----

    public static int getHorizontalRadius(int tier) {
        return 3 + Math.max(0, 2 * tier);
    }

    public int getHorizontalDiameter() {
        return getHorizontalRadius(getTier()) * 2 + 1;
    }

    public static int getVerticalRadius() {
        return 2;
    }

    public int getVerticalDiameter() {
        return getVerticalRadius() * 2 + 1;
    }

    public static long getEnergyCapacity(int tier) {
        return Math.max(GTValues.V[tier] * 64L, GTValues.V[tier] * (tier * GTValues.V[tier]));
    }

    public static int getWaterCapacity(int tier) {
        return tier * 32000;
    }

    public static int getWeedExCapacity(int tier) {
        return tier * Constants.WEEDEX_CAPACITY * 2;
    }

    public static int getFertilizerCapacity(int tier) {
        return tier * Constants.FERTILIZER_ITEM_FLUID_VALUE * 64 * 4;
    }

    private long powerUsage() {
        return GTValues.V[getTier()] / 8L;
    }

    private long powerUsageSecondary() {
        return GTValues.V[getTier()] / 32L;
    }

    private double getHarvestBonusChance() {
        return 0.05d * getTier();
    }

    public static boolean isWaterFluid(FluidStack stack) {
        return HydrationRegistry.instance.getPotency(stack) != null;
    }

    public static boolean isWeedExFluid(FluidStack stack) {
        return WeedEXRegistry.instance.getPotency(stack) != null;
    }

    public static boolean isFertilizerFluid(FluidStack stack) {
        return FertilizerRegistry.instance.getPotency(stack) != null;
    }

    public static boolean isWeedExCan(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemWeedEx;
    }

    public static boolean isFertilizerItem(ItemStack stack) {
        return !stack.isEmpty() && FertilizerRegistry.instance.isRegistered(stack);
    }

    /**
     * 按注册名比较两种流体：GregTech 为某材料返回的流体实例可能与流体注册表中的实例不同。
     */
    private static boolean isSameFluid(Fluid first, Fluid second) {
        if (first == null || second == null) return false;
        if (first == second) return true;
        return ForgeRegistries.FLUIDS.getKey(first) != null &&
                ForgeRegistries.FLUIDS.getKey(first).equals(ForgeRegistries.FLUIDS.getKey(second));
    }

    /**
     * 作物管理器的液态肥料储罐。
     * <p>
     * 与原机器一样，它始终使用普通液态肥料：富集肥料在输入时按等量肥料点转换，因此储罐、储罐指示条与配方查看器都不会出现第二种肥料流体。储罐仅作输入，能力与储罐本身都不会回吐任何流体。
     * </p>
     */
    private class FertilizerTank extends NotifiableFluidTank {

        FertilizerTank(int tier) {
            super(CropManagerMachine.this, 1, getFertilizerCapacity(tier), IO.IN);
            setFilter(CropManagerMachine::isFertilizerFluid);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canCapInput() || resource.isEmpty()) return 0;
            Fluid regularFertilizer = CropsGTMMaterials.liquidFertilizer.getFluid();
            if (!isSameFluid(resource.getFluid(), CropsGTMMaterials.enrichedFertilizer.getFluid())) {
                return super.fill(resource, action);
            }

            PotencyData regular = FertilizerRegistry.instance.getPotency(new FluidStack(regularFertilizer, 1));
            PotencyData enriched = FertilizerRegistry.instance.getPotency(resource);
            if (regular == null || enriched == null || regular.getPotency() <= 0) return 0;

            // 一 mB 富集肥料按其效力换算成等量的普通液态肥料
            int ratio = Math.max(1, enriched.getPotency() / regular.getPotency());
            int space = getTankCapacity(0) - getFluidInTank(0).getAmount();
            int accepted = Math.min(resource.getAmount(), space / ratio);
            if (accepted <= 0) return 0;
            int filled = fillInternal(new FluidStack(regularFertilizer, accepted * ratio), action);
            return filled / ratio;
        }
    }

    // ---- 生命周期 ----

    @Override
    public void onLoad() {
        super.onLoad();
        if (isRemote()) return;
        subscribeServerTick(this::onServerTick);
        subscribeServerTick(this::syncTanks);
        this.batteryAutoOutput.onLoad();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        this.batteryAutoOutput.onUnload();
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(this.weedExInput.storage);
        clearInventory(this.fertilizerInput.storage);
        clearInventory(this.output.storage);
        clearInventory(this.chargerInventory);
    }

    private void syncTanks() {
        if (getOffsetTimer() % 10 != 0) return;
        this.syncedWater = this.waterTank.getFluidInTank(0).getAmount();
        this.syncedWeedEx = this.weedExTank.getFluidInTank(0).getAmount();
        this.syncedFertilizer = this.fertilizerTank.getFluidInTank(0).getAmount();
    }

    private void onServerTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        this.batteryAutoOutput.updateBatterySubscription();
        if (!this.workingEnabled) return;
        if (getOffsetTimer() % GLOBAL_UPDATE_RATE != 0) return;
        if (this.energyContainer.getEnergyStored() < GTValues.V[getTier()]) return;

        int refreshRate = this.cycle.isEmpty() ? CACHE_REFRESH_EMPTY : CACHE_REFRESH_ANY;
        if (this.cycle.isCacheInvalid() || getOffsetTimer() % refreshRate == 0) {
            this.cycle.refreshCache();
        }
        this.cycle.run();
        this.batteryAutoOutput.updateAutoOutputSubscription();
    }

    // ---- 工作周期 ----

    @Override
    public Level level() {
        return getLevel();
    }

    @Override
    public BlockPos pos() {
        return getPos();
    }

    @Override
    public CropManagerSettings settings() {
        return this;
    }

    @Override
    public int horizontalRadius() {
        return getHorizontalRadius(getTier());
    }

    @Override
    public int verticalRadius() {
        return getVerticalRadius();
    }

    @Override
    public boolean canRunCycle() {
        return this.energyContainer.getEnergyStored() >= GTValues.V[getTier()];
    }

    @Override
    public boolean hasEnergy(long amount) {
        return this.energyContainer.getEnergyStored() >= amount;
    }

    @Override
    public void consumeEnergy(long amount) {
        this.energyContainer.removeEnergy(amount);
    }

    @Override
    public long harvestEnergyCost() {
        return powerUsage();
    }

    @Override
    public long secondaryEnergyCost() {
        return powerUsageSecondary();
    }

    @Override
    public double harvestBonus() {
        return getHarvestBonusChance();
    }

    @Override
    public void beforeWeedEx() {
        refillWeedEx();
    }

    @Override
    public boolean hasOutputSpace() {
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT; slot++) {
            ItemStack stack = this.output.getStackInSlot(slot);
            if (stack.isEmpty()) return true;
            if (stack.getCount() < Math.min(stack.getMaxStackSize(), this.output.getSlotLimit(slot))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把给定物品合并进输出库存，绕过能力过滤器。
     * 
     * @return 无法插入的物品数量。
     */
    @Override
    public int insertOutput(ItemStack stack) {
        int amount = stack.getCount();
        for (int slot = 0; slot < OUTPUT_SLOT_COUNT && amount > 0; slot++) {
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

    // ---- 次要功能 ----

    @Override
    public boolean applyWater(ICropStickTile crop) {
        FluidStack stored = this.waterTank.getFluidInTank(0);
        if (stored.isEmpty()) return false;
        PotencyData potency = HydrationRegistry.instance.getPotency(stored);
        if (potency == null) return false;
        if (crop.getWaterStorage() > WATER_THRESHOLD) return false;

        int potencyValue = CropsGTMConfig.scalePotency(potency.getPotency(), CropsGTMConfig.waterPotencyMultiplier);
        int units = CropsGTMConfig.scaleConsumption(potency.getUnitsConsumedPerApplication(),
                CropsGTMConfig.waterConsumptionMultiplier);
        int applications = CropManagerCycle.getApplications(stored.getAmount(), potencyValue, units,
                WATER_CAP - crop.getWaterStorage());
        if (applications <= 0) return false;
        int amount = applications * potencyValue;
        if (!crop.addWater(amount, WATER_THRESHOLD, WATER_CAP, true)) return false;
        if (!crop.addWater(amount, WATER_THRESHOLD, WATER_CAP, false)) return false;
        this.waterTank.drainInternal(applications * units, FluidAction.EXECUTE);
        return true;
    }

    /** 除草剂罐一次用量对应的除草剂流体 mB，按配置缩放。 */
    private static int getWeedExMbPerUse() {
        PotencyData weedExPotency = WeedEXRegistry.instance.getPotency(CropsGTMMaterials.weedEx.getFluid());
        int basePotency = weedExPotency != null ? weedExPotency.getPotency() : Constants.WEEDEX_POTENCY_PER_MB;
        int potencyPerMb = CropsGTMConfig.scalePotency(basePotency, CropsGTMConfig.weedExPotencyMultiplier);
        int base = Math.max(1, Constants.WEEDEX_POTENCY_PER_USE / potencyPerMb);
        return CropsGTMConfig.scaleConsumption(base, CropsGTMConfig.weedExConsumptionMultiplier);
    }

    /**
     * 把除草剂从槽位中的喷雾罐抽入内部储罐，一次用量相当于 {@link Constants#WEEDEX_POTENCY_PER_USE} 点除草剂。用尽后罐子变为空的喷雾罐，与玩家手动用尽时相同。
     */
    private void refillWeedEx() {
        int space = this.weedExTank.getTankCapacity(0) - this.weedExTank.getFluidInTank(0).getAmount();
        int mbPerUse = getWeedExMbPerUse();
        if (space < mbPerUse) return;
        for (int i = 0; i < WEEDEX_SLOT_COUNT; i++) {
            ItemStack stack = this.weedExInput.getStackInSlot(i);
            if (!(stack.getItem() instanceof ItemWeedEx can)) continue;
            int usesLeft = can.getUsesLeft(stack);
            int uses = Math.min(space / mbPerUse, usesLeft);
            if (uses <= 0) continue;
            this.weedExTank.fillInternal(new FluidStack(getWeedExFluidStored(), uses * mbPerUse), FluidAction.EXECUTE);
            space -= uses * mbPerUse;
            if (usesLeft - uses <= 0) {
                this.weedExInput.setStackInSlot(i, GTItems.SPRAY_EMPTY.asStack());
            } else {
                ItemStack copy = stack.copy();
                ItemSprayCan.setUsesLeft(copy, usesLeft - uses);
                this.weedExInput.setStackInSlot(i, copy);
            }
            if (space < mbPerUse) return;
        }
    }

    private FluidStack getWeedExFluidStored() {
        FluidStack stored = this.weedExTank.getFluidInTank(0);
        if (!stored.isEmpty()) {
            FluidStack copy = stored.copy();
            copy.setAmount(1);
            return copy;
        }
        return new FluidStack(CropsGTMMaterials.weedEx.getFluid(), 1);
    }

    @Override
    public boolean applyWeedEx(ICropStickTile crop) {
        FluidStack stored = this.weedExTank.getFluidInTank(0);
        if (stored.getAmount() < getWeedExMbPerUse()) return false;
        PotencyData potency = WeedEXRegistry.instance.getPotency(stored);
        if (potency == null) return false;
        if (crop.getWeedExStorage() > WEEDEX_THRESHOLD) return false;

        int potencyValue = CropsGTMConfig.scalePotency(potency.getPotency(), CropsGTMConfig.weedExPotencyMultiplier);
        int units = CropsGTMConfig.scaleConsumption(potency.getUnitsConsumedPerApplication(),
                CropsGTMConfig.weedExConsumptionMultiplier);
        int applications = CropManagerCycle.getApplications(stored.getAmount(), potencyValue, units,
                WEEDEX_CAP - crop.getWeedExStorage());
        if (applications <= 0) return false;
        int amount = Math.min(applications * potencyValue, WEEDEX_COST);
        if (!crop.addWeedEx(amount, WEEDEX_THRESHOLD, WEEDEX_CAP, true)) return false;
        if (!crop.addWeedEx(amount, WEEDEX_THRESHOLD, WEEDEX_CAP, false)) return false;
        this.weedExTank.drainInternal(applications * units, FluidAction.EXECUTE);
        return true;
    }

    @Override
    public boolean applyFertilizer(ICropStickTile crop) {
        int stored = crop.getFertilizerStorage();
        if (stored > FERTILIZER_THRESHOLD) return false;

        // 总是先使用液态肥料，与原机器一致
        FluidStack fluid = this.fertilizerTank.getFluidInTank(0);
        if (!fluid.isEmpty()) {
            PotencyData potency = FertilizerRegistry.instance.getPotency(fluid);
            if (potency == null) return false;
            int potencyValue = CropsGTMConfig.scalePotency(potency.getPotency(),
                    CropsGTMConfig.fertilizerPotencyMultiplier);
            int units = CropsGTMConfig.scaleConsumption(potency.getUnitsConsumedPerApplication(),
                    CropsGTMConfig.fertilizerConsumptionMultiplier);
            int applications = CropManagerCycle.getApplications(fluid.getAmount(), potencyValue, units,
                    FERTILIZER_CAP - crop.getFertilizerStorage());
            if (applications <= 0) return false;
            int amount = applications * potencyValue;
            if (!crop.addFertilizer(amount, FERTILIZER_THRESHOLD, FERTILIZER_CAP, true)) return false;
            if (!crop.addFertilizer(amount, FERTILIZER_THRESHOLD, FERTILIZER_CAP, false)) return false;
            this.fertilizerTank.drainInternal(applications * units, FluidAction.EXECUTE);
            return true;
        }

        for (int i = 0; i < FERTILIZER_SLOT_COUNT; i++) {
            ItemStack stack = this.fertilizerInput.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            PotencyData potency = FertilizerRegistry.instance.getPotency(stack);
            if (potency == null) continue;
            int potencyValue = CropsGTMConfig.scalePotency(potency.getPotency(),
                    CropsGTMConfig.fertilizerPotencyMultiplier);
            int units = CropsGTMConfig.scaleConsumption(potency.getUnitsConsumedPerApplication(),
                    CropsGTMConfig.fertilizerConsumptionMultiplier);
            if (stack.getCount() < units) continue;

            int cap = potency.getMaxStorage(FERTILIZER_CAP);
            int threshold = Math.max(cap / 2, cap - potencyValue / 2);
            if (stored > threshold) continue;
            if (!crop.addFertilizer(potencyValue, threshold, cap, true)) return false;
            if (!crop.addFertilizer(potencyValue, threshold, cap, false)) return false;
            ItemStack copy = stack.copy();
            copy.shrink(units);
            this.fertilizerInput.setStackInSlot(i, copy.isEmpty() ? ItemStack.EMPTY : copy);
            return true;
        }
        return false;
    }

    /**
     * @return 已存流体在不溢出作物的前提下还能施用多少次。
     */

    // ---- 能力接口 ----

    @Override
    public boolean isWorkingEnabled() {
        return this.workingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        this.workingEnabled = workingEnabled;
    }

    @Override
    public boolean isAutoOutputItems() {
        return this.autoOutputItems;
    }

    @Override
    public void setAutoOutputItems(boolean allow) {
        this.autoOutputItems = allow;
        this.batteryAutoOutput.updateAutoOutputSubscription();
    }

    @Override
    public boolean isAutoOutputEnabled() {
        return this.autoOutputItems;
    }

    @Override
    public void setAutoOutputEnabled(boolean value) {
        setAutoOutputItems(value);
    }

    @Override
    public boolean isAllowInputFromOutputSideItems() {
        return this.allowInputFromOutputSideItems;
    }

    @Override
    public void setAllowInputFromOutputSideItems(boolean allow) {
        this.allowInputFromOutputSideItems = allow;
    }

    @Override
    public Direction getOutputFacingItems() {
        return this.outputFacingItems;
    }

    @Override
    public void setOutputFacingItems(@Nullable Direction outputFacing) {
        this.outputFacingItems = outputFacing;
        this.batteryAutoOutput.updateAutoOutputSubscription();
    }

    // ---- 电池与自动输出 ----

    @Override
    public TieredEnergyMachine machine() {
        return this;
    }

    @Override
    public CustomItemStackHandler chargerInventory() {
        return this.chargerInventory;
    }

    @Override
    public NotifiableItemStackHandler outputHandler() {
        return this.output;
    }

    @Override
    public Direction getOutputFacing() {
        return this.outputFacingItems;
    }

    @Override
    public void onNeighborChanged(net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        this.batteryAutoOutput.updateAutoOutputSubscription();
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return CropManagerGui.createUI(this, entityPlayer);
    }

    @Override
    public Widget createUIWidget() {
        return CropManagerGui.createUIWidget(this);
    }

    int getSyncedWater() {
        return this.syncedWater;
    }

    int getSyncedWeedEx() {
        return this.syncedWeedEx;
    }

    int getSyncedFertilizer() {
        return this.syncedFertilizer;
    }

    public boolean isWaterEnabled() {
        return this.waterEnabled;
    }

    public void setWaterEnabled(boolean value) {
        this.waterEnabled = value;
    }

    public boolean isWeedExEnabled() {
        return this.weedExEnabled;
    }

    public void setWeedExEnabled(boolean value) {
        this.weedExEnabled = value;
    }

    public boolean isFertilizerEnabled() {
        return this.fertilizerEnabled;
    }

    public void setFertilizerEnabled(boolean value) {
        this.fertilizerEnabled = value;
    }

    public boolean isHarvestEnabled() {
        return this.harvestEnabled;
    }

    public void setHarvestEnabled(boolean value) {
        this.harvestEnabled = value;
    }
}
