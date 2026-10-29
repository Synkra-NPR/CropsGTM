package com.synkra.cropsgtm.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import com.synkra.cropsgtm.reference.Names;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 本模组的 Forge 配置。
 * <p>
 * 原 CropsNH 通过配置暴露的每个玩法数值在这里都可用，默认值与原值一致，未改动配置时的行为与原先硬编码常量完全相同。配置加载或重载时会快照到普通字段，热路径从不直接读取配置值。
 * </p>
 */
@Mod.EventBusSubscriber(modid = Names.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CropsGTMConfig {

    private CropsGTMConfig() {}

    public static final ForgeConfigSpec SPEC;

    // ---- 快照 ----

    /** 单个工业农场周期的时长（刻），不改变每刻产量。 */
    public static int industrialFarmCycleTicks = 100;
    /** 工业农场掉落的倍率。 */
    public static double industrialFarmOutputMultiplier = 1.0d;
    /** 工业农场生长速度的倍率。 */
    public static double industrialFarmGrowthMultiplier = 1.0d;
    /** 工业农场是否始终视为安装了超频生长加速模块。 */
    public static boolean alwaysHasOverclockedGrowthAcceleration = false;

    /** 作物架掉落的倍率。 */
    public static double cropStickOutputMultiplier = 1.0d;
    /** 作物架生长速度的倍率。 */
    public static double cropStickGrowthMultiplier = 1.0d;

    /** 所有水流体效力值的倍率。 */
    public static double waterPotencyMultiplier = 1.0d;
    /** 每次操作消耗水量的倍率。 */
    public static double waterConsumptionMultiplier = 1.0d;
    /** 所有肥料流体与肥料物品效力值的倍率。 */
    public static double fertilizerPotencyMultiplier = 1.0d;
    /** 每次操作消耗肥料量的倍率。 */
    public static double fertilizerConsumptionMultiplier = 1.0d;
    /** 流体与喷雾罐除草剂效力值的倍率。 */
    public static double weedExPotencyMultiplier = 1.0d;
    /** 每次操作消耗除草剂量的倍率。 */
    public static double weedExConsumptionMultiplier = 1.0d;

    /** 杂草是否可以生成与蔓延。 */
    public static boolean disableWeeds = false;
    /** 作物是否会患病并把病害传播给相邻作物。 */
    public static boolean disableDisease = false;
    /**
     * 配置中禁用的作物 id，从配置文件快照而来。
     * <p>
     * 物品与流体注册发生在 Forge 加载配置之前，因此注册流程通过 {@code CropAvailability} 直接读取磁盘上的配置文件，而不是使用该快照。
     * </p>
     */
    public static Set<String> disabledCrops = Set.of();
    /** 种子生长属性可达到的最高值。 */
    public static int maxGrowthStat = 31;
    /** 种子收益属性可达到的最高值。 */
    public static int maxGainStat = 31;
    /** 种子抗性属性可达到的最高值。 */
    public static int maxResistanceStat = 31;

    /** 所有作物始终拥有的养分点。 */
    public static int baseNutrientValue = 5;
    /** 水养分加成停止增长时的储水量。 */
    public static int maxWaterBonusAt = 100;
    /** 肥料养分加成停止增长的肥料储量。 */
    public static int maxFertilizerBonusAt = 100;
    /** 作物能看到天空时提供的养分点。 */
    public static int skyAccessBonus = 2;
    /** 当前生物群系的每个偏好标签所提供的养分点。 */
    public static int likedBiomeBonus = 14;
    /** 计入统计的偏好生物群系标签数量上限。 */
    public static int maxLikedBiomes = 2;
    /** 超过该湿度后开始计入湿度加成。 */
    public static float lowHumidityThreshold = 0.5f;
    /** 湿度加成达到最大值时的湿度。 */
    public static float highHumidityThreshold = 0.8f;
    /** 完全湿润的生物群系所提供的养分点。 */
    public static int highHumidityBonus = 14;
    /** 养分点的缩放系数，数值越高作物生长越快。 */
    public static int nutrientPointScale = 5;
    /** 每个作物等级所需的养分点。 */
    public static int nutrientsPerTier = 10;

    private static final ForgeConfigSpec.IntValue CYCLE_TICKS;
    private static final ForgeConfigSpec.DoubleValue FARM_OUTPUT;
    private static final ForgeConfigSpec.DoubleValue FARM_GROWTH;
    private static final ForgeConfigSpec.BooleanValue FARM_ALWAYS_OVERCLOCKED;
    private static final ForgeConfigSpec.DoubleValue STICK_OUTPUT;
    private static final ForgeConfigSpec.DoubleValue STICK_GROWTH;
    private static final ForgeConfigSpec.DoubleValue WATER_POTENCY;
    private static final ForgeConfigSpec.DoubleValue WATER_CONSUMPTION;
    private static final ForgeConfigSpec.DoubleValue FERTILIZER_POTENCY;
    private static final ForgeConfigSpec.DoubleValue FERTILIZER_CONSUMPTION;
    private static final ForgeConfigSpec.DoubleValue WEED_EX_POTENCY;
    private static final ForgeConfigSpec.DoubleValue WEED_EX_CONSUMPTION;
    private static final ForgeConfigSpec.BooleanValue DISABLE_WEEDS;
    private static final ForgeConfigSpec.BooleanValue DISABLE_DISEASE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_CROPS;
    private static final ForgeConfigSpec.IntValue MAX_GROWTH_STAT;
    private static final ForgeConfigSpec.IntValue MAX_GAIN_STAT;
    private static final ForgeConfigSpec.IntValue MAX_RESISTANCE_STAT;
    private static final ForgeConfigSpec.IntValue BASE_NUTRIENT_VALUE;
    private static final ForgeConfigSpec.IntValue MAX_WATER_BONUS_AT;
    private static final ForgeConfigSpec.IntValue MAX_FERTILIZER_BONUS_AT;
    private static final ForgeConfigSpec.IntValue SKY_ACCESS_BONUS;
    private static final ForgeConfigSpec.IntValue LIKED_BIOME_BONUS;
    private static final ForgeConfigSpec.IntValue MAX_LIKED_BIOMES;
    private static final ForgeConfigSpec.DoubleValue LOW_HUMIDITY_THRESHOLD;
    private static final ForgeConfigSpec.DoubleValue HIGH_HUMIDITY_THRESHOLD;
    private static final ForgeConfigSpec.IntValue HIGH_HUMIDITY_BONUS;
    private static final ForgeConfigSpec.IntValue NUTRIENT_POINT_SCALE;
    private static final ForgeConfigSpec.IntValue NUTRIENTS_PER_TIER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("工业农场的设置。", "Settings of the industrial farm multiblock.").push("industrial_farm");
        CYCLE_TICKS = builder.comment("工业农场单个工作周期的时长（tick），只改变运行节奏，不影响产率。",
                "Duration of a single industrial farm cycle in ticks, changes the pacing but not the yield per tick.")
                .defineInRange("cycleTicks", 100, 20, 72000);
        FARM_OUTPUT = builder
                .comment("工业农场产出系数，作用于收获到的所有物品。", "Multiplier applied to everything the industrial farm harvests.")
                .defineInRange("outputMultiplier", 1.0d, 0.0d, 100.0d);
        FARM_GROWTH = builder.comment("工业农场生长速率系数。", "Multiplier applied to the growth speed of the industrial farm.")
                .defineInRange("growthRateMultiplier", 1.0d, 0.0d, 100.0d);
        FARM_ALWAYS_OVERCLOCKED = builder.comment("开启后工业农场始终视为安装了超频生长加速模块，即使结构里没有该组件。",
                "When enabled the industrial farm always counts as if an overclocked growth acceleration unit is installed.")
                .define("alwaysOverclocked", false);
        builder.pop();

        builder.comment("作物架的设置。", "Settings of the crop sticks.").push("crop_sticks");
        STICK_OUTPUT = builder
                .comment("作物架产出系数，作用于收获到的所有物品。", "Multiplier applied to everything a crop stick harvests.")
                .defineInRange("outputMultiplier", 1.0d, 0.0d, 100.0d);
        STICK_GROWTH = builder.comment("作物架生长速率系数。", "Multiplier applied to the growth speed of the crop sticks.")
                .defineInRange("growthRateMultiplier", 1.0d, 0.0d, 100.0d);
        builder.pop();

        builder.comment("机器与物品所使用的流体的效力与消耗。", "Potency and consumption of the fluids the machines and items use.")
                .push("fluids");
        WATER_POTENCY = builder.comment("所有水流体的效力系数，数值越高每 mB 提供的水越多。",
                "Multiplier applied to the water potency of every water fluid, higher values give more water per mB.")
                .defineInRange("waterPotencyMultiplier", 1.0d, 0.01d, 100.0d);
        WATER_CONSUMPTION = builder
                .comment("每次操作的水消耗速率系数，数值越高耗水越多。",
                        "Multiplier applied to the water consumed per operation, higher values drain more water.")
                .defineInRange("waterConsumptionMultiplier", 1.0d, 0.01d, 100.0d);
        FERTILIZER_POTENCY = builder
                .comment("所有肥料流体与肥料的效力系数，数值越高提供的肥力越多。",
                        "Multiplier applied to the fertilizer potency of every fertilizer fluid and item.")
                .defineInRange("fertilizerPotencyMultiplier", 1.0d, 0.01d, 100.0d);
        FERTILIZER_CONSUMPTION = builder
                .comment("每次操作的肥料消耗速率系数，数值越高消耗越多。", "Multiplier applied to the fertilizer consumed per operation.")
                .defineInRange("fertilizerConsumptionMultiplier", 1.0d, 0.01d, 100.0d);
        WEED_EX_POTENCY = builder
                .comment("除草剂流体与喷雾罐的效力系数。",
                        "Multiplier applied to the weed-ex potency of the fluid and of the spray cans.")
                .defineInRange("weedExPotencyMultiplier", 1.0d, 0.01d, 100.0d);
        WEED_EX_CONSUMPTION = builder
                .comment("每次操作的除草剂消耗速率系数。", "Multiplier applied to the weed-ex consumed per operation.")
                .defineInRange("weedExConsumptionMultiplier", 1.0d, 0.01d, 100.0d);
        builder.pop();

        builder.comment("农作物的行为设置。", "Settings of the crop behaviour.").push("crops");
        DISABLE_WEEDS = builder.comment("开启后杂草不再生成也不再蔓延。", "When enabled weeds never spawn and never spread.")
                .define("disableWeeds", false);
        DISABLE_DISEASE = builder
                .comment("开启后作物不再生病，疾病也不再传染。", "When enabled crops never get sick and diseases never spread.")
                .define("disableDisease", false);
        DISABLED_CROPS = builder.comment("要禁用的作物 id 列表，例如 [\"cropsgtm:platina\"]。禁用的作物不会注册，依赖该作物的物品、流体和配方也不会注册。",
                "Crop ids to disable, for example [\"cropsgtm:platina\"]. Disabled crops are not registered, and the items, fluids and recipes that depend on them are not registered either.")
                .defineList("disabledCrops", List.of(), o -> o instanceof String);
        MAX_GROWTH_STAT = builder.comment("种子生长属性的最大值（1~31）。", "The highest value the growth stat of a seed can reach.")
                .defineInRange("maxGrowthStat", 31, 1, 31);
        MAX_GAIN_STAT = builder.comment("种子产量属性的最大值（1~31）。", "The highest value the gain stat of a seed can reach.")
                .defineInRange("maxGainStat", 31, 1, 31);
        MAX_RESISTANCE_STAT = builder
                .comment("种子抗性属性的最大值（1~31）。", "The highest value the resistance stat of a seed can reach.")
                .defineInRange("maxResistanceStat", 31, 1, 31);
        builder.pop();

        builder.comment("作物从各个方面获得的营养值。", "The nutrient points every crop gets from the different sources.")
                .push("nutrients");
        BASE_NUTRIENT_VALUE = builder.comment("所有作物都拥有的基础营养值。", "Nutrient points every crop always has.")
                .defineInRange("baseValue", 5, 0, 10000);
        MAX_WATER_BONUS_AT = builder
                .comment("储水量达到该值后不再提供额外的水营养值。", "The water storage at which the water nutrient bonus stops growing.")
                .defineInRange("waterBonusMax", 100, 1, 100000);
        MAX_FERTILIZER_BONUS_AT = builder
                .comment("肥料量达到该值后不再提供额外的肥料营养值。",
                        "The fertilizer storage at which the fertilizer nutrient bonus stops growing.")
                .defineInRange("fertilizerBonusMax", 100, 1, 100000);
        SKY_ACCESS_BONUS = builder
                .comment("作物能直接看到天空时提供的营养值。", "Nutrient points awarded when the crop can see the sky.")
                .defineInRange("skyAccessBonus", 2, 0, 10000);
        LIKED_BIOME_BONUS = builder
                .comment("当前群系每命中一个喜好标签提供的营养值。", "Nutrient points awarded per liked biome tag of the current biome.")
                .defineInRange("likedBiomeBonus", 14, 0, 10000);
        MAX_LIKED_BIOMES = builder.comment("喜好群系标签最多计数的个数。", "The maximum number of liked biome tags that are counted.")
                .defineInRange("maxLikedBiomes", 2, 0, 64);
        LOW_HUMIDITY_THRESHOLD = builder
                .comment("湿度高于该值才开始提供湿度营养加成。", "The humidity above which the humidity bonus starts to apply.")
                .defineInRange("lowHumidityThreshold", 0.5d, 0.0d, 1.0d);
        HIGH_HUMIDITY_THRESHOLD = builder
                .comment("湿度达到该值时湿度营养加成达到上限。", "The humidity at which the humidity bonus reaches its maximum.")
                .defineInRange("highHumidityThreshold", 0.8d, 0.0d, 1.0d);
        HIGH_HUMIDITY_BONUS = builder.comment("湿度拉满时提供的营养值。", "The nutrient points a fully humid biome is worth.")
                .defineInRange("highHumidityBonus", 14, 0, 10000);
        NUTRIENT_POINT_SCALE = builder
                .comment("营养值的放大倍数，数值越高作物长得越快。",
                        "The factor by which nutrient points are scaled, higher values make crops grow faster.")
                .defineInRange("nutrientPointScale", 5, 1, 1000);
        NUTRIENTS_PER_TIER = builder.comment("每级作物所需的营养值。", "The nutrient points required per crop tier.")
                .defineInRange("nutrientsPerTier", 10, 1, 10000);
        builder.pop();

        SPEC = builder.build();
    }

    @SubscribeEvent
    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        industrialFarmCycleTicks = CYCLE_TICKS.get();
        industrialFarmOutputMultiplier = FARM_OUTPUT.get();
        industrialFarmGrowthMultiplier = FARM_GROWTH.get();
        alwaysHasOverclockedGrowthAcceleration = FARM_ALWAYS_OVERCLOCKED.get();
        cropStickOutputMultiplier = STICK_OUTPUT.get();
        cropStickGrowthMultiplier = STICK_GROWTH.get();
        waterPotencyMultiplier = WATER_POTENCY.get();
        waterConsumptionMultiplier = WATER_CONSUMPTION.get();
        fertilizerPotencyMultiplier = FERTILIZER_POTENCY.get();
        fertilizerConsumptionMultiplier = FERTILIZER_CONSUMPTION.get();
        weedExPotencyMultiplier = WEED_EX_POTENCY.get();
        weedExConsumptionMultiplier = WEED_EX_CONSUMPTION.get();
        disableWeeds = DISABLE_WEEDS.get();
        disableDisease = DISABLE_DISEASE.get();
        disabledCrops = DISABLED_CROPS.get().stream().map(Object::toString).collect(Collectors.toUnmodifiableSet());
        maxGrowthStat = MAX_GROWTH_STAT.get();
        maxGainStat = MAX_GAIN_STAT.get();
        maxResistanceStat = MAX_RESISTANCE_STAT.get();
        baseNutrientValue = BASE_NUTRIENT_VALUE.get();
        maxWaterBonusAt = MAX_WATER_BONUS_AT.get();
        maxFertilizerBonusAt = MAX_FERTILIZER_BONUS_AT.get();
        skyAccessBonus = SKY_ACCESS_BONUS.get();
        likedBiomeBonus = LIKED_BIOME_BONUS.get();
        maxLikedBiomes = MAX_LIKED_BIOMES.get();
        lowHumidityThreshold = LOW_HUMIDITY_THRESHOLD.get().floatValue();
        highHumidityThreshold = HIGH_HUMIDITY_THRESHOLD.get().floatValue();
        highHumidityBonus = HIGH_HUMIDITY_BONUS.get();
        nutrientPointScale = NUTRIENT_POINT_SCALE.get();
        nutrientsPerTier = NUTRIENTS_PER_TIER.get();
    }

    /**
     * 用某个流体倍率缩放效力值。
     * 
     * @return 缩放后的效力，且不会低于 1，使配置过的流体始终有效
     */
    public static int scalePotency(int potency, double multiplier) {
        return Math.max(1, (int) Math.round(potency * multiplier));
    }

    /** @return 每次操作消耗的单位数，按给定倍率缩放。 */
    public static int scaleConsumption(int units, double multiplier) {
        return Math.max(1, (int) Math.round(units * multiplier));
    }
}
