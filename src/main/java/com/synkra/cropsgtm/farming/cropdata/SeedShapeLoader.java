package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.farming.SeedShapeDefinition;
import com.synkra.cropsgtm.farming.registries.SeedShapeRegistry;
import com.synkra.cropsgtm.reference.Names;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getFileName;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getInt;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getString;

/**
 * 从数据包重建种子剪影，作物定义通过 id 引用它们。
 */
final class SeedShapeLoader {

    private SeedShapeLoader() {}

    static void loadSeedShapes(Map<ResourceLocation, JsonElement> objects) {
        if (objects.isEmpty()) return;
        Map<String, SeedShapeDefinition> shapes = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                String id = getString(json, "id", getFileName(entry.getKey()));
                String texturePath = Names.MOD_ID + ":item/genericseed/" + id.toLowerCase(Locale.ROOT);
                SeedShapeDefinition shape = new SeedShapeDefinition(id, getInt(json, "model_data", 0),
                        new ResourceLocation(getString(json, "dark_texture", texturePath + "1")),
                        new ResourceLocation(getString(json, "light_texture", texturePath + "2")));
                if (shapes.put(id, shape) != null) {
                    CropsGTM.LOGGER.warn("Duplicate seed shape {}, the later definition wins.", id);
                }
            } catch (Exception exception) {
                CropsGTM.LOGGER.error("Could not load the seed shape definition {}", entry.getKey(), exception);
            }
        }
        SeedShapeRegistry.instance.reload(shapes);
        CropsGTM.LOGGER.info("Loaded {} seed shape definitions from the data packs.", shapes.size());
    }
}
