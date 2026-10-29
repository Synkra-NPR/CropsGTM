package com.synkra.cropsgtm.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

/**
 * 由能在作物从作物架上移除时替换默认种子掉落的物品实现。
 */
public interface ICropSeedDropOverride {

    /**
     * @param player       破坏作物的玩家
     * @param heldItem     玩家手中的物品
     * @param dropTracker  目前已收集的掉落物
     * @param tile         正在交互的作物架
     * @param hasHarvested 是否同时进行了收获
     * @return 返回 true 时跳过默认的种子掉落
     */
    boolean overrideSeedDrop(Player player, ItemStack heldItem, Collection<ItemStack> dropTracker, ICropStickTile tile,
                             boolean hasHarvested);
}
