package com.synkra.cropsgtm.farming;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

import javax.annotation.Nullable;

public class SeedStats implements ISeedStats {

    public static final SeedStats DEFAULT_ANALYZED = new SeedStats(Constants.MIN_SEED_STAT, Constants.MIN_SEED_STAT,
            Constants.MIN_SEED_STAT, true);

    private final byte growth;
    private final byte gain;
    private final byte resistance;
    private boolean analyzed;

    public SeedStats() {
        this(Constants.MIN_SEED_STAT, Constants.MIN_SEED_STAT, Constants.MIN_SEED_STAT);
    }

    public SeedStats(byte growth, byte gain, byte resistance) {
        this(growth, gain, resistance, false);
    }

    public SeedStats(byte growth, byte gain, byte resistance, boolean analyzed) {
        this.growth = clamp(growth, CropsGTMConfig.maxGrowthStat);
        this.gain = clamp(gain, CropsGTMConfig.maxGainStat);
        this.resistance = clamp(resistance, CropsGTMConfig.maxResistanceStat);
        this.analyzed = analyzed;
    }

    private static byte clamp(byte value, int maxValue) {
        return (byte) Math.max(Constants.MIN_SEED_STAT, Math.min(maxValue, value));
    }

    @Override
    public byte getGrowth() {
        return this.growth;
    }

    @Override
    public byte getGain() {
        return this.gain;
    }

    @Override
    public byte getResistance() {
        return this.resistance;
    }

    @Override
    public boolean isAnalyzed() {
        return this.analyzed;
    }

    @Override
    public void setAnalyzed(boolean analyzed) {
        this.analyzed = analyzed;
    }

    public SeedStats copy() {
        return new SeedStats(this.growth, this.gain, this.resistance, this.analyzed);
    }

    /**
     * 读取通用种子物品上存储的属性。
     * 
     * @return 该物品的属性；物品为空时返回 null。
     */
    public static @Nullable SeedStats getStatsFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return readFromNBT(stack.getTag());
    }

    public static SeedStats readFromNBT(@Nullable CompoundTag tag) {
        if (tag == null) return new SeedStats();
        byte growth = tag.contains(Names.NBT.growth, Tag.TAG_BYTE) ? tag.getByte(Names.NBT.growth) :
                Constants.MIN_SEED_STAT;
        byte gain = tag.contains(Names.NBT.gain, Tag.TAG_BYTE) ? tag.getByte(Names.NBT.gain) : Constants.MIN_SEED_STAT;
        byte resistance = tag.contains(Names.NBT.resistance, Tag.TAG_BYTE) ? tag.getByte(Names.NBT.resistance) :
                Constants.MIN_SEED_STAT;
        boolean analyzed = tag.contains(Names.NBT.analyzed, Tag.TAG_BYTE) && tag.getBoolean(Names.NBT.analyzed);
        return new SeedStats(growth, gain, resistance, analyzed);
    }

    public CompoundTag writeToNBT(CompoundTag tag) {
        tag.putByte(Names.NBT.growth, this.growth);
        tag.putByte(Names.NBT.gain, this.gain);
        tag.putByte(Names.NBT.resistance, this.resistance);
        tag.putBoolean(Names.NBT.analyzed, this.analyzed);
        return tag;
    }

    @Override
    public boolean equals(ISeedStats other) {
        return this.growth == other.getGrowth() && this.gain == other.getGain() &&
                this.resistance == other.getResistance() && this.analyzed == other.isAnalyzed();
    }
}
