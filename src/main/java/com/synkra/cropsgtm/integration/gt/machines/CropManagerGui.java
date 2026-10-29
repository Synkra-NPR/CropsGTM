package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.ToggleButtonWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import com.synkra.cropsgtm.integration.gt.client.CropsGTMGuiTextures;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * 构建作物管理器的页面。
 * <p>
 * 从 {@link CropManagerMachine} 中拆出，使机器类专注于工作逻辑，与 {@link IndustrialFarmGui} 从工业农场拆出的做法一致。
 * </p>
 */
final class CropManagerGui {

    private CropManagerGui() {}

    /** 页面宽度：槽位网格在右边缘前八像素结束，避免出现空列。 */
    static final int PAGE_WIDTH = 200;
    /** 机器页面的高度。 */
    static final int PAGE_HEIGHT = 88;
    /** 整个机器窗口的高度：页面、fancy 界面边框与玩家背包。 */
    static final int UI_HEIGHT = PAGE_HEIGHT + 8 + 86;

    static ModularUI createUI(CropManagerMachine machine, Player entityPlayer) {
        return new ModularUI(PAGE_WIDTH + 8, UI_HEIGHT, machine, entityPlayer)
                .widget(new FancyMachineUIWidget(machine, PAGE_WIDTH + 8, UI_HEIGHT));
    }

    static Widget createUIWidget(CropManagerMachine machine) {
        WidgetGroup group = new WidgetGroup(0, 0, PAGE_WIDTH, PAGE_HEIGHT);

        // 第一行是除草剂罐，其下方固体肥料构成 2x2 网格。
        // 槽位绑定原始存储而非可通知处理器：处理器只在
        // 只有在能力侧允许输出时才放出，而输入槽从不允许，因此玩家无法
        // 把除草剂罐或肥料从机器中取回。
        for (int i = 0; i < CropManagerMachine.WEEDEX_SLOT_COUNT; i++) {
            group.addWidget(new SlotWidget(machine.weedExInput.storage, i, 8 + i * 18, 8).setCanPutItems(true)
                    .setCanTakeItems(true).setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_WEED_EX));
        }
        for (int i = 0; i < CropManagerMachine.FERTILIZER_SLOT_COUNT; i++) {
            int col = i % 2;
            int row = i / 2;
            group.addWidget(new SlotWidget(machine.fertilizerInput.storage, i, 8 + col * 18, 26 + row * 18)
                    .setCanPutItems(true).setCanTakeItems(true)
                    .setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_FERTILIZER));
        }

        // 储罐指示条从槽位网格之后开始，因此不会与槽位重叠。
        int tankX = 8 + 2 * 18 + 8;
        addTankBar(group, tankX, machine::getSyncedWater, () -> CropManagerMachine.getWaterCapacity(machine.getTier()),
                CropsGTMGuiTextures.BAR_WATER, "cropsgtm_tooltip.cropManager.waterStorage");
        addTankBar(group, tankX + 16, machine::getSyncedWeedEx,
                () -> CropManagerMachine.getWeedExCapacity(machine.getTier()), CropsGTMGuiTextures.BAR_WEED_EX,
                "cropsgtm_tooltip.cropManager.weedEXStorage");
        addTankBar(group, tankX + 32, machine::getSyncedFertilizer,
                () -> CropManagerMachine.getFertilizerCapacity(machine.getTier()), CropsGTMGuiTextures.BAR_FERTILIZER,
                "cropsgtm_tooltip.cropManager.liquidFertilizerStorage");

        int outputX = tankX + 50;
        for (int i = 0; i < CropManagerMachine.OUTPUT_SLOT_COUNT; i++) {
            int col = i % 5;
            int row = i / 5;
            group.addWidget(new SlotWidget(machine.output.storage, i, outputX + col * 18, 8 + row * 18)
                    .setCanPutItems(false).setCanTakeItems(true).setBackground(GuiTextures.SLOT));
        }

        // 电池槽位于输出网格最右一列的下方。
        group.addWidget(new SlotWidget(machine.chargerInventory, 0, outputX + 4 * 18, 8 + 3 * 18).setCanPutItems(true)
                .setCanTakeItems(true).setBackground(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY));

        // 功能开关位于背包下方。
        int buttonY = 66;
        int buttonX = 8;
        addToggle(group, buttonX, buttonY, machine::isWaterEnabled, machine::setWaterEnabled,
                CropsGTMGuiTextures.TOGGLE_WATER, CropsGTMGuiTextures.TOGGLE_WATER_GRAY,
                "cropsgtm_tooltip.cropManager.toggle.water");
        addToggle(group, buttonX + 18, buttonY, machine::isWeedExEnabled, machine::setWeedExEnabled,
                CropsGTMGuiTextures.TOGGLE_WEED_EX, CropsGTMGuiTextures.TOGGLE_WEED_EX_GRAY,
                "cropsgtm_tooltip.cropManager.toggle.weedEX");
        addToggle(group, buttonX + 36, buttonY, machine::isFertilizerEnabled, machine::setFertilizerEnabled,
                CropsGTMGuiTextures.TOGGLE_FERTILIZER, CropsGTMGuiTextures.TOGGLE_FERTILIZER_GRAY,
                "cropsgtm_tooltip.cropManager.toggle.fertilizer");
        addToggle(group, buttonX + 54, buttonY, machine::isHarvestEnabled, machine::setHarvestEnabled,
                CropsGTMGuiTextures.TOGGLE_HARVEST, CropsGTMGuiTextures.TOGGLE_HARVEST_GRAY,
                "cropsgtm_tooltip.cropManager.toggle.harvest");

        return group;
    }

    private static void addTankBar(WidgetGroup group, int x, IntSupplier stored, IntSupplier capacity,
                                   IGuiTexture texture, String langKey) {
        ProgressWidget bar = new ProgressWidget(
                () -> capacity.getAsInt() <= 0 ? 0.0d : (double) stored.getAsInt() / capacity.getAsInt(), x, 8, 12, 54,
                new ProgressTexture(GuiTextures.SLOT, texture));
        bar.setFillDirection(ProgressTexture.FillDirection.DOWN_TO_UP);
        bar.setDynamicHoverTips(
                value -> Component.translatable(langKey, FormattingUtil.formatNumbers(stored.getAsInt()),
                        FormattingUtil.formatNumbers(capacity.getAsInt())).getString());
        group.addWidget(bar);
    }

    private static void addToggle(WidgetGroup group, int x, int y, BooleanSupplier getter, BooleanConsumer setter,
                                  IGuiTexture active, IGuiTexture idle, String langKey) {
        ToggleButtonWidget button = new ToggleButtonWidget(x, y, 16, 16, CropsGTMGuiTextures.TOGGLE_BUTTON_OFF, getter,
                setter);
        button.setShouldUseBaseBackground();
        button.setBaseTexture(CropsGTMGuiTextures.TOGGLE_BUTTON_OFF, idle);
        button.setPressedTexture(CropsGTMGuiTextures.TOGGLE_BUTTON_ON, active);
        button.setHoverTooltips(Component.translatable(langKey));
        group.addWidget(button);
    }
}
