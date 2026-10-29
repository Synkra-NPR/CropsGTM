package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.IMufflableMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.sound.AutoReleasedSound;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

import com.synkra.cropsgtm.api.BiomeClimate;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.ISeedData;
import com.synkra.cropsgtm.blocks.BlockEntityCropSticks;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.CropGrowthMath;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.requirements.SubSoilRequirement;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMachines;
import com.synkra.cropsgtm.integration.gt.items.ItemEnvironmentalModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * 工业农场：以工业规模种植一组种子，消耗水、肥料与能量。
 * <p>
 * 移植自原 CropNH 工业农场。结构是长度可变的多方块，种子床等级同时决定容量与中间层数。机器有三种模式，种子与下层土壤只能由输入模式放入、只能由输出模式取出，与原行为一致。
 * </p>
 */
public class IndustrialFarmMachine extends MultiblockControllerMachine implements IFancyUIMachine, IDisplayUIMachine,
                                   IWorkable, IMachineLife, IMufflableMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(IndustrialFarmMachine.class,
            MultiblockControllerMachine.MANAGED_FIELD_HOLDER);

    /** 默认模式，用于放入种子与下层土壤。 */
    public static final int MODE_INPUT = 0;
    /** 种植作物并产出资源的模式。 */
    public static final int MODE_FARM = 1;
    /** 把已存种子与下层土壤弹出，而不是种植作物。 */
    public static final int MODE_OUTPUT = 2;

    /** 种子槽在内部库存中的下标。 */
    public static final int SLOT_SEED = 0;
    /** 下层土壤槽在内部库存中的下标。 */
    public static final int SLOT_SUB_SOIL = 1;
    /** 环境模块槽位的起始下标。 */
    public static final int SLOT_ENV_START = 2;
    /** 内部库存的槽位数量。 */
    public static final int SLOT_COUNT = 4;

    /** 组件、乃至整个结构可具有的最低电压等级。 */
    public static final int MIN_TIER = GTValues.MV;
    /** 支持的最高电压等级，GregTech CEu 没有 MAX 机器。 */
    public static final int MAX_TIER = GTValues.OpV;
    /** 中间层数的最小值。 */
    public static final int MIN_SLICES = 1;
    /** 中间层数的最大值。 */
    public static final int MAX_SLICES = MAX_TIER - MIN_TIER + 1;

    /** 计算生长速度时模拟出的储水量。 */
    private static final int SIMULATED_WATER_STORAGE = 200;
    /** 计算生长速度时作物是否能看见天空。 */
    private static final boolean SIMULATED_CAN_SEE_SKY = true;
    /** 没有肥料可用时模拟出的肥料储量。 */
    private static final int SIMULATED_FERTILIZER_STORAGE = 200;

    /** 每个中间层的种子床数量。 */
    private static final int SEED_BEDS_PER_SLICE = 3;

    /** 结构预览的样板，按电压记忆化。 */
    private static final Map<Integer, BlockPattern> PREVIEW_PATTERNS = new HashMap<>();

    /**
     * @return 该电压等级可用的玻璃方块，顺序从允许的最弱玻璃到最强玻璃。
     */
    public static List<Block> getAllowedGlass(int tier) {
        List<Block> glasses = new ArrayList<>(4);
        if (tier < GTValues.EV) glasses.add(Blocks.GLASS);
        if (tier < GTValues.IV) glasses.add(GTBlocks.CASING_TEMPERED_GLASS.get());
        if (tier < GTValues.UV) glasses.add(GTBlocks.CASING_LAMINATED_GLASS.get());
        glasses.add(GTBlocks.FUSION_GLASS.get());
        return glasses;
    }

    /**
     * 控制器所处的状态。序号同时也是翻译键 {@code cropsgtm_tooltip.industrialFarm.status.<n>} 的编号与存档中存储的值。
     */
    public enum Status {

        OK,
        NO_SEED,
        SEEDS_FULL,
        SEED_OVERFLOW,
        SEED_BED_TIER_TOO_LOW,
        CANNOT_GROW,
        NOT_ENOUGH_WATER,
        NOT_ENOUGH_FERTILIZER,
        OUTPUT_FULL,
        NO_ENERGY,
        SUB_SOIL_NOT_FOUND,
        SUB_SOIL_MISMATCH,
        STORING,
        EJECTING,
        GLASS_TOO_WEAK,
        /** 结构通过了方块样板，但不符合组件规则所需的布局。 */
        STRUCTURE_INVALID,
        /** 结构的种子床与升级模块使用了不同电压。 */
        COMPONENT_TIER_MISMATCH,
        /** 种子床层数与种子床电压要求的长度不匹配。 */
        STRUCTURE_LENGTH_MISMATCH,
        /** 当前模式无事可做，机器在等待输入或模式切换。 */
        IDLE;

        private static final Status[] VALUES = values();

        /** @return 翻译键与存档中使用的编号。 */
        public int code() {
            return ordinal();
        }

        public static Status fromCode(int code) {
            return code >= 0 && code < VALUES.length ? VALUES[code] : OK;
        }

        /** @return 表示结构本身不可用的状态返回 true。 */
        public boolean isError() {
            return this == GLASS_TOO_WEAK || this == STRUCTURE_INVALID || this == COMPONENT_TIER_MISMATCH ||
                    this == STRUCTURE_LENGTH_MISMATCH;
        }
    }

    @Persisted
    @DescSynced
    private int machineMode = MODE_INPUT;
    @Persisted
    @DescSynced
    private int status = Status.OK.code();
    @Persisted
    @DescSynced
    private boolean workingEnabled = true;
    @Persisted
    @DescSynced
    private int cycleProgress = 0;

    /** 构成结构的种子床与升级模块的等级。 */
    @Persisted
    @DescSynced
    private int upgradeTier = -1;
    /**
     * 上次结构检查得到的组件电压，以及检测到的中间层数。它们在结构失效后仍保留，便于界面解释结构为何被拒绝。
     */
    @Persisted
    @DescSynced
    private int detectedTier = -1;
    @Persisted
    @DescSynced
    private int detectedSlices = 0;
    /**
     * 该控制器创建时对应的结构预览页电压，只对配方查看器的预览控制器设置，且从不持久化。
     */
    private int previewTier = -1;
    /** 控制器可容纳的种子与下层土壤方块数量。 */
    @Persisted
    @DescSynced
    private int seedCapacity = 0;
    @Persisted
    @DescSynced
    private int environmentalEnhancementUnitCount = 0;
    @Persisted
    @DescSynced
    private int growthAccelerationUnitCount = 0;
    @Persisted
    @DescSynced
    private int fertilizerUnitCount = 0;
    @Persisted
    @DescSynced
    private int advancedHarvestingUnitCount = 0;
    @Persisted
    @DescSynced
    private int overclockedGrowthAccelerationUnitCount = 0;
    @Persisted
    @DescSynced
    private int expectedOCs = 0;
    /** 机器生长时消耗的 EU/t。 */
    @Persisted
    @DescSynced
    private long expectedEUt = 0;
    /**
     * 当前周期是否有足够肥料获得加成。该值需要同步，因为界面中的产出预览使用同一套生长数值计算。
     */
    @DescSynced
    private boolean hasFertilizer = false;

    /** 需要播放工作音效时为 true，可用锤子像 GregTech 机器那样切换。 */
    @Persisted
    @DescSynced
    private boolean muffled = false;
    /** 客户端记忆的静音状态，用于在机器解除静音时重新开始音效。 */
    private boolean previouslyMuffled = true;
    /**
     * 同步标志，告知客户端是否应播放工作音效循环。它跟随工作覆盖层状态，使音效与动画覆盖层始终同时开始和停止。
     */
    @DescSynced
    @UpdateListener(methodName = "onWorkingSoundSynced")
    private boolean workingSoundActive = false;
    /** 当前正在循环播放的音效实例，仅在客户端使用。 */
    private Object workingSound;

    /** 存放种子、下层土壤与环境模块的内部库存。 */
    @Persisted
    public final NotifiableItemStackHandler inventory;

    /**
     * 种子槽解析后的种子定义，仅在背包或作物定义变化时重新解析，而不是每刻解析。
     */
    private @Nullable ISeedData cachedSeedData;
    private boolean seedDataValid = false;
    private int cachedRegistryGeneration = -1;

    /**
     * 已成型结构的配方处理器，收集一次而不是每刻扫描部件。它们惰性重建，因此缓存绝不会早于成型结构的部件存在。
     */
    private boolean handlerCacheDirty = true;
    private List<NotifiableEnergyContainer> energyInputs = List.of();
    private List<NotifiableFluidTank> fluidInputs = List.of();
    private List<NotifiableItemStackHandler> itemInputs = List.of();
    private List<NotifiableItemStackHandler> itemOutputs = List.of();

    /**
     * 显示界面上展示的产出预览，仅在种子、结构或肥料可用性变化时重建，而不是每次绘制界面都重建。
     */
    private List<Map.Entry<ItemStack, Double>> expectedDrops = List.of();
    private boolean expectedDropsValid = false;

    public IndustrialFarmMachine(IMachineBlockEntity holder) {
        super(holder);
        this.inventory = new NotifiableItemStackHandler(this, SLOT_COUNT, IO.NONE, IO.NONE, FarmInventory::new);
    }

    /**
     * 控制器的内部库存。种子槽与下层土壤槽的数量受种子容量限制，且只有输出模式才能把已存物品取回。
     */
    private class FarmInventory extends LargeItemStackHandler {

        FarmInventory(int size) {
            super(size);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_SEED || slot == SLOT_SUB_SOIL ? Math.max(1, seedCapacity) : 1;
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return IndustrialFarmMachine.this.isItemValid(slot, stack);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // 种子与下层土壤只能由输出模式取出
            if (slot < SLOT_ENV_START) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            // 任何槽位都可能被替换，解析后的种子定义会惰性重读
            IndustrialFarmMachine.this.seedDataValid = false;
            IndustrialFarmMachine.this.expectedDropsValid = false;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            super.deserializeNBT(nbt);
            // 同步内容直接写入槽位，绕过 onContentsChanged
            IndustrialFarmMachine.this.seedDataValid = false;
            IndustrialFarmMachine.this.expectedDropsValid = false;
        }
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    // ---- 参数 ----

    public int getUpgradeTier() {
        return this.upgradeTier;
    }

    public int getSeedCapacity() {
        return this.seedCapacity;
    }

    /**
     * @return 种子槽中存储的种子定义，惰性解析，并在背包或作物定义变化前复用。
     */
    @Nullable
    ISeedData getCachedSeedData() {
        int generation = CropRegistry.instance.getGeneration();
        if (this.seedDataValid && this.cachedRegistryGeneration == generation) return this.cachedSeedData;
        this.cachedSeedData = BlockEntityCropSticks.getSeedData(this.inventory.getStackInSlot(SLOT_SEED));
        this.cachedRegistryGeneration = generation;
        this.seedDataValid = true;
        return this.cachedSeedData;
    }

    /**
     * @return 显示界面上单次周期的平均掉落，种子、结构或肥料可用性变化前一直缓存。
     */
    List<Map.Entry<ItemStack, Double>> getExpectedDrops(ISeedData seedData, int seedCount) {
        if (this.expectedDropsValid) return this.expectedDrops;
        this.expectedDrops = IndustrialFarmDrops.getExpectedDropsPerCycle(seedData, seedCount,
                this.getGrowthProgressPerCycle(seedData), this.getHarvestRoundMultiplier());
        this.expectedDropsValid = true;
        return this.expectedDrops;
    }

    public int getMachineMode() {
        return this.machineMode;
    }

    public Status getStatus() {
        return Status.fromCode(this.status);
    }

    private void setStatus(Status status) {
        this.status = status.code();
    }

    /** @return 上次结构检查得到的种子床电压，用于解释被拒绝的结构。 */
    int getDetectedTier() {
        return this.detectedTier;
    }

    public int getCycleProgress() {
        return this.cycleProgress;
    }

    public int getEnvironmentalEnhancementUnitCount() {
        return this.environmentalEnhancementUnitCount;
    }

    public long getExpectedEUt() {
        return this.expectedEUt;
    }

    /** @return 每周期收获轮数的倍率。 */
    double getHarvestRoundMultiplier() {
        double multiplier = 1.0d + IndustrialFarmComponentBlock.getHarvestRoundBonus(this.upgradeTier);
        multiplier += this.fertilizerUnitCount * IndustrialFarmComponentBlock.FERTILIZER_HARVEST_ROUND_BONUS;
        multiplier *= 1.0d +
                this.advancedHarvestingUnitCount * IndustrialFarmComponentBlock.ADVANCED_HARVESTING_ROUND_MULTIPLIER;
        return multiplier;
    }

    /** @return 生长速度的倍率。 */
    private double getGrowthSpeedMultiplier() {
        double multiplier = 1.0d +
                this.growthAccelerationUnitCount * IndustrialFarmComponentBlock.GROWTH_ACCELERATION_SPEED_BONUS;
        multiplier *= 1.0d + this.fertilizerUnitCount * IndustrialFarmComponentBlock.FERTILIZER_SPEED_MULTIPLIER;
        multiplier *= this.expectedOCs <= 63 ? 1L << this.expectedOCs : Math.pow(2.0d, this.expectedOCs);
        return multiplier;
    }

    // ---- 结构 ----

    /**
     * 配方查看器的结构预览为每个电压各建一页。创建预览控制器方块实体时会把该页的电压告诉控制器，使该页可以使用只认识该电压方块的样板作为候选项；实际放置的控制器仍使用通用样板。
     */
    public void setPreviewTier(int tier) {
        this.previewTier = tier;
    }

    @Override
    public BlockPattern getPattern() {
        int tier = this.previewTier;
        if (tier >= MIN_TIER && tier <= MAX_TIER) {
            return PREVIEW_PATTERNS.computeIfAbsent(tier, value -> CropsGTMMachines
                    .createIndustrialFarmPattern((MultiblockMachineDefinition) self().getDefinition(), value));
        }
        return super.getPattern();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        // 新结构的部件可能提供不同的配方处理器
        this.handlerCacheDirty = true;
        this.ensureHandlerCache();
        this.refreshStructureData();
        this.expectedDropsValid = false;
    }

    @Override
    public void onStructureInvalid() {
        this.clearHandlerCache();
        this.expectedDropsValid = false;
        this.upgradeTier = -1;
        this.seedCapacity = 0;
        this.expectedEUt = 0;
        this.expectedOCs = 0;
        this.cycleProgress = 0;
        this.setStatus(Status.STRUCTURE_INVALID);
        super.onStructureInvalid();
    }

    /**
     * 把结构标记为无效，并为界面保留给定原因。
     * <p>
     * 直接调用父类实现，避免 {@link #onStructureInvalid()} 用通用原因覆盖该原因。
     * </p>
     */
    private void failStructure(Status reason) {
        super.onStructureInvalid();
        this.clearHandlerCache();
        this.expectedDropsValid = false;
        this.setStatus(reason);
    }

    /**
     * 从已成型结构的方块推导等级、升级模块数量与派生属性。组件与所需布局不符时结构会被判为无效。
     */
    private void refreshStructureData() {
        this.upgradeTier = -1;
        this.environmentalEnhancementUnitCount = 0;
        this.growthAccelerationUnitCount = 0;
        this.fertilizerUnitCount = 0;
        this.advancedHarvestingUnitCount = 0;
        this.overclockedGrowthAccelerationUnitCount = 0;
        this.seedCapacity = 0;
        this.expectedEUt = 0;
        this.expectedOCs = 0;

        Level level = getLevel();
        if (level == null) return;

        // 多方块缓存只保存带全局上限的谓词，即机壳
        // 外壳与种子床。它们的包围盒就是整个结构，因此每个组件都按
        // 通过扫描该盒体，而不是仅依赖缓存。
        this.detectedTier = -1;
        this.detectedSlices = 0;
        Set<BlockPos> cached = new HashSet<>(getMultiblockState().getCache());
        if (cached.isEmpty()) {
            failStructure(Status.STRUCTURE_INVALID);
            return;
        }
        IndustrialFarmStructure.Scan scan = IndustrialFarmStructure.scan(level, cached);
        this.environmentalEnhancementUnitCount = scan.environmentalUnits();
        this.growthAccelerationUnitCount = scan.growthAccelerationUnits();
        this.fertilizerUnitCount = scan.fertilizerUnits();
        this.advancedHarvestingUnitCount = scan.advancedHarvestingUnits();
        this.overclockedGrowthAccelerationUnitCount = scan.overclockedGrowthAccelerationUnits();

        // 检查顺序即玩家最先看到的内容：控制器背后的种子床决定
        // 整台机器的电压，然后结构本身必须正确搭建，只有
        // 然后升级模块的电压再与之比较。
        this.detectedTier = scan.seedBedTier();
        if (scan.seedBedCount() <= 0 || scan.seedBedTierMismatch() || scan.seedBedTier() < MIN_TIER ||
                scan.seedBedTier() > MAX_TIER) {
            failStructure(Status.STRUCTURE_INVALID);
            return;
        }
        this.upgradeTier = scan.seedBedTier();

        int slices = scan.seedBedCount() / SEED_BEDS_PER_SLICE;
        this.detectedSlices = slices;
        if (scan.seedBedCount() % SEED_BEDS_PER_SLICE != 0 ||
                slices != IndustrialFarmComponentBlock.getMultiLength(scan.seedBedTier())) {
            failStructure(Status.STRUCTURE_LENGTH_MISMATCH);
            return;
        }

        if (this.environmentalEnhancementUnitCount > IndustrialFarmComponentBlock.MAX_ENVIRONMENTAL_ENHANCEMENT_UNITS ||
                this.fertilizerUnitCount > IndustrialFarmComponentBlock.MAX_FERTILIZER_UNITS ||
                this.advancedHarvestingUnitCount > IndustrialFarmComponentBlock.MAX_ADVANCED_HARVESTING_UNITS ||
                (!CropsGTMConfig.alwaysHasOverclockedGrowthAcceleration &&
                        (this.overclockedGrowthAccelerationUnitCount >
                                IndustrialFarmComponentBlock.MAX_OVERCLOCKED_GROWTH_ACCELERATION_UNITS ||
                                (this.growthAccelerationUnitCount > 0 &&
                                        this.overclockedGrowthAccelerationUnitCount > 0)))) {
            failStructure(Status.STRUCTURE_INVALID);
            return;
        }

        // 玻璃必须承受种子床的电压，等级越高所需的
        // 玻璃必须
        if (!isGlassAllowed(scan.seedBedTier(), scan.vanillaGlass(), scan.temperedGlass(), scan.laminatedGlass(),
                scan.otherGlass())) {
            failStructure(Status.GLASS_TOO_WEAK);
            return;
        }

        // 升级模块必须使用与种子床相同的电压
        if (scan.upgradeUnitTierMismatch() ||
                (scan.upgradeUnitTier() >= 0 && scan.upgradeUnitTier() != scan.seedBedTier())) {
            failStructure(Status.COMPONENT_TIER_MISMATCH);
            return;
        }

        this.seedCapacity = IndustrialFarmComponentBlock.getCapacity(scan.seedBedTier());
        this.computePowerUsage();
        // 刚成型的机器尚未运行，由各模式处理器填写真实状态
        this.setStatus(Status.IDLE);
    }

    /**
     * 按种子床电压校验结构中的玻璃。
     * <p>
     * MV 与 HV 接受任意玻璃；EV 起只允许钢化玻璃或更好；IV 起只允许夹层玻璃或更好；UV 起只允许聚变玻璃。其他玻璃方块一律不允许。
     * </p>
     */
    private static boolean isGlassAllowed(int tier, int vanilla, int tempered, int laminated, int other) {
        if (other > 0) return false;
        if (vanilla > 0 && tier >= GTValues.EV) return false;
        if (tempered > 0 && tier >= GTValues.IV) return false;
        if (laminated > 0 && tier >= GTValues.UV) return false;
        return true;
    }

    /** 重新计算机器的 EU/t，包含超频生长加速模块。 */
    private void computePowerUsage() {
        long basePower = IndustrialFarmComponentBlock.getBaseEUt(this.upgradeTier);
        long powerUsage = basePower;
        powerUsage += (long) (basePower * IndustrialFarmComponentBlock.ENVIRONMENTAL_ENHANCEMENT_POWER_INCREASE *
                this.environmentalEnhancementUnitCount);
        powerUsage += (long) (basePower * IndustrialFarmComponentBlock.GROWTH_ACCELERATION_POWER_INCREASE *
                this.growthAccelerationUnitCount);
        powerUsage += (long) (basePower * IndustrialFarmComponentBlock.FERTILIZER_POWER_INCREASE *
                this.fertilizerUnitCount);
        powerUsage += (long) (basePower * IndustrialFarmComponentBlock.ADVANCED_HARVESTING_POWER_INCREASE *
                this.advancedHarvestingUnitCount);

        if (this.overclockedGrowthAccelerationUnitCount > 0 || CropsGTMConfig.alwaysHasOverclockedGrowthAcceleration) {
            long maxInput = this.getMaxInputEu();
            long consumption = powerUsage;
            int overclocks = 0;
            while (consumption <= maxInput / 4L) {
                consumption *= 4L;
                overclocks++;
            }
            this.expectedOCs = overclocks;
            this.expectedEUt = consumption;
        } else {
            this.expectedOCs = 0;
            this.expectedEUt = powerUsage;
        }
    }

    private long getMaxInputEu() {
        long eu = 0;
        for (NotifiableEnergyContainer container : this.energyInputs) {
            eu += container.getInputVoltage() * container.getInputAmperage();
        }
        return eu;
    }

    // ---- 生命周期 ----

    @Override
    public void onLoad() {
        super.onLoad();
        if (isRemote()) return;
        subscribeServerTick(this::onServerTick);
    }

    /**
     * 控制器被移除时，把已存种子、下层土壤与环境模块掉落到世界中。
     * <p>
     * 控制器不是 {@link com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine}，其内部库存不会写入机器物品，破坏或拆解时会丢失。原工业农场同样是直接掉落内容物。
     * </p>
     */
    @Override
    public void onMachineRemoved() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        for (int slot = 0; slot < this.inventory.storage.getSlots(); slot++) {
            ItemStack stack = this.inventory.storage.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            this.inventory.storage.setStackInSlot(slot, ItemStack.EMPTY);
            // 种子与下层土壤的存储量可能远超一组，因此被拆分成
            // 物品格式在这些物品被丢弃前能表达的数量
            int stackSize = Math.max(1, stack.getMaxStackSize());
            for (int remaining = stack.getCount(); remaining > 0; remaining -= stackSize) {
                Block.popResource(level, getPos(), stack.copyWithCount(Math.min(stackSize, remaining)));
            }
        }
    }

    private void onServerTick() {
        if (isRemote()) return;
        if (!isFormed()) {
            if (this.cycleProgress != 0) this.cycleProgress = 0;
            updateOverlayStatus();
            return;
        }
        // 处理器缓存只在结构变化后失效，否则这里只是一次标志检查
        this.ensureHandlerCache();
        int mode = this.machineMode;
        if (mode == MODE_INPUT) {
            if (getOffsetTimer() % 10 == 0) {
                // 状态始终描述这一 tick 的尝试，因此旧状态不会残留
                this.setStatus(Status.IDLE);
                this.ingestInputs();
            }
        } else if (mode == MODE_OUTPUT) {
            if (getOffsetTimer() % 5 == 0) {
                this.setStatus(Status.IDLE);
                this.ejectStored();
            }
        } else if (mode == MODE_FARM) {
            this.tickFarm();
        } else {
            this.machineMode = MODE_INPUT;
        }
        updateOverlayStatus();
    }

    /**
     * 把工作状态同步到 workable 覆盖层属性，方式与 GregTech 工作机器的配方逻辑一致。只有状态切换时才推送到渲染状态，因此属性变化触发的方块更新不会每刻执行。
     */
    private void updateOverlayStatus() {
        RecipeLogic.Status status;
        if (!isFormed() || this.machineMode != MODE_FARM) {
            status = RecipeLogic.Status.IDLE;
        } else if (!this.workingEnabled || this.getStatus() == Status.OUTPUT_FULL) {
            // 关闭的农场与已结束周期无法放入输出的农场使用
            // 暂停覆盖层，与挂起的 GregTech 机器完全相同
            status = RecipeLogic.Status.SUSPEND;
        } else {
            status = this.cycleProgress > 0 ? RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE;
        }

        // 工作音效跟随覆盖层，因此两者在同一次状态切换时同时起停
        boolean workingSound = status == RecipeLogic.Status.WORKING;
        if (this.workingSoundActive != workingSound) this.workingSoundActive = workingSound;

        MachineRenderState renderState = getRenderState();
        if (renderState.getValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS) == status) return;
        setRenderState(renderState.setValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS, status));
    }

    @Override
    public void clientTick() {
        super.clientTick();
        // 锤子解除静音后重新开始循环，与 GregTech 工作机器一致
        if (this.previouslyMuffled != this.muffled) {
            this.previouslyMuffled = this.muffled;
            updateWorkingSound();
        }
    }

    @SuppressWarnings("unused")
    private void onWorkingSoundSynced(boolean newValue, boolean oldValue) {
        if (isRemote()) updateWorkingSound();
    }

    /** @return 需要播放工作循环音效时返回 true。 */
    private boolean shouldPlayWorkingSound() {
        Level level = getLevel();
        return this.workingSoundActive && !this.muffled && ConfigHolder.INSTANCE.machines.machineSounds &&
                !isInValid() && level != null && level.isLoaded(getPos()) &&
                MetaMachine.getMachine(level, getPos()) == this;
    }

    /**
     * 开始或停止工作音效。GregTech 由工作机器的配方逻辑播放它；本机器有自己的周期，因此改由同步的工作标志驱动循环。
     */
    @OnlyIn(Dist.CLIENT)
    private void updateWorkingSound() {
        if (shouldPlayWorkingSound()) {
            SoundEntry sound = GTSoundEntries.FURNACE;
            if (this.workingSound instanceof AutoReleasedSound released) {
                if (released.soundEntry == sound && !released.isStopped()) return;
                released.release();
                this.workingSound = null;
            }
            this.workingSound = sound.playAutoReleasedSound(this::shouldPlayWorkingSound, getPos(), true, 0, 1, 1);
        } else if (this.workingSound instanceof AutoReleasedSound released) {
            released.release();
            this.workingSound = null;
        }
    }

    // ---- 输入模式 ----

    private boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (slot == SLOT_SEED || slot == SLOT_SUB_SOIL) {
            // 种子与下层土壤只能由输入模式放入
            return false;
        }
        int index = slot - SLOT_ENV_START;
        if (index < 0 || this.environmentalEnhancementUnitCount <= index) return false;
        if (!(stack.getItem() instanceof ItemEnvironmentalModule)) return false;
        return ItemEnvironmentalModule.getBiomeTag(stack) != null;
    }

    private void ingestInputs() {
        if (this.seedCapacity <= 0) return;

        ItemStack existing = this.inventory.getStackInSlot(SLOT_SEED);
        if (!existing.isEmpty()) {
            this.ingestIntoExisting(existing);
        } else {
            this.ingestNewSeeds();
        }
    }

    private void ingestIntoExisting(ItemStack existing) {
        if (existing.getCount() >= this.seedCapacity) {
            // 只有输入总线确实存有更多种子时才报告槽位已满
            if (this.countMatching(existing) > 0) this.setStatus(Status.SEEDS_FULL);
            return;
        }

        ISeedData seedData = BlockEntityCropSticks.getSeedData(existing);
        if (seedData == null) return;

        boolean needsSubSoil = this.requiresSubSoil(seedData.getCrop());
        ItemStack subSoil = this.inventory.getStackInSlot(SLOT_SUB_SOIL);
        ItemStack subSoilTemplate = null;
        if (needsSubSoil) {
            if (subSoil.isEmpty()) {
                subSoilTemplate = this.findSubSoilTemplate(seedData.getCrop());
                if (subSoilTemplate == null) {
                    this.setStatus(Status.SUB_SOIL_NOT_FOUND);
                    return;
                }
            } else if (!this.isValidSubSoil(seedData.getCrop(), subSoil)) {
                this.setStatus(Status.SUB_SOIL_MISMATCH);
                return;
            }
        }

        int seedSpace = this.seedCapacity - existing.getCount();
        int availableSeeds = this.countMatching(existing);
        int toInsert = Math.min(seedSpace, availableSeeds);
        if (needsSubSoil) {
            ItemStack template = subSoil.isEmpty() ? subSoilTemplate : subSoil;
            toInsert = Math.min(toInsert, this.countMatching(template));
        }
        if (toInsert <= 0) {
            // 空的输入总线不是错误，只有存在待处理种子却缺少下层土壤时才值得报告
            if (needsSubSoil && availableSeeds > 0) this.setStatus(Status.SUB_SOIL_NOT_FOUND);
            return;
        }

        int movedSeeds = this.consumeMatching(existing, toInsert);
        if (movedSeeds <= 0) return;

        if (needsSubSoil) {
            ItemStack template = subSoil.isEmpty() ? subSoilTemplate : subSoil;
            int storedSubSoil = subSoil.isEmpty() ? 0 : subSoil.getCount();
            int movedSubSoil = this.consumeMatching(template, movedSeeds);
            int totalSubSoil = storedSubSoil + movedSubSoil;
            this.inventory.setStackInSlot(SLOT_SUB_SOIL,
                    totalSubSoil <= 0 ? ItemStack.EMPTY : template.copyWithCount(totalSubSoil));
        }

        // 已存物品非空，因此其副本保留种子标签并获得被移动的标签
        this.inventory.setStackInSlot(SLOT_SEED, existing.copyWithCount(existing.getCount() + movedSeeds));
        this.setStatus(Status.STORING);
    }

    private void ingestNewSeeds() {
        ItemStack seedCandidate = null;
        for (NotifiableItemStackHandler bus : this.itemInputs) {
            for (int slot = 0; slot < bus.getSlots(); slot++) {
                ItemStack candidate = bus.getStackInSlot(slot);
                ISeedData seedData = BlockEntityCropSticks.getSeedData(candidate);
                if (seedData == null) continue;
                ICropCard crop = seedData.getCrop();
                if (crop.getMinSeedBedTier() > this.upgradeTier) {
                    this.setStatus(Status.SEED_BED_TIER_TOO_LOW);
                    continue;
                }
                if (this.getGrowthSpeedUnscaled(seedData, true) <= 0) {
                    this.setStatus(Status.CANNOT_GROW);
                    continue;
                }
                seedCandidate = candidate;
                break;
            }
            if (seedCandidate != null) break;
        }
        if (seedCandidate == null) return;

        ISeedData seedData = BlockEntityCropSticks.getSeedData(seedCandidate);
        if (seedData == null) return;

        ItemStack subSoilTemplate = null;
        if (this.requiresSubSoil(seedData.getCrop())) {
            ItemStack existingSubSoil = this.inventory.getStackInSlot(SLOT_SUB_SOIL);
            if (!existingSubSoil.isEmpty()) {
                if (!this.isValidSubSoil(seedData.getCrop(), existingSubSoil)) {
                    this.setStatus(Status.SUB_SOIL_MISMATCH);
                    return;
                }
                subSoilTemplate = existingSubSoil;
            } else {
                subSoilTemplate = this.findSubSoilTemplate(seedData.getCrop());
                if (subSoilTemplate == null) {
                    this.setStatus(Status.SUB_SOIL_NOT_FOUND);
                    return;
                }
            }
        }

        int availableSeeds = this.countMatching(seedCandidate);
        int amount = Math.min(this.seedCapacity, availableSeeds);
        if (subSoilTemplate != null) {
            int availableSubSoil = this.countMatching(subSoilTemplate);
            amount = Math.min(amount, availableSubSoil);
        }
        if (amount <= 0) return;

        // 模板刻意保持非空：copyWithCount 对数量为零会报告空物品
        // 为零，因此用空副本构建新内容会静默丢弃整个输入
        int movedSeeds = this.consumeMatching(seedCandidate, amount);
        if (movedSeeds <= 0) return;
        this.inventory.setStackInSlot(SLOT_SEED, seedCandidate.copyWithCount(movedSeeds));

        if (subSoilTemplate != null) {
            int alreadyStored = this.inventory.getStackInSlot(SLOT_SUB_SOIL).getCount();
            int movedSubSoil = this.consumeMatching(subSoilTemplate, movedSeeds);
            int totalSubSoil = alreadyStored + movedSubSoil;
            this.inventory.setStackInSlot(SLOT_SUB_SOIL,
                    totalSubSoil <= 0 ? ItemStack.EMPTY : subSoilTemplate.copyWithCount(totalSubSoil));
        }
        this.setStatus(Status.STORING);
    }

    private boolean requiresSubSoil(ICropCard crop) {
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (requirement instanceof SubSoilRequirement) return true;
        }
        return false;
    }

    /** @return 输入总线中满足该作物下层土壤需求的物品。 */
    private @Nullable ItemStack findSubSoilTemplate(ICropCard crop) {
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (!(requirement instanceof SubSoilRequirement subSoil)) continue;
            for (NotifiableItemStackHandler bus : this.itemInputs) {
                for (int slot = 0; slot < bus.getSlots(); slot++) {
                    ItemStack candidate = bus.getStackInSlot(slot);
                    BlockState state = getSubSoilState(candidate);
                    if (state == null || !subSoil.isValidSubSoil(state)) continue;
                    return candidate;
                }
            }
        }
        return null;
    }

    /** @return 已存下层土壤物品所代表的方块状态；不是方块时返回 null。 */
    private static @Nullable BlockState getSubSoilState(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) return null;
        return blockItem.getBlock().defaultBlockState();
    }

    private boolean isValidSubSoil(ICropCard crop, ItemStack stack) {
        BlockState state = getSubSoilState(stack);
        if (state == null) return false;
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (!(requirement instanceof SubSoilRequirement subSoil)) continue;
            if (subSoil.isValidSubSoil(state)) return true;
        }
        return false;
    }

    private int countMatching(ItemStack template) {
        int total = 0;
        for (NotifiableItemStackHandler bus : this.itemInputs) {
            for (int slot = 0; slot < bus.getSlots(); slot++) {
                ItemStack candidate = bus.getStackInSlot(slot);
                if (candidate.isEmpty() || !ItemStack.isSameItemSameTags(candidate, template)) continue;
                total += candidate.getCount();
            }
        }
        return total;
    }

    private int consumeMatching(ItemStack template, int amount) {
        int remaining = amount;
        for (NotifiableItemStackHandler bus : this.itemInputs) {
            for (int slot = 0; slot < bus.getSlots() && remaining > 0; slot++) {
                ItemStack candidate = bus.getStackInSlot(slot);
                if (candidate.isEmpty() || !ItemStack.isSameItemSameTags(candidate, template)) continue;
                ItemStack extracted = bus.extractItemInternal(slot, Math.min(remaining, candidate.getCount()), false);
                remaining -= extracted.getCount();
            }
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }

    // ---- 输出模式 ----

    private void ejectStored() {
        ItemStack seedStack = this.inventory.getStackInSlot(SLOT_SEED);
        ItemStack subSoilStack = this.inventory.getStackInSlot(SLOT_SUB_SOIL);
        if (seedStack.isEmpty() && subSoilStack.isEmpty()) {
            // 没有可弹出的内容，因此机器回退到中性状态
            this.setStatus(Status.IDLE);
            return;
        }

        int seedEjected = seedStack.isEmpty() ? 0 : this.eject(seedStack.copy(), seedStack.getCount());
        int subSoilEjected = subSoilStack.isEmpty() ? 0 : this.eject(subSoilStack.copy(), subSoilStack.getCount());

        if (!seedStack.isEmpty() && seedEjected <= 0 && !subSoilStack.isEmpty() && subSoilEjected <= 0) {
            this.setStatus(Status.OUTPUT_FULL);
            return;
        }

        if (!seedStack.isEmpty()) {
            int remaining = seedStack.getCount() - seedEjected;
            this.inventory.setStackInSlot(SLOT_SEED,
                    remaining <= 0 ? ItemStack.EMPTY : seedStack.copyWithCount(remaining));
        }
        if (!subSoilStack.isEmpty()) {
            int remaining = subSoilStack.getCount() - subSoilEjected;
            this.inventory.setStackInSlot(SLOT_SUB_SOIL,
                    remaining <= 0 ? ItemStack.EMPTY : subSoilStack.copyWithCount(remaining));
        }
        this.setStatus(Status.EJECTING);
    }

    /** @return 能放入输出总线的物品数量。 */
    private int eject(ItemStack stack, int amount) {
        int remaining = amount;
        for (NotifiableItemStackHandler bus : this.itemOutputs) {
            for (int slot = 0; slot < bus.getSlots() && remaining > 0; slot++) {
                remaining = bus.insertItemInternal(slot, stack.copyWithCount(remaining), false).getCount();
            }
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }

    // ---- 耕作模式 ----

    private void tickFarm() {
        if (!this.workingEnabled) {
            this.setStatus(Status.IDLE);
            return;
        }

        ItemStack seedStack = this.inventory.getStackInSlot(SLOT_SEED);
        ISeedData seedData = this.getCachedSeedData();
        if (seedData == null) {
            this.cycleProgress = 0;
            this.setStatus(Status.NO_SEED);
            return;
        }

        ICropCard crop = seedData.getCrop();
        if (seedStack.getCount() > this.seedCapacity) {
            this.cycleProgress = 0;
            this.setStatus(Status.SEED_OVERFLOW);
            return;
        }
        if (crop.getMinSeedBedTier() > this.upgradeTier) {
            this.cycleProgress = 0;
            this.setStatus(Status.SEED_BED_TIER_TOO_LOW);
            return;
        }
        if (!this.areGrowthRequirementsMet(seedData)) {
            this.cycleProgress = 0;
            this.setStatus(Status.CANNOT_GROW);
            return;
        }

        if (this.cycleProgress >= IndustrialFarmComponentBlock.getCycleDuration()) {
            // 等待掉落能放入输出总线，这种情况下进度会被保留
            if (!this.finishCycle(seedData, seedStack.getCount())) return;
            // 下一周期就在这一 tick 启动，因此机器从不会空闲
            // 两个完整周期
            this.cycleProgress = 0;
        }

        if (!this.consumeEnergy(this.expectedEUt)) {
            this.setStatus(Status.NO_ENERGY);
            return;
        }

        if (this.cycleProgress <= 0) {
            if (!this.startCycle(seedData)) return;
        }

        this.cycleProgress++;
        this.setStatus(Status.OK);
    }

    /** @return 周期能启动时返回 true，即水与肥料充足。 */
    private boolean startCycle(ISeedData seedData) {
        int waterNeeded = IndustrialFarmPotency.waterNeededPerCycle(this.upgradeTier, this.expectedOCs);
        if (IndustrialFarmPotency.availableWater(this.fluidInputs) < waterNeeded) {
            this.setStatus(Status.NOT_ENOUGH_WATER);
            return false;
        }

        int fertilizerNeeded = IndustrialFarmPotency.fertilizerNeededPerCycle(this.upgradeTier, this.expectedOCs);
        boolean hasFertilizer = IndustrialFarmPotency.availableFertilizer(this.fluidInputs, this.fertilizerUnitCount) >=
                fertilizerNeeded;
        if (this.fertilizerUnitCount > 0 && !hasFertilizer) {
            this.setStatus(Status.NOT_ENOUGH_FERTILIZER);
            return false;
        }
        this.hasFertilizer = hasFertilizer;
        // 预览使用当前周期的肥料加成
        this.expectedDropsValid = false;

        double progress = this.getGrowthProgressPerCycle(seedData);
        if (progress <= 0) {
            this.setStatus(Status.CANNOT_GROW);
            return false;
        }

        IndustrialFarmPotency.consumeWater(this.fluidInputs, waterNeeded);
        if (this.hasFertilizer) {
            IndustrialFarmPotency.consumeFertilizer(this.fluidInputs, fertilizerNeeded, this.fertilizerUnitCount);
        }
        // 调用方从周期第一刻开始计数，因此这里的进度从零开始
        this.cycleProgress = 0;
        this.setStatus(Status.OK);
        return true;
    }

    /** @return 产出的掉落能放入输出总线时返回 true。 */
    private boolean finishCycle(ISeedData seedData, int seedCount) {
        Level level = getLevel();
        if (level == null) return true;
        List<ItemStack> drops = IndustrialFarmDrops.getDropsPerCycle(seedData, seedCount,
                this.getGrowthProgressPerCycle(seedData), this.getHarvestRoundMultiplier(), level.random);
        if (drops.isEmpty()) return true;
        return this.insertDrops(drops);
    }

    private boolean insertDrops(List<ItemStack> drops) {
        List<NotifiableItemStackHandler> outputs = this.itemOutputs;
        if (outputs.isEmpty()) {
            this.setStatus(Status.OUTPUT_FULL);
            return false;
        }

        // 在任何插入之前确保所有掉落都能放下
        Map<NotifiableItemStackHandler, List<ItemStack>> simulated = new HashMap<>();
        for (NotifiableItemStackHandler bus : outputs) {
            List<ItemStack> copies = new ArrayList<>();
            for (int slot = 0; slot < bus.getSlots(); slot++) {
                copies.add(bus.getStackInSlot(slot).copy());
            }
            simulated.put(bus, copies);
        }

        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack drop : drops) {
            int left = drop.getCount();
            for (NotifiableItemStackHandler bus : outputs) {
                if (left <= 0) break;
                List<ItemStack> slots = simulated.get(bus);
                for (int slot = 0; slot < slots.size() && left > 0; slot++) {
                    ItemStack current = slots.get(slot);
                    // 真实插入使用相同的上限与过滤器，使模拟与插入
                    // 不会对某个槽位是否有空间产生分歧
                    int limit = Math.min(bus.getSlotLimit(slot), drop.getMaxStackSize());
                    if (current.isEmpty()) {
                        if (!bus.isItemValid(slot, drop)) continue;
                        int toAdd = Math.min(left, limit);
                        slots.set(slot, drop.copyWithCount(toAdd));
                        left -= toAdd;
                    } else if (ItemStack.isSameItemSameTags(current, drop)) {
                        int space = limit - current.getCount();
                        if (space <= 0) continue;
                        int toAdd = Math.min(space, left);
                        slots.set(slot, current.copyWithCount(current.getCount() + toAdd));
                        left -= toAdd;
                    }
                }
            }
            if (left > 0) remaining.add(drop.copyWithCount(left));
        }

        if (!remaining.isEmpty()) {
            this.setStatus(Status.OUTPUT_FULL);
            return false;
        }

        for (ItemStack drop : drops) {
            int left = drop.getCount();
            for (NotifiableItemStackHandler bus : outputs) {
                for (int slot = 0; slot < bus.getSlots() && left > 0; slot++) {
                    left = bus.insertItemInternal(slot, drop.copyWithCount(left), false).getCount();
                }
                if (left <= 0) break;
            }
        }
        return true;
    }

    boolean areGrowthRequirementsMet(ISeedData seedData) {
        ICropCard crop = seedData.getCrop();
        ItemStack subSoilStack = this.inventory.getStackInSlot(SLOT_SUB_SOIL);
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (requirement instanceof SubSoilRequirement subSoil) {
                BlockState state = getSubSoilState(subSoilStack);
                if (state == null || !subSoil.isValidSubSoil(state)) return false;
            }
        }
        return true;
    }

    // ---- 生长计算 ----

    /**
     * @return 作物在机器内部能获得的养分分，包含环境模块提供的生物群系标签。
     */
    private int getNutrientScore(ISeedData seedData) {
        Level level = getLevel();
        if (level == null) return 0;
        BlockPos pos = getPos();
        ICropCard crop = seedData.getCrop();

        Set<TagKey<Biome>> presentTags = new HashSet<>();
        Holder<Biome> biome = level.getBiome(pos);
        for (TagKey<Biome> tag : crop.getLikedBiomeTags()) {
            if (BiomeClimate.matches(biome, tag)) presentTags.add(tag);
        }
        for (int index = 0; index < this.environmentalEnhancementUnitCount; index++) {
            TagKey<Biome> tag = ItemEnvironmentalModule
                    .getBiomeTag(this.inventory.getStackInSlot(SLOT_ENV_START + index));
            if (tag != null && crop.getLikedBiomeTags().contains(tag)) presentTags.add(tag);
        }
        int likedBiomes = presentTags.size();

        float humidity = biome.value().getModifiedClimateSettings().downfall();
        int fertilizerStorage = this.hasFertilizer || this.fertilizerUnitCount > 0 ? SIMULATED_FERTILIZER_STORAGE : 0;
        return CropGrowthMath.getNutrientsPerCycle(likedBiomes, humidity, SIMULATED_CAN_SEE_SKY,
                SIMULATED_WATER_STORAGE, fertilizerStorage);
    }

    /** @return 作物在单个作物架上的生长速率。 */
    private int getGrowthSpeedUnscaled(ISeedData seedData, boolean assumeFertilizer) {
        boolean oldHasFertilizer = this.hasFertilizer;
        if (assumeFertilizer) this.hasFertilizer = true;
        try {
            return CropGrowthMath.getGrowthRate(this.getNutrientScore(seedData), seedData.getCrop().getTier(),
                    seedData.getStats().getGrowth());
        } finally {
            this.hasFertilizer = oldHasFertilizer;
        }
    }

    /**
     * @return 每个机器周期完成该作物生长周期的百分比；无法生长时返回小于零的值。
     */
    double getGrowthProgressPerCycle(ISeedData seedData) {
        int unscaledGrowthSpeed = this.getGrowthSpeedUnscaled(seedData, false);
        if (unscaledGrowthSpeed <= 0) return -1;
        int growthDuration = seedData.getCrop().getGrowthDuration();
        int growthTicksPerHarvest = ceilDiv(growthDuration, unscaledGrowthSpeed);
        return (1.0d / growthTicksPerHarvest) * IndustrialFarmComponentBlock.getCycleTickRateScalar() *
                this.getGrowthSpeedMultiplier() * CropsGTMConfig.industrialFarmGrowthMultiplier;
    }

    // ---- 辅助方法 ----

    private boolean consumeEnergy(long amount) {
        if (amount <= 0) return true;
        long remaining = amount;
        for (NotifiableEnergyContainer container : this.energyInputs) {
            long stored = container.getEnergyStored();
            if (stored <= 0) continue;
            long consumed = -container.changeEnergy(-Math.min(stored, remaining));
            remaining -= consumed;
            if (remaining <= 0) return true;
        }
        return false;
    }

    /** 结构自上次访问后发生变化时重建处理器缓存。 */
    private void ensureHandlerCache() {
        if (!this.handlerCacheDirty) return;
        this.handlerCacheDirty = false;
        this.energyInputs = this.collectHandlers(IO.IN, EURecipeCapability.CAP, NotifiableEnergyContainer.class);
        this.fluidInputs = this.collectHandlers(IO.IN, FluidRecipeCapability.CAP, NotifiableFluidTank.class);
        this.itemInputs = this.collectHandlers(IO.IN, ItemRecipeCapability.CAP, NotifiableItemStackHandler.class);
        this.itemOutputs = this.collectHandlers(IO.OUT, ItemRecipeCapability.CAP, NotifiableItemStackHandler.class);
    }

    /** 丢弃缓存的处理器，它们属于刚刚消失的结构。 */
    private void clearHandlerCache() {
        this.handlerCacheDirty = true;
        this.energyInputs = List.of();
        this.fluidInputs = List.of();
        this.itemInputs = List.of();
        this.itemOutputs = List.of();
    }

    private <T> List<T> collectHandlers(IO io, RecipeCapability<?> capability, Class<T> type) {
        List<T> handlers = new ArrayList<>();
        for (IRecipeHandler<?> handler : this.partHandlers(io, capability)) {
            if (type.isInstance(handler)) handlers.add(type.cast(handler));
        }
        return handlers;
    }

    /**
     * @return 多方块部件在指定方向上提供的、指定能力的配方处理器，与 GregTech 的配方逻辑收集方式一致。
     */
    private List<IRecipeHandler<?>> partHandlers(IO io, RecipeCapability<?> capability) {
        List<IRecipeHandler<?>> handlers = new ArrayList<>();
        for (IMultiPart part : getParts()) {
            for (RecipeHandlerList handlerList : part.getRecipeHandlers()) {
                if (!handlerList.isValid(io)) continue;
                handlers.addAll(handlerList.getCapability(capability));
            }
        }
        return handlers;
    }

    private static int ceilDiv(int dividend, int divisor) {
        return dividend / divisor + (dividend % divisor == 0 ? 0 : 1);
    }

    // ---- 能力接口 ----

    @Override
    public boolean isWorkingEnabled() {
        return this.workingEnabled;
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        this.workingEnabled = workingEnabled;
    }

    /**
     * 当前周期的进度。通过 {@link IWorkable} 暴露，使探针与配方查看器能为这台没有配方逻辑的机器显示进度条。
     */
    @Override
    public int getProgress() {
        return Math.min(this.cycleProgress, IndustrialFarmComponentBlock.getCycleDuration());
    }

    @Override
    public int getMaxProgress() {
        return IndustrialFarmComponentBlock.getCycleDuration();
    }

    @Override
    public boolean isActive() {
        // 已结束周期但无法放入输出的农场处于等待状态，而不是工作中
        return isFormed() && this.machineMode == MODE_FARM && this.workingEnabled && this.cycleProgress > 0 &&
                this.getStatus() != Status.OUTPUT_FULL;
    }

    @Override
    public boolean isMuffled() {
        return this.muffled;
    }

    @Override
    public void setMuffled(boolean muffled) {
        this.muffled = muffled;
    }

    /** 切换到指定模式，运行中的周期会被丢弃，因为它属于旧模式。 */
    public void setMachineMode(int mode) {
        if (mode < MODE_INPUT || mode > MODE_OUTPUT || this.machineMode == mode) return;
        this.machineMode = mode;
        this.cycleProgress = 0;
        this.setStatus(Status.IDLE);
    }

    /** 循环切换机器模式，正值向前、负值向后。 */
    public void cycleMode(int delta) {
        int next = this.machineMode + delta;
        if (next < MODE_INPUT) next = MODE_OUTPUT;
        if (next > MODE_OUTPUT) next = MODE_INPUT;
        this.machineMode = next;
        this.cycleProgress = 0;
        this.setStatus(Status.IDLE);
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (isRemote()) return InteractionResult.SUCCESS;
        this.cycleMode(playerIn.isShiftKeyDown() ? -1 : 1);
        playerIn.sendSystemMessage(Component.translatable("cropsgtm_chat.industrialFarm.mode.set",
                IndustrialFarmDisplay.getModeName(this.machineMode)));
        return InteractionResult.CONSUME;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(198, IndustrialFarmGui.UI_HEIGHT, this, entityPlayer)
                .widget(new FancyMachineUIWidget(this, 198, IndustrialFarmGui.UI_HEIGHT));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        IndustrialFarmDisplay.addDisplayText(this, textList);
        IDisplayUIMachine.super.addDisplayText(textList);
    }

    @Override
    public Widget createUIWidget() {
        return IndustrialFarmGui.createUIWidget(this);
    }
}
