package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.reference.ModResources;
import com.synkra.cropsgtm.reference.Names;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getCropId;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getModFolder;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.isModLoaded;

/**
 * 从数据包加载作物与突变定义。
 * <p>
 * 内置定义随附在 {@code data/cropsgtm/crops} 与
 * {@code data/cropsgtm/mutations}，数据包可以覆盖或扩展它们。定义在公共初始化阶段加载一次，使客户端与专用服务器都拥有它们；服务器在数据包重载时重新加载。
 * </p>
 * <p>
 * 该类只负责读取定义并调度解析器，JSON 结构由 {@link CropParser}、{@link MutationParser}、{@link SoilLoader}、{@link PotencyLoader} 与
 * {@link SeedShapeLoader} 处理。
 * </p>
 */
public final class CropDataLoader {

    private static final Gson GSON = new Gson();
    /** 相对数据包命名空间的目录，即 {@code data/<namespace>/crops}。 */
    private static final String CROPS_DIRECTORY = "crops";
    /** 相对数据包命名空间的目录，即 {@code data/<namespace>/mutations}。 */
    private static final String MUTATIONS_DIRECTORY = "mutations";
    /** 相对数据包命名空间的目录，即 {@code data/<namespace>/soils}。 */
    private static final String SOILS_DIRECTORY = "soils";
    /** 相对数据包命名空间的目录，即 {@code data/<namespace>/sub_soils}。 */
    private static final String SUB_SOILS_DIRECTORY = "sub_soils";
    /** 相对数据包命名空间的目录，即 {@code data/<namespace>/potency}。 */
    private static final String POTENCY_DIRECTORY = "potency";
    /** 相对数据包命名空间的目录，即 {@code data/<namespace>/seed_shapes}。 */
    private static final String SEED_SHAPES_DIRECTORY = "seed_shapes";

    private CropDataLoader() {}

    /**
     * 加载随附在本模组 jar 内的定义。在公共初始化阶段调用。
     */
    public static void loadBuiltin() {
        Path dataRoot = ModResources.builtin(ModResources.DATA_ROOT);
        if (dataRoot == null || !Files.isDirectory(dataRoot)) {
            CropsGTM.LOGGER.error("Could not find the built-in crop definitions at {}, no crops will be loaded.",
                    ModResources.DATA_ROOT);
            return;
        }
        load(readNestedDirectory(dataRoot.resolve(SEED_SHAPES_DIRECTORY)), readDirectory(SOILS_DIRECTORY),
                readDirectory(SUB_SOILS_DIRECTORY), readDirectory(POTENCY_DIRECTORY),
                readNestedDirectory(dataRoot.resolve(CROPS_DIRECTORY)),
                readNestedDirectory(dataRoot.resolve(MUTATIONS_DIRECTORY)));
    }

    /**
     * 数据包重载时在服务端重新加载定义。
     */
    public static void addReloadListeners(AddReloadListenerEvent event) {
        // 单个监听器加载所有目录；重载监听器可能并行运行，突变
        // 依赖作物先完成注册
        event.addListener(createReloadListener());
    }

    /**
     * 创建服务端数据包重载与客户端资源重载共用的监听器，使客户端在世界加载前也能获得作物定义。
     */
    public static SimpleJsonResourceReloadListener createReloadListener() {
        return new SimpleJsonResourceReloadListener(GSON, CROPS_DIRECTORY) {

            @Override
            protected void apply(Map<ResourceLocation, JsonElement> crops, ResourceManager resourceManager,
                                 ProfilerFiller profiler) {
                load(readResources(resourceManager, SEED_SHAPES_DIRECTORY),
                        readResources(resourceManager, SOILS_DIRECTORY),
                        readResources(resourceManager, SUB_SOILS_DIRECTORY),
                        readResources(resourceManager, POTENCY_DIRECTORY), crops,
                        readResources(resourceManager, MUTATIONS_DIRECTORY));
            }
        };
    }

    private static void load(Map<ResourceLocation, JsonElement> seedShapes, Map<ResourceLocation, JsonElement> soils,
                             Map<ResourceLocation, JsonElement> subSoils, Map<ResourceLocation, JsonElement> potency,
                             Map<ResourceLocation, JsonElement> crops, Map<ResourceLocation, JsonElement> mutations) {
        Set<String> skippedCrops = new HashSet<>();
        Set<String> skippedMods = new TreeSet<>();
        // 种子形状必须先于作物解析存在，它们通过 id 被引用
        SeedShapeLoader.loadSeedShapes(filterMissingMods(seedShapes, null, skippedMods));
        // 土壤与下层土壤必须先于作物解析存在，它们通过 id 被引用
        SoilLoader.loadSoils(soils);
        SoilLoader.loadSubSoils(subSoils);
        PotencyLoader.loadPotency(potency);
        // 绝不在数据包来源为空时丢弃内置定义，例如在
        // 首次客户端资源重载时数据目录尚不可见
        if (crops.isEmpty() && mutations.isEmpty()) {
            CropsGTM.LOGGER.warn("No crop definitions found in the data packs, keeping the current registry.");
            return;
        }
        CropParser.loadCrops(filterMissingMods(crops, skippedCrops, skippedMods), skippedCrops);
        MutationParser.loadMutations(filterMissingMods(mutations, null, skippedMods), skippedCrops);
        if (!skippedMods.isEmpty()) {
            CropsGTM.LOGGER.info("Skipping the definitions of the missing mods {}.", skippedMods);
        }
        MutationRegistry.instance.pruneMutationPools();
    }

