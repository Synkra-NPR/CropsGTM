package com.synkra.cropsgtm.blocks;

import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropMutation;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IMutationPool;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.CropGrowthMath;
import com.synkra.cropsgtm.farming.SeedData;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.reference.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;

import javax.annotation.Nullable;

/**
 * 为作物架掷一次繁殖或杂交判定。
 * <p>
 * 从 {@link BlockEntityCropSticks} 中拆出，因为这里的查找只读取相邻作物与突变登记表，从不触碰发起尝试的作物架状态。
 * </p>
 */
final class CropBreeding {

    private CropBreeding() {}

    /** @return 在作物架上种下了新种子时返回 true。 */
    static boolean attempt(BlockEntityCropSticks tile) {
        Level level = tile.getLevel();
        if (level == null) return false;
        if (level.random.nextInt(Constants.BREEDING_CHANCE) != 0) return false;
        List<ICropStickTile> neighbours = tile.getNeighbours();
        neighbours.removeIf(neighbour -> neighbour == null || !neighbour.hasCrop() || neighbour.hasWeed());
        if (neighbours.isEmpty()) return false;

        ICropCard result = findResult(tile, neighbours);
        if (result == null || neighbours.isEmpty() || !tile.isValidSoilForCrop(result) ||
                !tile.wouldCropBeAbleToGrow(result)) {
            return false;
        }

        Collection<ISeedStats> parentStats = new ArrayList<>();
        for (ICropStickTile neighbour : neighbours) {
            parentStats.add(neighbour.getSeed().getStats());
        }
        OptionalInt minFertilizer = neighbours.stream().mapToInt(ICropStickTile::getFertilizerStorage).min();
        int[] variations = CropGrowthMath.getStatVariation(neighbours.size() <= 1, minFertilizer.orElse(0));

        byte gain = CropGrowthMath.variateStat(variations, parentStats, ISeedStats::getGain, CropsGTMConfig.maxGainStat,
                level.random);
        byte resistance = CropGrowthMath.variateStat(variations, parentStats, ISeedStats::getResistance,
                CropsGTMConfig.maxResistanceStat, level.random);
        byte growth = CropGrowthMath.variateStat(variations, parentStats, ISeedStats::getGrowth,
                CropsGTMConfig.maxGrowthStat, level.random);

        tile.plantSeed(new SeedData(result, new SeedStats(growth, gain, resistance, false)));
        return true;
    }

    /**
     * 挑选亲本产出的作物。
     * <p>
     * 相邻列表会被收窄为真正参与结果的亲本，使后续的属性浮动只作用于这些亲本。
     * </p>
     */
    private static @Nullable ICropCard findResult(BlockEntityCropSticks tile, List<ICropStickTile> neighbours) {
        Level level = tile.getLevel();
        if (level == null) return null;
        // 有 50% 概率尝试杂交而不是普通繁殖。
        if (level.random.nextBoolean()) {
            List<ICropStickTile> crossingParents = new ArrayList<>(neighbours);
            crossingParents.removeIf(neighbour -> !neighbour.canCross());
            if (!crossingParents.isEmpty()) {
                ICropCard chosen = crossingParents.get(level.random.nextInt(crossingParents.size())).getSeed()
                        .getCrop();
                // 只有参与杂交的作物才算入新属性。
                crossingParents.removeIf(neighbour -> neighbour.getSeed().getCrop() != chosen);
                neighbours.clear();
                neighbours.addAll(crossingParents);
                return chosen;
            }
        }

        neighbours.removeIf(neighbour -> !neighbour.canBreed());
        if (neighbours.size() < 2) return null;
        List<ICropCard> breedingParents = new ArrayList<>();
        for (ICropStickTile neighbour : neighbours) {
            breedingParents.add(neighbour.getSeed().getCrop());
        }
        List<ICropMutation> deterministic = MutationRegistry.instance
                .getPossibleDeterministicMutations(breedingParents);
        if (deterministic != null && !deterministic.isEmpty()) {
            ICropMutation chosen = deterministic.get(level.random.nextInt(deterministic.size()));
            if (chosen.canBreed(breedingParents, level, tile)) {
                Collection<ICropCard> chosenParents = chosen.getParents();
                neighbours.removeIf(neighbour -> !chosenParents.contains(neighbour.getSeed().getCrop()));
                return chosen.getOutput();
            }
        }

        List<IMutationPool> pools = MutationRegistry.instance.getPossiblePoolMutations(breedingParents);
        if (pools != null && !pools.isEmpty()) {
            IMutationPool chosen = pools.get(level.random.nextInt(pools.size()));
            neighbours.removeIf(neighbour -> !chosen.contains(neighbour.getSeed().getCrop()));
            List<ICropCard> candidates = new ArrayList<>(chosen.getMembers());
            return candidates.get(level.random.nextInt(candidates.size()));
        }
        return null;
    }
}
