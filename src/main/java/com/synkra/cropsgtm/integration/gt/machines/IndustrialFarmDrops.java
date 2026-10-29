package com.synkra.cropsgtm.integration.gt.machines;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ISeedData;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.CropGrowthMath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 工业农场的掉落计算。
 * <p>
 * 公式只依赖种子、已存种子数量、单周期生长进度与收获轮数倍率，因此从机器类中拆出以便测试。
 * </p>
 */
public final class IndustrialFarmDrops {

    private IndustrialFarmDrops() {}

    /**
     * @return 在给定已存种子数量下单次周期产出的掉落，随机取整使用传入的随机源。
     */
    public static List<ItemStack> getDropsPerCycle(ISeedData seedData, int seedCount, double progressPerCycle,
                                                   double harvestRoundMultiplier, RandomSource random) {
        List<ItemStack> drops = new ArrayList<>();
        if (progressPerCycle <= 0) return drops;

        ICropCard crop = seedData.getCrop();
        ISeedStats stats = seedData.getStats();
        double avgDropIncrease = (stats.getGain() + 1) / 100.0d;
        double avgDropCount = CropGrowthMath.getAvgDropRounds(crop, stats.getGain()) * harvestRoundMultiplier;

        for (Map.Entry<ItemStack, Integer> entry : crop.getDropTable().entrySet()) {
            ItemStack drop = entry.getKey();
            double chance = entry.getValue() / 10_000.0d;
            double expected = (drop.getCount() + avgDropIncrease) * chance * avgDropCount * progressPerCycle *
                    seedCount * CropsGTMConfig.industrialFarmOutputMultiplier;
            int amount = (int) expected;
            if (random.nextDouble() < expected - amount) amount++;
            if (amount <= 0) continue;

            int maxStackSize = Math.max(1, drop.getMaxStackSize());
            while (amount > 0) {
                int toAdd = Math.min(maxStackSize, amount);
                drops.add(drop.copyWithCount(toAdd));
                amount -= toAdd;
            }
        }
        return drops;
    }

    /**
     * @return 单次周期每种掉落物的平均数量，按数量排序。算法与 {@link #getDropsPerCycle} 一致，只是不做随机取整，因此可以直接显示在界面上。
     */
    public static List<Map.Entry<ItemStack, Double>> getExpectedDropsPerCycle(ISeedData seedData, int seedCount,
                                                                              double progressPerCycle,
                                                                              double harvestRoundMultiplier) {
        List<Map.Entry<ItemStack, Double>> drops = new ArrayList<>();
        if (seedCount <= 0 || progressPerCycle <= 0) return drops;

        ICropCard crop = seedData.getCrop();
        ISeedStats stats = seedData.getStats();
        double avgDropIncrease = (stats.getGain() + 1) / 100.0d;
        double avgDropCount = CropGrowthMath.getAvgDropRounds(crop, stats.getGain()) * harvestRoundMultiplier;

        for (Map.Entry<ItemStack, Integer> entry : crop.getDropTable().entrySet()) {
            ItemStack drop = entry.getKey();
            double chance = entry.getValue() / 10_000.0d;
            double expected = (drop.getCount() + avgDropIncrease) * chance * avgDropCount * progressPerCycle *
                    seedCount * CropsGTMConfig.industrialFarmOutputMultiplier;
            if (expected <= 0) continue;
            drops.add(Map.entry(drop, expected));
        }
        drops.sort(Comparator.comparingDouble((Map.Entry<ItemStack, Double> entry) -> entry.getValue()).reversed());
        return drops;
    }
}
