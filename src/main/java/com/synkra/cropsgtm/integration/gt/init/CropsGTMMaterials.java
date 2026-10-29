package com.synkra.cropsgtm.integration.gt.init;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.fluids.FluidBuilder;

import net.minecraft.resources.ResourceLocation;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.farming.cropdata.CropAvailability;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;

import javax.annotation.Nullable;

/**
 * CropsGTM 添加的材质与流体。
 * <p>
 * GregTech 只懂得为材质创建流体，因此本模组需要的每种流体都依托一个材质。流体是真实注册表项，只能在启动阶段注册，因此这里硬编码；依赖已禁用或缺失作物的流体会被跳过。
 * </p>
 */
public final class CropsGTMMaterials {

    private CropsGTMMaterials() {}

    /** 液态肥料，由一个肥料物品提取而成。 */
    public static @Nullable Material liquidFertilizer;
    /** 富集肥料，效力是普通液态肥料的十倍。 */
    public static @Nullable Material enrichedFertilizer;
    /** 除草剂，由水与剧毒药水酿造。 */
    public static @Nullable Material weedEx;
    /** 酸性铱溶液，由铱花与邻苯二甲酸溶解而成。 */
    public static @Nullable Material acidicIridiumSolution;

    // 酒精家族，颜色只用于驱动流体与瓶子着色
    /** 发酵小麦汁，谷物酒精链的基础。 */
    public static @Nullable Material fermentedWheat;
    /** 谷物酒，由发酵小麦蒸馏而成。 */
    public static @Nullable Material korn;
    /** 双粒玉米酒，经两次蒸馏的小麦酒精。 */
    public static @Nullable Material doppelkorn;
    /** 发酵的芦苇水，甘蔗酒精链的基础。 */
    public static @Nullable Material fermentedReedwater;
    /** 糖酒，由发酵芦苇水蒸馏而成。 */
    public static @Nullable Material sugarWhine;
    /** 麦芽浆，酒精链的第一步。 */
    public static @Nullable Material mash;
    /** 发酵醪，发酵后的麦芽浆。 */
    public static @Nullable Material wash;
    /** 高度酒，由发酵醪蒸馏而成。 */
    public static @Nullable Material highProof;
    /** 真野格酒。 */
    public static @Nullable Material jagermeister;
    /** 仿制野格酒。 */
    public static @Nullable Material fakeJagermeister;

    /**
     * 注册本模组的材质。由 GregTech 的材质注册事件调用，该事件在 GregTech 材质注册表仍开放时触发。
     */
    public static void registerMaterials(MaterialEvent event) {
        liquidFertilizer = create("fertilizer", 0x2DAA2D);
        enrichedFertilizer = create("enriched_fertilizer", 0x28E515);
        weedEx = create("weed_ex", 0x6E3FA8);
        // 只有酸性铱溶液依赖作物，铱花被禁用或缺失时不注册
        acidicIridiumSolution = create("acidic_iridium_solution", 0x8FA6B8, "cropsgtm:iridine");

        fermentedWheat = create("fermented_wheat", 0xC9A227);
        korn = create("korn", 0xD8B25A);
        doppelkorn = create("doppelkorn", 0xD9A93C);
        fermentedReedwater = create("fermented_reedwater", 0xB8C46A);
        sugarWhine = create("sugar_whine", 0xC7788A);
        mash = create("mash", 0xC2A860);
        wash = create("wash", 0xCBB77A);
        highProof = create("high_proof", 0xE8D7A0);
        jagermeister = create("jagermeister", 0x5A3D1E);
        fakeJagermeister = create("fake_jagermeister", 0x6B4A24);
    }

    /**
     * @return 该路径对应的材料；依赖的作物被禁用或缺失时返回 null。
     */
    @Nullable
    private static Material create(String path, int color, String... dependencyCrops) {
        if (!CropAvailability.areAvailable(List.of(dependencyCrops))) {
            CropsGTM.LOGGER.info("Skipping the fluid {} because one of its crops is disabled or missing.", path);
            return null;
        }
        return new Material.Builder(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, path))
                .liquid(new FluidBuilder().temperature(300)).color(color).buildAndRegister();
    }
}
