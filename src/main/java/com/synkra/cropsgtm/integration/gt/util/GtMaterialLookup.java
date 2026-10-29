package com.synkra.cropsgtm.integration.gt.util;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;

import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;

/**
 * 为配方按 id 引用的 GregTech 材质与流体提供空值安全的查询。
 * <p>
 * GregTech 对未知 id 返回 null，而且大多数材质没有流体，否则每个调用方都要重复相同的空值与属性检查。
 * </p>
 */
public final class GtMaterialLookup {

    private GtMaterialLookup() {}

    /** @return 指定 id 的材质；GregTech 不认识时返回 null。 */
    @Nullable
    public static Material material(String id) {
        return GTCEuAPI.materialManager.getMaterial(id);
    }

    /** @return 指定 id 且有流体的材质；否则返回 null。 */
    @Nullable
    public static Material materialFluid(String id) {
        Material material = material(id);
        return material != null && material.hasProperty(PropertyKey.FLUID) ? material : null;
    }

    /** @return 该材料的流体；材料未知或没有流体时返回 null。 */
    @Nullable
    public static FluidStack fluid(String id, int amount) {
        Material material = materialFluid(id);
        return material == null ? null : material.getFluid(amount);
    }
}
