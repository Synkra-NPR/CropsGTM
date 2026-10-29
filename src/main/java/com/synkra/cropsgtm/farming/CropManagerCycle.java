package com.synkra.cropsgtm.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.synkra.cropsgtm.api.ICropStickTile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * GregTech 作物管理器与备用作物管理器共用的工作周期。
 * <p>
 * 周期扫描管理器周围的区域，执行已启用的维护功能并收获所有成熟作物。能量费用、储罐内容与维护功能的施用方式留在机器中，下方的宿主接口只暴露这些差异。
 * </p>
 */
public final class CropManagerCycle {

    /**
     * 维护功能把作物补充到的效力值。两个作物管理器共用这些数值，使 GregTech 机器与备用方块实体行为一致。
     */
    public static final int WATER_CAP = 200;
    public static final int WATER_THRESHOLD = 180;
    public static final int FERTILIZER_CAP = 200;
    public static final int FERTILIZER_THRESHOLD = 180;
    public static final int WEEDEX_CAP = 150;
    public static final int WEEDEX_THRESHOLD = 75;
    /** 单次施用最多增加的除草剂效力。 */
    public static final int WEEDEX_COST = 10;

    /** 周期需要从所属机器获得的全部信息。 */
    public interface Host {

        Level level();

        BlockPos pos();

        CropManagerSettings settings();

        int horizontalRadius();

        int verticalRadius();

        /** @return 机器能量足够启动一个周期时返回 true。 */
        boolean canRunCycle();

        boolean hasEnergy(long amount);

        void consumeEnergy(long amount);

        long harvestEnergyCost();

        long secondaryEnergyCost();

        double harvestBonus();

        boolean hasOutputSpace();

        /**
         * 把物品合并进输出库存。
         * 
         * @return 无法插入的物品数量。
         */
        int insertOutput(ItemStack stack);

        /** 对作物施用水；储罐与效力处理留在机器中。 */
        boolean applyWater(ICropStickTile crop);

        /** 对作物施用除草剂。 */
        boolean applyWeedEx(ICropStickTile crop);

        /** 对作物施用肥料。 */
        boolean applyFertilizer(ICropStickTile crop);

        /** 施用除草剂之前调用，GregTech 机器在此补充储罐。 */
        default void beforeWeedEx() {}

        /** 周期改变了机器状态时调用，备用机器在此标记自身需要保存。 */
        default void markChanged() {}
    }

    private final Host host;
    private final Set<ICropStickTile> cropCache = new LinkedHashSet<>();
    private final List<ItemStack> dropOverflow = new ArrayList<>();
    private boolean cacheInvalid = true;

    public CropManagerCycle(Host host) {
        this.host = host;
    }

    public boolean isCacheInvalid() {
        return this.cacheInvalid;
    }

    /** @return 尚未发现任何作物架时返回 true，此时机器使用更短的刷新间隔。 */
    public boolean isEmpty() {
        return this.cropCache.isEmpty();
    }

    /** 重建工作区域内的作物架列表。 */
    public void refreshCache() {
        this.cropCache.clear();
        Level level = this.host.level();
        if (level == null) return;
        BlockPos origin = this.host.pos();
        int horizontal = this.host.horizontalRadius();
        int vertical = this.host.verticalRadius();
        for (int y = -vertical; y <= vertical; y++) {
            for (int x = -horizontal; x <= horizontal; x++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    BlockEntity blockEntity = level.getBlockEntity(origin.offset(x, y, z));
                    if (blockEntity instanceof ICropStickTile crop) {
                        this.cropCache.add(crop);
                    }
                }
            }
        }
        this.cacheInvalid = false;
    }

    /**
     * 执行一个工作周期：先维护功能，再收获。
     * 
     * @return 机器状态发生变化时返回 true。
     */
    public boolean run() {
        if (!this.host.settings().isWorkingEnabled()) return false;
        if (!this.host.canRunCycle()) return false;
        boolean changed = processSecondaryFunctions();
        changed |= harvest();
        return changed;
    }

    /**
     * @return 已存流体在不溢出作物的前提下还能施用多少次。
     */
    public static int getApplications(int storedFluid, int potency, int units, int freeSpace) {
        if (freeSpace <= 0 || potency <= 0 || units <= 0) return 0;
        return Math.min(storedFluid / units, freeSpace / potency);
    }

    private boolean processSecondaryFunctions() {
        Level level = this.host.level();
        if (level == null) return false;
        CropManagerSettings settings = this.host.settings();
        boolean changed = false;
        for (ICropStickTile crop : this.cropCache) {
            if (!isValidCrop(level, crop)) continue;
            if (!this.host.hasEnergy(this.host.secondaryEnergyCost())) break;

            if (settings.isWaterEnabled() && this.host.applyWater(crop)) {
                this.host.consumeEnergy(this.host.secondaryEnergyCost());
                changed = true;
            }
            if (!this.host.hasEnergy(this.host.secondaryEnergyCost())) break;

            if (settings.isWeedExEnabled()) {
                this.host.beforeWeedEx();
                if (this.host.applyWeedEx(crop)) {
                    this.host.consumeEnergy(this.host.secondaryEnergyCost());
                    changed = true;
                }
            }
            if (!this.host.hasEnergy(this.host.secondaryEnergyCost())) break;

            if (settings.isFertilizerEnabled() && this.host.applyFertilizer(crop)) {
                this.host.consumeEnergy(this.host.secondaryEnergyCost());
                changed = true;
            }
        }
        if (changed) this.host.markChanged();
        return changed;
    }

    private boolean harvest() {
        if (!this.host.settings().isHarvestEnabled()) return false;
        if (!this.host.hasOutputSpace()) return false;
        boolean changed = flushOverflow();
        // 输出仍然满，现在收获只会堆积更多剩余
        if (!this.dropOverflow.isEmpty()) return changed;
        Level level = this.host.level();
        if (level == null) return changed;

        for (ICropStickTile crop : this.cropCache) {
            if (!isValidCrop(level, crop)) continue;
            if (!this.host.hasEnergy(this.host.harvestEnergyCost())) break;
            if (!crop.canHarvest()) continue;

            List<ItemStack> drops = crop.harvest(1.0d + this.host.harvestBonus());
            this.host.consumeEnergy(this.host.harvestEnergyCost());
            changed = true;
            if (drops == null) continue;
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) continue;
                int remaining = this.host.insertOutput(drop);
                if (remaining > 0) this.dropOverflow.add(drop.copyWithCount(remaining));
            }
        }
        if (changed) this.host.markChanged();
        return changed;
    }

    /** 尝试把上一周期剩余的掉落放入输出槽。 */
    private boolean flushOverflow() {
        if (this.dropOverflow.isEmpty()) return false;
        int before = this.dropOverflow.size();
        this.dropOverflow.removeIf(stack -> {
            int remaining = this.host.insertOutput(stack);
            if (remaining <= 0) return true;
            stack.setCount(remaining);
            return false;
        });
        return this.dropOverflow.size() != before;
    }

    private boolean isValidCrop(Level level, ICropStickTile crop) {
        if (level.getBlockEntity(crop.getBlockPos()) != crop) {
            this.cacheInvalid = true;
            return false;
        }
        return true;
    }
}
