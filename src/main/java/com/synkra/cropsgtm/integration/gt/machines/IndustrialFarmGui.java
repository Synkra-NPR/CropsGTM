package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import com.synkra.cropsgtm.integration.gt.client.CropsGTMGuiTextures;

/**
 * 构建工业农场的页面组件。
 * <p>
 * 从 {@link IndustrialFarmMachine} 中拆出，使机器类专注于工作逻辑。
 * </p>
 */
final class IndustrialFarmGui {

    private IndustrialFarmGui() {}

    private static final int SCREEN_HEIGHT = 112;
    private static final int CONTROL_ROW_Y = 120;
    private static final int PAGE_HEIGHT = CONTROL_ROW_Y + 18 + 4;
    static final int UI_HEIGHT = PAGE_HEIGHT + 8 + 86;

    /** 模式按钮的宽高，以及两个按钮之间的间距。 */
    private static final int MODE_BUTTON_SIZE = 18;
    private static final int MODE_BUTTON_GAP = 2;

    /** 按机器模式索引的模式图标。 */
    private static final IGuiTexture[] MODE_ICONS = { CropsGTMGuiTextures.MODE_INPUT, CropsGTMGuiTextures.MODE_FARM,
            CropsGTMGuiTextures.MODE_OUTPUT };

    /** 各模式的按钮贴图，第二张贴图为按下状态。 */
    private static final IGuiTexture[][] MODE_BUTTONS = { modeButton(MODE_ICONS[IndustrialFarmMachine.MODE_INPUT]),
            modeButton(MODE_ICONS[IndustrialFarmMachine.MODE_FARM]),
            modeButton(MODE_ICONS[IndustrialFarmMachine.MODE_OUTPUT]) };

    static Widget createUIWidget(IndustrialFarmMachine machine) {
        WidgetGroup group = new WidgetGroup(0, 0, 190, PAGE_HEIGHT);

        // 显示区正好位于控制行上方，因此页面中间没有空带
        group.addWidget(
                new DraggableScrollableWidgetGroup(4, 4, 182, SCREEN_HEIGHT).setBackground(machine.getScreenTexture())
                        .addWidget(new LabelWidget(4, 5, machine.self().getBlockState().getBlock().getDescriptionId()))
                        .addWidget(new ComponentPanelWidget(4, 17, machine::addDisplayText)
                                .textSupplier(machine.getLevel() != null && machine.getLevel().isClientSide ? null :
                                        machine::addDisplayText)
                                .setMaxWidthLimit(180).clickHandler(machine::handleDisplayClick)));

        // 三个模式按钮彼此相邻放置，同一时刻只有一个处于按下状态
        for (int mode = IndustrialFarmMachine.MODE_INPUT; mode <= IndustrialFarmMachine.MODE_OUTPUT; mode++) {
            group.addWidget(
                    createModeButton(machine, 4 + mode * (MODE_BUTTON_SIZE + MODE_BUTTON_GAP), CONTROL_ROW_Y, mode));
        }

        // 种子槽与下层土壤槽仍绑定处理器，它刻意拒绝任何交互，
        // 环境模块槽位使用原始存储，以便取出再放回
        group.addWidget(new SlotWidget(machine.inventory, IndustrialFarmMachine.SLOT_SEED, 112, CONTROL_ROW_Y)
                .setCanPutItems(false).setCanTakeItems(false)
                .setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_SEED));
        group.addWidget(new SlotWidget(machine.inventory, IndustrialFarmMachine.SLOT_SUB_SOIL, 130, CONTROL_ROW_Y)
                .setCanPutItems(false).setCanTakeItems(false).setBackground(GuiTextures.SLOT));
        group.addWidget(
                new SlotWidget(machine.inventory.storage, IndustrialFarmMachine.SLOT_ENV_START, 152, CONTROL_ROW_Y)
                        .setCanPutItems(true).setCanTakeItems(true)
                        .setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_ENVIRONMENTAL_MODULE));
        group.addWidget(
                new SlotWidget(machine.inventory.storage, IndustrialFarmMachine.SLOT_ENV_START + 1, 170, CONTROL_ROW_Y)
                        .setCanPutItems(true).setCanTakeItems(true)
                        .setBackground(GuiTextures.SLOT, CropsGTMGuiTextures.SLOT_ENVIRONMENTAL_MODULE));

        return group;
    }

    /**
     * 创建单个机器模式的按钮。
     * <p>
     * 当前模式下的按钮以按下状态绘制，三个按钮表现为一组单选按钮：按下一个会释放另外两个。
     * </p>
     */
    private static Widget createModeButton(IndustrialFarmMachine machine, int x, int y, int mode) {
        return new ButtonWidget(x, y, MODE_BUTTON_SIZE, MODE_BUTTON_SIZE,
                (ClickData data) -> machine.setMachineMode(mode)) {

            @Override
            public void updateScreen() {
                super.updateScreen();
                boolean pressed = machine.getMachineMode() == mode;
                setButtonTexture(MODE_BUTTONS[mode][pressed ? 1 : 0]);
                setHoverTooltips(IndustrialFarmDisplay.getModeName(mode));
            }
        };
    }

    /** 把给定图标绘制在松开与按下的开关按钮背景上。 */
    private static IGuiTexture[] modeButton(IGuiTexture icon) {
        return new IGuiTexture[] { modeIcon(icon, false), modeIcon(icon, true) };
    }

    private static IGuiTexture modeIcon(IGuiTexture icon, boolean pressed) {
        IGuiTexture background = GuiTextures.TOGGLE_BUTTON_BACK.getSubTexture(0, pressed ? 0.5f : 0, 1, 0.5f);
        return new GuiTextureGroup(background, (graphics, mouseX, mouseY, x, y, width, height) -> icon.draw(graphics,
                mouseX, mouseY, x + 1, y + 1, width - 2, height - 2));
    }
}
