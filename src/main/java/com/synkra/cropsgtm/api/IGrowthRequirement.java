package com.synkra.cropsgtm.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * 作物生长必须满足的条件。
 */
public interface IGrowthRequirement {

    boolean canGrow(Level level, BlockPos pos, ICropStickTile tile);

    Component getDescription();
}
