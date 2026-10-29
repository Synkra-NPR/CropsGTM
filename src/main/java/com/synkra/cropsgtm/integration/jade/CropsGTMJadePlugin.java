package com.synkra.cropsgtm.integration.jade;

import net.minecraft.world.level.block.Block;

import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.blocks.BlockCropSticks;
import com.synkra.cropsgtm.integration.gt.jade.IndustrialFarmPowerProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * 注册 Crops NH 的 Jade 扩展。
 * <p>
 * Jade 通过 {@link WailaPlugin} 注解发现该类，因此只有安装 Jade 时才会加载。
 * </p>
 */
@WailaPlugin
public class CropsGTMJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CropStickComponentProvider.INSTANCE, BlockCropSticks.class);
        if (CropsGTMPlatform.GREG_TECH_LOADED) {
            // 该提供器对每个不是工业农场控制器的方块都会直接退出，因此
            // 可以像 GregTech 注册自家提供器那样为所有方块注册
            registration.registerBlockComponent(IndustrialFarmPowerProvider.INSTANCE, Block.class);
        }
    }
}
