package com.synkra.cropsgtm.api;

import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;

/**
 * 确定性的繁殖配方。
 */
public interface ICropMutation {

    ICropCard getOutput();

    Collection<ICropCard> getParents();

    int getParentCount();

    /**
     * @return 繁殖该突变时作物繁殖机消耗的 EU/t。
     */
    long getBreedingMachineRecipeEUt();

    /**
     * @return 繁殖该突变所需机器的电压等级。
     */
    int getBreedingMachineRecipeTier();

    /**
     * @return 繁殖该突变时作物繁殖机需要的刻数。
     */
    int getBreedingMachineRecipeDuration();

    /**
     * @return 给定亲本列表满足该突变及其繁殖条件时返回 true。
     */
    boolean canBreed(Collection<ICropCard> parents, Level level, ICropStickTile tile);

    /**
     * @return 该突变除亲本作物外还需要的额外条件。
     */
    default List<IBreedingRequirement> getRequirements() {
        return List.of();
    }

    /**
     * @return 允许作物繁殖机繁殖该突变时返回 true。
     */
    default boolean canBreedInMachine(Collection<ICropCard> parents) {
        return true;
    }

    /**
     * @return 该突变只能在作物繁殖机中进行时返回 true。
     */
    default boolean isMachineOnly() {
        return false;
    }
}
