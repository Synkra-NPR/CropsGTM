package com.synkra.cropsgtm.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 由希望自行处理作物架左键点击的物品实现。
 */
public interface ICropLeftClickHandler {

    /**
     * @return 交互已被消耗时返回 true。
     */
    boolean onLeftClick(ICropStickTile tile, Player player, ItemStack heldItem);
}
