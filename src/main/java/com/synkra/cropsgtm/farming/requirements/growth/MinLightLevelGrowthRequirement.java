package com.synkra.cropsgtm.farming.requirements.growth;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IWorldGrowthRequirement;
import com.synkra.cropsgtm.reference.Names;

/**
 * 光照等级过低时阻止作物生长。
 * <p>
 * 判定使用天空光与方块光的合成值，与原版作物一致，因此露天作物在白天算作有光，而不是必须紧挨着火把。
 * </p>
 */
public class MinLightLevelGrowthRequirement implements IWorldGrowthRequirement {

    private final int minLightLevel;

    public MinLightLevelGrowthRequirement(int minLightLevel) {
        this.minLightLevel = minLightLevel;
    }

    public int getMinLightLevel() {
        return this.minLightLevel;
    }

    @Override
    public boolean canGrow(Level level, BlockPos pos, ICropStickTile tile) {
        return level.getRawBrightness(pos, 0) >= this.minLightLevel;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(Names.MOD_ID + "_growthReq.minLight.format", this.minLightLevel)
                .withStyle(ChatFormatting.GRAY);
    }
}
