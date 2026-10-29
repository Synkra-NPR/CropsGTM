package com.synkra.cropsgtm.integration.gt;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.MaterialBlock;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.synkra.cropsgtm.CropsGTM;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 下层土壤需求的 GregTech 材质匹配。
 * <p>
 * 核心的下层土壤需求只保存数据包中的材质名，解析为 GregTech 材质并检查材质方块在这里完成。
 * </p>
 */
public final class GtSubSoilMaterials {

    private GtSubSoilMaterials() {}

    /**
     * 丢弃 GregTech 不认识的材质 id。
     * 
     * @return 能解析的材质 id，顺序与传入一致
     */
    public static List<String> resolve(String requirementId, List<String> materialIds) {
        List<String> resolved = new ArrayList<>(materialIds.size());
        for (String materialId : materialIds) {
            if (GTCEuAPI.materialManager.getMaterial(materialId) == null) {
                CropsGTM.LOGGER.warn("The sub-soil {} refers to the unknown material {}", requirementId, materialId);
                continue;
            }
            resolved.add(materialId);
        }
        return resolved;
    }

    /**
     * @return 该方块状态是这些材料之一的矿石、粗矿或存储方块时返回 true。
     */
    public static boolean matches(Set<String> materialIds, BlockState state) {
        if (state == null || state.isAir()) return false;
        if (!(state.getBlock() instanceof MaterialBlock materialBlock)) return false;
        Material material = materialBlock.material;
        if (material == null || !materialIds.contains(material.getName())) return false;
        TagPrefix prefix = materialBlock.tagPrefix;
        return TagPrefix.ORES.containsKey(prefix) || prefix == TagPrefix.block || prefix == TagPrefix.rawOreBlock;
    }

    /**
     * @return 这些材料的矿石、粗矿与存储方块物品，用作繁殖机与复制机的催化剂并显示在配方查看器中。
     */
    public static List<ItemStack> getItems(Set<String> materialIds) {
        List<ItemStack> items = new ArrayList<>();
        for (String materialId : materialIds) {
            Material material = GTCEuAPI.materialManager.getMaterial(materialId);
            if (material == null) continue;
            // 矿石有花岗岩、深板岩、下界岩、砂岩等多种石种变体，全部列为催化剂
            for (TagPrefix prefix : TagPrefix.ORES.keySet()) {
                ItemStack stack = ChemicalHelper.get(prefix, material);
                if (!stack.isEmpty()) items.add(stack);
            }
            for (TagPrefix prefix : new TagPrefix[] { TagPrefix.rawOreBlock, TagPrefix.block }) {
                ItemStack stack = ChemicalHelper.get(prefix, material);
                if (!stack.isEmpty()) items.add(stack);
            }
        }
        return items;
    }
}
