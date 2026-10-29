package com.synkra.cropsgtm.integration.gt.jade;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import com.synkra.cropsgtm.integration.gt.machines.IndustrialFarmMachine;
import com.synkra.cropsgtm.reference.Names;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * 展示工业农场种植作物时的功耗，沿用 GregTech 为自家机器工作状态所用的布局。
 * <p>
 * 工业农场不运行 {@link com.gregtechceu.gtceu.api.machine.trait.RecipeLogic}，因此 GregTech
 * 的配方逻辑提供器无法描述它。所需数值已由控制器同步到客户端，该提供器可以自行绘制该行。
 * </p>
 */
public enum IndustrialFarmPowerProvider implements IBlockComponentProvider {

    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Names.MOD_ID,
            "industrial_farm_power");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof MetaMachineBlockEntity machineBlock) ||
                !(machineBlock.getMetaMachine() instanceof IndustrialFarmMachine farm)) {
            return;
        }
        // 电力行只在农场真正工作时显示，已结束周期的农场
        // 无法放入输出的会挂起且不消耗电力
        if (!farm.isActive()) {
            return;
        }
        int tier = farm.getUpgradeTier();
        long eut = farm.getExpectedEUt();
        if (tier < 0 || tier >= GTValues.V.length || eut <= 0) return;

        long voltage = GTValues.V[tier];
        if (voltage <= 0) return;
        MutableComponent power = Component
                .translatable("gtceu.jade.amperage_use", FormattingUtil.formatNumber2Places((float) eut / voltage))
                .withStyle(ChatFormatting.RED)
                .append(Component.translatable("gtceu.jade.at").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(GTValues.VNF[tier]).withStyle(style -> style.withColor(GTValues.VC[tier])))
                .append(Component
                        .translatable("gtceu.universal.padded_parentheses",
                                Component.translatable("gtceu.recipe.eu.total", FormattingUtil.formatNumbers(eut)))
                        .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.translatable("gtceu.top.energy_consumption").append(" ").append(power));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
