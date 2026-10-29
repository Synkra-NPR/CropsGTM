package com.synkra.cropsgtm.api;

import net.minecraft.world.level.Level;

import java.util.Collection;

/**
 * 在作物架上杂交时检查的繁殖条件。
 */
public interface IWorldBreedingRequirement extends IBreedingRequirement {

    boolean canBreed(Collection<ICropCard> parents, Level level, ICropStickTile tile);
}
