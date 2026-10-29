package com.synkra.cropsgtm.reference;

/**
 * Crops NH 使用的常量与默认值。
 */
public final class Constants {

    private Constants() {}

    /** 任意种子属性可达到的最小值。 */
    public static final byte MIN_SEED_STAT = 1;

    /** 电压等级名称，下标即 GregTech 电压等级。 */
    public static final String[] VOLTAGE_NAMES = { "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv",
            "uev", "uiv", "uxv", "opv" };

    /** 杂草开始向相邻作物架蔓延所需的收益属性。 */
    public static final int SPREAD_WEED_WHEN_GROWTH_AT_DEFAULT = 24;

    /** 灌满一个除草剂喷雾罐所需的除草剂流体量。 */
    public static final int WEEDEX_CAPACITY = 750;
    /** 单个肥料物品代表的液态肥料量。 */
    public static final int FERTILIZER_ITEM_FLUID_VALUE = 144;

    /** 每 mB 除草剂流体提供的除草剂点数，数据包未定义时使用该值。 */
    public static final int WEEDEX_POTENCY_PER_MB = 5;
    /** 使用一次除草剂喷雾罐所施加的除草剂点数。 */
    public static final int WEEDEX_POTENCY_PER_USE = 50;
    /** 满罐除草剂喷雾罐的可用次数。 */
    public static final int WEEDEX_USES_PER_CAN = 75;
    /** 缺少 GregTech 时，由一瓶剧毒药水制成的除草剂罐可用次数。 */
    public static final int WEEDEX_USES_NO_GT = 25;
    /** 满罐植物治疗喷雾剂的可用次数。 */
    public static final int PLANT_CURE_USES = 100;
    /** 缺少 GregTech 时，由一瓶治疗药水制成的植物治疗剂可用次数。 */
    public static final int PLANT_CURE_USES_NO_GT = 25;
    /** 施用除草剂前作物架所需的可用空间。 */
    public static final int WEEDEX_FILL_THRESHOLD = 90;
    /** 作物架可容纳的最大除草剂量。 */
    public static final int WEEDEX_MAX_STORAGE = 100;

    /** 洒水壶可容纳的最大水量。 */
    public static final int WATERING_CAN_MAX_WATER = 3600;

    /** 强化洒水壶可存储的最大水量。 */
    public static final int WATERING_CAN_REINFORCED_MAX_WATER = 10000;
    /** 一次洒水壶用量消耗的水量。 */
    public static final int WATERING_CAN_WATER_PER_USE = 10;

    /** 范围内没有作物架时，水壶每次浇水刻消耗的水量。 */
    public static final int WATERING_CAN_IDLE_DRAIN = 20;

    /** 强化洒水壶浇灌区域的半径。 */
    public static final int WATERING_CAN_REINFORCED_RADIUS = 2;

    /** 超强化洒水壶可存储的最大水量。 */
    public static final int WATERING_CAN_SUPER_REINFORCED_MAX_WATER = 50000;
    /** 超强化洒水壶每次浇水刻给予每个作物的水量。 */
    public static final int WATERING_CAN_SUPER_REINFORCED_WATER_PER_USE = 20;
    /** 超强化洒水壶浇灌区域的半径。 */
    public static final int WATERING_CAN_SUPER_REINFORCED_RADIUS = 2;

    /** 究极强化洒水壶每次浇水刻给予每个作物的水量。 */
    public static final int WATERING_CAN_ULTRA_REINFORCED_WATER_PER_USE = 30;
    /** 究极强化洒水壶浇灌区域的半径。 */
    public static final int WATERING_CAN_ULTRA_REINFORCED_RADIUS = 3;
    /** 洒水壶围绕点击方块浇灌的半径（方块）。 */
    public static final int WATERING_CAN_RADIUS = 1;

    /** 原版花朵掉落的染料部分的概率（万分之一）。 */
    public static final int FLOWER_DYE_DROP_CHANCE = 7500;
    /** 原版花朵掉落的物品部分的概率（万分之一）。 */
    public static final int FLOWER_ITEM_DROP_CHANCE = 2500;

    /** 两次作物生长更新之间的刻数。 */
    public static final int TICK_RATE = 256;

    /** 生长速度的最小标量值。 */
    public static final int BASE_GROWTH_SPEED = 6;

    /** 手工填满时作物架可容纳的最大肥料量。 */
    public static final int MANUAL_FERTILIZER_MAX_STORAGE = 100;
    /** 手工追加肥料时所需的最小可用空间。 */
    public static final int MANUAL_FERTILIZER_MIN_THRESHOLD = 10;
    /** 作物架可容纳的最大水量。 */
    public static final int MAX_WATER_STORAGE = 100;

    /** 双作物架每次生长刻尝试繁殖的概率（N 分之一）。 */
    public static final int BREEDING_CHANCE = 3;
    /** 空作物架每次生长刻生成杂草的概率（N 分之一）。 */
    public static final int WEED_SPAWN_CHANCE = 100;
    /** 用于掷杂草蔓延的数值。 */
    public static final int WEED_SPREAD_CHANCE = 50;
    /** 杂草是否可以覆盖已种植的作物。 */
    public static final boolean WEEDS_WIPE_PLANTS = true;

    /** 蔓延时使用中属性浮动所需的最低肥料量。 */
    public static final int MED_FERTILIZER_SPREADING_THRESHOLD = 50;
    /** 蔓延时使用高属性浮动所需的最低肥料量。 */
    public static final int HIGH_FERTILIZER_SPREADING_THRESHOLD = MANUAL_FERTILIZER_MAX_STORAGE + 1;
    /** 繁殖时使用中属性浮动所需的最低肥料量。 */
    public static final int MED_FERTILIZER_BREEDING_THRESHOLD = 50;
    /** 繁殖时使用高属性浮动所需的最低肥料量。 */
    public static final int HIGH_FERTILIZER_BREEDING_THRESHOLD = MANUAL_FERTILIZER_MAX_STORAGE + 1;

    public static final int[] LOW_FERTILIZER_SPREADING_VARIATIONS = { -1, 0, 0 };
    public static final int[] MED_FERTILIZER_SPREADING_VARIATIONS = { 0 };
    public static final int[] HIGH_FERTILIZER_SPREADING_VARIATIONS = { 0, 0, 0, 1, 2, 3, 4 };
    public static final int[] LOW_FERTILIZER_BREEDING_VARIATIONS = { -1, -1, 0, 1, 1 };
    public static final int[] MED_FERTILIZER_BREEDING_VARIATIONS = { -2, -1, 0, 1, 2 };
    public static final int[] HIGH_FERTILIZER_BREEDING_VARIATIONS = { 0, 0, 0, 1, 2, 3, 4 };

    /** 作物可被踩踏前的最小下落高度。 */
    public static final float MIN_FALL_DISTANCE_FOR_TRAMPLING = 0.75f;
}
