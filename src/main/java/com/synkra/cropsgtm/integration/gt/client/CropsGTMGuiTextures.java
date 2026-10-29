package com.synkra.cropsgtm.integration.gt.client;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

import com.synkra.cropsgtm.reference.Names;

/**
 * CropsGTM 添加的界面贴图。
 */
public interface CropsGTMGuiTextures {

    /** 除草剂输入槽使用的灰色槽位覆盖层。 */
    IGuiTexture SLOT_WEED_EX = slotOverlay("weed_ex_slot");

    /** 固体肥料输入槽使用的灰色槽位覆盖层。 */
    IGuiTexture SLOT_FERTILIZER = slotOverlay("fertilizer_slot");

    /** 工业农场种子槽使用的灰色槽位覆盖层。 */
    IGuiTexture SLOT_SEED = slotOverlay("seed_slot");

    /** 工业农场环境模块槽位使用的灰色槽位覆盖层。 */
    IGuiTexture SLOT_ENVIRONMENTAL_MODULE = slotOverlay("environmental_module_slot");

    /** 显示作物架的灰色槽位覆盖层，作物繁殖机槽位使用。 */
    IGuiTexture SLOT_CROP_STICK = slotOverlay("crop_stick_slot");

    /** 开关按钮覆盖层：浇水。 */
    IGuiTexture TOGGLE_WATER = toggleOverlay("water_toggle");
    IGuiTexture TOGGLE_WATER_GRAY = toggleOverlay("water_toggle_gray");
    /** 开关按钮覆盖层：除草剂施用。 */
    IGuiTexture TOGGLE_WEED_EX = toggleOverlay("weed_ex_toggle");
    IGuiTexture TOGGLE_WEED_EX_GRAY = toggleOverlay("weed_ex_toggle_gray");
    /** 开关按钮覆盖层：施肥。 */
    IGuiTexture TOGGLE_FERTILIZER = toggleOverlay("fertilizer_toggle");
    IGuiTexture TOGGLE_FERTILIZER_GRAY = toggleOverlay("fertilizer_toggle_gray");
    /** 开关按钮覆盖层：收获。 */
    IGuiTexture TOGGLE_HARVEST = toggleOverlay("harvest_toggle");
    IGuiTexture TOGGLE_HARVEST_GRAY = toggleOverlay("harvest_toggle_gray");

    /** 工业农场机器模式图标，取自原模组的 GT 覆盖层。 */
    IGuiTexture MODE_INPUT = toggleOverlay("mode_input");
    IGuiTexture MODE_FARM = toggleOverlay("mode_farm");
    IGuiTexture MODE_OUTPUT = toggleOverlay("mode_output");

    /** 开关按钮背景：空闲帧与激活帧。 */
    IGuiTexture TOGGLE_BUTTON_OFF = image("textures/gui/widget/toggle_button_off.png");
    IGuiTexture TOGGLE_BUTTON_ON = image("textures/gui/widget/toggle_button_on.png");

    /** 竖直储罐指示条：水。 */
    IGuiTexture BAR_WATER = bar("cropmanager_water");
    /** 竖直储罐指示条：除草剂。 */
    IGuiTexture BAR_WEED_EX = bar("cropmanager_weed_ex");
    /** 竖直储罐指示条：液态肥料。 */
    IGuiTexture BAR_FERTILIZER = bar("cropmanager_liquid_fertilizer");

    static IGuiTexture image(String path) {
        return new ResourceTexture(Names.MOD_ID + ":" + path);
    }

    static IGuiTexture bar(String name) {
        return image("textures/gui/progressbar/" + name + ".png");
    }

    static IGuiTexture slotOverlay(String name) {
        return image("textures/gui/overlay_slot/" + name + ".png");
    }

    static IGuiTexture toggleOverlay(String name) {
        return image("textures/gui/overlay_button/" + name + ".png");
    }
}
