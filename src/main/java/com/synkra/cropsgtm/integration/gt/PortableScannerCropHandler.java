package com.synkra.cropsgtm.integration.gt;

import com.gregtechceu.gtceu.common.data.GTItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

import com.synkra.cropsgtm.blocks.BlockEntityCropSticks;

import java.util.ArrayList;
import java.util.List;

/**
 * 让 GregTech 便携式扫描仪像植物透镜一样读取作物架。
 * <p>
 * 扫描仪无法直接扩展：它的交互会消耗被作用的每个方块。该事件在方块与物品处理点击之前运行，因此改在这里处理作物架，扫描仪会输出透镜读数（其中包含已存的肥料、水与除草剂数值），随后是养分分与各来源的贡献。
 * </p>
 */
public final class PortableScannerCropHandler {

    private PortableScannerCropHandler() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        ItemStack heldItem = event.getItemStack();
        if (!GTItems.PORTABLE_SCANNER.isIn(heldItem) && !GTItems.PORTABLE_DEBUG_SCANNER.isIn(heldItem)) {
            return;
        }
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof BlockEntityCropSticks cropSticks)) {
            return;
        }

        // 与植物透镜一样，扫描会把已种种子标记为已分析
        cropSticks.analyzeSeed();
        Player player = event.getEntity();
        List<Component> information = new ArrayList<>();
        cropSticks.getPlantLensStatus(information);
        cropSticks.getNutrientStatus(information);
        for (Component line : information) {
            player.sendSystemMessage(line);
        }

        // 避免扫描仪与作物架再次处理同一次点击
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
