package com.synkra.cropsgtm.integration.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.mutation.CropMutation;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.integration.CropRecipeText;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 展示某个作物的全部确定性繁殖配方：上方是亲本，中间是结果种子，其下是所需土壤与下层土壤，布局下方是额外繁殖条件。只在作物繁殖机中生效的突变也会列出。
 */
public class BreedingJeiCategory extends AbstractRecipeCategory<BreedingJeiCategory.BreedingJeiRecipe> {

    public static final RecipeType<BreedingJeiRecipe> RECIPE_TYPE = RecipeType.create(CropsGTM.MOD_ID, "breeding",
            BreedingJeiRecipe.class);

    private static final int WIDTH = 180;
    private static final int BACKGROUND_WIDTH = 166;
    private static final int BACKGROUND_HEIGHT = 87;
    /** 物品的位置，背景边框在它周围留出一个像素。 */
    private static final int OUTPUT_X = 75;
    private static final int OUTPUT_Y = 22;
    private static final int SOIL_Y = 43;
    private static final int SUB_SOIL_Y = 64;
    private static final int[] PARENT_X = { 44, 106, 23, 127 };
    private static final int PARENT_Y = 22;

    private static final ResourceLocation BACKGROUND_TWO = new ResourceLocation(CropsGTM.MOD_ID,
            "textures/gui/nei/breeding2.png");
    private static final ResourceLocation BACKGROUND_FOUR = new ResourceLocation(CropsGTM.MOD_ID,
            "textures/gui/nei/breeding4.png");

    private final IDrawable backgroundTwo;
    private final IDrawable backgroundFour;

    public BreedingJeiCategory(IJeiHelpers helpers) {
        super(RECIPE_TYPE, Component.translatable("cropsgtm_tooltip.viewer.breeding.name"), helpers.getGuiHelper()
                .createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CropsGTMBlocks.cropSticks())), WIDTH,
                BACKGROUND_HEIGHT + 4 + maxTextHeight());
        this.backgroundTwo = helpers.getGuiHelper().createDrawable(BACKGROUND_TWO, 5, 11, BACKGROUND_WIDTH,
                BACKGROUND_HEIGHT);
        this.backgroundFour = helpers.getGuiHelper().createDrawable(BACKGROUND_FOUR, 5, 11, BACKGROUND_WIDTH,
                BACKGROUND_HEIGHT);
    }

    /**
     * @return 最高繁殖页所需的文本高度，该分类的高度由所有配方共用。
     */
    private static int maxTextHeight() {
        int height = 0;
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.hideFromNEI()) continue;
            Collection<CropMutation> mutations = MutationRegistry.instance.getDeterministicMutationsForCrop(crop);
            if (mutations == null) continue;
            for (CropMutation mutation : mutations) {
                height = Math.max(height,
                        CropRecipeText.textHeight(CropRecipeText.breedingLines(mutation, crop), WIDTH - 4));
            }
        }
        return height;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<BreedingJeiRecipe> recipes = new ArrayList<>();
        for (ICropCard output : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (output.hideFromNEI()) continue;
            Collection<CropMutation> mutations = MutationRegistry.instance.getDeterministicMutationsForCrop(output);
            if (mutations == null) continue;
            int index = 0;
            for (CropMutation mutation : mutations) {
                // 只在该机器中才会生效的突变也一并保留在此页，
                // 布局下方的繁殖要求用来提示玩家需要这台机器
                recipes.add(new BreedingJeiRecipe(mutation, index++));
            }
        }
        registration.addRecipes(RECIPE_TYPE, recipes);
    }

    @Override
    public ResourceLocation getRegistryName(BreedingJeiRecipe recipe) {
        return recipe.id;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BreedingJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.parents.size() && i < PARENT_X.length; i++) {
            builder.addInputSlot(PARENT_X[i], PARENT_Y).addItemStack(recipe.parents.get(i));
        }
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y).addItemStack(recipe.output);
        if (!recipe.soils.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, OUTPUT_X, SOIL_Y).addItemStacks(recipe.soils);
        }
        if (!recipe.subSoils.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, OUTPUT_X, SUB_SOIL_Y).addItemStacks(recipe.subSoils);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BreedingJeiRecipe recipe, IFocusGroup focuses) {
        int y = BACKGROUND_HEIGHT + 2;
        List<FormattedText> lines = new ArrayList<>(recipe.lines.size());
        lines.addAll(recipe.lines);
        builder.addText(lines, WIDTH - 4, getHeight() - y).setPosition(2, y).setColor(CropRecipeText.TEXT_COLOR)
                .setShadow(false).setLineSpacing(1);
    }

    @Override
    public void draw(BreedingJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX,
                     double mouseY) {
        IDrawable background = recipe.parents.size() > 2 ? this.backgroundFour : this.backgroundTwo;
        background.draw(guiGraphics, 0, 0);
    }

    public static final class BreedingJeiRecipe {

        private final ResourceLocation id;
        private final ItemStack output;
        private final List<ItemStack> parents;
        private final List<ItemStack> soils;
        private final List<ItemStack> subSoils;
        private final List<Component> lines;

        private BreedingJeiRecipe(CropMutation mutation, int index) {
            this.id = new ResourceLocation(CropsGTM.MOD_ID, "/breeding/" + mutation.getOutput().getId()
                    .toLowerCase(Locale.ROOT).replace(':', '/').replaceAll("[^a-z0-9_/.-]", "_") + "/" + index);

            ICropCard crop = mutation.getOutput();
            this.output = crop.getSeedItem(SeedStats.DEFAULT_ANALYZED);
            this.parents = new ArrayList<>(mutation.getParentCount());
            for (ICropCard parent : mutation.getParents()) {
                this.parents.add(parent.getSeedItem(SeedStats.DEFAULT_ANALYZED));
            }
            this.soils = CropRecipeText.nonEmptyStacks(crop.getSoilTypes().getDisplayStacks());
            this.subSoils = CropRecipeText.subSoilStacks(crop);
            this.lines = CropRecipeText.breedingLines(mutation, crop);
        }
    }
}
