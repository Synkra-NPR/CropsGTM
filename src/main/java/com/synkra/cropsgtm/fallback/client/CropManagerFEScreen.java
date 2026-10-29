package com.synkra.cropsgtm.fallback.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.synkra.cropsgtm.fallback.BlockEntityCropManagerFE;
import com.synkra.cropsgtm.fallback.CropManagerFEMenu;

import java.util.List;
import java.util.Optional;

/**
 * 备用作物管理器的普通容器界面。
 * <p>
 * 机器槽位、三个储罐指示条与能量条都用原版界面辅助绘制，不涉及贴图集或 LDLib 组件。
 * </p>
 */
public class CropManagerFEScreen extends AbstractContainerScreen<CropManagerFEMenu> {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 226;

    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 16;

    private static final int TANK_X = 90;
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 14;
    private static final int TANK_HEIGHT = 54;
    private static final int TANK_SPACING = 20;

    private static final int ENERGY_BAR_X = 70;
    private static final int ENERGY_BAR_Y = 18;
    private static final int ENERGY_BAR_WIDTH = TANK_WIDTH;
    private static final int ENERGY_BAR_HEIGHT = TANK_HEIGHT;
    private static final int ENERGY_COLOR = 0xFFD94040;

    private static final int[] TANK_COLORS = { 0xFF3F76E4, 0xFF6FA82A, 0xFF9A4FD6 };
    private static final int[] TANK_CAPACITIES = { BlockEntityCropManagerFE.WATER_CAPACITY,
            BlockEntityCropManagerFE.FERTILIZER_CAPACITY, BlockEntityCropManagerFE.WEED_EX_CAPACITY };
    private static final String[] TANK_KEYS = { "cropsgtm_ui.water", "cropsgtm_ui.fertilizer", "cropsgtm_ui.weed_ex" };

    /** 按钮 id 与 {@code BlockEntityCropManagerFE#toggleFeature} 一致。 */
    private static final int BUTTON_AUTO_OUTPUT = 0;
    private static final int BUTTON_WORKING = 1;
    private static final int BUTTON_HARVEST = 2;
    private static final int BUTTON_WATER = 3;
    private static final int BUTTON_FERTILIZER = 4;
    private static final int BUTTON_WEED_EX = 5;

    private final Button[] featureButtons = new Button[6];

    public CropManagerFEScreen(CropManagerFEMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.inventoryLabelX = 48;
        this.inventoryLabelY = 136;
    }

    @Override
    protected void init() {
        super.init();
        // 显式居中自定义尺寸的界面，原版计算只认识默认尺寸
        this.leftPos = (this.width - WIDTH) / 2;
        this.topPos = (this.height - HEIGHT) / 2;
        this.createButton(BUTTON_WORKING, 8, 96);
        this.createButton(BUTTON_HARVEST, 88, 96);
        this.createButton(BUTTON_AUTO_OUTPUT, 168, 96);
        this.createButton(BUTTON_WATER, 8, 114);
        this.createButton(BUTTON_FERTILIZER, 88, 114);
        this.createButton(BUTTON_WEED_EX, 168, 114);
    }

