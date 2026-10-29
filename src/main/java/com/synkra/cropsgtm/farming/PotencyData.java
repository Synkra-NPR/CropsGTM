package com.synkra.cropsgtm.farming;

/**
 * 描述某物品或流体提供多少某种资源（肥料、水、除草剂）。
 */
public class PotencyData {

    private final int potency;
    private final int maxStorage;
    private final int unitsConsumedPerApplication;

    public PotencyData(int potency) {
        this(potency, 0, 1);
    }

    public PotencyData(int potency, int maxStorage) {
        this(potency, maxStorage, 1);
    }

    public PotencyData(int potency, int maxStorage, int unitsConsumedPerApplication) {
        this.potency = potency;
        this.maxStorage = maxStorage;
        this.unitsConsumedPerApplication = unitsConsumedPerApplication;
    }

    public int getPotency() {
        return this.potency;
    }

    public int getMaxStorage() {
        return this.maxStorage;
    }

    /**
     * @param defaultMax 使用该条目的机制自身的存储上限
     * @return 实际采用的存储上限，不会超过机制的上限
     */
    public int getMaxStorage(int defaultMax) {
        if (this.maxStorage <= 0) return defaultMax;
        return Math.min(defaultMax, this.maxStorage);
    }

    public int getUnitsConsumedPerApplication() {
        return this.unitsConsumedPerApplication;
    }
}
