package com.synkra.cropsgtm.farming;

/**
 * GregTech 作物管理器与备用作物管理器共用的开关。
 * <p>
 * 按钮 id 就是备用容器界面发送的 id，使两台机器对同一个按钮的解释一致。GregTech 机器把开关存在同步字段中，备用机器持久化在自己的方块实体标签里，下面的常量保证键名同步。
 * </p>
 */
public interface CropManagerSettings {

    String NBT_AUTO_OUTPUT = "AutoOutput";
    String NBT_WORKING = "Working";
    String NBT_HARVEST = "HarvestEnabled";
    String NBT_WATER = "WaterEnabled";
    String NBT_WEED_EX = "WeedExEnabled";
    String NBT_FERTILIZER = "FertilizerEnabled";

    /** 容器按钮 id，顺序与备用界面的按钮相同。 */
    int BUTTON_AUTO_OUTPUT = 0;
    int BUTTON_WORKING = 1;
    int BUTTON_HARVEST = 2;
    int BUTTON_WATER = 3;
    int BUTTON_FERTILIZER = 4;
    int BUTTON_WEED_EX = 5;

    boolean isWorkingEnabled();

    void setWorkingEnabled(boolean value);

    boolean isHarvestEnabled();

    void setHarvestEnabled(boolean value);

    boolean isWaterEnabled();

    void setWaterEnabled(boolean value);

    boolean isFertilizerEnabled();

    void setFertilizerEnabled(boolean value);

    boolean isWeedExEnabled();

    void setWeedExEnabled(boolean value);

    boolean isAutoOutputEnabled();

    void setAutoOutputEnabled(boolean value);

    /** 切换给定容器按钮 id 对应的功能。 */
    default void toggleFeature(int id) {
        switch (id) {
            case BUTTON_AUTO_OUTPUT -> setAutoOutputEnabled(!isAutoOutputEnabled());
            case BUTTON_WORKING -> setWorkingEnabled(!isWorkingEnabled());
            case BUTTON_HARVEST -> setHarvestEnabled(!isHarvestEnabled());
            case BUTTON_WATER -> setWaterEnabled(!isWaterEnabled());
            case BUTTON_FERTILIZER -> setFertilizerEnabled(!isFertilizerEnabled());
            case BUTTON_WEED_EX -> setWeedExEnabled(!isWeedExEnabled());
            default -> {}
        }
    }
}
