package com.synkra.cropsgtm.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.api.ICropRightClickHandler;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 植物治疗喷雾罐，治愈患病作物并留下少量肥料，避免它立刻再次患病。
 * <p>
 * 移植自原 CropNH 植物治疗剂，只是把一次性物品改成喷雾罐，可用 {@link Constants#PLANT_CURE_USES} 次；未安装 GregTech 时为
 * {@link Constants#PLANT_CURE_USES_NO_GT} 次。
 * </p>
 */
public class ItemPlantCure extends ItemSprayCan implements ICropRightClickHandler {

    /** 治疗剂治愈作物后在作物架中留下的肥料量。 */
    private static final int FERTILIZER_AFTER_CURE = 25;

    public ItemPlantCure() {
        super(CropsGTMPlatform.GREG_TECH_LOADED ? Constants.PLANT_CURE_USES : Constants.PLANT_CURE_USES_NO_GT);
    }

    @Override
    public boolean onRightClick(ICropStickTile tile, Player player, ItemStack heldItem) {
        Level level = tile.getLevel();
        if (level.isClientSide) return true;
        if (getUsesLeft(heldItem) <= 0) return false;
        if (tile.cureDisease()) {
            // 治疗剂能让作物在一段时间内保持健康，整片治愈则依靠施用除草剂
            // 用作物管理器处理整片区域，然后逐个治愈作物
            tile.addFertilizer(FERTILIZER_AFTER_CURE, FERTILIZER_AFTER_CURE, FERTILIZER_AFTER_CURE, false);
            if (!player.isCreative()) consumeUse(player, heldItem);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.item.plantCure").withStyle(ChatFormatting.GRAY));
    }
}
