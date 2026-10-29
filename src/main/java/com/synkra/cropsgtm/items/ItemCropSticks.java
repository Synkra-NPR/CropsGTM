package com.synkra.cropsgtm.items;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.synkra.cropsgtm.api.ICropRightClickHandler;
import com.synkra.cropsgtm.api.ICropStickTile;

/**
 * 作物架物品，手持至少两个并在潜行时放置会生成双作物架。
 */
public class ItemCropSticks extends BlockItem implements ICropRightClickHandler {

    public ItemCropSticks(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Player player = context.getPlayer();
        boolean placingCross = player != null && player.isShiftKeyDown() &&
                (player.isCreative() || context.getItemInHand().getCount() >= 2);
        InteractionResult result = super.place(context);
        if (!result.consumesAction() || !placingCross) return result;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof ICropStickTile tile) {
            tile.setCrossCrop(true);
            level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.5f, 0.8f);
            if (player != null && !player.isCreative()) context.getItemInHand().shrink(1);
        }
        return result;
    }

    @Override
    public boolean onRightClick(ICropStickTile tile, Player player, ItemStack heldItem) {
        Level level = tile.getLevel();
        if (level.isClientSide) return true;
        if (!tile.canPlantSeed()) return false;
        tile.setCrossCrop(true);
        level.playSound(null, tile.getBlockPos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.5f, 0.8f);
        if (!player.isCreative()) heldItem.shrink(1);
        return true;
    }
}
