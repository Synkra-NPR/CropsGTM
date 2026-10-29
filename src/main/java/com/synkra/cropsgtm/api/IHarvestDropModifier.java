package com.synkra.cropsgtm.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * 由能增加或改变收获掉落的物品实现。
 */
public interface IHarvestDropModifier {

    /**
     * @param player      执行收获的玩家
     * @param heldItem    玩家手中的物品
     * @param dropTracker 作物产生的掉落物
     * @param tile        承载该作物的作物架
     */
    void modifyCropDrops(Player player, ItemStack heldItem, Collection<ItemStack> dropTracker, ICropStickTile tile);
}
