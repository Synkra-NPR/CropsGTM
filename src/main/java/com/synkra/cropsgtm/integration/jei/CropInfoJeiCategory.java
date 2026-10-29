package com.synkra.cropsgtm.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.CropGrowthMath;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.CropRecipeText;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 展示作物的种植方式：种子、可种植的土壤、所需下层土壤、收获掉落，以及布局下方的作物信息，与 EMI 页面保持一致。
 */
public class CropInfoJeiCategory extends AbstractRecipeCategory<CropInfoJeiCategory.CropInfoJeiRecipe> {

    public static final RecipeType<CropInfoJeiRecipe> RECIPE_TYPE = RecipeType.create(CropsGTM.MOD_ID, "crop_info",
            CropInfoJeiRecipe.class);

    private static final int WIDTH = 180;
    private static final int BACKGROUND_WIDTH = 166;
    private static final int BACKGROUND_HEIGHT = 85;
    private static final int SLOT = 18;
    /** 物品的位置，背景边框在它周围留出一个像素。 */
    private static final int SEED_X = 33;
    private static final int SEED_Y = 20;
    private static final int SOIL_X = 33;
    private static final int SOIL_Y = 41;
    private static final int SUB_SOIL_X = 33;
    private static final int SUB_SOIL_Y = 62;
    private static final int OUTPUT_X = 104;
    private static final int OUTPUT_Y = 16;
    private static final int MAX_OUTPUTS = 9;
    private static final int PROBABILITY_COLOR = 0xFFA500;

    private static final ResourceLocation BACKGROUND = new ResourceLocation(CropsGTM.MOD_ID,
            "textures/gui/nei/crop_list.png");

    private final IDrawable background;

    public CropInfoJeiCategory(IJeiHelpers helpers) {
        super(RECIPE_TYPE, Component.translatable("cropsgtm_tooltip.viewer.cropInfo.name"), categoryIcon(helpers),
                WIDTH, BACKGROUND_HEIGHT + 4 + maxTextHeight());
        this.background = helpers.getGuiHelper().createDrawable(BACKGROUND, 5, 11, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
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

    /**
     * @return 最高作物页所需的文本高度，该分类的高度由所有配方共用。
     */
    private static int maxTextHeight() {
        int height = 0;
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.hideFromNEI()) continue;
            height = Math.max(height, CropRecipeText.textHeight(CropRecipeText.cropInfoLines(crop), WIDTH - 4));
        }
        return height;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<CropInfoJeiRecipe> recipes = new ArrayList<>();
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.hideFromNEI()) continue;
            recipes.add(new CropInfoJeiRecipe(crop));
        }
        registration.addRecipes(RECIPE_TYPE, recipes);
    }

    @Override
    public ResourceLocation getRegistryName(CropInfoJeiRecipe recipe) {
        return recipe.id;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CropInfoJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(SEED_X, SEED_Y).addItemStack(recipe.seed);
        if (!recipe.soils.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, SOIL_X, SOIL_Y).addItemStacks(recipe.soils);
        }
        if (!recipe.subSoils.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, SUB_SOIL_X, SUB_SOIL_Y).addItemStacks(recipe.subSoils);
        }

        int displayCount = Math.min(recipe.outputs.size(), MAX_OUTPUTS);
        for (int i = 0; i < displayCount; i++) {
            builder.addOutputSlot(OUTPUT_X + (i % 3) * SLOT, OUTPUT_Y + (i / 3) * SLOT)
                    .addItemStack(recipe.outputs.get(i).stack());
        }
        // 页面只放得下九条掉落，其余仍需可搜索
        if (recipe.outputs.size() > MAX_OUTPUTS) {
            List<ItemStack> hidden = new ArrayList<>(recipe.outputs.size() - MAX_OUTPUTS);
            for (int i = MAX_OUTPUTS; i < recipe.outputs.size(); i++) {
                hidden.add(recipe.outputs.get(i).stack());
            }
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(hidden);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, CropInfoJeiRecipe recipe, IFocusGroup focuses) {
        int y = BACKGROUND_HEIGHT + 2;
        List<FormattedText> lines = new ArrayList<>(recipe.lines.size());
        lines.addAll(recipe.lines);
        builder.addText(lines, WIDTH - 4, getHeight() - y).setPosition(2, y).setColor(CropRecipeText.TEXT_COLOR)
                .setShadow(false).setLineSpacing(1);

        // 概率覆盖层是组件，因此在物品 sprite 之后绘制并保持可读
        int displayCount = Math.min(recipe.outputs.size(), MAX_OUTPUTS);
        for (int i = 0; i < displayCount; i++) {
            builder.addWidget(new ChanceWidget(OUTPUT_X + (i % 3) * SLOT, OUTPUT_Y + (i / 3) * SLOT,
                    String.format(Locale.ROOT, "%.2f%%", recipe.outputs.get(i).chance() * 100.0f)));
        }
    }

    @Override
    public void draw(CropInfoJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX,
                     double mouseY) {
        this.background.draw(guiGraphics, 0, 0);
    }

    private record ChanceWidget(int x, int y, String text) implements IRecipeWidget {

        @Override
        public ScreenPosition getPosition() {
            return new ScreenPosition(this.x, this.y);
        }

        @Override
        public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0, 0.0, 250.0);
            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
            guiGraphics.drawString(Minecraft.getInstance().font, this.text, 2, 2, PROBABILITY_COLOR, false);
            guiGraphics.pose().popPose();
        }
    }

    private record Drop(ItemStack stack, float chance) {}

    public static final class CropInfoJeiRecipe {

        private final ResourceLocation id;
        private final ItemStack seed;
        private final List<ItemStack> soils;
        private final List<ItemStack> subSoils;
        private final List<Drop> outputs = new ArrayList<>();
        private final List<Component> lines;

        private CropInfoJeiRecipe(ICropCard crop) {
            this.id = new ResourceLocation(CropsGTM.MOD_ID, "/crop_info/" +
                    crop.getId().toLowerCase(Locale.ROOT).replace(':', '/').replaceAll("[^a-z0-9_/.-]", "_"));
            this.seed = crop.getSeedItem(SeedStats.DEFAULT_ANALYZED);
            this.soils = CropRecipeText.nonEmptyStacks(crop.getSoilTypes().getDisplayStacks());
            this.subSoils = CropRecipeText.subSoilStacks(crop);

            // 掉落物按概率从高到低排序
            List<Map.Entry<ItemStack, Integer>> drops = new ArrayList<>(crop.getDropTable().entrySet());
            drops.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            double rounds = CropGrowthMath.getAvgDropRounds(crop, 1);
            for (Map.Entry<ItemStack, Integer> drop : drops) {
                float chance = drop.getValue() / 10000.0f;
                long amount = Math.max(1, Math.round(rounds * chance * drop.getKey().getCount()));
                this.outputs.add(new Drop(drop.getKey().copyWithCount((int) Math.min(64, amount)), chance));
            }

            this.lines = CropRecipeText.cropInfoLines(crop);
        }
    }
}
