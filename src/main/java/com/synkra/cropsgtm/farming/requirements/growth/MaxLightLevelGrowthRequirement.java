package com.synkra.cropsgtm.farming.requirements.growth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IWorldGrowthRequirement;
import com.synkra.cropsgtm.reference.Names;

/**
 * 光照等级过高时阻止作物生长。
 * <p>
 * 判定使用天空光与方块光的合成值，因此蘑菇等喜阴作物在光天化日下确实会拒绝生长，而不只是对火把有反应。
 * </p>
 */
public class MaxLightLevelGrowthRequirement implements IWorldGrowthRequirement {

    private final int maxLightLevel;

    public MaxLightLevelGrowthRequirement(int maxLightLevel) {
        this.maxLightLevel = maxLightLevel;
    }

    public int getMaxLightLevel() {
        return this.maxLightLevel;
    }

    @Override
    public boolean canGrow(Level level, BlockPos pos, ICropStickTile tile) {
        return level.getRawBrightness(pos, 0) <= this.maxLightLevel;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(Names.MOD_ID + "_growthReq.maxLight.format", this.maxLightLevel)
                .withStyle(ChatFormatting.GRAY);
    }
}
