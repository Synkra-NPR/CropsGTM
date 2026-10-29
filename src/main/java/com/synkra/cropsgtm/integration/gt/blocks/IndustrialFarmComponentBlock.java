package com.synkra.cropsgtm.integration.gt.blocks;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.integration.gt.machines.CropManagerMachine;
import com.synkra.cropsgtm.reference.Constants;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 用于搭建工业农场的带等级组件之一。
 * <p>
 * 组件类型与电压等级按方块实例固定，每个等级都注册为独立方块，使结构检查无需额外的方块状态属性即可识别已安装组件的等级。加成数值与原 CropNH 完全一致。
 * </p>
 */
public class IndustrialFarmComponentBlock extends Block {

    /** @return 单个工业农场周期的时长（刻），取自配置。 */
    public static int getCycleDuration() {
        return CropsGTMConfig.industrialFarmCycleTicks;
    }

    /** @return 一次作物架生长刻与一次工业农场周期之间的比例。 */
    public static double getCycleTickRateScalar() {
        return (double) getCycleDuration() / Constants.TICK_RATE;
    }

    /** 可安装进工业农场的组件类型。 */
    public enum ComponentType {

        SEED_BED("seedBed", GTValues.MV),
        FERTILIZER_UNIT("fertilizerUnit", GTValues.MV),
        GROWTH_ACCELERATION_UNIT("growthAccelerationUnit", GTValues.MV),
        ADVANCED_HARVESTING_UNIT("advancedHarvestingUnit", GTValues.MV),
        ENVIRONMENTAL_ENHANCEMENT_UNIT("environmentalEnhancementUnit", GTValues.MV),
        OVERCLOCKED_GROWTH_ACCELERATION_UNIT("overclockedGrowthAccelerationUnit", GTValues.ZPM);

        private final String langKey;
        private final int minTier;

        ComponentType(String langKey, int minTier) {
            this.langKey = langKey;
            this.minTier = minTier;
        }

        public String getLangKey() {
            return this.langKey;
        }

        public int getMinTier() {
            return this.minTier;
        }

        public int getMaxTier() {
            // GregTech CEu 没有 MAX 机器，组件止步于最后一个电力机器等级
            return GTValues.OpV;
        }

        /** @return 该组件的注册 id 前缀，例如 {@code seed_bed}。 */
        public String getIdPrefix() {
            StringBuilder builder = new StringBuilder();
            for (char c : this.langKey.toCharArray()) {
                if (Character.isUpperCase(c)) builder.append('_');
                builder.append(Character.toLowerCase(c));
            }
            return builder.toString();
        }
    }

    // ---- 种子床属性 ----

    /** 种子床容量的倍率。 */
    private static final double SEED_BED_SLOT_MULTIPLIER = 1.0d;
    /** 每个种子床等级提供的收获轮数加成。 */
    private static final double SEED_BED_HARVEST_ROUND_BONUS = 0.2d;

    /** @return 指定种子床等级下控制器可存储的种子与下层土壤方块数量。 */
    public static int getCapacity(int tier) {
        int diameter = CropManagerMachine.getHorizontalRadius(tier) * 2 + 1;
        return (int) ((diameter * diameter) * SEED_BED_SLOT_MULTIPLIER);
    }

    /** @return 指定种子床等级下结构应有的中间层数。 */
    public static int getMultiLength(int tier) {
        return tier - GTValues.MV + 1;
    }

    /** @return 指定种子床等级下每周期消耗的水效力。 */
    public static int getWaterConsumption(int tier) {
        return (int) Math.ceil(getCapacity(tier) * getCycleTickRateScalar());
    }

    /** @return 指定种子床等级下每周期消耗的肥料效力。 */
    public static int getFertilizerConsumption(int tier) {
        return getWaterConsumption(tier);
    }

    /** @return 指定种子床等级下工业农场的基础 EU/t。 */
    public static long getBaseEUt(int tier) {
        return GTValues.V[tier] * 30L / 32L;
    }

    /** @return 指定种子床等级的附加收获轮数加成。 */
    public static double getHarvestRoundBonus(int tier) {
        return tier * SEED_BED_HARVEST_ROUND_BONUS;
    }

    // ---- 升级模块属性 ----

    public static final int MAX_ENVIRONMENTAL_ENHANCEMENT_UNITS = 2;
    public static final double ENVIRONMENTAL_ENHANCEMENT_POWER_INCREASE = 0.5d;

    public static final double GROWTH_ACCELERATION_SPEED_BONUS = 1.0d;
    public static final double GROWTH_ACCELERATION_POWER_INCREASE = 1.25d;

