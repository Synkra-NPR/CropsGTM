package com.synkra.cropsgtm.integration.gt;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTItems;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import com.google.gson.JsonObject;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMIndustrialBlocks;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMIndustrialItems;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMachines;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMaterials;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMRecipeTypes;

import java.util.List;

/**
 * CropsGTM 中所有需要 GregTech CEu Modern 的部分的入口。
 * <p>
 * 模组核心从不直接引用 GregTech 的类。它检查 {@code CropsGTMPlatform.GREG_TECH_LOADED}，仅在为真时调用该门面，因此核心在类路径中没有 GregTech 时仍可加载。
 * </p>
 */
public final class GtIntegration {

    private GtIntegration() {}

    /**
     * 注册 GregTech 机器、材质、配方类型、工业农场方块与物品。
     * <p>
     * 必须在模组构造阶段运行，早于 registrate 与注册事件触发。
     * </p>
     */
    public static void init(IEventBus modBus) {
        CropsGTMMachines.REGISTRATE.registerRegistrate();
        CropsGTMMachines.init();
        CropsGTMIndustrialBlocks.init();
        CropsGTMIndustrialItems.init();
        // 配方类型必须在 GregTech 冻结配方类型注册表之前存在
        modBus.addGenericListener(GTRecipeType.class, CropsGTMRecipeTypes::register);
        // 只有在 GregTech 机器注册表仍开放时才能添加机器定义
        modBus.addGenericListener(MachineDefinition.class, CropsGTMMachines::register);
        // 必须在 GregTech 材质注册表仍开放时添加材质
        modBus.addListener(CropsGTMMaterials::registerMaterials);
        // 便携式扫描仪作用于作物架时会取代植物透镜
        MinecraftForge.EVENT_BUS.addListener(PortableScannerCropHandler::onRightClickBlock);
    }

    /** 把 GregTech 集成物品加入本模组的创造模式标签页。 */
    public static void addCreativeTabItems(CreativeModeTab.Output output) {
        CropsGTMIndustrialItems.addTabItems(output);
    }

    /**
     * 解析作物定义中的 {@code "gt"} 材质掉落，例如 {@code {"gt": "dust", "material": "gold", "count": 2}}。
     */
    public static ItemStack resolveGtDrop(JsonObject json, int count) {
        return GtItemResolver.resolve(json, count);
    }

    /**
     * 解析下层土壤定义的材质 id，并对未知项发出警告后丢弃。
     * 
     * @return GregTech 中存在的材质 id
     */
    public static List<String> resolveSubSoilMaterials(String requirementId, List<String> materialIds) {
        return GtSubSoilMaterials.resolve(requirementId, materialIds);
    }

    /** @return 空的 GregTech 喷雾罐，即用尽后的喷雾罐会变成的容器。 */
    public static ItemStack emptySprayCan() {
        return GTItems.SPRAY_EMPTY.asStack();
    }
}
