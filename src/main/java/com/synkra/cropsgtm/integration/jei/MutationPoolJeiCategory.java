package com.synkra.cropsgtm.integration.jei;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IMutationPool;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.integration.CropRecipeText;
import com.synkra.cropsgtm.reference.Names;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.registration.IRecipeRegistration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 按原版作物页面的排布展示互斥池：每个成员显示为种子，每行九个，下方显示池的名称。
 */
public class MutationPoolJeiCategory extends AbstractRecipeCategory<MutationPoolJeiCategory.MutationPoolJeiRecipe> {

    public static final RecipeType<MutationPoolJeiRecipe> RECIPE_TYPE = RecipeType.create(CropsGTM.MOD_ID,
            "mutation_pool", MutationPoolJeiRecipe.class);

    private static final int WIDTH = 170;
    private static final int BACKGROUND_WIDTH = 162;
    private static final int SLOT = 18;
    private static final int COLUMNS = 9;
    /** 物品的位置，背景边框在它周围留出一个像素。 */
    private static final int SEED_X = 3;
    private static final int SEED_Y = 1;

    private static final ResourceLocation BACKGROUND = new ResourceLocation(CropsGTM.MOD_ID,
            "textures/gui/nei/mutation_pool.png");

    private final IDrawable top;
    private final IDrawable middle;
    private final IDrawable bottom;

    public MutationPoolJeiCategory(IJeiHelpers helpers) {
        super(RECIPE_TYPE, Component.translatable("cropsgtm_tooltip.viewer.pool.name"), helpers.getGuiHelper()
                .createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CropsGTMBlocks.cropSticks())), WIDTH,
                maxRows() * SLOT + 2 + maxTextHeight());
        IGuiHelper guiHelper = helpers.getGuiHelper();
        // 背景由顶部、任意行数的中间段和底部切片拼成
        this.top = guiHelper.createDrawable(BACKGROUND, 0, 0, BACKGROUND_WIDTH, 9);
        this.middle = guiHelper.createDrawable(BACKGROUND, 0, 10, BACKGROUND_WIDTH, SLOT);
        this.bottom = guiHelper.createDrawable(BACKGROUND, 0, 29, BACKGROUND_WIDTH, 9);
    }

    /**
     * @return 最大互斥池所需的行数，该分类的高度由所有配方共用。
     */
    private static int maxRows() {
        int rows = 1;
        for (IMutationPool pool : MutationRegistry.instance.getMutationPools()) {
            rows = Math.max(rows, (pool.getMembers().size() + COLUMNS - 1) / COLUMNS);
        }
        return rows;
    }

    /**
     * @return 最长池名所需的文本高度，该分类的高度由所有配方共用。
     */
    private static int maxTextHeight() {
        int height = 10;
        for (IMutationPool pool : MutationRegistry.instance.getMutationPools()) {
            height = Math.max(height, CropRecipeText.textHeight(List.of(poolName(pool)), WIDTH - 4));
        }
        return height;
    }

    private static Component poolName(IMutationPool pool) {
        return Component.translatable("cropsgtm_tooltip.viewer.pool.poolName",
                Component.translatable(Names.MOD_ID + "_mutationPool." + pool.getName()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<MutationPoolJeiRecipe> recipes = new ArrayList<>();
        for (IMutationPool pool : MutationRegistry.instance.getMutationPools()) {
            recipes.add(new MutationPoolJeiRecipe(pool));
        }
        registration.addRecipes(RECIPE_TYPE, recipes);
    }

    @Override
    public ResourceLocation getRegistryName(MutationPoolJeiRecipe recipe) {
        return recipe.id;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MutationPoolJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.members.size(); i++) {
            builder.addInputSlot(SEED_X + SLOT * (i % COLUMNS), SEED_Y + SLOT * (i / COLUMNS))
                    .addItemStack(recipe.members.get(i));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MutationPoolJeiRecipe recipe, IFocusGroup focuses) {
        int y = recipe.textY + 2;
        builder.addText(List.of((FormattedText) recipe.poolName), WIDTH - 4, getHeight() - y).setPosition(2, y)
                .setColor(CropRecipeText.TEXT_COLOR).setShadow(false).setLineSpacing(1);
    }

    @Override
    public void draw(MutationPoolJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        this.top.draw(guiGraphics, 2, 0);
        int y = 9;
        for (int row = 1; row < recipe.rows; row++) {
            this.middle.draw(guiGraphics, 2, y);
            y += SLOT;
        }
        this.bottom.draw(guiGraphics, 2, y);
    }

    public static final class MutationPoolJeiRecipe {

        private final ResourceLocation id;
        private final List<ItemStack> members;
        private final Component poolName;
        private final int rows;
        private final int textY;

        private MutationPoolJeiRecipe(IMutationPool pool) {
            this.id = new ResourceLocation(CropsGTM.MOD_ID, "/mutation_pool/" +
                    pool.getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_/.-]", "_"));

            List<ICropCard> sorted = new ArrayList<>(pool.getMembers());
            sorted.sort(Comparator.comparing(crop -> Component.translatable(crop.getUnlocalizedName()).getString()));
            this.members = new ArrayList<>(sorted.size());
            for (ICropCard crop : sorted) {
                this.members.add(crop.getSeedItem(SeedStats.DEFAULT_ANALYZED));
            }

            this.rows = Math.max(1, (this.members.size() + COLUMNS - 1) / COLUMNS);
            this.poolName = poolName(pool);
            // 背景高 rows * SLOT，名称从其下 2 像素处开始
            this.textY = this.rows * SLOT;
        }
    }
}
