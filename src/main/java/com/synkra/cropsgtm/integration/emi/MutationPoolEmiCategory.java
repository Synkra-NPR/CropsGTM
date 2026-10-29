package com.synkra.cropsgtm.integration.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IMutationPool;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.CropRecipeText;
import com.synkra.cropsgtm.reference.Names;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 按原版作物页面的排布展示互斥池：每个成员显示为种子，每行九个，下方显示池的名称。
 */
public class MutationPoolEmiCategory extends EmiRecipeCategory {

    public static final MutationPoolEmiCategory CATEGORY = new MutationPoolEmiCategory();

    private MutationPoolEmiCategory() {
        super(new ResourceLocation(CropsGTM.MOD_ID, "mutation_pool"), cropSticksIcon());
    }

    /** @return 用作分类图标的作物架物品，与原模组使用的图标一致。 */
    static EmiStack cropSticksIcon() {
        return EmiStack.of(CropsGTMBlocks.cropSticks().asItem());
    }

    @Override
    public Component getName() {
        return Component.translatable("cropsgtm_tooltip.viewer.pool.name");
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (IMutationPool pool : MutationRegistry.instance.getMutationPools()) {
            registry.addRecipe(new MutationPoolEmiRecipe(pool));
        }
    }

    public static class MutationPoolEmiRecipe extends BasicEmiRecipe {

        private static final int WIDTH = 170;
        private static final int BACKGROUND_WIDTH = 162;
        private static final int TEXTURE_SIZE = 256;
        private static final int SLOT = 18;
        private static final int COLUMNS = 9;
        private static final int SEED_X = 2;
        private static final int SEED_Y = 0;
        private static final int TEXT_COLOR = 0x404040;

        private static final ResourceLocation BACKGROUND = new ResourceLocation(CropsGTM.MOD_ID,
                "textures/gui/nei/mutation_pool.png");

        private final List<EmiStack> members;
        private final Component poolName;
        private final int rows;

        public MutationPoolEmiRecipe(IMutationPool pool) {
            super(CATEGORY,
                    new ResourceLocation(CropsGTM.MOD_ID,
                            "/mutation_pool/" +
                                    pool.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_/.-]", "_")),
                    WIDTH, 0);

            List<ICropCard> sorted = new ArrayList<>(pool.getMembers());
            sorted.sort(Comparator.comparing(crop -> Component.translatable(crop.getUnlocalizedName()).getString()));
            this.members = new ArrayList<>(sorted.size());
            for (ICropCard crop : sorted) {
                EmiStack seed = CropInfoEmiCategory.seedStack(crop);
                this.members.add(seed);
                this.inputs.add(seed);
            }

            this.rows = Math.max(1, (this.members.size() + COLUMNS - 1) / COLUMNS);
            this.poolName = Component.translatable("cropsgtm_tooltip.viewer.pool.poolName",
                    Component.translatable(Names.MOD_ID + "_mutationPool." + pool.getName()));
            // 背景高 rows * SLOT，名称从其下 2 像素处开始，再多
            // 在页面底部添加一行空文本
            this.height = this.rows * SLOT + 2 + CropRecipeText.textHeight(List.of(this.poolName), WIDTH - 4);
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            // 背景由顶部、任意行数的中间段和底部切片拼成
            widgets.addTexture(BACKGROUND, 2, 0, BACKGROUND_WIDTH, 9, 0, 0, BACKGROUND_WIDTH, 9, TEXTURE_SIZE,
                    TEXTURE_SIZE);
            int y = 9;
            for (int row = 1; row < this.rows; row++) {
                widgets.addTexture(BACKGROUND, 2, y, BACKGROUND_WIDTH, SLOT, 0, 10, BACKGROUND_WIDTH, SLOT,
                        TEXTURE_SIZE, TEXTURE_SIZE);
                y += SLOT;
            }
            widgets.addTexture(BACKGROUND, 2, y, BACKGROUND_WIDTH, 9, 0, 29, BACKGROUND_WIDTH, 9, TEXTURE_SIZE,
                    TEXTURE_SIZE);
            y += 9;

            for (int i = 0; i < this.members.size(); i++) {
                widgets.addSlot(this.members.get(i), SEED_X + SLOT * (i % COLUMNS), SEED_Y + SLOT * (i / COLUMNS))
                        .drawBack(false);
            }

            widgets.addText(this.poolName, 2, y + 2, TEXT_COLOR, false);
        }
    }
}
