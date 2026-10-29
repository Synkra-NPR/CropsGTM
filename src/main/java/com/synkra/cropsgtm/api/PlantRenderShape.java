package com.synkra.cropsgtm.api;

/**
 * 作物立在作物架上时植物的不同渲染方式。
 */
public enum PlantRenderShape {
    /** 覆盖整个方块的四张交叉平面，大多数作物使用。 */
    HASH,
    /** 两张交叉平面，用于高而细的作物。 */
    X,
    /** 在方块内向外展开的四张交叉平面，花朵使用。 */
    FLOWER
}
