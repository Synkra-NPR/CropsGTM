package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.integration.gt.client.CropsGTMGuiTextures;
import com.synkra.cropsgtm.integration.gt.client.widget.RecipeArrowWidget;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMRecipeTypes;
import com.synkra.cropsgtm.integration.gt.items.GeneDataOrb;
import com.synkra.cropsgtm.items.ItemGenericSeed;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;

/**
 * 电力作物基因提取机：把已分析种子的一项基因读入数据宝珠。
 * <p>
 * 移植自原 CropNH 基因提取机。由编程电路选择基因，提取受作物的繁殖电压等级限制，时长与 EU/t 与原机器完全一致。
 * </p>
 */
public class CropGeneExtractorMachine extends SimpleTieredMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CropGeneExtractorMachine.class, SimpleTieredMachine.MANAGED_FIELD_HOLDER);

    public static final int INPUT_SLOT_COUNT = 2;
    public static final int OUTPUT_SLOT_COUNT = 1;
    public static final int SEED_SLOT = 0;
    public static final int ORB_SLOT = 1;

    /** 样本提取的时长，与原机器一样为五分钟。 */
    public static final int DURATION_SPECIMEN = 5 * (int) GTValues.MINUTES;
    /** 一项属性提取的时长，为样本提取时长的一半。 */
    public static final int DURATION_STAT = DURATION_SPECIMEN / 2;

    public static final int CIRCUIT_SPECIMEN = 1;
    public static final int CIRCUIT_GROWTH = 2;
    public static final int CIRCUIT_GAIN = 3;
    public static final int CIRCUIT_RESISTANCE = 4;

    public CropGeneExtractorMachine(IMachineBlockEntity holder, int tier) {
        // 该机器完全没有流体储罐
        super(holder, tier, ignored -> 0);
    }

    @Override
    protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, INPUT_SLOT_COUNT, IO.IN)
                .setFilter(CropGeneExtractorMachine::isValidInput);
    }

    @Override
    protected NotifiableItemStackHandler createExportItemHandler(Object... args) {
        return new NotifiableItemStackHandler(this, OUTPUT_SLOT_COUNT, IO.OUT);
    }

    @Override
    protected NotifiableFluidTank createImportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 0, 0, IO.IN);
    }

    @Override
    protected NotifiableFluidTank createExportFluidHandler(Object... args) {
        return new NotifiableFluidTank(this, 0, 0, IO.OUT);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new GeneExtractorRecipeLogic(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ---- 参数 ----

    public static boolean isSeed(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemGenericSeed;
    }

    public static boolean isGeneOrb(ItemStack stack) {
        return GeneDataOrb.isGeneOrb(stack);
    }

    /** @return 输入槽接受的物品返回 true：种子与空白基因数据宝珠。 */
    public static boolean isValidInput(ItemStack stack) {
        // 只接受空白宝珠，提取绝不能消耗已填充的宝珠
        return isSeed(stack) || GeneDataOrb.isBlankOrb(stack);
    }

    /**
     * @return 处理该作物所需的电压等级，限制在原机器支持的范围内：最低 EV，最高 UXV。
     */
    public static int getVoltageTier(ICropCard crop) {
        return Math.min(GTValues.UXV, Math.max(GTValues.EV, crop.getMachineBreedingRecipeTier()));
    }

    public static int getRecipeDuration(int circuit) {
        return circuit == CIRCUIT_SPECIMEN ? DURATION_SPECIMEN : DURATION_STAT;
    }

    public static long getRecipeEUt(ICropCard crop) {
        return GTValues.VA[getVoltageTier(crop)];
    }

    public double getProgressPercent() {
        return this.recipeLogic.getProgressPercent();
    }

    // ---- 提取 ----

    /**
     * 构建当前提取周期的配方。
     * <p>
     * 机器没有静态配方表：种子携带任意属性，由电路决定把哪一项写入宝珠，因此配方由机器内实际物品构建。种子与空白宝珠是配方输入，周期开始时由 GregTech 消耗；填充后的宝珠是产出。
     * </p>
     */
    public Iterator<GTRecipe> searchRecipe() {
        int circuit = IntCircuitBehaviour.getCircuitConfiguration(this.circuitInventory.getStackInSlot(0));
        if (circuit < CIRCUIT_SPECIMEN || circuit > CIRCUIT_RESISTANCE) return Collections.emptyIterator();

        ItemStack seedStack = ItemStack.EMPTY;
        ItemStack orbStack = ItemStack.EMPTY;
        for (int slot = 0; slot < this.importItems.getSlots(); slot++) {
            ItemStack stack = this.importItems.getStackInSlot(slot);
            if (seedStack.isEmpty() && isSeed(stack)) {
                seedStack = stack;
            } else if (orbStack.isEmpty() && GeneDataOrb.isBlankOrb(stack)) {
                orbStack = stack;
            }
        }
        if (seedStack.isEmpty() || orbStack.isEmpty()) return Collections.emptyIterator();

        ICropCard crop = CropRegistry.instance.get(seedStack);
        if (crop == null) return Collections.emptyIterator();
        SeedStats stats = SeedStats.getStatsFromStack(seedStack);
        if (stats == null || !stats.isAnalyzed()) return Collections.emptyIterator();

        int minTier = getVoltageTier(crop);
        if (getTier() < minTier) return Collections.emptyIterator();

        String type;
        String value;
        switch (circuit) {
            case CIRCUIT_SPECIMEN -> {
                type = GeneDataOrb.TYPE_SPECIMEN;
                value = crop.getId();
            }
            case CIRCUIT_GROWTH -> {
                type = GeneDataOrb.TYPE_GROWTH;
                value = Byte.toString(stats.getGrowth());
            }
            case CIRCUIT_GAIN -> {
                type = GeneDataOrb.TYPE_GAIN;
                value = Byte.toString(stats.getGain());
            }
            default -> {
                type = GeneDataOrb.TYPE_RESISTANCE;
                value = Byte.toString(stats.getResistance());
            }
        }

        ItemStack outputStack = GeneDataOrb.create(type, value);
        GTRecipe recipe = GTRecipeBuilder.ofRaw().inputItems(seedStack.copyWithCount(1))
                .inputItems(orbStack.copyWithCount(1)).outputItems(outputStack).duration(getRecipeDuration(circuit))
                .EUt(getRecipeEUt(crop)).buildRawRecipe();
        return Collections.singletonList(recipe).iterator();
    }

    /** 查找当前提取配方的配方逻辑，而不是读取配方表。 */
    public static class GeneExtractorRecipeLogic extends RecipeLogic {

        public GeneExtractorRecipeLogic(CropGeneExtractorMachine machine) {
            super(machine);
        }

        @Override
        public @NotNull Iterator<GTRecipe> searchRecipe() {
            return ((CropGeneExtractorMachine) this.machine).searchRecipe();
        }

        /**
         * 电路不是配方输入，因此完成后的配方会被丢弃并重新查找，而不是重新开始，从而能感知运行期间被改动的电路。
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
        WidgetGroup group = new WidgetGroup(0, 0, 122, 82);

        LabelWidget title = new LabelWidget(8, 6, self().getDefinition().asStack().getDescriptionId());
        title.setDropShadow(false);
        title.setColor(0x404040);
        group.addWidget(title);

        int startX = 21;
        int rowY = 26;

        group.addWidget(new SlotWidget(this.importItems.storage, SEED_SLOT, startX, rowY).setCanPutItems(true)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_SEED)
                .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.seed")));
        group.addWidget(new SlotWidget(this.importItems.storage, ORB_SLOT, startX + 18, rowY).setCanPutItems(true)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.blankOrb")));

        group.addWidget(createProgressArrow(startX + 40, rowY));
        group.addWidget(new SlotWidget(this.exportItems.storage, 0, startX + 60, rowY).setCanPutItems(false)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT)
                .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.geneOrb")));

        // 输出槽下方的电池，电路位于 GregTech 的配置器面板中
        group.addWidget(new SlotWidget(this.chargerInventory, 0, startX + 60, rowY + 22).setCanPutItems(true)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY)
                .setHoverTooltips(Component.translatable("cropsgtm_tooltip.slot.battery")));

        return group;
    }

    private ProgressWidget createProgressArrow(int x, int y) {
        return new RecipeArrowWidget(x, y, this::getProgressPercent, self().getDefinition().asStack(),
                CropsGTMRecipeTypes.CROP_GENE_EXTRACTOR_RECIPES.getCategory().registryKey.toString());
    }
}
