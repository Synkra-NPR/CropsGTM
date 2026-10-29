package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock.ComponentType;

import java.util.Collection;

/**
 * 统计已成型工业农场内的组件。
 * <p>
 * 多方块缓存只保存带全局上限的谓词，即机壳外壳与种子床。它们的包围盒就是整个结构，因此通过扫描该包围盒来统计组件，而不是仅依赖缓存。
 * </p>
 */
final class IndustrialFarmStructure {

    private IndustrialFarmStructure() {}

    /** 已成型结构包围盒内发现的组件。 */
    record Scan(int seedBedCount, int seedBedTier, boolean seedBedTierMismatch, int upgradeUnitTier,
                boolean upgradeUnitTierMismatch, int vanillaGlass, int temperedGlass, int laminatedGlass,
                int otherGlass, int environmentalUnits, int growthAccelerationUnits, int fertilizerUnits,
                int advancedHarvestingUnits, int overclockedGrowthAccelerationUnits) {}

    static Scan scan(Level level, Collection<BlockPos> cache) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : cache) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        int seedBedCount = 0;
        int seedBedTier = -1;
        boolean seedBedTierMismatch = false;
        int upgradeUnitTier = -1;
        boolean upgradeUnitTierMismatch = false;
        int vanillaGlass = 0;
        int temperedGlass = 0;
        int laminatedGlass = 0;
        int otherGlass = 0;
        int environmentalUnits = 0;
        int growthAccelerationUnits = 0;
        int fertilizerUnits = 0;
        int advancedHarvestingUnits = 0;
        int overclockedGrowthAccelerationUnits = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState blockState = level.getBlockState(new BlockPos(x, y, z));
                    Block block = blockState.getBlock();
                    if (block == Blocks.GLASS) {
                        vanillaGlass++;
                    } else if (block == GTBlocks.CASING_TEMPERED_GLASS.get()) {
                        temperedGlass++;
                    } else if (block == GTBlocks.CASING_LAMINATED_GLASS.get()) {
                        laminatedGlass++;
                    } else if (block != GTBlocks.FUSION_GLASS.get() && block instanceof GlassBlock) {
                        otherGlass++;
                    }
                    if (!(block instanceof IndustrialFarmComponentBlock component)) continue;

                    // 这里刻意不用枚举 switch：生成的 $SwitchMap 类不支持热替换
                    ComponentType type = component.getType();
                    int tier = component.getTier();
                    if (type == ComponentType.SEED_BED) {
                        seedBedCount++;
                        if (seedBedTier < 0) seedBedTier = tier;
                        else if (seedBedTier != tier) seedBedTierMismatch = true;
                    } else {
                        if (upgradeUnitTier < 0) upgradeUnitTier = tier;
                        else if (upgradeUnitTier != tier) upgradeUnitTierMismatch = true;

                        // 组件类型计数只对升级模块有意义
                        if (type == ComponentType.ENVIRONMENTAL_ENHANCEMENT_UNIT) {
                            environmentalUnits++;
                        } else if (type == ComponentType.GROWTH_ACCELERATION_UNIT) {
                            growthAccelerationUnits++;
                        } else if (type == ComponentType.FERTILIZER_UNIT) {
                            fertilizerUnits++;
                        } else if (type == ComponentType.ADVANCED_HARVESTING_UNIT) {
                            advancedHarvestingUnits++;
                        } else if (type == ComponentType.OVERCLOCKED_GROWTH_ACCELERATION_UNIT) {
                            overclockedGrowthAccelerationUnits++;
                        }
                    }
                }
            }
        }

        return new Scan(seedBedCount, seedBedTier, seedBedTierMismatch, upgradeUnitTier, upgradeUnitTierMismatch,
                vanillaGlass, temperedGlass, laminatedGlass, otherGlass, environmentalUnits, growthAccelerationUnits,
                fertilizerUnits, advancedHarvestingUnits, overclockedGrowthAccelerationUnits);
    }
}
