package com.synkra.cropsgtm;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.synkra.cropsgtm.client.CropsGTMClient;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.fallback.FallbackContent;
import com.synkra.cropsgtm.farming.cropdata.CropAvailability;
import com.synkra.cropsgtm.farming.cropdata.CropDataLoader;
import com.synkra.cropsgtm.farming.cropdata.CropExistsCondition;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.gt.GtIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CropsGTM：把 CropNH 移植到 Minecraft 1.20.1 的 GregTech CEu Modern。
 * <p>
 * 原 CropNH 由 InfinityRaider、mitchej123、C0bra5 与 GTNH 团队编写，其本身是 InfinityRaider 的 Agricraft 分支。两者均以 LGPL-3.0
 * 授权。本移植保留原始版权与许可声明。
 * </p>
 */
@Mod(CropsGTM.MOD_ID)
public class CropsGTM {

    public static final String MOD_ID = "cropsgtm";
    public static final Logger LOGGER = LoggerFactory.getLogger("CropsGTM");

    public CropsGTM() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CropsGTMConfig.SPEC);
        // 配方条件用于限制因模组缺失而被禁用作物的机器配方
        CraftingHelper.register(CropExistsCondition.SERIALIZER);
        // 物品与流体在注册事件中注册，该事件在 Forge 加载
        // 配置，因此禁用作物列表必须在此读取
        CropAvailability.loadEarly();
        if (CropsGTMPlatform.GREG_TECH_LOADED) {
            GtIntegration.init(modBus);
        }
        // 备用机器始终注册，其配方是唯一没有 GregTech 的内容
        // 受条件限制；安装 GregTech 时只能从创造模式菜单获得
        FallbackContent.init(modBus);
        CropsGTMBlocks.BLOCKS.register(modBus);
        CropsGTMBlocks.BLOCK_ENTITIES.register(modBus);
        CropsGTMItems.ITEMS.register(modBus);
        CropsGTMItems.TABS.register(modBus);
        modBus.addListener(this::loadComplete);
        MinecraftForge.EVENT_BUS.addListener(CropDataLoader::addReloadListeners);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            CropsGTMClient.init(modBus);
        }
    }

    /**
     * 在所有模组完成公共初始化后加载数据包定义，使解析掉落物时 GregTech 的材质与物品已可用。
     */
    private void loadComplete(final FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            CropDataLoader.loadBuiltin();
            LOGGER.info("CropsGTM 1.20.1 loaded, GregTech integration is {}.",
                    CropsGTMPlatform.GREG_TECH_LOADED ? "enabled" : "disabled");
        });
    }
}
