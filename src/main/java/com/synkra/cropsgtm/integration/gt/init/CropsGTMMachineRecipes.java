package com.synkra.cropsgtm.integration.gt.init;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.data.recipe.CraftingComponent;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock.ComponentType;
import com.synkra.cropsgtm.integration.gt.items.ItemEnvironmentalModule;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;
import java.util.function.Consumer;

import static com.gregtechceu.gtceu.data.recipe.GTCraftingComponents.*;

/**
 * 机器、工业农场组件与砖砌农业机壳的合成配方。
 * <p>
 * 形状取自原模组。仅 GT5 可用的合成辅助被改写为现代 GregTech 合成部件；在 GregTech CEu 中没有对应物的部分（世界加速器、流体单元、施肥泥土）替换为用途相同的物品。
 * </p>
 */
public final class CropsGTMMachineRecipes {

    private CropsGTMMachineRecipes() {}

    /** MV 电路用作环境模块装配机配方的催化剂。 */
    private static final int MODULE_RECIPE_TIER = GTValues.MV;
    /** 装配机配方的时长（刻）。 */
    private static final int MODULE_RECIPE_DURATION = 300;
    /** 砖砌农业机壳装配机配方消耗的水量（mB）。 */
    private static final int CASING_WATER = 1000;
    /** 洒水壶装配机配方的时长（刻）。 */
    private static final int WATERING_CAN_RECIPE_DURATION = 100;
    /** 洒水壶装配机配方所依赖的集成电路配置。 */
    private static final int WATERING_CAN_RECIPE_CIRCUIT = 12;

    public static void init(Consumer<FinishedRecipe> provider) {
        addHempRecipes(provider);
        addCropStickRecipes(provider);
        addWateringCanRecipes(provider);
        addComponentRecipes(provider);
        addMachineRecipes(provider);
        addIndustrialFarmRecipe(provider);
        addEnvironmentalModuleRecipes(provider);
    }

