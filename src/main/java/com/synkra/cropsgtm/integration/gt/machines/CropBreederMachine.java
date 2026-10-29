package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IAutoOutputItem;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropMutation;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.farming.requirements.SubSoilRequirement;
import com.synkra.cropsgtm.integration.gt.client.CropsGTMGuiTextures;
import com.synkra.cropsgtm.integration.gt.client.widget.RecipeArrowWidget;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMaterials;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMRecipeTypes;
import com.synkra.cropsgtm.items.ItemGenericSeed;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 电力作物繁殖机：组合已分析的种子产出新作物。
 * <p>
 * 移植自原 CropNH 作物繁殖机。亲本处理、流体消耗与产出概率公式保持一致；原机器使用液态肥料，本版本改用注册在水分登记表中的水。
 * </p>
 */
public class CropBreederMachine extends WorkableTieredMachine
                                implements IFancyUIMachine, IAutoOutputItem, BatteryAutoOutputSupport.Host {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CropBreederMachine.class,
            WorkableTieredMachine.MANAGED_FIELD_HOLDER);

    public static final int FERTILIZER_PER_TIER = 144;
    public static final int FERTILIZER_PER_STAT = FERTILIZER_PER_TIER / 2;

    public static final int MIN_CHANCE = 40;
    public static final int MAX_CHANCE = 100;
    public static final int CHANCE_INCREASE_PER_TIER = 10;

    private static final int MIN_INPUT_SLOT_COUNT = 3;
    private static final int MAX_INPUT_SLOT_COUNT = 6;
    private static final int OUTPUT_SLOT_COUNT = 1;

    /** 电池槽，机器同样可以用电池物品供电。 */
    @Persisted
    public final CustomItemStackHandler chargerInventory;

    /** 自动输出配置。 */
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
    private ISubscription energySubs;
    /** 电池与自动输出行为，与其他作物机器共用。 */
    private final BatteryAutoOutputSupport batteryAutoOutput = new BatteryAutoOutputSupport(this);

    public CropBreederMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, CropBreederMachine::getCustomFluidCapacity);
        this.outputFacingItems = getFrontFacing().getOpposite();

        // 充电槽只接受单个电池，与 GregTech 机器保持一致
        this.chargerInventory = new CustomItemStackHandler(1) {

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }
        };
        this.chargerInventory.setFilter(item -> GTCapabilityHelper.getElectricItem(item) != null);
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, getInputSlotCount(getTier()), IO.IN)
                .setFilter(CropBreederMachine::isValidInput);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, OUTPUT_SLOT_COUNT, IO.OUT);
    }

    @Override
    protected NotifiableFluidTank createImportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 1, getCustomFluidCapacity(getTier()), IO.IN)
                .setFilter(CropBreederMachine::isFertilizerFluid);
    }

    @Override
    protected NotifiableFluidTank createExportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 0, 0, IO.OUT);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new CropBreederRecipeLogic(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ---- 参数 ----

    public static int getInputSlotCount(int tier) {
        return tier < GTValues.HV ? MIN_INPUT_SLOT_COUNT : MAX_INPUT_SLOT_COUNT;
    }

    public static boolean isValidSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGenericSeed;
    }

    /**
     * @return 输入槽接受的物品返回 true：种子，以及能作为突变下层土壤催化剂的方块物品。
     */
    public static boolean isValidInput(ItemStack stack) {
        return isValidSeed(stack) || (!stack.isEmpty() && stack.getItem() instanceof BlockItem);
    }

    public static boolean isFertilizerFluid(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == CropsGTMMaterials.enrichedFertilizer.getFluid();
    }

    public static int getFluidAmount(int tier, int growth, int gain, int resistance) {
        return Math.max(1, tier * FERTILIZER_PER_TIER) +
                Math.max(1, (growth + gain + resistance) * FERTILIZER_PER_STAT);
    }

    public static int getOutputChance(int tier) {
        return Math.min(MAX_CHANCE, MIN_CHANCE + (tier - GTValues.LV) * CHANCE_INCREASE_PER_TIER);
    }

    public static int getCustomFluidCapacity(int tier) {
        return tier < GTValues.IV ? getCapacityForTier(tier) / 5 : getCapacityForTier(tier);
    }

    private static int getCapacityForTier(int tier) {
        return switch (tier) {
            case GTValues.LV -> 16000;
            case GTValues.MV -> 32000;
            case GTValues.HV -> 64000;
            case GTValues.EV -> 128000;
            default -> 256000;
        };
    }

    public double getProgressPercent() {
        return this.recipeLogic.getProgressPercent();
    }

    /**
     * 配方电压取机器自身的电压等级，配方查看器与 Jade 用它把配方的 EU/t 拆成电流与电压等级。缺省时会退回 LV，超频后的高压配方会被显示成离谱的 LV 电流。
     */
    @Override
    public long getDisplayRecipeVoltage() {
        return GTValues.V[getTier()];
    }

    // ---- 生命周期 ----

    @Override
    public void onLoad() {
        // 父类负责构建配方处理器列表，并把配方逻辑订阅到其中
        super.onLoad();
        if (isRemote()) return;
        this.batteryAutoOutput.onLoad();
        // 充电槽或内部缓存变化时，必须重新开始电池放电
        this.chargerInventory.setOnContentsChanged(this.batteryAutoOutput::updateBatterySubscription);
        this.energySubs = this.energyContainer.addChangedListener(this.batteryAutoOutput::updateBatterySubscription);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        this.batteryAutoOutput.onUnload();
        if (this.energySubs != null) {
            this.energySubs.unsubscribe();
            this.energySubs = null;
        }
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(this.importItems.storage);
        clearInventory(this.exportItems.storage);
        clearInventory(this.chargerInventory);
    }

    // ---- 电池与自动输出 ----

    @Override
    public WorkableTieredMachine machine() {
        return this;
    }

    @Override
    public CustomItemStackHandler chargerInventory() {
        return this.chargerInventory;
    }

    @Override
    public NotifiableItemStackHandler outputHandler() {
        return this.exportItems;
    }

    @Override
    public boolean isAutoOutputEnabled() {
        return this.autoOutputItems;
    }

    @Override
    public Direction getOutputFacing() {
        return this.outputFacingItems;
    }

    @Override
    public void onNeighborChanged(net.minecraft.world.level.block.Block block, net.minecraft.core.BlockPos fromPos,
                                  boolean isMoving) {
        super.onNeighborChanged(block, fromPos, isMoving);
        this.batteryAutoOutput.updateAutoOutputSubscription();
    }

    // ---- 能力接口 ----

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

    // ---- 繁殖 ----

    /**
     * 构建当前繁殖周期的配方；若没有可繁殖的亲本组合则返回空配方。
     * <p>
     * 机器没有静态配方表：亲本与结果都携带任意种子属性，因此配方由输入槽中实际存在的种子构建。之后 GregTech
     * 会像处理其他工作机器一样消耗亲本与肥料、抽取能量并应用超频。产出概率在构建配方时掷出，掷失败仍会消耗亲本与肥料，但不会产出。
     * </p>
     */
    public Iterator<GTRecipe> searchRecipe() {
        Map<ICropCard, Integer> parentSlots = new LinkedHashMap<>();
        Map<ICropCard, SeedStats> parentStats = new LinkedHashMap<>();
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            ItemStack stack = this.importItems.getStackInSlot(i);
            if (!isValidSeed(stack)) continue;
            ICropCard crop = CropRegistry.instance.get(stack);
            if (crop == null) continue;
            SeedStats stats = SeedStats.getStatsFromStack(stack);
            if (stats == null || !stats.isAnalyzed()) continue;
            if (parentSlots.putIfAbsent(crop, i) == null) {
                parentStats.put(crop, stats);
            }
        }
        if (parentSlots.size() < 2) return Collections.emptyIterator();

        List<ICropCard> parents = new ArrayList<>(parentSlots.keySet());
        List<ICropMutation> mutations = MutationRegistry.instance.getPossibleDeterministicMutations(parents);
        if (mutations == null || mutations.isEmpty()) return Collections.emptyIterator();
        mutations.sort(Comparator.comparingInt(ICropMutation::getParentCount).reversed());

        int fluidStored = this.importFluids.getFluidInTank(0).getAmount();
        for (ICropMutation mutation : mutations) {
            if (mutation.getBreedingMachineRecipeEUt() > GTValues.V[getTier()]) continue;
            if (!mutation.canBreedInMachine(parents)) continue;

            SeedStats newStats = getAverageStats(mutation, parentStats);
            int fluidAmount = getFluidAmount(mutation.getOutput().getTier(), newStats.getGrowth(), newStats.getGain(),
                    newStats.getResistance());
            if (fluidAmount > fluidStored) continue;

            // 该突变消耗作物所需的下层土壤方块之一，与原机器一致
            List<ItemStack> catalysts = SubSoilRequirement.collectCatalysts(mutation.getOutput());
            Ingredient catalystIngredient = catalysts.isEmpty() ? null :
                    Ingredient.of(catalysts.toArray(new ItemStack[0]));
            if (catalystIngredient != null && !this.hasItemInput(catalystIngredient)) continue;

            ItemStack[] parentItems = parentSlots.values().stream()
                    .map(slot -> this.importItems.getStackInSlot(slot).copyWithCount(1)).toArray(ItemStack[]::new);
            GTRecipeBuilder builder = GTRecipeBuilder.ofRaw().inputItems(parentItems)
                    .inputFluids(new FluidStack(CropsGTMMaterials.enrichedFertilizer.getFluid(), fluidAmount))
                    .duration(mutation.getBreedingMachineRecipeDuration()).EUt(mutation.getBreedingMachineRecipeEUt());
            if (catalystIngredient != null) {
                builder.inputItems(catalystIngredient);
            }
            if (getLevel().random.nextInt(MAX_CHANCE) < getOutputChance(getTier())) {
                builder.outputItems(mutation.getOutput().getSeedItem(newStats).copyWithCount(1));
            }
            return Collections.singletonList(builder.buildRawRecipe()).iterator();
        }
        return Collections.emptyIterator();
    }

    /** @return 任一输入槽中存在匹配该原料的物品时返回 true。 */
    private boolean hasItemInput(Ingredient ingredient) {
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            if (ingredient.test(this.importItems.getStackInSlot(i))) return true;
        }
        return false;
    }

    private static SeedStats getAverageStats(ICropMutation mutation, Map<ICropCard, SeedStats> parentStats) {
        int growth = 0;
        int gain = 0;
        int resistance = 0;
        int count = 0;
        for (ICropCard parent : mutation.getParents()) {
            SeedStats stats = parentStats.get(parent);
            if (stats == null) continue;
            growth += stats.getGrowth();
            gain += stats.getGain();
            resistance += stats.getResistance();
            count++;
        }
        if (count == 0) return SeedStats.DEFAULT_ANALYZED;
        return new SeedStats((byte) (growth / count), (byte) (gain / count), (byte) (resistance / count), true);
    }

    /** 查找当前突变配方的配方逻辑，而不是读取配方表。 */
    public static class CropBreederRecipeLogic extends RecipeLogic {

        public CropBreederRecipeLogic(CropBreederMachine machine) {
            super(machine);
        }

        @Override
        public @NotNull Iterator<GTRecipe> searchRecipe() {
            return ((CropBreederMachine) this.machine).searchRecipe();
        }

        /**
         * 每个周期消耗的种子与产出都不同，因此完成后的配方会被丢弃并重新查找，而不是按默认实现那样重新开始。
         */
        @Override
        public void onRecipeFinish() {
            machine.afterWorking();
            if (lastRecipe == null) return;
            handleRecipeIO(lastRecipe, IO.OUT);
            lastRecipe = null;
            lastOriginRecipe = null;
            progress = 0;
            duration = 0;
            isActive = false;
            markLastRecipeDirty();
            setStatus(Status.IDLE);
            updateTickSubscription();
        }
    }

    // ---- 界面 ----

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 140, 74);

        // 整个内容在页面中居中：横向 21..119，纵向
        // 以输入网格居中，流体槽位于最左侧输入槽下方
        int startX = 21;
        int slotCount = getInputSlotCount(getTier());
        int rows = (slotCount + 2) / 3;
        int rowY = rows == 1 ? 18 : 9;
        int arrowY = rowY + (rows - 1) * 9;

        group.addWidget(new TankWidget(this.importFluids, 0, startX, rowY + rows * 18, true, true)
                .setBackground(GuiTextures.FLUID_SLOT));

        for (int i = 0; i < slotCount; i++) {
            int col = i % 3;
            int row = i / 3;
            int y = rowY + row * 18;
            group.addWidget(new SlotWidget(this.importItems.storage, i, startX + col * 18, y).setCanPutItems(true)
                    .setCanTakeItems(true).setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_CROP_STICK));
        }

        group.addWidget(createProgressArrow(startX + 60, arrowY));
        group.addWidget(new SlotWidget(this.exportItems.storage, 0, startX + 80, arrowY).setCanPutItems(false)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_CROP_STICK));
        group.addWidget(new SlotWidget(this.chargerInventory, 0, startX + 80, arrowY + 20).setCanPutItems(true)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY));

        return group;
    }

    private ProgressWidget createProgressArrow(int x, int y) {
        return new RecipeArrowWidget(x, y, this::getProgressPercent, self().getDefinition().asStack(),
                CropsGTMRecipeTypes.CROP_BREEDER_RECIPES.getCategory().registryKey.toString());
    }
}
