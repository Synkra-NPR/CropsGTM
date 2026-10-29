package com.synkra.cropsgtm.integration.gt.client;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

import com.synkra.cropsgtm.integration.gt.init.CropsGTMIndustrialItems;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMaterials;

/**
 * 用对应 GregTech 流体的颜色给酒精瓶染色。
 * <p>
 * 仅在安装 GregTech 时加载，瓶子物品本身属于核心模组。
 * </p>
 */
public final class AlcoholItemColors {

    private AlcoholItemColors() {}

    private static final String[] BOTTLE_IDS = { "fermented_wheat_bottle", "korn_bottle", "doppelkorn_bottle",
            "fermented_reedwater_bottle", "sugar_whine_bottle", "mash_bottle", "wash_bottle", "high_proof_bottle",
            "jagermeister_bottle", "fake_jagermeister_bottle" };

    public static void register(RegisterColorHandlersEvent.Item event) {
        // 酒精瓶共用一个贴图，并按其流体着色
        Material[] alcohols = { CropsGTMMaterials.fermentedWheat, CropsGTMMaterials.korn, CropsGTMMaterials.doppelkorn,
                CropsGTMMaterials.fermentedReedwater, CropsGTMMaterials.sugarWhine, CropsGTMMaterials.mash,
                CropsGTMMaterials.wash, CropsGTMMaterials.highProof, CropsGTMMaterials.jagermeister,
                CropsGTMMaterials.fakeJagermeister };
        for (int i = 0; i < alcohols.length && i < BOTTLE_IDS.length; i++) {
            Item bottle = CropsGTMIndustrialItems.contentItem(BOTTLE_IDS[i]);
            Material material = alcohols[i];
            if (bottle == null || material == null) continue;
            event.register((stack, tintIndex) -> material.getMaterialRGB(), bottle);
        }
    }
}