    /**
     * 洒水壶由边框材料的四块板加一根杆、以及紧固材料的一个环和一颗螺丝装配而成。基础水壶也可以用 GregTech 锤与螺丝刀在工作台上合成。
     */
    private static void addWateringCanRecipes(Consumer<FinishedRecipe> provider) {
        wateringCanAssemblerRecipe(provider, "watering_can", CropsGTMItems.wateringCan.get(), GTMaterials.Iron,
                GTMaterials.Steel, GTValues.MV);
        wateringCanAssemblerRecipe(provider, "watering_can_reinforced", CropsGTMItems.reinforcedWateringCan.get(),
                GTMaterials.Steel, GTMaterials.Aluminium, GTValues.MV);
        wateringCanAssemblerRecipe(provider, "watering_can_super_reinforced",
                CropsGTMItems.superReinforcedWateringCan.get(), GTMaterials.Aluminium, GTMaterials.StainlessSteel,
                GTValues.HV);
        wateringCanAssemblerRecipe(provider, "watering_can_ultra_reinforced",
                CropsGTMItems.ultraReinforcedWateringCan.get(), GTMaterials.StainlessSteel, GTMaterials.Titanium,
                GTValues.EV);

        VanillaRecipeHelper.addShapedRecipe(provider,
                ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "watering_can_from_crafting_table"),
                new ItemStack(CropsGTMItems.wateringCan.get()), "hRd", "PPG", "PPS", 'R',
                new MaterialEntry(TagPrefix.ring, GTMaterials.Steel), 'P',
                new MaterialEntry(TagPrefix.plate, GTMaterials.Iron), 'G',
                new MaterialEntry(TagPrefix.rod, GTMaterials.Iron), 'S',
                new MaterialEntry(TagPrefix.screw, GTMaterials.Steel));
    }

    private static void wateringCanAssemblerRecipe(Consumer<FinishedRecipe> provider, String id, Item output,
                                                   Material frame, Material fastening, int tier) {
        GTRecipeTypes.ASSEMBLER_RECIPES
                .recipeBuilder(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, id + "_from_parts"))
                .inputItems(TagPrefix.plate, frame, 4).inputItems(TagPrefix.rod, frame, 1)
                .inputItems(TagPrefix.ring, fastening, 1).inputItems(TagPrefix.screw, fastening, 1)
                .circuitMeta(WATERING_CAN_RECIPE_CIRCUIT).outputItems(new ItemStack(output))
                .duration(WATERING_CAN_RECIPE_DURATION).EUt(GTValues.VA[tier]).save(provider);
    }

    /**
     * 研磨大麻茎可得到用于建造机壳的麻屑，并有四分之一的概率得到原模组的压缩植物球。GregTech CEu 中没有原版的压缩植物球，改为掉落普通植物球。
     */
    private static void addHempRecipes(Consumer<FinishedRecipe> provider) {
        ItemStack stem = produce("hemp_stem");
        ItemStack hurds = produce("hemp_hurd");
        if (stem.isEmpty() || hurds.isEmpty()) return;

        GTRecipeTypes.MACERATOR_RECIPES
                .recipeBuilder(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "hemp_hurd_from_hemp_stem"))
                .inputItems(stem).outputItems(hurds.copy()).chancedOutput(GTItems.PLANT_BALL.asStack(), 2500, 0)
                .duration(20).EUt(GTValues.VA[GTValues.ULV]).save(provider);

        // 铝制边框加四份麻屑与生石灰，消解成砖砌农业机壳
        GTRecipeTypes.ASSEMBLER_RECIPES
                .recipeBuilder(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "industrial_farm_casing"))
                .inputItems(new MaterialEntry(TagPrefix.frameGt, GTMaterials.Aluminium))
                .inputItems(hurds.copyWithCount(4)).inputItems(TagPrefix.dust, GTMaterials.Quicklime)
                .inputFluids(GTMaterials.Water.getFluid(CASING_WATER))
                .outputItems(new ItemStack(CropsGTMIndustrialItems.industrialFarmCasing.get())).duration(5 * 20)
                .EUt(GTValues.VA[GTValues.MV]).save(provider);
    }

    /**
     * 四根长木棍合成四根作物架，装配机可把八根变成十六根。
     */
    private static void addCropStickRecipes(Consumer<FinishedRecipe> provider) {
        shaped(provider, "crop_sticks", new ItemStack(CropsGTMItems.cropSticks.get(), 4), GTValues.LV, "S S", "S S",
                'S', new MaterialEntry(TagPrefix.rodLong, GTMaterials.Wood));

        GTRecipeTypes.ASSEMBLER_RECIPES
                .recipeBuilder(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "crop_sticks_from_long_rods"))
                .inputItems(IntCircuitBehaviour.stack(2)).inputItems(TagPrefix.rodLong, GTMaterials.Wood, 8)
                .outputItems(CropsGTMItems.cropSticks.get(), 16).duration(20).EUt(GTValues.VA[GTValues.ULV])
                .save(provider);
    }

    /**
     * 工业农场组件。它们由带等级机壳、带等级电路与对应 GregTech 部件组装而成，与原机器完全一致。
     */
    private static void addComponentRecipes(Consumer<FinishedRecipe> provider) {
        int maxTier = maxRecipeTier();
        for (int tier = GTValues.MV; tier <= maxTier; tier++) {
            ItemStack casing = new ItemStack(CropsGTMIndustrialItems.industrialFarmCasing.get());

            componentRecipe(provider, ComponentType.SEED_BED, tier, "DDD", "CHC", "PIP", 'D', Blocks.DIRT, 'C', CIRCUIT,
                    'H', HULL, 'P', PIPE_NORMAL, 'I', GTMachines.FLUID_IMPORT_HATCH[tier].asStack());

            componentRecipe(provider, ComponentType.ADVANCED_HARVESTING_UNIT, tier, "RSR", "CHC", "WBW", 'R', ROBOT_ARM,
                    'S', SENSOR, 'C', CIRCUIT, 'H', HULL, 'W', CABLE, 'B', casing.copy());

            componentRecipe(provider, ComponentType.ENVIRONMENTAL_ENHANCEMENT_UNIT, tier, "EEE", "CHC", "WBW", 'E',
                    EMITTER, 'C', CIRCUIT, 'H', HULL, 'W', CABLE, 'B', casing.copy());

            componentRecipe(provider, ComponentType.FERTILIZER_UNIT, tier, "RPS", "CHC", "WBW", 'R', ROBOT_ARM, 'P',
                    PUMP, 'S', SENSOR, 'C', CIRCUIT, 'H', HULL, 'W', CABLE, 'B', casing.copy());

            componentRecipe(provider, ComponentType.GROWTH_ACCELERATION_UNIT, tier, "RPE", "CHC", "WBW", 'R', ROBOT_ARM,
                    'P', PUMP, 'E', EMITTER, 'C', CIRCUIT, 'H', HULL, 'W', CABLE, 'B', casing.copy());
        }

        // 超频模块是生长加速模块的升级，其世界加速器
        // 由于 GregTech CEu 没有世界加速器物品，改用带等级电力马达
        for (int tier = GTValues.ZPM; tier <= maxTier; tier++) {
            componentRecipe(provider, ComponentType.OVERCLOCKED_GROWTH_ACCELERATION_UNIT, tier, "FAF", "CGC", "WBW",
                    'F', FIELD_GENERATOR, 'A', MOTOR, 'C', CIRCUIT, 'G',
                    componentItem(ComponentType.GROWTH_ACCELERATION_UNIT, tier), 'W', CABLE, 'B',
                    new ItemStack(CropsGTMIndustrialItems.industrialFarmCasing.get()));
        }
    }

    /** 五个单方块机器系列，每个配方对应原机器的某个等级。 */
    private static void addMachineRecipes(Consumer<FinishedRecipe> provider) {
        int maxTier = maxRecipeTier();
        for (int tier = GTValues.LV; tier <= maxTier; tier++) {
            addMachineRecipe(provider, CropsGTMMachines.CROP_MANAGERS[tier], "ASA", "PHP", "CIC", 'A', ROBOT_ARM, 'S',
                    SENSOR, 'P', PLATE, 'H', HULL, 'C', CABLE, 'I', GTMachines.FLUID_IMPORT_HATCH[tier].asStack());

            addMachineRecipe(provider, CropsGTMMachines.CROP_BREEDERS[tier], "SAS", "CHC", "WDW", 'S',
                    new ItemStack(CropsGTMItems.cropSticks.get()), 'A', ROBOT_ARM, 'C', CIRCUIT, 'H', HULL, 'W', CABLE,
                    'D', Blocks.DIRT);

            addMachineRecipe(provider, CropsGTMMachines.SEED_GENERATORS[tier], "SAW", "CHC", "BDB", 'S',
                    new ItemStack(CropsGTMItems.spade.get()), 'A', ROBOT_ARM, 'W',
                    new ItemStack(CropsGTMItems.wateringCan.get()), 'C', CIRCUIT, 'H', HULL, 'B', CABLE, 'D',
                    Blocks.DIRT);
        }

        for (int tier = GTValues.EV; tier <= maxTier; tier++) {
            addMachineRecipe(provider, CropsGTMMachines.CROP_GENE_EXTRACTORS[tier], "SES", "WHW", "CSC", 'S', SENSOR,
                    'E', EMITTER, 'W', CABLE, 'H', HULL, 'C', BETTER_CIRCUIT);

            addMachineRecipe(provider, CropsGTMMachines.CROP_SYNTHESIZERS[tier], "FCF", "CHC", "EEE", 'F',
                    FIELD_GENERATOR, 'C', BETTER_CIRCUIT, 'H', HULL, 'E', EMITTER);
        }
    }

    /** 工业农场控制器是以 MV 作物管理器为核心的 MV 机器。 */
    private static void addIndustrialFarmRecipe(Consumer<FinishedRecipe> provider) {
        if (CropsGTMMachines.INDUSTRIAL_FARM == null) return;
        MachineDefinition cropManager = CropsGTMMachines.CROP_MANAGERS[GTValues.MV];
        if (cropManager == null) return;

        shaped(provider, "industrial_farm", CropsGTMMachines.INDUSTRIAL_FARM.asStack(), GTValues.MV, "PWS", "CMC",
                "ICI", 'P', ROBOT_ARM, 'W', new ItemStack(CropsGTMItems.wateringCan.get()), 'S', SENSOR, 'C', CIRCUIT,
                'M', cropManager.asStack(), 'I', GTMachines.ITEM_IMPORT_BUS[GTValues.MV].asStack());
    }

    /**
     * 环境模块。四个机壳加一个 MV 电路构成空白模块，每个生物群系变体由它与该群系的八个方块组装而成。
     */
    private static void addEnvironmentalModuleRecipes(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.ASSEMBLER_RECIPES
                .recipeBuilder(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "environmental_module_base"))
                .inputItems(new ItemStack(CropsGTMIndustrialItems.industrialFarmCasing.get(), 4))
                .inputItems(CustomTags.MV_CIRCUITS).outputItems(ItemEnvironmentalModule.createBlank())
                .duration(MODULE_RECIPE_DURATION).EUt(GTValues.VA[MODULE_RECIPE_TIER]).save(provider);

        environmentalModule(provider, "hot", new ItemStack(Items.MAGMA_BLOCK, 8));
        environmentalModule(provider, "cold", new ItemStack(Items.ICE, 8));
        environmentalModule(provider, "sparse", new ItemStack(Items.DIRT, 8));
        environmentalModule(provider, "dense", new ItemStack(Items.DARK_OAK_SAPLING, 8));
        environmentalModule(provider, "wet", new ItemStack(Items.KELP, 8));
        environmentalModule(provider, "dry", new ItemStack(Items.CACTUS, 8));
        environmentalModule(provider, "savanna", new ItemStack(Items.ACACIA_SAPLING, 8));
        environmentalModule(provider, "coniferous", new ItemStack(Items.SPRUCE_SAPLING, 8));
        environmentalModule(provider, "jungle", new ItemStack(Items.JUNGLE_SAPLING, 8));
        environmentalModule(provider, "spooky", new ItemStack(Items.SOUL_SAND, 8));
        environmentalModule(provider, "dead", new ItemStack(Items.ROTTEN_FLESH, 8));
        environmentalModule(provider, "lush", new ItemStack(Items.GRASS, 8));
        environmentalModule(provider, "nether", new ItemStack(Items.NETHERRACK, 8));
        environmentalModule(provider, "end", new ItemStack(Items.END_STONE, 8));
        environmentalModule(provider, "mushroom", new ItemStack(Items.RED_MUSHROOM, 4),
                new ItemStack(Items.BROWN_MUSHROOM, 4));
        environmentalModule(provider, "magical", new ItemStack(Items.ENDER_EYE, 8));
        environmentalModule(provider, "ocean", new ItemStack(Items.INK_SAC, 8));
        environmentalModule(provider, "river", new ItemStack(Items.COD, 8));
        environmentalModule(provider, "mesa", new ItemStack(Items.TERRACOTTA, 8));
        environmentalModule(provider, "forest", new ItemStack(Items.OAK_SAPLING, 8));
        environmentalModule(provider, "plains", new ItemStack(Items.GRASS_BLOCK, 8));
        environmentalModule(provider, "mountain", new ItemStack(Items.CYAN_TERRACOTTA, 8));
        environmentalModule(provider, "hills", new ItemStack(Items.COBBLESTONE, 8));
        environmentalModule(provider, "swamp", new ItemStack(Items.LILY_PAD, 8));
        environmentalModule(provider, "sandy", new ItemStack(Items.SAND, 8));
        environmentalModule(provider, "snowy", new ItemStack(Items.SNOW_BLOCK, 8));
        environmentalModule(provider, "wasteland", new ItemStack(Items.DEAD_BUSH, 8));
        environmentalModule(provider, "beach", new ItemStack(Items.SUGAR_CANE, 8));

        // 原模组会为少数群系提供额外方块
        environmentalModule(provider, "ocean", new ItemStack(Items.TROPICAL_FISH, 8));
        environmentalModule(provider, "river", new ItemStack(Items.SALMON, 8));
        environmentalModule(provider, "river", new ItemStack(CropsGTMItems.goldfish.get(), 8));
        environmentalModule(provider, "mesa", new ItemStack(Items.RED_SAND, 8));
    }

    private static void environmentalModule(Consumer<FinishedRecipe> provider, String variantName,
                                            ItemStack... ingredients) {
        int index = environmentalModuleIndex(variantName);
        if (index < 0 || ingredients.length == 0) return;

        // 少数变体接受多个方块，用首个原料区分配方
        ResourceLocation ingredientId = ForgeRegistries.ITEMS.getKey(ingredients[0].getItem());
        String suffix = ingredientId == null ? "" : "_" + ingredientId.getPath();
        var builder = GTRecipeTypes.ASSEMBLER_RECIPES
                .recipeBuilder(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID,
                        "environmental_module_" + variantName + suffix))
                .inputItems(ItemEnvironmentalModule.createBlank());
        for (ItemStack ingredient : ingredients) {
            builder.inputItems(ingredient);
        }
        builder.outputItems(ItemEnvironmentalModule.createVariant(index)).duration(MODULE_RECIPE_DURATION)
                .EUt(GTValues.VA[MODULE_RECIPE_TIER]).save(provider);
    }

    /** @return 模块变体的下标；变体不存在时返回 -1。 */
    private static int environmentalModuleIndex(String name) {
        List<ItemEnvironmentalModule.Variant> variants = ItemEnvironmentalModule.getVariants();
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).name().equals(name)) return i;
        }
        return -1;
    }

    /**
     * 为机器定义添加有序合成配方，跳过当前 GregTech 配置下不可合成的等级。
     */
    private static void addMachineRecipe(Consumer<FinishedRecipe> provider, MachineDefinition machine,
                                         Object... recipe) {
        if (machine == null) return;
        int tier = machine.getTier();
        if (tier < GTValues.LV || tier > maxRecipeTier()) return;
        shaped(provider, machine.getName(), machine.asStack(), tier, recipe);
    }

    /** 为某个工业农场组件等级添加有序合成配方。 */
    private static void componentRecipe(Consumer<FinishedRecipe> provider, ComponentType type, int tier,
                                        Object... recipe) {
        shaped(provider, CropsGTMIndustrialBlocks.componentId(type, tier), componentItem(type, tier), tier, recipe);
    }

    /**
     * 注册有序配方，并先把配方中的 GregTech 合成部件替换为指定电压等级的物品。
     */
    private static void shaped(Consumer<FinishedRecipe> provider, String id, ItemStack output, int tier,
                               Object... recipe) {
        for (int i = 0; i < recipe.length; i++) {
            if (recipe[i] instanceof CraftingComponent component) {
                recipe[i] = resolveComponent(component, tier);
            }
        }
        VanillaRecipeHelper.addShapedRecipe(provider, ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, id), output,
                recipe);
    }

    /**
     * @return 指定等级的 GregTech 合成部件物品。
     *         <p>
     *         GregTech 只为自身内容定义了到 UHV 的板与线缆、到 OpV 的电力部件。更高等级改用当前可用的最好部件，使高压机器仍有合理的配方。
     *         </p>
     */
    private static Object resolveComponent(CraftingComponent component, int tier) {
        if (tier <= GTValues.UHV) return component.get(tier);
        // 外壳与电路已定义到 MAX
        if (component == HULL || component == CIRCUIT || component == BETTER_CIRCUIT) return component.get(tier);
        if (component == PLATE) return new MaterialEntry(TagPrefix.plate, GTMaterials.Neutronium);
        if (component == CABLE) return new MaterialEntry(TagPrefix.cableGtSingle, GTMaterials.Europium);
        if (component == PIPE_NORMAL) return component.get(GTValues.UHV);
        return component.get(GTValues.OpV);
    }

    private static ItemStack componentItem(ComponentType type, int tier) {
        return new ItemStack(CropsGTMIndustrialItems.INDUSTRIAL_FARM_COMPONENT_ITEMS.get(type)[tier].get());
    }

    /**
     * @return 拥有机器配方的最高电压等级。
     *         <p>
     *         GregTech CEu 没有 MAX 机器：开启高电压时电力机器止步于 OpV，否则止步于 UHV。
     *         </p>
     */
    private static int maxRecipeTier() {
        return GTCEuAPI.isHighTier() ? GTValues.OpV : GTValues.UHV;
    }

    private static ItemStack produce(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
