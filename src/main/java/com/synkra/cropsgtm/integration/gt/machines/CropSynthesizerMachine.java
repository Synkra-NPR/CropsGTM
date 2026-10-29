package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.integration.gt.client.CropsGTMGuiTextures;
import com.synkra.cropsgtm.integration.gt.client.widget.RecipeArrowWidget;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMRecipeTypes;
import com.synkra.cropsgtm.integration.gt.items.GeneDataOrb;
import com.synkra.cropsgtm.reference.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;

/**
 * 电力作物合成机：用四个基因数据宝珠组装出新的已分析种子。
 * <p>
 * 移植自原 CropNH 作物合成机。每种基因类型各需一个宝珠，UU 物质消耗与时长沿用原公式，机器以作物所需电压等级的三安培运行。
 * </p>
 */
public class CropSynthesizerMachine extends SimpleTieredMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CropSynthesizerMachine.class, SimpleTieredMachine.MANAGED_FIELD_HOLDER);

    /** 合成作物每个等级所需的 UU 物质 mB。 */
    public static final int UUM_PER_TIER = 750;
    /** 合成种子每点属性所需的 UU 物质 mB。 */
    public static final int UUM_PER_STAT = 100;
    public static final int AMPERAGE = 3;

    private static final int INPUT_SLOT_COUNT = 4;
    private static final int OUTPUT_SLOT_COUNT = 1;

    /** 合成的基础时长，与原机器一样为五分钟。 */
    private static final int BASE_RECIPE_TIME = 300 * (int) GTValues.SECONDS;
    private static final double BASE_RECIPE_TIME_LOG_BASE = Math.log(16.0d);

    public CropSynthesizerMachine(IMachineBlockEntity holder, int tier) {
        super(holder, tier, CropSynthesizerMachine::getCustomFluidCapacity);
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, INPUT_SLOT_COUNT, IO.IN)
                .setFilter(CropSynthesizerMachine::isGeneOrb);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, OUTPUT_SLOT_COUNT, IO.OUT);
    }

    @Override
    protected NotifiableFluidTank createImportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 1, getCustomFluidCapacity(getTier()), IO.IN)
                .setFilter(CropSynthesizerMachine::isUUMFluid);
    }

    @Override
    protected NotifiableFluidTank createExportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 0, 0, IO.OUT);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new SynthesizerRecipeLogic(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected long getMaxInputOutputAmperage() {
        // 配方会抽取机器电压的三安培，因此能量容器必须能接收
        return AMPERAGE;
    }

    // ---- 参数 ----

    public static boolean isGeneOrb(ItemStack stack) {
        return GeneDataOrb.isGeneOrb(stack);
    }

    public static boolean isUUMFluid(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid() == GTMaterials.UUMatter.getFluid();
    }

    public static int getFluidAmount(ICropCard crop, int growth, int gain, int resistance) {
        return crop.getTier() * UUM_PER_TIER + (growth + gain + resistance) * UUM_PER_STAT;
    }

    /** 从一级作物的五分钟起，到十六级作物的十分钟止。 */
    public static int getRecipeDuration(ICropCard crop) {
        int tier = Math.max(1, crop.getTier());
        return (int) (BASE_RECIPE_TIME + BASE_RECIPE_TIME * Math.log(tier) / BASE_RECIPE_TIME_LOG_BASE);
    }

    public static long getRecipeEUt(ICropCard crop) {
        return (long) GTValues.VA[CropGeneExtractorMachine.getVoltageTier(crop)] * AMPERAGE;
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

    // ---- 合成 ----

    /**
     * 构建当前合成周期的配方。
     * <p>
     * 宝珠是配方输入，UU 物质是配方流体输入，周期开始时由 GregTech 消耗。四个宝珠按标签中存储的基因类型匹配，每种类型各需一个，样本宝珠决定最终合成的作物。
     * </p>
     */
    public Iterator<GTRecipe> searchRecipe() {
        int fluidStored = this.importFluids.getFluidInTank(0).getAmount();

        ICropCard crop = null;
        ItemStack specimenOrb = ItemStack.EMPTY;
        ItemStack growthOrb = ItemStack.EMPTY;
        ItemStack gainOrb = ItemStack.EMPTY;
        ItemStack resistanceOrb = ItemStack.EMPTY;
        int growth = 0;
        int gain = 0;
        int resistance = 0;

        for (int slot = 0; slot < this.importItems.getSlots(); slot++) {
            ItemStack stack = this.importItems.getStackInSlot(slot);
            if (!GeneDataOrb.isGeneOrb(stack)) continue;
            int value = GeneDataOrb.getStatValue(stack);
            switch (GeneDataOrb.getType(stack)) {
                case GeneDataOrb.TYPE_SPECIMEN -> {
                    if (specimenOrb.isEmpty()) {
                        specimenOrb = stack;
                        crop = CropRegistry.instance.get(GeneDataOrb.getValue(stack));
                    }
                }
                case GeneDataOrb.TYPE_GROWTH -> {
                    if (growthOrb.isEmpty() && isValidStat(value)) {
                        growthOrb = stack;
                        growth = value;
                    }
                }
                case GeneDataOrb.TYPE_GAIN -> {
                    if (gainOrb.isEmpty() && isValidStat(value)) {
                        gainOrb = stack;
                        gain = value;
                    }
                }
                case GeneDataOrb.TYPE_RESISTANCE -> {
                    if (resistanceOrb.isEmpty() && isValidStat(value)) {
                        resistanceOrb = stack;
                        resistance = value;
                    }
                }
                default -> {}
            }
        }

        if (crop == null || growthOrb.isEmpty() || gainOrb.isEmpty() || resistanceOrb.isEmpty()) {
            return Collections.emptyIterator();
        }

        int minTier = CropGeneExtractorMachine.getVoltageTier(crop);
        if (getTier() < minTier) return Collections.emptyIterator();

        int fluidAmount = getFluidAmount(crop, growth, gain, resistance);
        if (fluidAmount > fluidStored) return Collections.emptyIterator();

        ItemStack outputStack = crop.getSeedItem(new SeedStats((byte) growth, (byte) gain, (byte) resistance, true))
                .copyWithCount(1);
        GTRecipe recipe = GTRecipeBuilder.ofRaw()
                .inputItems(specimenOrb.copyWithCount(1), growthOrb.copyWithCount(1), gainOrb.copyWithCount(1),
                        resistanceOrb.copyWithCount(1))
                .inputFluids(new FluidStack(GTMaterials.UUMatter.getFluid(), fluidAmount)).outputItems(outputStack)
                .duration(getRecipeDuration(crop)).EUt(getRecipeEUt(crop)).buildRawRecipe();
        return Collections.singletonList(recipe).iterator();
    }

    private static boolean isValidStat(int value) {
        int max = Math.max(CropsGTMConfig.maxGrowthStat,
                Math.max(CropsGTMConfig.maxGainStat, CropsGTMConfig.maxResistanceStat));
        return value >= Constants.MIN_SEED_STAT && value <= max;
    }

    /** 查找当前合成配方的配方逻辑，而不是读取配方表。 */
    public static class SynthesizerRecipeLogic extends RecipeLogic {

        public SynthesizerRecipeLogic(CropSynthesizerMachine machine) {
            super(machine);
        }

        @Override
        public @NotNull Iterator<GTRecipe> searchRecipe() {
            return ((CropSynthesizerMachine) this.machine).searchRecipe();
        }

        /**
         * 每个周期组合的宝珠都不同，因此完成后的配方会被丢弃并重新查找，而不是按默认实现那样重新开始。
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
        WidgetGroup group = new WidgetGroup(0, 0, 140, 82);

        // 整个内容为 88x54 并在页面中居中：箭头与输出槽位于
        // 2x2 宝珠网格的垂直中点，而不是紧邻其第一行
        int startX = (140 - 88) / 2;
        int rowY = (82 - 54) / 2;
        int arrowX = startX + 2 * 18 + 8;
        int arrowY = rowY + 9;
        int outputX = arrowX + 18 + 8;

        // 四个基因宝珠构成 2x2 网格，与机器槽位一致
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            int col = i % 2;
            int row = i / 2;
            group.addWidget(new SlotWidget(this.importItems.storage, i, startX + col * 18, rowY + row * 18)
                    .setCanPutItems(true).setCanTakeItems(true).setBackground(GuiTextures.SLOT)
                    .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.geneOrbInput")));
        }

        group.addWidget(createProgressArrow(arrowX, arrowY));
        group.addWidget(new SlotWidget(this.exportItems.storage, 0, outputX, arrowY).setCanPutItems(false)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_SEED)
                .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.seedOutput")));

        // 宝珠网格下方的 UU 物质储罐，以及输出槽下方的电池
        int bottomY = rowY + 2 * 18;
        group.addWidget(
                new TankWidget(this.importFluids, 0, startX, bottomY, true, true).setBackground(GuiTextures.FLUID_SLOT)
                        .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.uuMatter")));
        group.addWidget(new SlotWidget(this.chargerInventory, 0, outputX, bottomY).setCanPutItems(true)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY)
                .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.battery")));

        return group;
    }

    private ProgressWidget createProgressArrow(int x, int y) {
        return new RecipeArrowWidget(x, y, this::getProgressPercent, self().getDefinition().asStack(),
                CropsGTMRecipeTypes.CROP_SYNTHESIZER_RECIPES.getCategory().registryKey.toString());
    }
}
