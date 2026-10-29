package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * 作物数据解析器共用的 JSON 访问器与定义 id 辅助方法。
 */
final class DefinitionJson {

    private DefinitionJson() {}

    /** @return 该键对应的数组；键缺失或不是数组时返回空数组。 */
    static JsonArray getArray(JsonObject json, String key) {
        JsonArray array = json.getAsJsonArray(key);
        return array != null ? array : new JsonArray();
    }

    static String getString(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : fallback;
    }

    static int getInt(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    static float getFloat(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }

    static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    static List<String> parseStringList(JsonArray array) {
        if (array == null) return List.of();
        List<String> list = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            list.add(element.getAsString());
        }
        return list;
    }

    /** @return 定义的 id；文件未声明时返回由文件名推导的 id。 */
    static String getCropId(ResourceLocation fileId, JsonObject json) {
        if (json.has("id") && !json.get("id").isJsonNull()) {
            return json.get("id").getAsString();
        }
        return getFileNameId(fileId);
    }

    /**
     * @return 定义的命名空间与文件名，不含它所在的 {@code <modid>/} 目录。
     */
    static String getFileNameId(ResourceLocation fileId) {
        return fileId.getNamespace() + ":" + getFileName(fileId);
    }

    /**
     * @return 定义文件路径的最后一段，不含命名空间与 {@code <modid>/} 目录。
     */
    static String getFileName(ResourceLocation fileId) {
        String path = fileId.getPath();
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    /**
     * @return 定义文件所在目录的名称；直接位于定义目录下的文件返回 {@code null}。
     */
    @Nullable
    static String getModFolder(ResourceLocation fileId) {
        String path = fileId.getPath();
        int slash = path.indexOf('/');
        return slash < 0 ? null : path.substring(0, slash);
    }

    static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
