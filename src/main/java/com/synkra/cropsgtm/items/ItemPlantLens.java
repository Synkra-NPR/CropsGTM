package com.synkra.cropsgtm.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropRightClickHandler;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.blocks.BlockEntityCropSticks;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * 植物透镜会打印作物架上生长的内容，并把已种种子标记为已分析。
 * <p>
 * 透镜使用普通右键，无需潜行。
 * </p>
 */
public class ItemPlantLens extends Item implements ICropRightClickHandler {

    public ItemPlantLens() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCropSticks cropSticks)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        analyze(cropSticks, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean onRightClick(ICropStickTile tile, Player player, ItemStack heldItem) {
        if (!(tile instanceof BlockEntityCropSticks cropSticks)) return false;
        Level level = player.level();
        if (level.isClientSide) return true;
        analyze(cropSticks, player);
        return true;
    }

    private static void analyze(BlockEntityCropSticks cropSticks, @Nullable Player player) {
        // 方块实体在下一 tick 把已分析标志同步给客户端
        cropSticks.analyzeSeed();
        if (player != null) {
            List<Component> information = new ArrayList<>();
            cropSticks.getPlantLensStatus(information);
            for (Component line : information) {
                player.sendSystemMessage(line);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.item.plantLens").withStyle(ChatFormatting.GRAY));
    }
}
