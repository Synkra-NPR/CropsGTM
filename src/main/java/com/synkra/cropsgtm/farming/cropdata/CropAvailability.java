package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.reference.ModResources;
import com.synkra.cropsgtm.reference.Names;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 注册表填充时已存在的作物。
 * <p>
 * 物品与流体在 Forge 加载配置和数据包应用之前注册，因此注册流程无法询问
 * {@code CropRegistry}。该类根据内置作物定义与磁盘上的配置文件得出同样答案，使物品或流体可以声明其依赖的作物，并在其中之一被禁用或缺失时保持未注册。
 * </p>
 * <p>
 * 这里只能看到内置定义；数据包之后可以新增或覆盖作物，但无法再注册物品。配置是包作者在注册表填充前禁用作物所支持的方式。
 * </p>
 */
public final class CropAvailability {

    private static final String CROPS_DIRECTORY = ModResources.DATA_ROOT + "/crops";
    /** 被禁用作物的列表，用于日志与注册检查。 */
    private static final Set<String> DISABLED = new LinkedHashSet<>();
    /** 将被注册的作物 id，即模组已加载且未被禁用的定义。 */
    private static final Set<String> AVAILABLE = new LinkedHashSet<>();
    private static boolean loaded = false;

    private CropAvailability() {}

    /**
     * 构建可用性索引。在模组构造阶段调用，早于任何注册。
     */
    public static void loadEarly() {
        if (loaded) return;
        loaded = true;
        DISABLED.addAll(readDisabledCrops());
        DISABLED.addAll(CropsGTMConfig.disabledCrops);
        AVAILABLE.addAll(readBuiltinCrops());
        AVAILABLE.removeAll(DISABLED);
        if (!DISABLED.isEmpty()) {
            CropsGTM.LOGGER.info("The config disables the crops {}.", DISABLED);
        }
    }

    /** @return 该作物被配置禁用时返回 true。 */
    public static boolean isDisabled(String cropId) {
        return DISABLED.contains(cropId);
    }

    /**
     * @return 给定的所有作物都将被注册时返回 true，空集合视为满足。
     */
    public static boolean areAvailable(Collection<String> cropIds) {
        for (String cropId : cropIds) {
            if (!AVAILABLE.contains(cropId)) return false;
        }
        return true;
    }

    private static Set<String> readBuiltinCrops() {
        Set<String> crops = new LinkedHashSet<>();
        Path directory = ModResources.builtin(CROPS_DIRECTORY);
        if (directory == null || !Files.isDirectory(directory)) return crops;
        // 定义直接存放在目录中，或分组放在 <modid>/ 目录下
        try (Stream<Path> files = Files.walk(directory, 2)) {
            files.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        String relative = directory.relativize(path).toString().replace('\\', '/');
                        String id = relative.substring(0, relative.length() - ".json".length());
                        ResourceLocation fileId = new ResourceLocation(Names.MOD_ID, id);
                        String folder = DefinitionJson.getModFolder(fileId);
                        if (folder != null && !"core".equals(folder) && !DefinitionJson.isModLoaded(folder)) {
                            return;
                        }
                        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            JsonElement element = JsonParser.parseReader(reader);
                            if (!element.isJsonObject()) return;
                            JsonObject json = element.getAsJsonObject();
                            if (!CropParser.areRequiredModsLoaded(json)) return;
                            crops.add(DefinitionJson.getCropId(fileId, json));
                        } catch (Exception exception) {
                            CropsGTM.LOGGER.error("Could not read the crop definition {}.", path, exception);
                        }
                    });
        } catch (IOException exception) {
            CropsGTM.LOGGER.error("Could not list the crop definitions in {}.", directory, exception);
        }
        return crops;
    }

    /**
     * Forge 在注册表填充完成后才加载配置，因此早期注册阶段直接读取文件。文件缺失只表示当前尚未禁用任何作物。
     */
    private static Set<String> readDisabledCrops() {
        Set<String> crops = new LinkedHashSet<>();
        Path path = FMLPaths.CONFIGDIR.get().resolve(Names.MOD_ID + "-common.toml");
        if (!Files.isRegularFile(path)) return crops;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CommentedConfig config = new TomlParser().parse(reader);
            Object value = config.get(List.of("crops", "disabledCrops"));
            if (value instanceof List<?> list) {
                for (Object element : list) {
                    if (element != null) crops.add(element.toString());
                }
            }
        } catch (Exception exception) {
            CropsGTM.LOGGER.error("Could not read the disabled crops from {}.", path, exception);
        }
        return crops;
    }
}
