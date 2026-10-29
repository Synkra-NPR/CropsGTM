package com.synkra.cropsgtm.integration.gt.init;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import com.synkra.cropsgtm.farming.cropdata.CropAvailability;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock.ComponentType;
import com.synkra.cropsgtm.integration.gt.items.GeneDataOrb;
import com.synkra.cropsgtm.integration.gt.items.ItemCropAttributeEditor;
import com.synkra.cropsgtm.integration.gt.items.ItemEnvironmentalModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * GregTech 集成的物品，仅在安装 GregTech 时注册。
 * <p>
 * 该类在缺少 GregTech 时绝不会被加载，因此可以安全引用其物品所需的 GregTech 类。
 * </p>
 */
public final class CropsGTMIndustrialItems {

    private CropsGTMIndustrialItems() {}

    /** 强制静态初始化块在物品注册事件触发前运行。 */
    public static void init() {}

    public static final EnumMap<ComponentType, RegistryObject<BlockItem>[]> INDUSTRIAL_FARM_COMPONENT_ITEMS = new EnumMap<>(
            ComponentType.class);

    /** 电力属性编辑器依托 GregTech 的电力物品能力。 */
    public static final RegistryObject<ItemCropAttributeEditor> cropAttributeEditor = CropsGTMItems.ITEMS
            .register("crop_attribute_editor", ItemCropAttributeEditor::new);

    public static final RegistryObject<BlockItem> industrialFarmCasing = CropsGTMItems.ITEMS.register(
            "industrial_farm_casing",
            () -> new BlockItem(CropsGTMIndustrialBlocks.industrialFarmCasing(), new Item.Properties()));

    /** 环境模块只被工业农场使用。 */
    public static final RegistryObject<ItemEnvironmentalModule> environmentalModule = CropsGTMItems.ITEMS
            .register("environmental_module", ItemEnvironmentalModule::new);

    /** 基因数据宝珠由 GregTech 作物机器填充与消耗。 */
    public static final RegistryObject<GeneDataOrb> geneDataOrb = CropsGTMItems.ITEMS.register("gene_data_orb",
            GeneDataOrb::new);

    /**
     * 无属性的产物物品、产物链中的无属性物品以及酒精瓶：注册路径 → 依赖的作物。
     * <p>
     * 依赖作物被禁用或缺失时该物品不注册，避免留下孤立的产物链。物品是真实注册表项，只能在启动阶段注册，因此这里硬编码；数据包无法追加物品。
     * </p>
     */
    private static final Map<String, List<String>> CONTENT_ITEM_DEFINITIONS = contentItemDefinitions();

    public static final List<RegistryObject<Item>> CONTENT_ITEMS = new ArrayList<>();

    private static final Map<String, RegistryObject<Item>> CONTENT_ITEMS_BY_PATH = new LinkedHashMap<>();

    static {
        CONTENT_ITEM_DEFINITIONS.forEach((path, dependencies) -> {
            if (!CropAvailability.areAvailable(dependencies)) return;
            RegistryObject<Item> item = CropsGTMItems.ITEMS.register(path, () -> new Item(new Item.Properties()));
            CONTENT_ITEMS.add(item);
            CONTENT_ITEMS_BY_PATH.put(path, item);
        });
    }

