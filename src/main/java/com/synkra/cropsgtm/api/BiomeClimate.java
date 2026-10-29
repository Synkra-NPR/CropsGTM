package com.synkra.cropsgtm.api;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 把作物偏好的气候标签与生物群系进行匹配。
 * <p>
 * 作物定义要求诸如 {@code forge:is_hot} 或 {@code minecraft:is_jungle} 的生物群系标签。大型生物群系模组（Biomes O' Plenty、Oh The Biomes We've
 * Gone、Regions Unexplored）自带这些标签，但许多其他模组（Twilight Forest、The Aether、Alex's Caves 等）没有，而 Forge
 * 已不再提供自动分类用的生物群系字典。因此当某个模组生物群系没有显式标签覆盖时，会根据每个生物群系注册的温度与湿度近似判断其气候，使无标签的模组生物群系仍能算作炎热、寒冷、多雪、干燥或潮湿。原版生物群系始终带有真实标签，因此该回退不会覆盖它们。
 * </p>
 */
public final class BiomeClimate {

    private BiomeClimate() {}

    /**
     * @param biome 要检查的生物群系
     * @param tag   作物偏好的标签
     * @return 该生物群系是否满足该标签，直接带标签或通过气候回退判定都算
     */
    public static boolean matches(Holder<Biome> biome, TagKey<Biome> tag) {
        if (biome.is(tag)) return true;
        // 原版生物群系始终带标签，回退机制只用于填补其他模组的空缺
        boolean vanilla = biome.unwrapKey().map(key -> "minecraft".equals(key.location().getNamespace())).orElse(false);
        if (vanilla) return false;
        Biome.ClimateSettings climate = biome.value().getModifiedClimateSettings();
        float temperature = climate.temperature();
        // 作物定义在解析数据包时会创建自己的标签键，因此这些标签
        // 按位置而非实例进行比较
        ResourceLocation id = tag.location();
        if (id.equals(CropBiomeTags.SNOWY.location())) return climate.hasPrecipitation() && temperature <= 0.15f;
        if (id.equals(CropBiomeTags.COLD.location())) return temperature <= 0.25f;
        if (id.equals(CropBiomeTags.HOT.location())) return temperature >= 1.0f;
        if (id.equals(CropBiomeTags.WET.location())) return climate.downfall() >= 0.85f;
        if (id.equals(CropBiomeTags.DRY.location())) return climate.downfall() <= 0.1f;
        return false;
    }
}
