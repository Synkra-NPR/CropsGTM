package com.synkra.cropsgtm.farming;

import net.minecraft.resources.ResourceLocation;

import com.synkra.cropsgtm.reference.Names;

/**
 * 由数据包声明的种子剪影。
 * <p>
 * 作物定义通过 id 引用该形状，形状本身只通过自定义模型数据选择通用种子的物品模型。保留贴图路径是为了让数据包说明模型应使用哪些图层，游戏本身并不读取它们。
 * </p>
 * 
 * @param id           作物定义引用的 id
 * @param modelData    绘制剪影的物品模型的自定义模型数据
 * @param darkTexture  暗色图层的贴图
 * @param lightTexture 亮色图层的贴图
 */
public record SeedShapeDefinition(String id, int modelData, ResourceLocation darkTexture,
                                  ResourceLocation lightTexture) {

    /** 数据包未提供形状时使用的形状，与原版剪影的模型一致。 */
    public static SeedShapeDefinition fallback() {
        return new SeedShapeDefinition("vanilla", 8, new ResourceLocation(Names.MOD_ID, "item/genericseed/vanilla1"),
                new ResourceLocation(Names.MOD_ID, "item/genericseed/vanilla2"));
    }
}
