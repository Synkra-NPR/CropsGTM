package com.synkra.cropsgtm.integration.gt.client.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.client.CropsGTMRecipeViewer;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * 机器进度箭头：悬停显示提示，点击在配方查看器中打开该机器的配方页。
 * <p>
 * GT 箭头贴图包含两张上下堆叠的帧，第一张是空闲箭头，第二张是用于填充的高亮箭头。
 * </p>
 */
public class RecipeArrowWidget extends ProgressWidget implements IRecipeIngredientSlot {

    private final ItemStack machineStack;
    private final String recipeCategoryId;

    public RecipeArrowWidget(int x, int y, DoubleSupplier progress, ItemStack machineStack, String recipeCategoryId) {
        super(progress, x, y, 18, 18,
                new ProgressTexture(IGuiTexture.EMPTY, GuiTextures.PROGRESS_BAR_ARROW.getSubTexture(0, 0.5, 1, 0.5)));
        this.machineStack = machineStack;
        this.recipeCategoryId = recipeCategoryId;
        setFillDirection(ProgressTexture.FillDirection.LEFT_TO_RIGHT);
        setBackground(GuiTextures.PROGRESS_BAR_ARROW.getSubTexture(0, 0, 1, 0.5));
        setHoverTooltips(Component.translatable("cropsgtm_tooltip.machine.viewRecipes"));
    }

    @Override
    public List<Object> getXEIIngredients() {
        return List.of(this.machineStack);
    }

    @Override
    public Object getXEIIngredientOverMouse(double mouseX, double mouseY) {
        return isMouseOverElement(mouseX, mouseY) ? this.machineStack : null;
    }

    @Override
    public IngredientIO getIngredientIO() {
        return IngredientIO.CATALYST;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverElement(mouseX, mouseY)) {
            CropsGTMRecipeViewer.openCategory(this.recipeCategoryId);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
