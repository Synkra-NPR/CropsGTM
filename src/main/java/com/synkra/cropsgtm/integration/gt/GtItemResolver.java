package com.synkra.cropsgtm.integration.gt;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;

import net.minecraft.world.item.ItemStack;

import com.google.gson.JsonObject;

/**
 * 解析作物定义中的 GregTech 材质掉落。
 */
final class GtItemResolver {

    private GtItemResolver() {}

    /**
     * @return 掉落定义中 {@code "gt"} 条目描述的物品；标签前缀或材料不存在时返回空物品。
     */
    static ItemStack resolve(JsonObject json, int count) {
        TagPrefix prefix = getTagPrefix(getString(json, "gt", ""));
        Material material = GTCEuAPI.materialManager.getMaterial(getString(json, "material", ""));
        if (prefix == null || material == null) return ItemStack.EMPTY;
        return ChemicalHelper.get(prefix, material, count);
    }

    private static TagPrefix getTagPrefix(String name) {
        return switch (name) {
            case "dust" -> TagPrefix.dust;
            case "dustTiny", "tinyDust" -> TagPrefix.dustTiny;
            case "dustSmall", "smallDust" -> TagPrefix.dustSmall;
            case "gem" -> TagPrefix.gem;
            case "gemExquisite", "exquisiteGem" -> TagPrefix.gemExquisite;
            case "gemFlawless", "flawlessGem" -> TagPrefix.gemFlawless;
            case "gemFlawed", "flawedGem" -> TagPrefix.gemFlawed;
            case "gemChipped", "chippedGem" -> TagPrefix.gemChipped;
            case "crushedPurified", "purifiedOre" -> TagPrefix.crushedPurified;
            case "crushed", "crushedOre" -> TagPrefix.crushed;
            case "plate" -> TagPrefix.plate;
            case "ingot" -> TagPrefix.ingot;
            case "nugget" -> TagPrefix.nugget;
            case "rodLong", "longRod" -> TagPrefix.rodLong;
            case "block" -> TagPrefix.block;
            default -> TagPrefix.get(name);
        };
    }

    private static String getString(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }
}
