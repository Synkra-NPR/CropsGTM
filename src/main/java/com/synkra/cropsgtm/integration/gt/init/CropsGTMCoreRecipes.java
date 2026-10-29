package com.synkra.cropsgtm.integration.gt.init;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.recipe.ingredient.NBTPredicateIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.nbtpredicate.NBTPredicates;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.mutation.CropMutation;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.gt.items.GeneDataOrb;
import com.synkra.cropsgtm.integration.gt.machines.CropBreederMachine;
import com.synkra.cropsgtm.integration.gt.machines.CropGeneExtractorMachine;
import com.synkra.cropsgtm.integration.gt.machines.CropSynthesizerMachine;
import com.synkra.cropsgtm.integration.gt.machines.SeedGeneratorMachine;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.ModResources;
import com.synkra.cropsgtm.reference.Names;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

/**
 * 核心 GregTech 配方：肥料与除草剂液体、铲子、毒粉，以及作物机器的配方查看条目。
 * <p>
 * 所有配方都在数据生成阶段生成，原模组也以同样方式注册。
 * </p>
 */
public final class CropsGTMCoreRecipes {

    private CropsGTMCoreRecipes() {}

    /** 一个肥料物品可提取出的流体量。 */
    private static final int FERTILIZER_ITEM_FLUID = Constants.FERTILIZER_ITEM_FLUID_VALUE;
    /** 单份除草剂原料可转换成的流体量。 */
    private static final int WEEDEX_FLUID = Constants.WEEDEX_CAPACITY;
    /** 一个蜘蛛眼可转换成的流体量，比其他原料更弱。 */
    private static final int WEEDEX_FLUID_WEAK = WEEDEX_FLUID * 2 / 3;

    public static void init(Consumer<FinishedRecipe> provider) {
        var liquidFertilizer = CropsGTMMaterials.liquidFertilizer.getFluid();
        var enrichedFertilizer = CropsGTMMaterials.enrichedFertilizer.getFluid();
        var weedEx = CropsGTMMaterials.weedEx.getFluid();

        // 肥料物品 -> 液态肥料
        GTRecipeTypes.EXTRACTOR_RECIPES.recipeBuilder("fertilizer_from_item").inputItems(GTItems.FERTILIZER.get(), 1)
                .outputFluids(new FluidStack(liquidFertilizer, FERTILIZER_ITEM_FLUID)).duration(16).EUt(1)
                .save(provider);

        // 液态肥料 -> 富集肥料
        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder("enriched_fertilizer")
                .inputFluids(new FluidStack(liquidFertilizer, 1000)).inputItems(GTItems.FERTILIZER.get(), 4)
                .outputFluids(new FluidStack(enrichedFertilizer, 1000)).duration(20).EUt(GTValues.VA[GTValues.LV])
                .save(provider);

        // 由水与剧毒药水制取除草剂
        addWeedExRecipes(provider, GTMaterials.Water.getFluid(), "water");
        addWeedExRecipes(provider, GTMaterials.DistilledWater.getFluid(), "distilled_water");

        // 除草剂罐与植物治疗剂在装罐机中灌装进 GregTech 喷雾罐
        GTRecipeTypes.CANNER_RECIPES.recipeBuilder("weed_ex_spray_can").inputItems(GTItems.SPRAY_EMPTY)
                .inputFluids(new FluidStack(weedEx, Constants.WEEDEX_CAPACITY)).outputItems(CropsGTMItems.weedEx.get())
                .duration(40).EUt(15).save(provider);

        GTRecipeTypes.CANNER_RECIPES.recipeBuilder("plant_cure_spray_can").inputItems(GTItems.SPRAY_EMPTY)
                .inputItems(GTItems.FERTILIZER.get(), 2).inputFluids(new FluidStack(enrichedFertilizer, 1000))
                .outputItems(CropsGTMItems.plantCure.get()).duration(40).EUt(15).save(provider);
    }

    private static void addWeedExRecipes(Consumer<FinishedRecipe> provider,
                                         net.minecraft.world.level.material.Fluid water, String waterName) {
        var weedEx = CropsGTMMaterials.weedEx.getFluid();

        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder("weed_ex_from_poison_powder_" + waterName)
                .inputFluids(new FluidStack(water, WEEDEX_FLUID)).inputItems(CropsGTMItems.poisonPowder.get(), 1)
                .outputFluids(new FluidStack(weedEx, WEEDEX_FLUID)).duration(20).EUt(GTValues.VA[GTValues.LV])
                .save(provider);

        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder("weed_ex_from_pufferfish_" + waterName)
                .inputFluids(new FluidStack(water, WEEDEX_FLUID)).inputItems(Items.PUFFERFISH, 1)
                .outputFluids(new FluidStack(weedEx, WEEDEX_FLUID)).duration(20).EUt(GTValues.VA[GTValues.LV])
                .save(provider);

        GTRecipeTypes.CHEMICAL_RECIPES.recipeBuilder("weed_ex_from_spider_eye_" + waterName)
                .inputFluids(new FluidStack(water, WEEDEX_FLUID_WEAK)).inputItems(Items.SPIDER_EYE, 1)
                .outputFluids(new FluidStack(weedEx, WEEDEX_FLUID_WEAK)).duration(20).EUt(GTValues.VA[GTValues.LV])
                .save(provider);
    }

