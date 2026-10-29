package com.synkra.cropsgtm.api;

import com.synkra.cropsgtm.farming.registries.SoilRegistry;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * 解析数据包作物定义引用的土壤 id。
 * <p>
 * 土壤列表本身由 {@code data/cropsgtm/soils} 定义，该类只映射作物文件中使用的 id，包括旧式的蛇形命名。
 * </p>
 */
public final class CropsGTMSoilTypes {

    /** 未声明土壤的作物所使用的土壤。 */
    public static final String DEFAULT = "farmland";

    /** 在数据包定义出现之前以蛇形命名书写的土壤 id。 */
    private static final Map<String, String> ALIASES = Map.of("dirt_grass", "dirtGrass", "nether_mushroom",
            "netherMushroom", "oil_sands", "oilSands", "slimy_dirt", "slimyDirt");

    private CropsGTMSoilTypes() {}

    /**
     * @return 指定 id 的土壤列表；没有数据包定义时返回 null。
     */
    public static @Nullable ISoilList get(String id) {
        if (id == null) return null;
        String resolved = ALIASES.getOrDefault(id, id);
        if ("global".equals(resolved)) return SoilRegistry.instance.allSoils();
        if (!SoilRegistry.instance.contains(resolved)) return null;
        return SoilRegistry.instance.get(resolved);
    }

    /**
     * @return 指定 id 的土壤列表；数据包未定义该 id 时先回退到默认土壤，再回退到全局土壤列表。
     */
    public static ISoilList getOrDefault(String id) {
        ISoilList soil = get(id);
        if (soil != null) return soil;
        ISoilList farmland = get(DEFAULT);
        return farmland != null ? farmland : SoilRegistry.instance.allSoils();
    }
}
