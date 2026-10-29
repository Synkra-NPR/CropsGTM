package com.synkra.cropsgtm.api;

/**
 * 作物在世界中的作物架上生长时检查的生长条件。
 */
public interface IWorldGrowthRequirement extends IGrowthRequirement {

    /**
     * @return 未通过的检查只阻止收获、作物仍继续生长时返回 true。
     */
    default boolean onlyPreventsHarvest() {
        return false;
    }
}
