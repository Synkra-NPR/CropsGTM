package com.synkra.cropsgtm.farming.requirements.breeding;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IWorldBreedingRequirement;
import com.synkra.cropsgtm.reference.Names;

import java.util.Collection;

/**
 * 阻止该突变在作物架上繁殖，它只能在作物繁殖机中繁殖。
 */
public class MachineOnlyBreedingRequirement implements IWorldBreedingRequirement {

    @Override
    public boolean canBreed(Collection<ICropCard> parents, Level level, ICropStickTile tile) {
        return false;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(Names.MOD_ID + "_breedingReq.lockout.machineOnly").withStyle(ChatFormatting.GRAY);
    }
}
