package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.api.CropsGTMSoilTypes;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ISoilList;
import com.synkra.cropsgtm.api.PlantRenderShape;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.CropCard;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.farming.requirements.SubSoilRequirement;
import com.synkra.cropsgtm.farming.requirements.growth.MachineOnlyGrowthRequirement;
import com.synkra.cropsgtm.farming.requirements.growth.MaxLightLevelGrowthRequirement;
import com.synkra.cropsgtm.farming.requirements.growth.MinLightLevelGrowthRequirement;
import com.synkra.cropsgtm.integration.gt.GtIntegration;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getBoolean;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getCropId;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getFloat;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getInt;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getModFolder;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getString;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.isModLoaded;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.parseStringList;

/**
 * 解析并注册数据包中的作物定义。
 */
final class CropParser {

    private CropParser() {}

    static void loadCrops(Map<ResourceLocation, JsonElement> objects, Set<String> skippedCrops) {
        List<CropCard> crops = new ArrayList<>();
        Set<String> cropIds = new HashSet<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                String cropId = getCropId(entry.getKey(), json);
                if (CropsGTMConfig.disabledCrops.contains(cropId) || CropAvailability.isDisabled(cropId)) {
                    skippedCrops.add(cropId);
                    CropsGTM.LOGGER.info("Skipping the crop {} because the config disables it.", cropId);
                    continue;
                }
                if (!areRequiredModsLoaded(json)) {
                    skippedCrops.add(cropId);
                    CropsGTM.LOGGER.info("Skipping the crop {} because one of its required mods is missing.", cropId);
                    continue;
                }
                // 解析器会忽略指向未知物品的掉落或替代种子，
                // 作物本身仍与剩余内容一起注册
                CropCard crop = parseCrop(entry.getKey(), json);
                // 重复 id 会中止所有作物的注册，因此后读取的文件被丢弃
                if (!cropIds.add(crop.getId())) {
                    CropsGTM.LOGGER.warn("Duplicate crop id {}, the definition {} is skipped.", crop.getId(),
                            entry.getKey());
                    continue;
                }
                crops.add(crop);
            } catch (Exception exception) {
                CropsGTM.LOGGER.error("Could not load the crop definition {}", entry.getKey(), exception);
            }
        }
        // 杂草必须先注册，其余按 id 排序以获得稳定的注册顺序
        crops.sort(Comparator.comparing((CropCard crop) -> !crop.isWeed()).thenComparing(ICropCard::getId));
        CropRegistry.instance.reload(crops);

        Map<String, List<ICropCard>> pools = new LinkedHashMap<>();
        for (CropCard crop : crops) {
            for (String pool : crop.getMutationPools()) {
                pools.computeIfAbsent(pool, key -> new ArrayList<>()).add(crop);
            }
        }
        MutationRegistry.instance.reloadPools(pools);
        CropsGTM.LOGGER.info("Loaded {} crop definitions from the data packs.", crops.size());
    }

    private static CropCard parseCrop(ResourceLocation fileId, JsonObject json) {
        String id = getCropId(fileId, json);
        // 为存档兼容，作物 id 保留原有大小写，因此无法解析为
        // 这里的资源位置；改为手动拆分命名空间与路径
        int split = id.indexOf(':');
        String namespace = split < 0 ? Names.MOD_ID : id.substring(0, split);
        String path = split < 0 ? id : id.substring(split + 1);

        JsonArray colors = json.getAsJsonArray("colors");
        int color1 = colors != null && colors.size() > 0 ? parseColor(colors.get(0)) : 0xFFFFFF;
        int color2 = colors != null && colors.size() > 1 ? parseColor(colors.get(1)) : color1;

        CropCard.Builder builder = CropCard.builder().id(namespace, path).colors(color1, color2)
                .langKey(getString(json, "lang_key", null)).creator(getString(json, "creator", "CropsGTM Team"))
                .flavourText(getString(json, "flavour_text", null)).tier(getInt(json, "tier", 1))
                .growthDuration(getInt(json, "growth_duration", -1))
                .machineBreedingTier(getInt(json, "machine_breeding_tier", 1))
                .minSeedBedTier(getInt(json, "min_seed_bed_tier", -1))
                .entityDamage(getFloat(json, "entity_damage", -1.0f))
                // 收获轮数的基础数量，不是概率
                .dropRounds(getFloat(json, "drop_rounds", -1.0f))
                .crossingThreshold(getFloat(json, "crossing_threshold", 0.8f))
                .breedingThreshold(getFloat(json, "breeding_threshold", 0.8f))
                .startsSpreadingWeedsAt(getInt(json, "starts_spreading_weeds_at", 24))
                .hideFromNEI(getBoolean(json, "hide_from_nei", false))
                .seedShape(getString(json, "seed_shape", "vanilla")).seedModelData(getInt(json, "seed_model_data", 0))
                .machineBreedingOnly(getBoolean(json, "machine_breeding_only", false))
                .seedEnchanted(getBoolean(json, "seed_enchanted", false))
                .renderShape(
                        parseEnum(PlantRenderShape.class, getString(json, "render_shape", null), PlantRenderShape.HASH))
                .soilTypes(resolveSoil(getString(json, "soil", "farmland"), fileId))
                .sprites(parseSprites(fileId, path, json))
                .weed("weed".equalsIgnoreCase(getString(json, "behavior", "none")))
                .mutationPools(parseStringList(json.getAsJsonArray("mutation_pools")));

        CropCard crop = builder.build();
        parseDrops(json, crop);
        parseAlternateSeeds(json, crop);
        parseLikedBiomes(json, crop);
        parseGrowthRequirements(json, crop);
        return crop;
    }

    private static void parseDrops(JsonObject json, CropCard crop) {
        JsonArray drops = json.getAsJsonArray("drops");
        if (drops == null) return;
        for (JsonElement element : drops) {
            if (isFromMissingMod(element)) continue;
            JsonObject drop = element.getAsJsonObject();
            ItemStack stack = parseItemStack(drop);
            if (stack.isEmpty()) {
                // 只有 GregTech 是硬性依赖，可选模组的掉落会被静默跳过
                if (getBoolean(drop, "required", true)) {
                    CropsGTM.LOGGER.error("The crop {} contains an invalid drop {}", crop.getId(), drop);
                } else {
                    CropsGTM.LOGGER.debug("Skipping the optional drop {} of the crop {}", drop, crop.getId());
                }
                continue;
            }
            crop.addDrop(stack, getInt(drop, "chance", 10000));
        }
    }

    private static void parseAlternateSeeds(JsonObject json, CropCard crop) {
        JsonArray seeds = json.getAsJsonArray("alternate_seeds");
        if (seeds == null) return;
        for (JsonElement element : seeds) {
            if (isFromMissingMod(element)) continue;
            ItemStack stack = parseItemStack(element);
            if (stack.isEmpty()) {
                CropsGTM.LOGGER.error("The crop {} contains an invalid alternate seed {}", crop.getId(), element);
                continue;
            }
            crop.addAlternateSeed(stack);
        }
    }

    /**
     * @return 该条目是可选的且其模组未加载时返回 true，这种条目会从定义中剔除，而不是让整个作物失效。
     */
    private static boolean isFromMissingMod(JsonElement element) {
        if (!element.isJsonObject()) return false;
        String mod = getString(element.getAsJsonObject(), "mod", null);
        return mod != null && !isModLoaded(mod);
    }

    private static void parseLikedBiomes(JsonObject json, CropCard crop) {
        JsonArray biomes = json.getAsJsonArray("liked_biomes");
        if (biomes == null) return;
        for (JsonElement element : biomes) {
            TagKey<Biome> tag = TagKey.create(Registries.BIOME, new ResourceLocation(element.getAsString()));
            crop.addLikedBiomes(tag);
        }
    }

    private static void parseGrowthRequirements(JsonObject json, CropCard crop) {
        JsonArray requirements = json.getAsJsonArray("growth_requirements");
        if (requirements == null) return;
        for (JsonElement element : requirements) {
            JsonObject requirement = element.getAsJsonObject();
            String type = getString(requirement, "type", "");
            switch (type) {
                case "min_light" -> crop
                        .addGrowthRequirement(new MinLightLevelGrowthRequirement(getInt(requirement, "level", 0)));
                case "max_light" -> crop
                        .addGrowthRequirement(new MaxLightLevelGrowthRequirement(getInt(requirement, "level", 15)));
                case "sub_soil" -> crop.addGrowthRequirement(SubSoilRequirement.get(getString(requirement, "id", "")));
                case "machine_only" -> crop.addGrowthRequirement(new MachineOnlyGrowthRequirement());
                default -> CropsGTM.LOGGER.error("The crop {} contains an unknown growth requirement {}", crop.getId(),
                        type);
            }
        }
    }

    /**
     * @return 作物定义中列出的模组全部已加载时返回 true，否则跳过该定义。
     */
    static boolean areRequiredModsLoaded(JsonObject json) {
        JsonArray requiredMods = json.getAsJsonArray("required_mods");
        if (requiredMods == null) {
            String mod = getString(json, "mod", null);
            return mod == null || isModLoaded(mod);
        }
        for (JsonElement element : requiredMods) {
            if (!isModLoaded(element.getAsString())) {
                return false;
            }
        }
        return true;
    }

    private static ISoilList resolveSoil(String id, ResourceLocation fileId) {
        ISoilList soil = CropsGTMSoilTypes.get(id);
        if (soil == null) {
            CropsGTM.LOGGER.error("The crop {} refers to the unknown soil {}", fileId, id);
            return CropsGTMSoilTypes.getOrDefault(CropsGTMSoilTypes.DEFAULT);
        }
        return soil;
    }

    private static List<ResourceLocation> parseSprites(ResourceLocation fileId, String path, JsonObject json) {
        // 定义可以显式列出各阶段贴图，有些模组会跳过阶段或使用其他后缀
        JsonArray explicit = json.getAsJsonArray("sprites");
        if (explicit != null && explicit.size() > 0) {
            List<ResourceLocation> sprites = new ArrayList<>(explicit.size());
            for (JsonElement element : explicit) {
                sprites.add(new ResourceLocation(element.getAsString()));
            }
            return sprites;
        }
        JsonObject sprite = json.getAsJsonObject("sprite");
        // 贴图按与定义相同的模组目录分组，因此默认路径
        // 当两个模组添加同名作物时保持唯一
        String folder = getModFolder(fileId);
        String base = Names.MOD_ID + ":block/crops/" + (folder == null ? "" : folder + "/") +
                path.toLowerCase(Locale.ROOT) + "/";
        int min = 1;
        int max = 4;
        if (sprite != null) {
            base = getString(sprite, "base", base);
            min = getInt(sprite, "min", min);
            max = getInt(sprite, "max", max);
        }
        List<ResourceLocation> sprites = new ArrayList<>(Math.max(1, max - min + 1));
        for (int stage = min; stage <= max; stage++) {
            sprites.add(new ResourceLocation(base + stage));
        }
        return sprites;
    }

    private static ItemStack parseItemStack(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return parseItemId(element.getAsString(), 1);
        }
        JsonObject json = element.getAsJsonObject();
        int count = Math.max(1, getInt(json, "count", 1));
        if (json.has("gt")) {
            // 没有 GregTech 时无法解析材质掉落，因此跳过该作物
            if (!CropsGTMPlatform.GREG_TECH_LOADED) return ItemStack.EMPTY;
            return GtIntegration.resolveGtDrop(json, count);
        }
        return parseItemId(getString(json, "item", ""), count);
    }

    private static ItemStack parseItemId(String id, int count) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    private static int parseColor(JsonElement element) {
        String value = element.getAsString().trim();
        if (value.startsWith("#")) value = value.substring(1);
        try {
            return (int) Long.parseLong(value, 16);
        } catch (NumberFormatException exception) {
            try {
                return element.getAsInt();
            } catch (Exception ignored) {
                return 0xFFFFFF;
            }
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        if (name == null) return fallback;
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(name)) {
                return constant;
            }
        }
        CropsGTM.LOGGER.error("Unknown {} value {}", type.getSimpleName(), name);
        return fallback;
    }
}
