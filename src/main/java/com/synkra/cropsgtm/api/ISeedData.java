package com.synkra.cropsgtm.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

/**
 * 作物及其属性的组合。
 */
public interface ISeedData {

    @NotNull
    ICropCard getCrop();

    @NotNull
    ISeedStats getStats();

    ItemStack getStack();

    void setAnalyzed(boolean analyzed);

    @NotNull
    CompoundTag writeToNBT();

    @NotNull
    ItemStack getCropSeeds(int amount);
}