    /** 铲子的配方，与原模组使用的有序配方相同。 */
    public static void initTools(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, new ResourceLocation(CropsGTM.MOD_ID, "spade"),
                new ItemStack(CropsGTMItems.spade.get()), "fPh", "PSP", " T ", 'P',
                new MaterialEntry(TagPrefix.plateDouble, GTMaterials.Steel), 'S', new ItemStack(Items.IRON_SHOVEL), 'T',
                new MaterialEntry(TagPrefix.rodLong, GTMaterials.Wood));

        VanillaRecipeHelper.addShapedRecipe(provider, new ResourceLocation(CropsGTM.MOD_ID, "reinforced_spade"),
                new ItemStack(CropsGTMItems.reinforcedSpade.get()), "fPh", "PHP", "RSR", 'P',
                new MaterialEntry(TagPrefix.plateDense, GTMaterials.Steel), 'H',
                new ItemStack(CropsGTMItems.spade.get()), 'S', new MaterialEntry(TagPrefix.rodLong, GTMaterials.Wood),
                'R', new MaterialEntry(TagPrefix.plate, GTMaterials.Rubber));
    }

    /** 产出毒粉的配方。 */
    public static void initPoisonPowder(Consumer<FinishedRecipe> provider) {
        GTRecipeTypes.MACERATOR_RECIPES.recipeBuilder("poison_powder_from_red_mushroom")
                .inputItems(Items.RED_MUSHROOM, 1).outputItems(CropsGTMItems.poisonPowder.get(), 1).duration(20).EUt(2)
                .save(provider);

        GTRecipeTypes.MACERATOR_RECIPES.recipeBuilder("poison_powder_from_poisonous_potato")
                .inputItems(Items.POISONOUS_POTATO, 1).outputItems(CropsGTMItems.poisonPowder.get(), 1).duration(15)
                .EUt(GTValues.VA[GTValues.LV]).save(provider);

        GTRecipeTypes.MACERATOR_RECIPES.recipeBuilder("poison_powder_from_spider_eye").inputItems(Items.SPIDER_EYE, 1)
                .outputItems(CropsGTMItems.poisonPowder.get(), 1).duration(15).EUt(GTValues.VA[GTValues.LV])
                .save(provider);
    }

    /**
     * 注册基因提取机与作物合成机在配方查看器中展示的配方。
     * <p>
     * 种子生成配方是静态的，作为数据包配方放在 {@code data/cropsgtm/recipes/seed_generator}。需要额外下层土壤方块的作物繁殖配方放在同级的
     * {@code data/cropsgtm/recipes/crop_breeder}，其余突变会获得只消耗亲本种子与富集肥料的默认配方。
     * </p>
     */
    public static void initMachineRecipes(Consumer<FinishedRecipe> provider) {
        SeedStats stats = SeedStats.DEFAULT_ANALYZED;

        // 基因提取机：由电路决定把种子的哪项基因写入数据宝珠
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            String cropId = crop.getId().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            for (int circuit = 1; circuit <= 4; circuit++) {
                CropsGTMRecipeTypes.CROP_GENE_EXTRACTOR_RECIPES
                        .recipeBuilder(
                                new ResourceLocation(CropsGTM.MOD_ID, "crop_gene_extractor/" + cropId + "/" + circuit))
                        .inputItems(seedIngredient(crop)).inputItems(IntCircuitBehaviour.stack(circuit))
                        .outputItems(GeneDataOrb.create(geneType(circuit), geneValue(crop, circuit)))
                        .duration(CropGeneExtractorMachine.getRecipeDuration(circuit))
                        .EUt(CropGeneExtractorMachine.getRecipeEUt(crop)).save(provider);
            }
        }

        // 作物合成机：每种基因各一个宝珠加 UU 物质可重建已分析种子
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            String cropId = crop.getId().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            CropsGTMRecipeTypes.CROP_SYNTHESIZER_RECIPES
                    .recipeBuilder(new ResourceLocation(CropsGTM.MOD_ID, "crop_synthesizer/" + cropId))
                    .inputItems(GeneDataOrb.create(GeneDataOrb.TYPE_SPECIMEN, crop.getId()),
                            GeneDataOrb.create(GeneDataOrb.TYPE_GROWTH, Byte.toString(stats.getGrowth())),
                            GeneDataOrb.create(GeneDataOrb.TYPE_GAIN, Byte.toString(stats.getGain())),
                            GeneDataOrb.create(GeneDataOrb.TYPE_RESISTANCE, Byte.toString(stats.getResistance())))
                    .inputFluids(new FluidStack(GTMaterials.UUMatter.getFluid(),
                            CropSynthesizerMachine.getFluidAmount(crop, stats.getGrowth(), stats.getGain(),
                                    stats.getResistance())))
                    .output(ItemRecipeCapability.CAP, seedIngredient(crop))
                    .duration(CropSynthesizerMachine.getRecipeDuration(crop))
                    .EUt(CropSynthesizerMachine.getRecipeEUt(crop)).save(provider);
        }

        addDefaultSeedGeneratorRecipes(provider);
        addDefaultBreedingRecipes(provider);
    }

    /**
     * 为每个在 {@code data/cropsgtm/recipes/seed_generator} 中没有配方的作物注册默认种子生成配方。默认配方以与数据包配方相同的富集肥料消耗，复制一颗使用默认已分析属性的种子。
     */
    private static void addDefaultSeedGeneratorRecipes(Consumer<FinishedRecipe> provider) {
        BuiltinRecipes builtin = builtinRecipes("seed_generator");
        Set<String> usedIds = new HashSet<>(builtin.fileNames());
        SeedStats stats = SeedStats.DEFAULT_ANALYZED;
        int generated = 0;
        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
            if (builtin.cropIds().contains(crop.getId())) continue;
            String id = uniqueRecipePath(usedIds, defaultRecipePath(crop));
            CropsGTMRecipeTypes.SEED_GENERATOR_RECIPES
                    .recipeBuilder(ResourceLocation.fromNamespaceAndPath(CropsGTM.MOD_ID, "seed_generator/" + id))
                    .inputItems(seedIngredient(crop))
                    .inputFluids(new FluidStack(CropsGTMMaterials.enrichedFertilizer.getFluid(),
                            SeedGeneratorMachine.getFluidAmount(stats.getGrowth(), stats.getGain(),
                                    stats.getResistance())))
                    .output(ItemRecipeCapability.CAP, seedIngredient(crop))
                    .duration(SeedGeneratorMachine.BASE_RECIPE_DURATION).EUt(SeedGeneratorMachine.BASE_RECIPE_EUT)
                    .save(provider);
            generated++;
        }
        CropsGTM.LOGGER.info("Registered {} default seed generator recipes, {} crops keep an explicit recipe.",
                generated, builtin.cropIds().size());
    }

    /**
     * 为每个在 {@code data/cropsgtm/recipes/crop_breeder} 中没有配方的突变注册默认繁殖配方。默认配方只消耗亲本种子与富集肥料，需要额外下层土壤方块的突变保留其数据包配方。
     */
    private static void addDefaultBreedingRecipes(Consumer<FinishedRecipe> provider) {
        BuiltinRecipes builtin = builtinRecipes("crop_breeder");
        Set<String> usedIds = new HashSet<>(builtin.fileNames());
        SeedStats stats = SeedStats.DEFAULT_ANALYZED;
        int generated = 0;
        for (ICropCard output : CropRegistry.instance.getAllInRegistrationOrder()) {
            Collection<CropMutation> mutations = MutationRegistry.instance.getDeterministicMutationsForCrop(output);
            if (mutations == null || mutations.isEmpty()) continue;
            if (builtin.cropIds().contains(output.getId())) continue;
            int index = 0;
            for (CropMutation mutation : mutations) {
                // 一个作物可以有多个突变，后面的会加后缀以保持 id 唯一
                String base = index == 0 ? defaultRecipePath(output) : defaultRecipePath(output) + "_" + index;
                index++;
                String id = uniqueRecipePath(usedIds, base);
                var builder = CropsGTMRecipeTypes.CROP_BREEDER_RECIPES
                        .recipeBuilder(ResourceLocation.fromNamespaceAndPath(CropsGTM.MOD_ID, "crop_breeder/" + id))
                        .inputFluids(new FluidStack(CropsGTMMaterials.enrichedFertilizer.getFluid(),
                                CropBreederMachine.getFluidAmount(output.getTier(), stats.getGrowth(), stats.getGain(),
                                        stats.getResistance())))
                        .duration(mutation.getBreedingMachineRecipeDuration())
                        .EUt(mutation.getBreedingMachineRecipeEUt());
                for (ICropCard parent : mutation.getParents()) {
                    builder.inputItems(seedIngredient(parent));
                }
                builder.chancedOutput(seedIngredient(output), CropBreederMachine.getOutputChance(GTValues.LV) * 100, 0)
                        .save(provider);
                generated++;
            }
        }
        CropsGTM.LOGGER.info("Registered {} default crop breeder recipes, {} mutations keep an explicit recipe.",
                generated, builtin.cropIds().size());
    }

    /**
     * 读取指定目录下的内置配方。作物 id 从配方产出中读取，使覆盖率检查准确；同时返回文件名，避免生成的配方 id 覆盖数据包配方。
     */
    private static BuiltinRecipes builtinRecipes(String folder) {
        Set<String> cropIds = new HashSet<>();
        Set<String> fileNames = new HashSet<>();
        for (Path path : ModResources.jsonFiles(ModResources.DATA_ROOT + "/recipes/" + folder)) {
            String name = path.getFileName().toString();
            fileNames.add(name.substring(0, name.length() - ".json".length()));
            String cropId = readRecipeOutput(path);
            if (cropId != null) cropIds.add(cropId);
        }
        return new BuiltinRecipes(cropIds, fileNames);
    }

    /** @return 配方首个产出的作物 id；无法读取时返回 null。 */
    @Nullable
    private static String readRecipeOutput(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject output = json.getAsJsonObject("outputs").getAsJsonArray("item").get(0).getAsJsonObject()
                    .getAsJsonObject("content");
            JsonObject predicate = output.getAsJsonObject("predicate");
            return predicate == null ? null : predicate.get("value").getAsString();
        } catch (Exception exception) {
            return null;
        }
    }

    /** 某个目录的内置配方，按已覆盖的作物 id 与已占用的 id 拆分。 */
    private record BuiltinRecipes(Set<String> cropIds, Set<String> fileNames) {}

    /** @return 作物 id 的小写路径，同时也是其配方文件名。 */
    private static String cropPath(ICropCard crop) {
        return crop.getId().substring(crop.getId().indexOf(':') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * @return 生成配方的配方路径。本模组的作物使用显式配方；其他模组的作物会带上命名空间，避免同路径的作物（例如两个番茄）与本模组配方冲突。
     */
    private static String defaultRecipePath(ICropCard crop) {
        String path = cropPath(crop);
        String namespace = cropNamespace(crop);
        return namespace.equals(Names.MOD_ID) ? path : namespace.toLowerCase(Locale.ROOT) + "/" + path;
    }

    /** @return 作物 id 的命名空间；id 不含命名空间时返回本模组的命名空间。 */
    private static String cropNamespace(ICropCard crop) {
        int split = crop.getId().indexOf(':');
        return split < 0 ? Names.MOD_ID : crop.getId().substring(0, split);
    }

    /**
     * @return 传入路径；若已被占用则返回带数字后缀的第一个空闲路径。大小写不同的作物 id 会落到同一个小写路径上，因此生成的配方 id 必须唯一。
     */
    private static String uniqueRecipePath(Set<String> usedIds, String path) {
        String id = path;
        int suffix = 1;
        while (!usedIds.add(id)) {
            id = path + "_" + suffix;
            suffix++;
        }
        return id;
    }

    /** @return 编程电路所提取的基因类型。 */
    private static String geneType(int circuit) {
        return switch (circuit) {
            case CropGeneExtractorMachine.CIRCUIT_SPECIMEN -> GeneDataOrb.TYPE_SPECIMEN;
            case CropGeneExtractorMachine.CIRCUIT_GROWTH -> GeneDataOrb.TYPE_GROWTH;
            case CropGeneExtractorMachine.CIRCUIT_GAIN -> GeneDataOrb.TYPE_GAIN;
            default -> GeneDataOrb.TYPE_RESISTANCE;
        };
    }

    /** @return 编程电路从默认已分析种子中提取的基因值。 */
    private static String geneValue(ICropCard crop, int circuit) {
        SeedStats stats = SeedStats.DEFAULT_ANALYZED;
        return switch (circuit) {
            case CropGeneExtractorMachine.CIRCUIT_SPECIMEN -> crop.getId();
            case CropGeneExtractorMachine.CIRCUIT_GROWTH -> Byte.toString(stats.getGrowth());
            case CropGeneExtractorMachine.CIRCUIT_GAIN -> Byte.toString(stats.getGain());
            default -> Byte.toString(stats.getResistance());
        };
    }

    /**
     * 构建能通过配方 JSON 往返的种子原料：种子的作物存于其 NBT，只有 {@link NBTPredicateIngredient} 会把该标签写回 JSON。
     */
    private static Ingredient seedIngredient(ICropCard crop) {
        return NBTPredicateIngredient.of(crop.getSeedItem(SeedStats.DEFAULT_ANALYZED),
                NBTPredicates.eqString(Names.NBT.crop, crop.getId()));
    }
}
