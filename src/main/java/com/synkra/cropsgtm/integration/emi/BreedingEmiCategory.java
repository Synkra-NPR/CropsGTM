package com.synkra.cropsgtm.integration.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.mutation.CropMutation;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.integration.CropRecipeText;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * 展示某个作物的全部确定性繁殖配方：上方是亲本，中间是结果种子，其下是所需土壤与下层土壤，布局下方是额外繁殖条件。只在作物繁殖机中生效的突变也会列出，其条件行会说明该作物只能在机器中繁殖。
 */
public class BreedingEmiCategory extends EmiRecipeCategory {

    public static final BreedingEmiCategory CATEGORY = new BreedingEmiCategory();

    private BreedingEmiCategory() {
        super(new ResourceLocation(CropsGTM.MOD_ID, "breeding"), MutationPoolEmiCategory.cropSticksIcon());
    }

    @Override
    public Component getName() {
        return Component.translatable("cropsgtm_tooltip.viewer.breeding.name");
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (ICropCard output : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (output.hideFromNEI()) continue;
            Collection<CropMutation> mutations = MutationRegistry.instance.getDeterministicMutationsForCrop(output);
            if (mutations == null) continue;
            int index = 0;
            for (CropMutation mutation : mutations) {
                // 只在该机器中才会生效的突变也一并保留在此页，
                // 布局下方的繁殖要求用来提示玩家需要这台机器
                registry.addRecipe(new CropBreedingEmiRecipe(mutation, index++));
            }
        }
    }

    public static class CropBreedingEmiRecipe extends BasicEmiRecipe {

        private static final int WIDTH = 180;
        private static final int BACKGROUND_WIDTH = 166;
        private static final int BACKGROUND_HEIGHT = 87;
        private static final int TEXTURE_SIZE = 256;
        private static final int SLOT = 18;
        private static final int OUTPUT_X = 74;
        private static final int OUTPUT_Y = 21;
        private static final int SOIL_Y = 42;
        private static final int SUB_SOIL_Y = 63;
        private static final int TEXT_COLOR = 0x404040;
        private static final int[] PARENT_X = { 43, 105, 22, 126 };
        private static final int PARENT_Y = 21;

        private static final ResourceLocation BACKGROUND_TWO = new ResourceLocation(CropsGTM.MOD_ID,
                "textures/gui/nei/breeding2.png");
        private static final ResourceLocation BACKGROUND_FOUR = new ResourceLocation(CropsGTM.MOD_ID,
                "textures/gui/nei/breeding4.png");

        private final EmiStack output;
        private final List<EmiStack> parents;
        private final List<EmiStack> soils;
        private final List<EmiStack> subSoils;
        private final List<Component> lines = new ArrayList<>();
        private final ResourceLocation background;

        public CropBreedingEmiRecipe(CropMutation mutation, int index) {
            super(CATEGORY,
                    new ResourceLocation(CropsGTM.MOD_ID, "/breeding/" + mutation.getOutput().getId()
                            .toLowerCase(Locale.ROOT).replace(':', '/').replaceAll("[^a-z0-9_/.-]", "_") + "/" + index),
                    WIDTH, 0);

            ICropCard crop = mutation.getOutput();
            this.output = CropInfoEmiCategory.seedStack(crop);
            this.outputs.add(this.output);
            this.background = mutation.getParentCount() > 2 ? BACKGROUND_FOUR : BACKGROUND_TWO;

            this.parents = new ArrayList<>(mutation.getParentCount());
            for (ICropCard parent : mutation.getParents()) {
                EmiStack seed = CropInfoEmiCategory.seedStack(parent);
                this.parents.add(seed);
                this.inputs.add(seed);
            }

            this.soils = CropInfoEmiCategory.CropInfoEmiRecipe
                    .toEmiStacks(CropRecipeText.nonEmptyStacks(crop.getSoilTypes().getDisplayStacks()));
            this.subSoils = CropInfoEmiCategory.CropInfoEmiRecipe.toEmiStacks(CropRecipeText.subSoilStacks(crop));
            for (EmiStack soil : this.soils) {
                this.catalysts.add(soil);
            }
            for (EmiStack subSoil : this.subSoils) {
                this.catalysts.add(subSoil);
            }

            this.lines.addAll(CropRecipeText.breedingLines(mutation, crop));

            this.height = BACKGROUND_HEIGHT + 4 + CropRecipeText.textHeight(this.lines, WIDTH - 4);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // 背景包含作物架图标、亲本槽、箭头与土壤槽
            widgets.addTexture(this.background, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 5, 11, BACKGROUND_WIDTH,
                    BACKGROUND_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

            for (int i = 0; i < this.parents.size() && i < PARENT_X.length; i++) {
                widgets.addSlot(this.parents.get(i), PARENT_X[i], PARENT_Y).drawBack(false);
            }
            widgets.addSlot(this.output, OUTPUT_X, OUTPUT_Y).drawBack(false).recipeContext(this);
            if (!this.soils.isEmpty()) {
                widgets.addGeneratedSlot(random -> this.soils.get(random.nextInt(this.soils.size())), 1, OUTPUT_X,
                        SOIL_Y).drawBack(false);
            }
            if (!this.subSoils.isEmpty()) {
                widgets.addGeneratedSlot(random -> this.subSoils.get(random.nextInt(this.subSoils.size())), 2, OUTPUT_X,
                        SUB_SOIL_Y).drawBack(false);
            }

            int y = BACKGROUND_HEIGHT + 2;
            for (Component line : this.lines) {
                int height = CropRecipeText.textHeight(List.of(line), WIDTH - 4);
                widgets.addText(line, 2, y, TEXT_COLOR, false);
                y += height;
            }
        }
    }
}
