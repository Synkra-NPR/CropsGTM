package com.synkra.cropsgtm.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 作物可种植其上的方块集合（具名）。
 */
public interface ISoilList {

    String getId();

    /**
     * @return 该土壤列表接受给定方块状态时返回 true。
     */
    boolean isRegistered(BlockState state);

    ISoilList register(TagKey<Block> tag);

    ISoilList register(Block... blocks);

    /**
     * @return 在种子提示中展示该土壤列表所用的物品。
     */
    List<ItemStack> getDisplayStacks();

    /**
     * @return 描述该列表土壤的翻译键，显示在已分析的种子上。
     */
    String getUnlocalizedItemTooltip();

    /**
     * @return 在不适合作物生长的土壤上播种时显示的翻译键。
     */
    String getUnlocalizedWrongSoilMessage();
}
