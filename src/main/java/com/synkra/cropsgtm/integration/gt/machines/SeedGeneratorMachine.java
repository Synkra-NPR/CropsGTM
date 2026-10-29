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
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.requirements.SubSoilRequirement;
import com.synkra.cropsgtm.integration.gt.client.widget.RecipeArrowWidget;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMaterials;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMRecipeTypes;
import com.synkra.cropsgtm.items.ItemGenericSeed;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

/**
 * 电力种子生成机：以相同属性复制单个已分析种子。
 * <p>
 * 移植自原 CropNH 种子生成机。流体消耗与基础配方保持一致；原机器使用液态肥料，本版本改用注册在水分登记表中的水。
 * </p>
 */
public class SeedGeneratorMachine extends WorkableTieredMachine
                                  implements IFancyUIMachine, IAutoOutputItem, BatteryAutoOutputSupport.Host {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(SeedGeneratorMachine.class,
            WorkableTieredMachine.MANAGED_FIELD_HOLDER);

    public static final int BASE_RECIPE_DURATION = 8 * (int) GTValues.SECONDS;
    // 配方电压下为 0.9375A，与原模组 TierEU.RECIPE_LV 的取值相同，因此
    // 能量源与机器之间的线损不会让配方断供
    public static final long BASE_RECIPE_EUT = GTValues.VA[GTValues.LV];
    public static final int FERTILIZER_PER_STAT = 36;

    private static final int INPUT_SLOT_COUNT = 2;
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
    /**
     * 运行中配方所复制的种子。种子是模板且不属于配方输入，因此在周期结束时再次检查，确认它仍在某个输入槽中。
     */
    private ItemStack activeTemplate = ItemStack.EMPTY;

    public SeedGeneratorMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, SeedGeneratorMachine::getCustomFluidCapacity);
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
        return new NotifiableItemStackHandler(this, INPUT_SLOT_COUNT, IO.IN)
                .setFilter(SeedGeneratorMachine::isValidInput);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, OUTPUT_SLOT_COUNT, IO.OUT);
    }

    @Override
    protected NotifiableFluidTank createImportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 1, getCustomFluidCapacity(getTier()), IO.IN)
                .setFilter(SeedGeneratorMachine::isFertilizerFluid);
    }

    @Override
    protected NotifiableFluidTank createExportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 0, 0, IO.OUT);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new SeedGeneratorRecipeLogic(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ---- 参数 ----

    public static boolean isValidSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGenericSeed;
    }

    /**
     * @return 输入槽接受的物品返回 true：种子，以及能作为作物复制催化剂的方块物品。
     */
    public static boolean isValidInput(ItemStack stack) {
        return isValidSeed(stack) || (!stack.isEmpty() && stack.getItem() instanceof BlockItem);
    }

    public static boolean isFertilizerFluid(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == CropsGTMMaterials.enrichedFertilizer.getFluid();
    }

    public static int getFluidAmount(int growth, int gain, int resistance) {
        return Math.max(1, (growth + gain + resistance) * FERTILIZER_PER_STAT);
    }

    public static int getCustomFluidCapacity(int tier) {
        return tier < GTValues.IV ? getCapacityForTier(tier) / 10 : getCapacityForTier(tier);
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

    // ---- 复制 ----

    /**
     * 构建当前周期的配方；若当前没有可复制的种子则返回空配方。
     * <p>
     * 机器没有静态配方表：携带任意属性的种子取自输入槽。与原机器一致，种子只是模板、不会被消耗，因此不计入配方输入；取而代之的是消耗作物下层土壤需求中的一种方块作为催化剂，产出为种子的副本。GregTech
     * 抽取能量并应用超频的方式与其他工作机器完全相同。
     * </p>
     */
    public Iterator<GTRecipe> searchRecipe() {
        int fluidStored = this.importFluids.getFluidInTank(0).getAmount();
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            ItemStack stack = this.importItems.getStackInSlot(i);
            if (!isValidSeed(stack)) continue;

            ICropCard crop = CropRegistry.instance.get(stack);
            if (crop == null) continue;
            // 无法杂交的作物不由种子生成机处理。
            if (crop.getCrossingThreshold() < 0.0f) continue;

            SeedStats stats = SeedStats.getStatsFromStack(stack);
            if (stats == null || !stats.isAnalyzed()) continue;

            int fluidAmount = getFluidAmount(stats.getGrowth(), stats.getGain(), stats.getResistance());
            if (fluidAmount > fluidStored) continue;

            // 每周期消耗作物的一个下层土壤方块
            List<ItemStack> catalysts = SubSoilRequirement.collectCatalysts(crop);
            Ingredient catalystIngredient = catalysts.isEmpty() ? null :
                    Ingredient.of(catalysts.toArray(new ItemStack[0]));
            if (catalystIngredient != null && !this.hasItemInput(catalystIngredient)) continue;

            this.activeTemplate = stack.copyWithCount(1);
            GTRecipeBuilder builder = GTRecipeBuilder.ofRaw().outputItems(stack.copyWithCount(1))
                    .inputFluids(new FluidStack(CropsGTMMaterials.enrichedFertilizer.getFluid(), fluidAmount))
                    .duration(BASE_RECIPE_DURATION).EUt(BASE_RECIPE_EUT);
            if (catalystIngredient != null) {
                builder.inputItems(catalystIngredient);
            }
            return Collections.singletonList(builder.buildRawRecipe()).iterator();
        }
        this.activeTemplate = ItemStack.EMPTY;
        return Collections.emptyIterator();
    }

    /** @return 任一输入槽中存在匹配该原料的物品时返回 true。 */
    private boolean hasItemInput(Ingredient ingredient) {
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            if (ingredient.test(this.importItems.getStackInSlot(i))) return true;
        }
        return false;
    }

    /** @return 运行中配方所复制的种子仍在某个输入槽中时返回 true。 */
    public boolean hasActiveTemplate() {
        if (this.activeTemplate.isEmpty()) return false;
        for (int i = 0; i < this.importItems.getSlots(); i++) {
            ItemStack stack = this.importItems.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, this.activeTemplate)) return true;
        }
        return false;
    }

    public void clearActiveTemplate() {
        this.activeTemplate = ItemStack.EMPTY;
    }

    /** 查找当前复制配方的配方逻辑，而不是读取配方表。 */
    public static class SeedGeneratorRecipeLogic extends RecipeLogic {

        public SeedGeneratorRecipeLogic(SeedGeneratorMachine machine) {
            super(machine);
        }

        @Override
        public @NotNull Iterator<GTRecipe> searchRecipe() {
            return ((SeedGeneratorMachine) this.machine).searchRecipe();
        }

        /**
         * 每个周期复制的是输入槽中当前的种子，因此完成后的配方会被丢弃并重新查找，而不是按默认实现那样重新开始。
         */
        @Override
        public void onRecipeFinish() {
            SeedGeneratorMachine seedGenerator = (SeedGeneratorMachine) this.machine;
            // 种子是模板而非配方输入，因此周期只在它仍存在时才结算
            // 在输入槽中
            if (lastRecipe == null || !seedGenerator.hasActiveTemplate()) {
                seedGenerator.clearActiveTemplate();
                resetRecipeLogic();
                return;
            }
            machine.afterWorking();
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
        WidgetGroup group = new WidgetGroup(0, 0, 122, 74);

        // 整个内容在页面中居中，横向 21..101、纵向 18..56
        int startX = 21;
        int rowY = 18;

        // 一个常规的 18x18 流体槽，立即可见已存流体，类似小型 GregTech
        // 机器那样，而不是像进度条那样填满的高条。它位于
        // 最左侧输入槽，与物品输入网格对齐
        group.addWidget(new TankWidget(this.importFluids, 0, startX, rowY + 18, true, true)
                .setBackground(GuiTextures.FLUID_SLOT));

        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            group.addWidget(new SlotWidget(this.importItems.storage, i, startX + i * 18, rowY).setCanPutItems(true)
                    .setCanTakeItems(true).setBackground(GuiTextures.SLOT));
        }

        group.addWidget(createProgressArrow(startX + 42, rowY));
        group.addWidget(new SlotWidget(this.exportItems.storage, 0, startX + 62, rowY).setCanPutItems(false)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT));
        group.addWidget(new SlotWidget(this.chargerInventory, 0, startX + 62, rowY + 20).setCanPutItems(true)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY));

        return group;
    }

    private ProgressWidget createProgressArrow(int x, int y) {
        return new RecipeArrowWidget(x, y, this::getProgressPercent, self().getDefinition().asStack(),
                CropsGTMRecipeTypes.SEED_GENERATOR_RECIPES.getCategory().registryKey.toString());
    }
}
