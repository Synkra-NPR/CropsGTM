package com.synkra.cropsgtm.farming;

import net.minecraft.util.RandomSource;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.reference.Constants;

import java.util.Collection;
import java.util.function.ToIntFunction;

/**
 * 只依赖作物定义、种子属性与配置的作物计算。
 * <p>
 * 由作物架、工业农场与配方查看器的预览共用。
 * </p>
 */
public final class CropGrowthMath {

    private CropGrowthMath() {}

    /**
     * 预先算好的 {@code 1.03^gain}；收益属性被限制在很小的范围内，在收获与预览路径上查表比调用 {@link Math#pow} 更省。
     */
    private static final double[] GAIN_DROP_MULTIPLIER = new double[CropsGTMConfig.maxGainStat + 1];

    static {
        for (int gain = 0; gain < GAIN_DROP_MULTIPLIER.length; gain++) {
            GAIN_DROP_MULTIPLIER[gain] = Math.pow(1.03D, gain);
        }
    }

    public static double getAvgDropRounds(ICropCard crop, int gain) {
        int index = Math.max(0, Math.min(GAIN_DROP_MULTIPLIER.length - 1, gain));
        return crop.getBaseDropRounds() * GAIN_DROP_MULTIPLIER[index];
    }

    /**
     * 作物架的养分点按来源拆分，使玩家能看出作物生长速度来自哪里。
     * 
     * @param base            所有作物都会获得的基础点数
     * @param water           已存水提供的点数
     * @param fertilizer      已存肥料提供的点数
     * @param sky             能看到天空提供的点数
     * @param climate         生物群系提供的点数，取湿度与偏好群系加成中的较高者
     * @param fromLikedBiomes {@code climate} 是否来自偏好群系标签而非湿度
     * @param total           各来源之和，即该作物架的养分分
     */
    public record NutrientBreakdown(int base, int water, int fertilizer, int sky, int climate, boolean fromLikedBiomes,
                                    int total) {

        public static NutrientBreakdown of(int likedBiomeTagsCount, float biomeHumidity, boolean canSeeSky,
                                           int waterStorage, int fertilizerStorage) {
            likedBiomeTagsCount = Math.min(CropsGTMConfig.maxLikedBiomes, likedBiomeTagsCount);
            waterStorage = Math.min(CropsGTMConfig.maxWaterBonusAt, waterStorage);
            fertilizerStorage = Math.min(CropsGTMConfig.maxFertilizerBonusAt, fertilizerStorage);

            int base = CropsGTMConfig.baseNutrientValue;
            int water = (waterStorage + 9) / 10;
            int fertilizer = (fertilizerStorage + 9) / 10;
            int sky = canSeeSky ? CropsGTMConfig.skyAccessBonus : 0;
            float humidityBonus = (biomeHumidity - CropsGTMConfig.lowHumidityThreshold) /
                    (CropsGTMConfig.highHumidityThreshold - CropsGTMConfig.lowHumidityThreshold);
            humidityBonus = Math.max(0.0f, Math.min(1.0f, humidityBonus)) * CropsGTMConfig.highHumidityBonus;
            int likedBiomeBonus = likedBiomeTagsCount * CropsGTMConfig.likedBiomeBonus;
            boolean fromLikedBiomes = likedBiomeBonus > 0 && likedBiomeBonus >= (int) humidityBonus;
            int climate = fromLikedBiomes ? likedBiomeBonus : (int) humidityBonus;
            return new NutrientBreakdown(base, water, fertilizer, sky, climate, fromLikedBiomes,
                    base + water + fertilizer + sky + climate);
        }

        /**
         * @return 在当前配置下作物架能达到的最高养分分。
         */
        public static int maxTotal() {
            // 偏好群系加成只取决于群系，因此这里把湿度保持在最小值
            return of(CropsGTMConfig.maxLikedBiomes, CropsGTMConfig.lowHumidityThreshold, true,
                    CropsGTMConfig.maxWaterBonusAt, CropsGTMConfig.maxFertilizerBonusAt).total();
        }
    }

    public static int getNutrientsPerCycle(int likedBiomeTagsCount, float biomeHumidity, boolean canSeeSky,
                                           int waterStorage, int fertilizerStorage) {
        return NutrientBreakdown.of(likedBiomeTagsCount, biomeHumidity, canSeeSky, waterStorage, fertilizerStorage)
                .total();
    }

    public static int getGrowthRate(int nutrientPoints, int tier, int growth) {
        nutrientPoints *= CropsGTMConfig.nutrientPointScale;
        int need = tier * CropsGTMConfig.nutrientsPerTier;
        if (need < 0) return 0;
        int baseSpeed = Constants.BASE_GROWTH_SPEED + growth;
        if (nutrientPoints >= need) {
            return baseSpeed * (100 + (nutrientPoints - need)) / 100;
        }
        return Math.max(baseSpeed * (100 - (need - nutrientPoints) * 4) / 100, 0);
    }

    /** @return 一次繁殖尝试的属性浮动表，由亲本数量与肥料决定。 */
    public static int[] getStatVariation(boolean hasSingleParent, int minFertilizer) {
        if (hasSingleParent) {
            if (minFertilizer >= Constants.HIGH_FERTILIZER_SPREADING_THRESHOLD) {
                return Constants.HIGH_FERTILIZER_SPREADING_VARIATIONS;
            }
            if (minFertilizer >= Constants.MED_FERTILIZER_SPREADING_THRESHOLD) {
                return Constants.MED_FERTILIZER_SPREADING_VARIATIONS;
            }
            return Constants.LOW_FERTILIZER_SPREADING_VARIATIONS;
        }
        if (minFertilizer >= Constants.HIGH_FERTILIZER_BREEDING_THRESHOLD) {
            return Constants.HIGH_FERTILIZER_BREEDING_VARIATIONS;
        }
        if (minFertilizer >= Constants.MED_FERTILIZER_BREEDING_THRESHOLD) {
            return Constants.MED_FERTILIZER_BREEDING_VARIATIONS;
        }
        return Constants.LOW_FERTILIZER_BREEDING_VARIATIONS;
    }

    public static byte variateStat(int[] variations, Collection<ISeedStats> parentStats,
                                   ToIntFunction<ISeedStats> collector, int maxValue, RandomSource random) {
        int newValue = parentStats.stream().mapToInt(collector).reduce(0, Integer::sum) / parentStats.size();
        int variation = variations[random.nextInt(variations.length)];
        return (byte) Math.max(Constants.MIN_SEED_STAT, Math.min(maxValue, newValue + variation));
    }
}