    public static final int MAX_FERTILIZER_UNITS = 1;
    public static final double FERTILIZER_POWER_INCREASE = 0.5d;
    public static final double FERTILIZER_SPEED_MULTIPLIER = 0.5d;
    public static final double FERTILIZER_HARVEST_ROUND_BONUS = 0.5d;

    public static final int MAX_ADVANCED_HARVESTING_UNITS = 2;
    public static final double ADVANCED_HARVESTING_ROUND_MULTIPLIER = 0.2d;
    public static final double ADVANCED_HARVESTING_POWER_INCREASE = 0.5d;

    public static final int MAX_OVERCLOCKED_GROWTH_ACCELERATION_UNITS = 1;

    private final ComponentType type;
    private final int tier;

    public IndustrialFarmComponentBlock(ComponentType type, int tier) {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED)
                // 硬度与爆炸抗性与 GregTech 机壳、加热线圈一致
                .strength(5.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                // 组件通过 getDrops 自行掉落，而不是使用战利品表
                .noLootTable());
        this.type = type;
        this.tier = tier;
    }

    public ComponentType getType() {
        return this.type;
    }

    public int getTier() {
        return this.tier;
    }

    /**
     * 语言文件沿用包含 UMV 等级的旧电压编号，而本移植从现代编号开始。UXV 及以上整体错开一位。
     */
    public int getLangTier() {
        return this.tier >= GTValues.UXV ? this.tier + 1 : this.tier;
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable(this.getDescriptionId());
    }

    /**
     * 方块物品的名称来自方块，因此该键就是玩家在背包与配方查看器中看到的名称。
     */
    @Override
    public String getDescriptionId() {
        return "cropsgtm." + this.type.getLangKey() + "." + this.getLangTier() + ".name";
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        boolean advanced = flag.isAdvanced();
        // 这里刻意不用枚举 switch：生成的 $SwitchMap 类不支持热替换
        ComponentType type = this.type;
        if (type == ComponentType.SEED_BED) {
            this.addSeedBedTooltip(tooltip, advanced);
        } else if (type == ComponentType.ENVIRONMENTAL_ENHANCEMENT_UNIT) {
            this.addEnvironmentalTooltip(tooltip, advanced);
        } else if (type == ComponentType.GROWTH_ACCELERATION_UNIT) {
            this.addGrowthAccelerationTooltip(tooltip, advanced);
        } else if (type == ComponentType.FERTILIZER_UNIT) {
            this.addFertilizerTooltip(tooltip, advanced);
        } else if (type == ComponentType.ADVANCED_HARVESTING_UNIT) {
            this.addAdvancedHarvestingTooltip(tooltip, advanced);
        } else if (type == ComponentType.OVERCLOCKED_GROWTH_ACCELERATION_UNIT) {
            this.addOverclockedTooltip(tooltip);
        }
    }

    private void addSeedBedTooltip(List<Component> tooltip, boolean advanced) {
        tooltip.add(gray("cropsgtm_tooltip.seedBed.0"));
        tooltip.add(gray("cropsgtm_tooltip.seedBed.7"));
        tooltip.add(gray("cropsgtm_tooltip.seedBed.1", FormattingUtil.formatNumbers(getBaseEUt(this.tier)) + " EU/t",
                GTValues.VNF[this.tier]));
        tooltip.add(gray("cropsgtm_tooltip.seedBed.2", FormattingUtil.formatNumbers(getCapacity(this.tier))));
        tooltip.add(gray("cropsgtm_tooltip.seedBed.3",
                FormattingUtil.formatNumbers(getWaterConsumption(this.tier)) + " mB"));
        tooltip.add(gray("cropsgtm_tooltip.seedBed.4",
                FormattingUtil.formatNumbers(getFertilizerConsumption(this.tier)) + " mB"));
        tooltip.add(gray("cropsgtm_tooltip.seedBed.5" + (advanced ? ".adv" : ""),
                percentage(getHarvestRoundBonus(this.tier))));
        int length = getMultiLength(this.tier);
        tooltip.add(gray("cropsgtm_tooltip.seedBed.6." + (length == 1 ? "single" : "plural"),
                FormattingUtil.formatNumbers(length)));
    }

    private void addEnvironmentalTooltip(List<Component> tooltip, boolean advanced) {
        tooltip.add(gray("cropsgtm_tooltip.environmentalEnhancementUnit.0"));
        tooltip.add(gray("cropsgtm_tooltip.environmentalEnhancementUnit.1"));
        tooltip.add(gray("cropsgtm_tooltip.environmentalEnhancementUnit.3"));
        tooltip.add(gray("cropsgtm_tooltip.environmentalEnhancementUnit.2" + (advanced ? ".adv" : ""),
                percentage(ENVIRONMENTAL_ENHANCEMENT_POWER_INCREASE)));
        tooltip.add(gray("cropsgtm_tooltip.upgradeTierMustMatchSeedBed"));
        tooltip.add(gray("cropsgtm_tooltip.upgradeCountLimited", MAX_ENVIRONMENTAL_ENHANCEMENT_UNITS));
    }

    private void addGrowthAccelerationTooltip(List<Component> tooltip, boolean advanced) {
        tooltip.add(gray("cropsgtm_tooltip.growthAccelerationUnit.0"));
        tooltip.add(gray("cropsgtm_tooltip.growthAccelerationUnit.1" + (advanced ? ".adv" : ""),
                percentage(GROWTH_ACCELERATION_SPEED_BONUS)));
        tooltip.add(gray("cropsgtm_tooltip.growthAccelerationUnit.2" + (advanced ? ".adv" : ""),
                percentage(GROWTH_ACCELERATION_POWER_INCREASE)));
        tooltip.add(gray("cropsgtm_tooltip.growthAccelerationUnit.3"));
        tooltip.add(gray("cropsgtm_tooltip.upgradeTierMustMatchSeedBed"));
    }

    private void addFertilizerTooltip(List<Component> tooltip, boolean advanced) {
        tooltip.add(gray("cropsgtm_tooltip.fertilizerUnit.0"));
        tooltip.add(gray("cropsgtm_tooltip.fertilizerUnit.1",
                FormattingUtil.formatNumbers(getFertilizerConsumption(this.tier)) + " mB"));
        tooltip.add(gray("cropsgtm_tooltip.fertilizerUnit.2"));
        tooltip.add(gray("cropsgtm_tooltip.fertilizerUnit.3"));
        tooltip.add(gray("cropsgtm_tooltip.fertilizerUnit.4" + (advanced ? ".adv" : ""),
                percentage(FERTILIZER_SPEED_MULTIPLIER)));
        tooltip.add(gray("cropsgtm_tooltip.fertilizerUnit.5" + (advanced ? ".adv" : ""),
                percentage(FERTILIZER_HARVEST_ROUND_BONUS)));
        tooltip.add(gray("cropsgtm_tooltip.fertilizerUnit.6" + (advanced ? ".adv" : ""),
                percentage(FERTILIZER_POWER_INCREASE)));
        tooltip.add(gray("cropsgtm_tooltip.upgradeTierMustMatchSeedBed"));
        tooltip.add(gray("cropsgtm_tooltip.upgradeCountLimited", MAX_FERTILIZER_UNITS));
    }

    private void addAdvancedHarvestingTooltip(List<Component> tooltip, boolean advanced) {
        tooltip.add(gray("cropsgtm_tooltip.advancedHarvestingUnit.0"));
        tooltip.add(gray("cropsgtm_tooltip.advancedHarvestingUnit.1" + (advanced ? ".adv" : ""),
                percentage(ADVANCED_HARVESTING_ROUND_MULTIPLIER)));
        tooltip.add(gray("cropsgtm_tooltip.advancedHarvestingUnit.2" + (advanced ? ".adv" : ""),
                percentage(ADVANCED_HARVESTING_POWER_INCREASE)));
        tooltip.add(gray("cropsgtm_tooltip.upgradeTierMustMatchSeedBed"));
        tooltip.add(gray("cropsgtm_tooltip.upgradeCountLimited", MAX_ADVANCED_HARVESTING_UNITS));
    }

    private void addOverclockedTooltip(List<Component> tooltip) {
        tooltip.add(gray("cropsgtm_tooltip.overclockedGrowthAccelerationUnit.0"));
        tooltip.add(gray("cropsgtm_tooltip.overclockedGrowthAccelerationUnit.1"));
        tooltip.add(gray("cropsgtm_tooltip.overclockedGrowthAccelerationUnit.2"));
        tooltip.add(gray("cropsgtm_tooltip.overclockedGrowthAccelerationUnit.3"));
        tooltip.add(gray("cropsgtm_tooltip.upgradeTierMustMatchSeedBed"));
        tooltip.add(gray("cropsgtm_tooltip.upgradeCountLimited", MAX_OVERCLOCKED_GROWTH_ACCELERATION_UNITS));
    }

    private static Component gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    private static String percentage(double value) {
        StringBuilder builder = new StringBuilder();
        long hundredths = Math.round(value * 10000.0d);
        builder.append(hundredths / 100);
        long remainder = hundredths % 100;
        if (remainder != 0) {
            builder.append('.');
            if (remainder < 10) builder.append('0');
            builder.append(remainder);
        }
        builder.append('%');
        return builder.toString();
    }
}
