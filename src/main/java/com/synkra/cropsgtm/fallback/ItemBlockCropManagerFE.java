package com.synkra.cropsgtm.fallback;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.synkra.cropsgtm.CropsGTMPlatform;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 备用作物管理器的方块物品，其提示说明机器的供能方式。
 */
public class ItemBlockCropManagerFE extends BlockItem {

    public ItemBlockCropManagerFE(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("cropsgtm_tooltip.cropManagerFE.area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("cropsgtm_tooltip.cropManagerFE.energy").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("cropsgtm_tooltip.cropManagerFE.tanks").withStyle(ChatFormatting.GRAY));
        if (CropsGTMPlatform.GREG_TECH_LOADED) {
            tooltip.add(
                    Component.translatable("cropsgtm_tooltip.cropManagerFE.gt_disabled").withStyle(ChatFormatting.RED));
        }
    }
}
