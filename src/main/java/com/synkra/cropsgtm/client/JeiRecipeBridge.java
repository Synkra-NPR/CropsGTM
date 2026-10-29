package com.synkra.cropsgtm.client;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.runtime.IJeiRuntime;

import java.util.List;

/**
 * 隔离对 JEI 的硬引用，使 {@link CropsGTMRecipeViewer} 在未安装 JEI 时也能使用。
 */
public final class JeiRecipeBridge {

    private static IJeiRuntime runtime;

    private JeiRecipeBridge() {}

    public static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void onRuntimeUnavailable() {
        runtime = null;
    }

    public static void openCategory(String categoryId) {
        IJeiRuntime jeiRuntime = runtime;
        if (jeiRuntime == null) return;
        jeiRuntime.getRecipeManager().getRecipeType(new ResourceLocation(categoryId))
                .ifPresent(type -> jeiRuntime.getRecipesGui().showTypes(List.of(type)));
    }
}
