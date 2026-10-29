package com.synkra.cropsgtm.integration.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.init.CropsGTMItems;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.Locale;

/**
 * 展示可用作作物种子的原版与模组物品，对应原作模组的替代种子页面。
 */
public class AlternateSeedEmiCategory extends EmiRecipeCategory {

    public static final AlternateSeedEmiCategory CATEGORY = new AlternateSeedEmiCategory();

    private AlternateSeedEmiCategory() {
        super(new ResourceLocation(CropsGTM.MOD_ID, "alternate_seeds"), CropInfoEmiCategory.categoryIcon());
    }

    @Override
    public Component getName() {
        return Component.translatable("cropsgtm_tooltip.viewer.altSeeds.name");
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.hideFromNEI()) continue;
            int index = 0;
            for (ItemStack alternateSeed : crop.getAlternateSeeds()) {
                if (alternateSeed.isEmpty()) continue;
                registry.addRecipe(new AlternateSeedEmiRecipe(crop, alternateSeed, index++));
            }
        }
    }

    public static class AlternateSeedEmiRecipe extends BasicEmiRecipe {

        private static final int WIDTH = 170;
        private static final int HEIGHT = 22;
        /** 边框在贴图表中的位置。 */
        private static final int BACKGROUND_U = 59;
        private static final int BACKGROUND_V = 12;
        /** 边框在贴图表中的尺寸。 */
        private static final int BACKGROUND_WIDTH = 58;
        private static final int BACKGROUND_HEIGHT = 18;
        /** 叠加在边框位置上的绘制偏移，使边框与槽位对齐。 */
        private static final int BACKGROUND_OFFSET_X = -5;
        private static final int BACKGROUND_OFFSET_Y = -11;
        private static final int TEXTURE_SIZE = 256;
        /** 第二个槽位在贴图表中的位置。 */
        private static final int OUTPUT_U = 100;
        private static final int INPUT_X = BACKGROUND_U + BACKGROUND_OFFSET_X;
        private static final int INPUT_Y = BACKGROUND_V + BACKGROUND_OFFSET_Y;
        private static final int OUTPUT_X = OUTPUT_U + BACKGROUND_OFFSET_X;
        private static final int OUTPUT_Y = BACKGROUND_V + BACKGROUND_OFFSET_Y;

        private static final ResourceLocation BACKGROUND = new ResourceLocation(CropsGTM.MOD_ID,
                "textures/gui/nei/alternate_seeds.png");

        private final EmiStack input;
        private final EmiStack output;

        public AlternateSeedEmiRecipe(ICropCard crop, ItemStack alternateSeed, int index) {
            super(CATEGORY,
                    new ResourceLocation(CropsGTM.MOD_ID, "/alternate_seed/" +
                            crop.getId().toLowerCase(Locale.ROOT).replace(':', '/').replaceAll("[^a-z0-9_/.-]", "_") +
                            "/" + index),
                    WIDTH, HEIGHT);

            this.input = EmiStack.of(alternateSeed);
            this.output = CropInfoEmiCategory.seedStack(crop);
            this.inputs.add(this.input);
            this.outputs.add(this.output);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // 边框位于贴图表中的 (59, 12)，绘制在同一位置加上
            // 偏移量使其槽位与 EMI 槽位对齐
            widgets.addTexture(BACKGROUND, BACKGROUND_U + BACKGROUND_OFFSET_X, BACKGROUND_V + BACKGROUND_OFFSET_Y,
                    BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_U, BACKGROUND_V, BACKGROUND_WIDTH,
                    BACKGROUND_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
            widgets.addSlot(this.input, INPUT_X, INPUT_Y).drawBack(false);
            widgets.addSlot(this.output, OUTPUT_X, OUTPUT_Y).drawBack(false).recipeContext(this);
        }
    }
}