    private void createButton(int id, int x, int y) {
        this.featureButtons[id] = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
            }
        }).bounds(this.leftPos + x, this.topPos + y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.updateButton(BUTTON_AUTO_OUTPUT, "cropsgtm_ui.auto_output", this.menu.isAutoOutput());
        this.updateButton(BUTTON_WORKING, "cropsgtm_ui.working", this.menu.isWorkingEnabled());
        this.updateButton(BUTTON_HARVEST, "cropsgtm_ui.harvest", this.menu.isHarvestEnabled());
        this.updateButton(BUTTON_WATER, "cropsgtm_ui.water_enabled", this.menu.isWaterEnabled());
        this.updateButton(BUTTON_FERTILIZER, "cropsgtm_ui.fertilizer_enabled", this.menu.isFertilizerEnabled());
        this.updateButton(BUTTON_WEED_EX, "cropsgtm_ui.weed_ex_enabled", this.menu.isWeedExEnabled());
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        int[] values = { this.menu.getWater(), this.menu.getFertilizer(), this.menu.getWeedEx() };
        for (int tank = 0; tank < 3; tank++) {
            int x = TANK_X + tank * TANK_SPACING;
            if (this.isHovering(x, TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
                graphics.renderTooltip(this.font, List.of(Component.translatable(TANK_KEYS[tank]),
                        Component.translatable(TANK_KEYS[tank] + ".fill").withStyle(ChatFormatting.GRAY),
                        Component.literal(values[tank] + " / " + TANK_CAPACITIES[tank]).withStyle(ChatFormatting.GRAY)),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        if (this.isHovering(ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, mouseX, mouseY)) {
            graphics.renderTooltip(this.font,
                    List.of(Component.translatable("cropsgtm_ui.energy"),
                            Component.translatable("cropsgtm_ui.energy.fill").withStyle(ChatFormatting.GRAY),
                            Component.literal(this.menu.getEnergy() + " / " + BlockEntityCropManagerFE.ENERGY_CAPACITY)
                                    .withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private void updateButton(int id, String key, boolean enabled) {
        this.featureButtons[id].setMessage(Component.translatable(key + (enabled ? ".on" : ".off")));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFFC6C6C6);
        graphics.fill(x, y, x + WIDTH, y + 2, 0xFFFFFFFF);
        graphics.fill(x, y, x + 2, y + HEIGHT, 0xFFFFFFFF);
        graphics.fill(x, y + HEIGHT - 2, x + WIDTH, y + HEIGHT, 0xFF555555);
        graphics.fill(x + WIDTH - 2, y, x + WIDTH, y + HEIGHT, 0xFF555555);

        for (Slot slot : this.menu.slots) {
            drawSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }

        int[] values = { this.menu.getWater(), this.menu.getFertilizer(), this.menu.getWeedEx() };
        for (int tank = 0; tank < 3; tank++) {
            drawTank(graphics, x + TANK_X + tank * TANK_SPACING, y + TANK_Y, values[tank], TANK_CAPACITIES[tank],
                    TANK_COLORS[tank]);
        }
        drawTank(graphics, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, this.menu.getEnergy(),
                BlockEntityCropManagerFE.ENERGY_CAPACITY, ENERGY_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040,
                false);

        drawSmall(graphics, Integer.toString(this.menu.getWater()), TANK_X, TANK_Y + TANK_HEIGHT + 1);
        drawSmall(graphics, Integer.toString(this.menu.getFertilizer()), TANK_X + TANK_SPACING,
                TANK_Y + TANK_HEIGHT + 1);
        drawSmall(graphics, Integer.toString(this.menu.getWeedEx()), TANK_X + TANK_SPACING * 2,
                TANK_Y + TANK_HEIGHT + 1);
        drawSmall(graphics, Integer.toString(this.menu.getEnergy()), ENERGY_BAR_X, TANK_Y + TANK_HEIGHT + 1);
    }

    private void drawSmall(GuiGraphics graphics, String text, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 1.0f);
        graphics.drawString(this.font, text, x * 2, y * 2, 0x404040, false);
        graphics.pose().popPose();
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    private static void drawTank(GuiGraphics graphics, int x, int y, int value, int capacity, int color) {
        graphics.fill(x, y, x + TANK_WIDTH, y + TANK_HEIGHT, 0xFF373737);
        int filled = capacity > 0 ? Math.round((TANK_HEIGHT - 4) * Math.min(value, capacity) / (float) capacity) : 0;
        if (filled > 0) {
            graphics.fill(x + 2, y + TANK_HEIGHT - 2 - filled, x + TANK_WIDTH - 2, y + TANK_HEIGHT - 2, color);
        }
        graphics.fill(x, y, x + TANK_WIDTH, y + 1, 0xFF8B8B8B);
        graphics.fill(x, y + TANK_HEIGHT - 1, x + TANK_WIDTH, y + TANK_HEIGHT, 0xFF8B8B8B);
        graphics.fill(x, y, x + 1, y + TANK_HEIGHT, 0xFF8B8B8B);
        graphics.fill(x + TANK_WIDTH - 1, y, x + TANK_WIDTH, y + TANK_HEIGHT, 0xFF8B8B8B);
    }
}
