package com.synkra.cropsgtm;

import net.minecraftforge.fml.ModList;

/**
 * 检测本模组的可选集成。
 * <p>
 * 基础种植内容可独立运行，所有依赖 GregTech CEu Modern 的内容仅在该模组存在时注册。这里直接读取模组列表，因此该类从不引用 GregTech 的类，保证核心在类路径中没有 GregTech 时也能加载。
 * </p>
 */
public final class CropsGTMPlatform {

    /** GregTech CEu Modern 是否已安装。 */
    public static final boolean GREG_TECH_LOADED = ModList.get().isLoaded("gtceu");

    private CropsGTMPlatform() {}
}
