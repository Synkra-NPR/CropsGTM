package com.synkra.cropsgtm.farming.requirements.breeding;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IMachineBreedingRequirement;
import com.synkra.cropsgtm.reference.Names;

import java.util.Collection;

/**
 * 阻止该突变在作物繁殖机中繁殖，它只能在作物架上繁殖。
 */
public class WorldOnlyBreedingRequirement implements IMachineBreedingRequirement {

    @Override
    public boolean canBreedInMachine(Collection<ICropCard> parents) {
        return false;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(Names.MOD_ID + "_breedingReq.lockout.worldOnly").withStyle(ChatFormatting.GRAY);
    }
}
