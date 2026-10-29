package com.synkra.cropsgtm.farming.registries;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.farming.SeedShapeDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 存放数据包声明的种子剪影，作物定义通过 id 引用它们。
 */
public class SeedShapeRegistry {

    public static final SeedShapeRegistry instance = new SeedShapeRegistry();

    private static final String DEFAULT_ID = "vanilla";

    private final Map<String, SeedShapeDefinition> shapes = new LinkedHashMap<>();
    private SeedShapeDefinition defaultShape = SeedShapeDefinition.fallback();

    private SeedShapeRegistry() {}

    /**
     * 用数据包声明的形状替换现有形状。
     */
    public void reload(Map<String, SeedShapeDefinition> definitions) {
        this.shapes.clear();
        this.shapes.putAll(definitions);
        SeedShapeDefinition vanilla = this.shapes.get(DEFAULT_ID);
        if (vanilla != null) {
            this.defaultShape = vanilla;
            return;
        }
        this.defaultShape = this.shapes.values().stream().findFirst().orElse(SeedShapeDefinition.fallback());
    }

    /**
     * @return 指定 id 的外观形状；id 未知时返回默认形状。
     */
    public SeedShapeDefinition get(String id) {
        SeedShapeDefinition shape = this.shapes.get(id);
        if (shape != null) return shape;
        CropsGTM.LOGGER.error("The seed shape {} is unknown, the default shape is used instead.", id);
        return this.defaultShape;
    }

    public SeedShapeDefinition getDefault() {
        return this.defaultShape;
    }
}