    private static Map<String, List<String>> contentItemDefinitions() {
        Map<String, List<String>> definitions = new LinkedHashMap<>();
        defineContentItem(definitions, "argentia_leaf", "cropsgtm:argentia");
        defineContentItem(definitions, "auronia_leaf", "cropsgtm:auronia");
        defineContentItem(definitions, "bauxia_leaf", "cropsgtm:bauxia");
        defineContentItem(definitions, "bobs_yer_uncle_berry", "cropsgtm:bobs_yer_uncle_ranks");
        defineContentItem(definitions, "canola_flower", "cropsgtm:canola");
        defineContentItem(definitions, "coppon_fiber", "cropsgtm:coppon");
        defineContentItem(definitions, "diareed", "cropsgtm:diareed");
        defineContentItem(definitions, "ferrofern_leaf", "cropsgtm:ferrofern");
        defineContentItem(definitions, "galvania_leaf", "cropsgtm:galvania");
        defineContentItem(definitions, "gaia_wart", "cropsgtm:gaia_wart");
        defineContentItem(definitions, "hemp_stem", "cropsgtm:hemp");
        defineContentItem(definitions, "hops", "cropsgtm:hops");
        defineContentItem(definitions, "indigo_blossom", "cropsgtm:indigo");
        defineContentItem(definitions, "iridine_flower", "cropsgtm:iridine");
        defineContentItem(definitions, "max_tomato", "cropsgtm:tomato");
        defineContentItem(definitions, "micadia_flower", "cropsgtm:micadia");
        defineContentItem(definitions, "milk_wart", "cropsgtm:milk_wart");
        defineContentItem(definitions, "nickelback_leaf", "cropsgtm:nickelback");
        defineContentItem(definitions, "oil_berry", "cropsgtm:oil_berry");
        defineContentItem(definitions, "osmianth_flower", "cropsgtm:osmianth");
        defineContentItem(definitions, "platina_leaf", "cropsgtm:platina");
        defineContentItem(definitions, "plumbilia_leaf", "cropsgtm:plumbilia");
        defineContentItem(definitions, "pyrolusium_leaf", "cropsgtm:pyrolusium");
        defineContentItem(definitions, "reactoria_leaf", "cropsgtm:reactoria");
        defineContentItem(definitions, "reactoria_stem", "cropsgtm:reactoria");
        defineContentItem(definitions, "salty_root", "cropsgtm:salty_root");
        defineContentItem(definitions, "scheelinium_leaf", "cropsgtm:scheelinium");
        defineContentItem(definitions, "space_flower", "cropsgtm:space_flower");
        defineContentItem(definitions, "star_wart", "cropsgtm:star_wart");
        defineContentItem(definitions, "stargatium_leaf", "cropsgtm:stargatium");
        defineContentItem(definitions, "sugar_beet", "cropsgtm:sugar_beet");
        defineContentItem(definitions, "sulfur_doped_galvania_residue", "cropsgtm:galvania", "cropsgtm:thiosulfine");
        defineContentItem(definitions, "sulfur_doped_plumbilia_residue", "cropsgtm:plumbilia", "cropsgtm:thiosulfine");
        defineContentItem(definitions, "thiosulfine_flower", "cropsgtm:thiosulfine");
        defineContentItem(definitions, "thunder_leaf", "cropsgtm:god_of_thunder");
        defineContentItem(definitions, "tine_twig", "cropsgtm:tine");
        defineContentItem(definitions, "titania_leaf", "cropsgtm:titania");
        defineContentItem(definitions, "uua_berry", "cropsgtm:transformium");
        defineContentItem(definitions, "uum_berry", "cropsgtm:transformium");
        defineContentItem(definitions, "hemp_hurd", "cropsgtm:hemp");
        defineContentItem(definitions, "processed_star_wart", "cropsgtm:star_wart");
        // 酒精瓶没有依赖作物，始终注册
        defineContentItem(definitions, "fermented_wheat_bottle");
        defineContentItem(definitions, "korn_bottle");
        defineContentItem(definitions, "doppelkorn_bottle");
        defineContentItem(definitions, "fermented_reedwater_bottle");
        defineContentItem(definitions, "sugar_whine_bottle");
        defineContentItem(definitions, "mash_bottle");
        defineContentItem(definitions, "wash_bottle");
        defineContentItem(definitions, "high_proof_bottle");
        defineContentItem(definitions, "jagermeister_bottle");
        defineContentItem(definitions, "fake_jagermeister_bottle");
        return Collections.unmodifiableMap(definitions);
    }

    private static void defineContentItem(Map<String, List<String>> definitions, String path,
                                          String... dependencyCrops) {
        definitions.put(path, List.of(dependencyCrops));
    }

