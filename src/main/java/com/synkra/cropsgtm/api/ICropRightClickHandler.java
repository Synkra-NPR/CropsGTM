package com.synkra.cropsgtm.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 由希望自行处理作物架右键点击的物品实现。
 */
public interface ICropRightClickHandler {

    /**
     * @return 交互已被消耗时返回 true。
     */
    boolean onRightClick(ICropStickTile tile, Player player, ItemStack heldItem);
}
