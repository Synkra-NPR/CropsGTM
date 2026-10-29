package com.synkra.cropsgtm.integration.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.ISeedData;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.blocks.BlockEntityCropSticks;
import com.synkra.cropsgtm.reference.Names;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

/**
 * 展示作物架上已种的作物、土壤储量与生长进度。
 */
public enum CropStickComponentProvider implements IBlockComponentProvider {

    INSTANCE;

    private static final ResourceLocation UID = new ResourceLocation(CropsGTM.MOD_ID, "crop_sticks");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof BlockEntityCropSticks cropSticks)) return;

        Component soilLine = Component.translatable(Names.MOD_ID + "_tooltip.plantLens.soil",
                cropSticks.getFertilizerStorage(), cropSticks.getWaterStorage(), cropSticks.getWeedExStorage());

        if (!cropSticks.hasCrop()) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.empty"));
            tooltip.add(soilLine);
            return;
        }

        if (cropSticks.hasWeed()) {
            tooltip.add(soilLine);
            tooltip.add(
                    Component.translatable(Names.MOD_ID + "_tooltip.plantLens.weeds").withStyle(ChatFormatting.RED));
            return;
        }

        ISeedData seed = cropSticks.getSeed();
        if (seed == null) return;
        ICropCard crop = seed.getCrop();
        ISeedStats stats = seed.getStats();
        boolean analyzed = stats.isAnalyzed();

        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.seed",
                Component.translatable(crop.getUnlocalizedName()).withStyle(ChatFormatting.GREEN)));

        if (analyzed) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.stats", (int) stats.getGrowth(),
                    (int) stats.getGain(), (int) stats.getResistance()));
        }

        tooltip.add(soilLine);

        if (cropSticks.isSick()) {
            tooltip.add(
                    Component.translatable(Names.MOD_ID + "_tooltip.plantLens.isSick").withStyle(ChatFormatting.RED));
        }

        // 方块实体只在服务端执行需求检查，这里再执行一次使客户端
        // 显示作物为何不生长（例如缺少下层土壤方块）
        cropSticks.areGrowthRequirementsMet();
        for (IGrowthRequirement requirement : cropSticks.getFailedChecks()) {
            tooltip.add(requirement.getDescription().copy().withStyle(ChatFormatting.RED));
        }

        if (!analyzed) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.scanToViewStats"));
        }

        // 生长进度始终显示，即使种子尚未分析
        int progress = cropSticks.getGrowthProgress();
        int duration = crop.getGrowthDuration();
        float ratio = duration <= 0 ? 0.0f : Math.min(1.0f, (float) progress / duration);
        IElementHelper helper = tooltip.getElementHelper();
        tooltip.add(helper.progress(ratio,
                Component.translatable(Names.MOD_ID + "_tooltip.jade.progress", progress, duration),
                helper.progressStyle().color(0xFF4CBB17).textColor(-1),
                Util.make(BoxStyle.DEFAULT, style -> style.borderColor = 0xFF555555), true));
    }
}