    static {
        for (ComponentType type : ComponentType.values()) {
            @SuppressWarnings("unchecked")
            RegistryObject<BlockItem>[] items = new RegistryObject[CropsGTMIndustrialBlocks.VOLTAGE_NAMES.length];
            INDUSTRIAL_FARM_COMPONENT_ITEMS.put(type, items);
            for (int tier = type.getMinTier(); tier <= type.getMaxTier(); tier++) {
                final int itemTier = tier;
                items[tier] = CropsGTMItems.ITEMS.register(CropsGTMIndustrialBlocks.componentId(type, tier),
                        () -> new BlockItem(CropsGTMIndustrialBlocks.industrialFarmComponent(type, itemTier),
                                new Item.Properties()));
            }
        }
    }

    /** @return 按定义顺序排列的已注册产物物品。 */
    public static List<RegistryObject<Item>> contentItems() {
        return Collections.unmodifiableList(CONTENT_ITEMS);
    }

    /**
     * @return 该 id 对应的已注册内容物品；其依赖的作物被禁用或缺失而未注册时返回 null。
     */
    @Nullable
    public static Item contentItem(String path) {
        RegistryObject<Item> item = CONTENT_ITEMS_BY_PATH.get(path);
        return item == null || !item.isPresent() ? null : item.get();
    }

    public static GeneDataOrb geneDataOrb() {
        return geneDataOrb.get();
    }

    /**
     * 把 GregTech 集成物品加入创造模式标签页。
     */
    public static void addTabItems(CreativeModeTab.Output output) {
        // 编辑器同时提供空与满电状态，与 GregTech 电力物品一致
        output.accept(ItemCropAttributeEditor.createEmpty());
        output.accept(ItemCropAttributeEditor.createCharged());
        output.accept(ItemEnvironmentalModule.createBlank());
        for (int variant = 0; variant < ItemEnvironmentalModule.getVariants().size(); variant++) {
            output.accept(ItemEnvironmentalModule.createVariant(variant));
        }
        output.accept(GeneDataOrb.createEmpty());
        for (RegistryObject<Item> content : contentItems()) {
            output.accept(new ItemStack(content.get()));
        }
        output.accept(new ItemStack(industrialFarmCasing.get()));
        for (ComponentType type : ComponentType.values()) {
            RegistryObject<BlockItem>[] components = INDUSTRIAL_FARM_COMPONENT_ITEMS.get(type);
            for (int tier = type.getMinTier(); tier <= type.getMaxTier(); tier++) {
                output.accept(new ItemStack(components[tier].get()));
            }
        }
        for (int tier = 1; tier < CropsGTMMachines.CROP_MANAGERS.length; tier++) {
            MachineDefinition definition = CropsGTMMachines.CROP_MANAGERS[tier];
            if (definition != null) output.accept(definition.asStack());
        }
        for (int tier = 1; tier < CropsGTMMachines.CROP_BREEDERS.length; tier++) {
            MachineDefinition definition = CropsGTMMachines.CROP_BREEDERS[tier];
            if (definition != null) output.accept(definition.asStack());
        }
        for (int tier = 1; tier < CropsGTMMachines.SEED_GENERATORS.length; tier++) {
            MachineDefinition definition = CropsGTMMachines.SEED_GENERATORS[tier];
            if (definition != null) output.accept(definition.asStack());
        }
        for (int tier = 1; tier < CropsGTMMachines.CROP_GENE_EXTRACTORS.length; tier++) {
            MachineDefinition definition = CropsGTMMachines.CROP_GENE_EXTRACTORS[tier];
            if (definition != null) output.accept(definition.asStack());
        }
        for (int tier = 1; tier < CropsGTMMachines.CROP_SYNTHESIZERS.length; tier++) {
            MachineDefinition definition = CropsGTMMachines.CROP_SYNTHESIZERS[tier];
            if (definition != null) output.accept(definition.asStack());
        }
        output.accept(CropsGTMMachines.INDUSTRIAL_FARM.asStack());
    }
}
