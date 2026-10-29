package com.synkra.cropsgtm.client;

import net.minecraftforge.fml.ModList;

/**
 * 打开机器物品的配方查看页，用于玩家点击机器界面内的进度箭头时。
 */
public final class CropsGTMRecipeViewer {

    private CropsGTMRecipeViewer() {}

    public static void openCategory(String categoryId) {
        if (categoryId == null) return;
        try {
            if (ModList.get().isLoaded("emi")) {
                EmiRecipeBridge.openCategory(categoryId);
            } else if (ModList.get().isLoaded("jei")) {
                JeiRecipeBridge.openCategory(categoryId);
            }
        } catch (Throwable ignored) {
            // 配方查看器是可选功能，绝不让一次界面点击把游戏搞崩
        }
    }
}