    /**
     * 丢弃存放在"未加载模组"同名目录中的定义。
     * <p>
     * 模组专用定义位于 {@code crops/<modid>/} 与 {@code mutations/<modid>/}，因此缺失模组只需检查目录，而不必解析并逐个检查物品。{@code core}
     * 目录存放无任何可选模组也能运行的定义，始终加载。
     * </p>
     * 
     * @param skippedCrops 被丢弃定义的作物 id 会加入此集合，使引用它们的突变静默跳过；传 {@code null} 表示不收集
     */
    private static Map<ResourceLocation, JsonElement> filterMissingMods(Map<ResourceLocation, JsonElement> definitions,
                                                                        @Nullable Set<String> skippedCrops,
                                                                        Set<String> skippedMods) {
        Map<ResourceLocation, JsonElement> available = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : definitions.entrySet()) {
            String folder = getModFolder(entry.getKey());
            // core 目录存放不依赖任何可选模组的作物，因此始终
            // 即使 "core" 并不是模组 id 也会加载
            if (folder == null || "core".equals(folder) || isModLoaded(folder)) {
                available.put(entry.getKey(), entry.getValue());
                continue;
            }
            skippedMods.add(folder);
            if (skippedCrops == null) continue;
            try {
                // 引用这些作物的突变同样必须静默跳过
                skippedCrops.add(getCropId(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (Exception ignored) {
                // 若该模组存在，作物加载器会报告格式错误的文件
            }
        }
        return available;
    }

    /**
     * 读取某个目录的定义，文件可放在 {@code <modid>/} 子目录中。返回的 id 保留目录，使不同模组的定义可以共用文件名。
     */
    private static Map<ResourceLocation, JsonElement> readNestedDirectory(Path directory) {
        Map<ResourceLocation, JsonElement> definitions = new LinkedHashMap<>();
        if (!Files.isDirectory(directory)) return definitions;
        // 支持两级：crops/<file>.json 与 crops/<modid>/<file>.json
        try (Stream<Path> files = Files.walk(directory, 2)) {
            files.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".json")).sorted()
                    .forEach(path -> {
                        String relative = directory.relativize(path).toString().replace('\\', '/');
                        String id = relative.substring(0, relative.length() - ".json".length());
                        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            definitions.put(new ResourceLocation(Names.MOD_ID, id), JsonParser.parseReader(reader));
                        } catch (IOException exception) {
                            CropsGTM.LOGGER.error("Could not read the crop definition {}", path, exception);
                        }
                    });
        } catch (IOException exception) {
            CropsGTM.LOGGER.error("Could not list the crop definitions in {}", directory, exception);
        }
        return definitions;
    }

    /**
     * 读取某个内置目录的定义，其文件不分子目录。
     */
    private static Map<ResourceLocation, JsonElement> readDirectory(String directory) {
        Map<ResourceLocation, JsonElement> definitions = new LinkedHashMap<>();
        for (Path path : ModResources.jsonFiles(ModResources.DATA_ROOT + "/" + directory)) {
            String fileName = path.getFileName().toString();
            String id = fileName.substring(0, fileName.length() - ".json".length());
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                definitions.put(new ResourceLocation(Names.MOD_ID, id), JsonParser.parseReader(reader));
            } catch (IOException exception) {
                CropsGTM.LOGGER.error("Could not read the crop definition {}", path, exception);
            }
        }
        return definitions;
    }

    private static Map<ResourceLocation, JsonElement> readResources(ResourceManager resourceManager, String directory) {
        Map<ResourceLocation, JsonElement> definitions = new LinkedHashMap<>();
        String prefix = directory + "/";
        Map<ResourceLocation, Resource> files = resourceManager.listResources(directory,
                path -> path.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : files.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            if (!path.startsWith(prefix)) continue;
            String id = path.substring(prefix.length(), path.length() - ".json".length());
            ResourceLocation definitionId = new ResourceLocation(fileId.getNamespace(), id);
            try (Reader reader = entry.getValue().openAsReader()) {
                definitions.put(definitionId, JsonParser.parseReader(reader));
            } catch (Exception exception) {
                CropsGTM.LOGGER.error("Could not read the crop definition {}", definitionId, exception);
            }
        }
        return definitions;
    }
}
