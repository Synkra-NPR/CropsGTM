package com.synkra.cropsgtm.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 作物架方块实体向生长其上的作物暴露的接口。
 */
public interface ICropStickTile {

    boolean hasCrop();

    boolean hasWeed();

    boolean isSick();

    @Nullable
    ISeedData getSeed();

    boolean isCrossCrop();

    void setCrossCrop(boolean status);

    boolean canPlantSeed();

    void plantSeed(ISeedData seedData);

    void clear();

    boolean isValidSoilForCrop(ICropCard cropCard);

    boolean canGrow();

    /**
     * @return 已种植作物的生长条件全部满足时返回 true。
     */
    boolean areGrowthRequirementsMet();

    boolean isMature();

    int getGrowthProgress();

    void setGrowthProgress(int progress);

    float getGrowthPercent();

    int calcGrowthRate();

    boolean canHarvest();

    @Nullable
    List<ItemStack> harvest(double dropMultiplier);

    void spawnWeed();

    void transferDisease();

    boolean cureDisease();

    boolean addFertilizer(int amount, int threshold, int maxStorage, boolean simulate);

    int getFertilizerStorage();

    boolean addWater(int amount, int threshold, int maxStorage, boolean simulate);

    int getWaterStorage();

    boolean addWeedEx(int amount, int threshold, int maxStorage, boolean simulate);

    int getWeedExStorage();

    List<ICropStickTile> getNeighbours();

    boolean canCross();

    boolean canBreed();

    @Nullable
    ItemStack getSeedStack();

    @Nullable
    ItemStack getSeedDrop();

    /**
     * 在作物架位置掉落一个物品。
     */
    void dropItem(ItemStack stack);

    /**
     * 播放移除作物的音效。
     */
    void playCropRemovalSound();

    Level getLevel();

    BlockPos getBlockPos();
}
