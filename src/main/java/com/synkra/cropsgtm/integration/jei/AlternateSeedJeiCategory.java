package com.synkra.cropsgtm.integration.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.init.CropsGTMItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 展示可用作作物种子的原版与模组物品，对应原作模组的替代种子页面。
 */
public class AlternateSeedJeiCategory extends AbstractRecipeCategory<AlternateSeedJeiCategory.AlternateSeedJeiRecipe> {

    public static final RecipeType<AlternateSeedJeiRecipe> RECIPE_TYPE = RecipeType.create(CropsGTM.MOD_ID,
            "alternate_seeds", AlternateSeedJeiRecipe.class);

    private static final int WIDTH = 170;
    private static final int HEIGHT = 20;
    /** 物品的位置，背景边框在它周围留出一个像素。 */
    private static final int INPUT_X = 55;
    private static final int INPUT_Y = 2;
    private static final int OUTPUT_X = 96;
    private static final int OUTPUT_Y = 2;
    /** 边框在贴图表中的位置，以及绘制它所用的偏移。 */
    private static final int BACKGROUND_U = 59;
    private static final int BACKGROUND_V = 12;
    private static final int BACKGROUND_WIDTH = 58;
    private static final int BACKGROUND_HEIGHT = 18;
    private static final int BACKGROUND_X = INPUT_X - 1;
    private static final int BACKGROUND_Y = INPUT_Y - 1;

    private static final ResourceLocation BACKGROUND = new ResourceLocation(CropsGTM.MOD_ID,
            "textures/gui/nei/alternate_seeds.png");

    private final IDrawable background;

    public AlternateSeedJeiCategory(IJeiHelpers helpers) {
        super(RECIPE_TYPE, Component.translatable("cropsgtm_tooltip.viewer.altSeeds.name"), categoryIcon(helpers),
                WIDTH, HEIGHT);
        this.background = helpers.getGuiHelper().createDrawable(BACKGROUND, BACKGROUND_U, BACKGROUND_V,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
    }

    /**
     * @return 用作分类图标的、已完成分析的种子物品，这样图标不会退化成没有作物标签的物品。
     */
    private static IDrawable categoryIcon(IJeiHelpers helpers) {
        ICropCard crop = CropRegistry.instance.get("cropsgtm:wheat");
        ItemStack stack = crop == null ? new ItemStack(CropsGTMItems.genericSeed()) :
                crop.getSeedItem(SeedStats.DEFAULT_ANALYZED);
        return helpers.getGuiHelper().createDrawableIngredient(VanillaTypes.ITEM_STACK, stack);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<AlternateSeedJeiRecipe> recipes = new ArrayList<>();
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.hideFromNEI()) continue;
            int index = 0;
            for (ItemStack alternateSeed : crop.getAlternateSeeds()) {
                if (alternateSeed.isEmpty()) continue;
                recipes.add(new AlternateSeedJeiRecipe(crop, alternateSeed, index++));
            }
        }
        registration.addRecipes(RECIPE_TYPE, recipes);
    }

    @Override
    public ResourceLocation getRegistryName(AlternateSeedJeiRecipe recipe) {
        return recipe.id;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlternateSeedJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(INPUT_X, INPUT_Y).addItemStack(recipe.input);
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y).addItemStack(recipe.output);
    }

    @Override
    public void draw(AlternateSeedJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        this.background.draw(guiGraphics, BACKGROUND_X, BACKGROUND_Y);
    }

    public static final class AlternateSeedJeiRecipe {

        private final ResourceLocation id;
        private final ItemStack input;
        private final ItemStack output;

        private AlternateSeedJeiRecipe(ICropCard crop, ItemStack alternateSeed, int index) {
            this.id = new ResourceLocation(CropsGTM.MOD_ID,
                    "/alternate_seed/" +
                            crop.getId().toLowerCase(Locale.ROOT).replace(':', '/').replaceAll("[^a-z0-9_/.-]", "_") +
                            "/" + index);
            this.input = alternateSeed;
            this.output = crop.getSeedItem(SeedStats.DEFAULT_ANALYZED);
        }
    }
}
