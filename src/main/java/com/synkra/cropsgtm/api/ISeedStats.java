package com.synkra.cropsgtm.api;

import net.minecraft.nbt.CompoundTag;

/**
 * 种子可以拥有的三项属性。
 */
public interface ISeedStats {

    byte getGrowth();

    byte getGain();

    byte getResistance();

    boolean isAnalyzed();

    void setAnalyzed(boolean analyzed);

    boolean equals(ISeedStats other);

    /**
     * 把这些属性写入给定标签并返回它。
     */
    CompoundTag writeToNBT(CompoundTag tag);
}
