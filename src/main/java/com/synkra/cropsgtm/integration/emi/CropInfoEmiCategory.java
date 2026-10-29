package com.synkra.cropsgtm.integration.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.CropGrowthMath;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.CropRecipeText;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 展示作物的种植方式：种子、可种植的土壤、所需下层土壤、收获掉落，以及布局下方的作物信息。
 */
public class CropInfoEmiCategory extends EmiRecipeCategory {

    public static final CropInfoEmiCategory CATEGORY = new CropInfoEmiCategory();

    private CropInfoEmiCategory() {
        super(new ResourceLocation(CropsGTM.MOD_ID, "crop_info"), categoryIcon());
    }

    /**
     * @return 用作分类图标的、已完成分析的种子物品，这样通用种子没有标签时图标不会退化成方块物品。
     */
    static EmiStack categoryIcon() {
        ICropCard crop = CropRegistry.instance.get("cropsgtm:wheat");
        if (crop == null) return EmiStack.of(CropsGTMItems.genericSeed());
        return seedStack(crop);
    }

    /**
     * @return 该作物已完成分析的种子在 EMI 中的表示。种子被排除在 EMI 的批量渲染之外，否则共享的种子物品只会按其中一种自定义模型数据渲染。
     */
    static EmiStack seedStack(ICropCard crop) {
        EmiStack stack = EmiStack.of(crop.getSeedItem(SeedStats.DEFAULT_ANALYZED));
        if (stack instanceof dev.emi.emi.screen.StackBatcher.Batchable batchable) batchable.setUnbatchable();
        return stack;
    }

    @Override
    public Component getName() {
        return Component.translatable("cropsgtm_tooltip.viewer.cropInfo.name");
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (crop.hideFromNEI()) continue;
            registry.addRecipe(new CropInfoEmiRecipe(crop));
        }
    }

    /**
     * @return 列出该作物生长更快的生物群系的气候行。
     */
    public static Component climateLine(ICropCard crop) {
        Set<TagKey<Biome>> biomes = crop.getLikedBiomeTags();
        if (biomes.isEmpty()) return Component.translatable("cropsgtm_tooltip.viewer.climate.none");
        String joined = biomes.stream().map(tag -> tag.location().toString()).collect(Collectors.joining(", "));
        return Component.translatable("cropsgtm_tooltip.viewer.climate", joined);
    }

    public static class CropInfoEmiRecipe extends BasicEmiRecipe {

        private static final int WIDTH = 180;
        private static final int BACKGROUND_WIDTH = 166;
        private static final int BACKGROUND_HEIGHT = 85;
        private static final int TEXTURE_SIZE = 256;
        private static final int SLOT = 18;
        private static final int SEED_X = 32;
        private static final int SEED_Y = 19;
        private static final int SOIL_X = 32;
        private static final int SOIL_Y = 40;
        private static final int SUB_SOIL_X = 32;
        private static final int SUB_SOIL_Y = 61;
        private static final int OUTPUT_X = 103;
        private static final int OUTPUT_Y = 15;
        private static final int PROBABILITY_COLOR = 0xFFA500;
        private static final int MAX_OUTPUTS = 9;
        private static final int TEXT_COLOR = 0x404040;

        private static final ResourceLocation BACKGROUND = new ResourceLocation(CropsGTM.MOD_ID,
                "textures/gui/nei/crop_list.png");

        private final EmiStack seed;
        private final List<EmiStack> soils;
        private final List<EmiStack> subSoils;
        private final List<Component> lines = new ArrayList<>();

        public CropInfoEmiRecipe(ICropCard crop) {
            super(CATEGORY,
                    new ResourceLocation(CropsGTM.MOD_ID, "/crop_info/" +
                            crop.getId().toLowerCase(Locale.ROOT).replace(':', '/').replaceAll("[^a-z0-9_/.-]", "_")),
                    WIDTH, 0);

            this.seed = seedStack(crop);
            this.inputs.add(this.seed);

            this.soils = toEmiStacks(CropRecipeText.nonEmptyStacks(crop.getSoilTypes().getDisplayStacks()));
            this.subSoils = toEmiStacks(CropRecipeText.subSoilStacks(crop));
            for (EmiStack soil : this.soils) {
                this.catalysts.add(soil);
            }
            for (EmiStack subSoil : this.subSoils) {
                this.catalysts.add(subSoil);
            }

            // 掉落物按概率从高到低排序
            List<Map.Entry<ItemStack, Integer>> drops = new ArrayList<>(crop.getDropTable().entrySet());
            drops.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            double rounds = CropGrowthMath.getAvgDropRounds(crop, 1);
            for (Map.Entry<ItemStack, Integer> drop : drops) {
                float chance = drop.getValue() / 10000.0f;
                long amount = Math.max(1, Math.round(rounds * chance * drop.getKey().getCount()));
                this.outputs.add(EmiStack.of(drop.getKey()).setAmount(amount).setChance(chance));
            }

            this.lines.addAll(CropRecipeText.cropInfoLines(crop));

            this.height = BACKGROUND_HEIGHT + 4 + CropRecipeText.textHeight(this.lines, WIDTH - 4);
        }

        public static List<EmiStack> toEmiStacks(List<ItemStack> stacks) {
            List<EmiStack> result = new ArrayList<>(stacks.size());
            for (ItemStack stack : stacks) {
                if (!stack.isEmpty()) result.add(EmiStack.of(stack));
            }
            return result;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // 背景包含作物架图标、槽位边框与箭头
            widgets.addTexture(BACKGROUND, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, 5, 11, BACKGROUND_WIDTH,
                    BACKGROUND_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

            widgets.addSlot(this.seed, SEED_X, SEED_Y).drawBack(false);
            if (!this.soils.isEmpty()) {
                widgets.addGeneratedSlot(random -> this.soils.get(random.nextInt(this.soils.size())), 1, SOIL_X, SOIL_Y)
                        .drawBack(false);
            }
            if (!this.subSoils.isEmpty()) {
                widgets.addGeneratedSlot(random -> this.subSoils.get(random.nextInt(this.subSoils.size())), 2,
                        SUB_SOIL_X, SUB_SOIL_Y).drawBack(false);
            }
            int outputCount = Math.min(this.outputs.size(), MAX_OUTPUTS);
            for (int i = 0; i < outputCount; i++) {
                widgets.addSlot(this.outputs.get(i), OUTPUT_X + (i % 3) * SLOT, OUTPUT_Y + (i / 3) * SLOT)
                        .drawBack(false).recipeContext(this);
            }
            // 概率覆盖层在每排槽位之后添加并使用较大的 z 值，否则物品
            // sprite（z=100）与槽位悬停高亮（z=200）覆盖文本
            for (int i = 0; i < outputCount; i++) {
                int x = OUTPUT_X + (i % 3) * SLOT;
                int y = OUTPUT_Y + (i / 3) * SLOT;
                String chance = String.format(Locale.ROOT, "%.2f%%", this.outputs.get(i).getChance() * 100.0f);
                widgets.addDrawable(x, y, SLOT, SLOT, (draw, mouseX, mouseY, delta) -> {
                    draw.pose().pushPose();
                    draw.pose().translate(0.0, 0.0, 250.0);
                    draw.pose().scale(0.5f, 0.5f, 1.0f);
                    draw.drawString(Minecraft.getInstance().font, chance, 2, 2, PROBABILITY_COLOR, false);
                    draw.pose().popPose();
                });
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
