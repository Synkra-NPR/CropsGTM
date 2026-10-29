package com.synkra.cropsgtm.api;

import net.minecraft.network.chat.Component;

/**
 * 繁殖突变在亲本作物之外还必须满足的条件。
 */
public interface IBreedingRequirement {

    /**
     * @return 配方查看器中显示的描述。
     */
    Component getDescription();
}
