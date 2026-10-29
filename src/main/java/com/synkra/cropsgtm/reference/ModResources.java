package com.synkra.cropsgtm.reference;

import net.minecraftforge.fml.ModList;

import com.synkra.cropsgtm.CropsGTM;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * 访问模组文件内随附的资源。
 * <p>
 * 开发环境中的模组文件是磁盘目录，正式环境的 jar 是 zip 文件系统。两者都在这里统一解析，调用方只需处理 {@link Path}。
 * </p>
 */
public final class ModResources {

    /** 本模组数据包目录在模组文件中的根路径。 */
    public static final String DATA_ROOT = "data/" + Names.MOD_ID;

    /** 游戏运行期间目录列表不会变化，配方在重载时会重新扫描。 */
    private static final Map<String, List<Path>> JSON_FILES = new ConcurrentHashMap<>();

    private ModResources() {}

    /**
     * @return 模组文件内指定路径的资源；模组文件或路径缺失时返回 null。
     */
    @Nullable
    public static Path builtin(String path) {
        var modFileInfo = ModList.get().getModFileById(Names.MOD_ID);
        if (modFileInfo == null) return null;
        return modFileInfo.getFile().findResource(path);
    }

    /**
     * @return 模组文件内指定目录下的 json 文件，按名称排序；目录不存在时返回空列表。列表会被缓存：配方每次重载都会重新扫描，而目录本身不会变化。
     */
    public static List<Path> jsonFiles(String directory) {
        return JSON_FILES.computeIfAbsent(directory, key -> {
            Path path = builtin(key);
            if (path == null || !Files.isDirectory(path)) return List.of();
            try (Stream<Path> files = Files.list(path)) {
                return files.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList();
            } catch (IOException exception) {
                CropsGTM.LOGGER.error("Could not list the built-in resources in {}.", key, exception);
                return List.of();
            }
        });
    }
}
