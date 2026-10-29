package com.synkra.cropsgtm.farming.requirements.growth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IWorldGrowthRequirement;
import com.synkra.cropsgtm.reference.Names;

/**
 * 带此需求的作物可在作物架上生长，但只能在工业农场内收获。
 */
public class MachineOnlyGrowthRequirement implements IWorldGrowthRequirement {

    @Override
    public boolean canGrow(Level level, BlockPos pos, ICropStickTile tile) {
        return false;
    }

    @Override
    public boolean onlyPreventsHarvest() {
        return true;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(Names.MOD_ID + "_growthReq.lockout.machineOnly").withStyle(ChatFormatting.GRAY);
    }
}
