package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.mutation.CropMutation;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getBoolean;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getFileNameId;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getString;

/**
 * 注册数据包中的突变定义，被引用的作物必须先注册。
 */
final class MutationParser {

    private MutationParser() {}

    static void loadMutations(Map<ResourceLocation, JsonElement> objects, Set<String> skippedCrops) {
        MutationRegistry.instance.clearMutations();
        List<Map.Entry<ResourceLocation, JsonElement>> entries = new ArrayList<>(objects.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (Map.Entry<ResourceLocation, JsonElement> entry : entries) {
            try {
                registerMutation(entry.getKey(), entry.getValue().getAsJsonObject(), skippedCrops);
            } catch (Exception exception) {
                CropsGTM.LOGGER.error("Could not load the mutation definition {}", entry.getKey(), exception);
            }
        }
        CropsGTM.LOGGER.info("Loaded {} mutation definitions from the data packs.",
                MutationRegistry.instance.getAllMutations().size());
    }

    private static void registerMutation(ResourceLocation fileId, JsonObject json, Set<String> skippedCrops) {
        String outputId = getString(json, "output", getFileNameId(fileId));
        ICropCard output = CropRegistry.instance.get(outputId);
        if (output == null) {
            logMissingMutationCrop(fileId, outputId, skippedCrops);
            return;
        }
        JsonArray parentArray = json.getAsJsonArray("parents");
        if (parentArray == null || parentArray.size() < 2 || parentArray.size() > 4) {
            CropsGTM.LOGGER.error("The mutation {} has to declare between 2 and 4 parents", fileId);
            return;
        }
        List<ICropCard> parents = new ArrayList<>(parentArray.size());
        for (JsonElement element : parentArray) {
            ICropCard parent = CropRegistry.instance.get(element.getAsString());
            if (parent == null) {
                logMissingMutationCrop(fileId, element.getAsString(), skippedCrops);
                return;
            }
            parents.add(parent);
        }
        CropMutation mutation = switch (parents.size()) {
            case 2 -> new CropMutation(output, parents.get(0), parents.get(1));
            case 3 -> new CropMutation(output, parents.get(0), parents.get(1), parents.get(2));
            default -> new CropMutation(output, parents.get(0), parents.get(1), parents.get(2), parents.get(3));
        };
        if (getBoolean(json, "machine_only", false) || output.isMachineBreedingOnly()) mutation.machineOnly();
        MutationRegistry.instance.register(mutation);
    }

    private static void logMissingMutationCrop(ResourceLocation fileId, String cropId, Set<String> skippedCrops) {
        // 有意跳过必需模组缺失的作物，引用它们的突变同样跳过
        if (skippedCrops.contains(cropId)) {
            CropsGTM.LOGGER.debug("Skipping the mutation {} because the crop {} is disabled.", fileId, cropId);
            return;
        }
        CropsGTM.LOGGER.error("The mutation {} refers to the unknown crop {}", fileId, cropId);
    }
}
