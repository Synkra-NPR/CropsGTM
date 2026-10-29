package com.synkra.cropsgtm.client;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.client.render.CropSticksRenderer;
import com.synkra.cropsgtm.fallback.FallbackContent;
import com.synkra.cropsgtm.fallback.client.CropManagerFEScreen;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.gt.client.AlcoholItemColors;

/**
 * 客户端初始化，仅在物理客户端运行时加载。
 */
@Mod.EventBusSubscriber(modid = CropsGTM.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CropsGTMClient {

    private CropsGTMClient() {}

    private static final int DEFAULT_PRIMARY_COLOR = 0xB7BB3F;
    private static final int DEFAULT_SECONDARY_COLOR = 0x00E210;

    public static void init(IEventBus modBus) {
        modBus.addListener(CropsGTMClient::registerRenderers);
        modBus.addListener(CropsGTMClient::registerItemColors);
        modBus.addListener(CropsGTMClient::onAtlasStitched);
        // 备用机器始终注册，因此其界面也始终可用
        modBus.addListener(CropsGTMClient::registerFallbackScreens);
    }

    private static void registerFallbackScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(FallbackContent.cropManagerMenu.get(), CropManagerFEScreen::new));
    }

    private static void onAtlasStitched(TextureStitchEvent.Post event) {
        if (TextureAtlas.LOCATION_BLOCKS.equals(event.getAtlas().location())) {
            CropSticksRenderer.clearSpriteCache();
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CropsGTMBlocks.cropSticksTE(), CropSticksRenderer::new);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new ItemColor() {

            @Override
            public int getColor(ItemStack stack, int tintIndex) {
                ICropCard crop = CropRegistry.instance.get(stack);
                if (crop == null) {
                    return tintIndex == 0 ? DEFAULT_PRIMARY_COLOR : DEFAULT_SECONDARY_COLOR;
                }
                return tintIndex == 0 ? crop.getPrimarySeedColor() : crop.getSecondarySeedColor();
            }
        }, CropsGTMItems.genericSeed());

        if (CropsGTMPlatform.GREG_TECH_LOADED) {
            AlcoholItemColors.register(event);
        }
    }
}
