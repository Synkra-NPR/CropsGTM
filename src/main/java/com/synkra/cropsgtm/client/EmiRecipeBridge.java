package com.synkra.cropsgtm.client;

import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;

/**
 * 隔离对 EMI 的硬引用，使 {@link CropsGTMRecipeViewer} 在未安装 EMI 时也能使用。
 */
final class EmiRecipeBridge {

    private EmiRecipeBridge() {}

    static void openCategory(String categoryId) {
        ResourceLocation id = new ResourceLocation(categoryId);
        for (EmiRecipeCategory category : EmiApi.getRecipeManager().getCategories()) {
            if (category.getId().equals(id)) {
                EmiApi.displayRecipeCategory(category);
                return;
            }
        }
    }
}
