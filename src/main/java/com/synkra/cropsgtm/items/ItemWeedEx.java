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
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 除草剂喷雾罐，对作物架施用除草剂，使其在一段时间内抵抗杂草。
 * <p>
 * 一次用量为作物架补充 {@link Constants#WEEDEX_POTENCY_PER_USE} 点除草剂，罐子在 {@link Constants#WEEDEX_USES_PER_CAN} 次后耗尽；未安装 GregTech
 * 时为 {@link Constants#WEEDEX_USES_NO_GT} 次。
 * </p>
 */
public class ItemWeedEx extends ItemSprayCan implements ICropRightClickHandler {

    public ItemWeedEx() {
        super(CropsGTMPlatform.GREG_TECH_LOADED ? Constants.WEEDEX_USES_PER_CAN : Constants.WEEDEX_USES_NO_GT);
    }

    @Override
    public boolean onRightClick(ICropStickTile tile, Player player, ItemStack heldItem) {
        Level level = tile.getLevel();
        if (level.isClientSide) return true;
        if (getUsesLeft(heldItem) <= 0) return false;
        if (!tile.addWeedEx(
                CropsGTMConfig.scalePotency(Constants.WEEDEX_POTENCY_PER_USE, CropsGTMConfig.weedExPotencyMultiplier),
                Constants.WEEDEX_FILL_THRESHOLD, Constants.WEEDEX_MAX_STORAGE, false)) {
            return false;
        }
        if (!player.isCreative()) consumeUse(player, heldItem);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.item.weedEx").withStyle(ChatFormatting.GRAY));
        tooltip.add(
                Component.translatable(Names.MOD_ID + "_tooltip.item.weedEx.warning").withStyle(ChatFormatting.YELLOW));
    }
}
