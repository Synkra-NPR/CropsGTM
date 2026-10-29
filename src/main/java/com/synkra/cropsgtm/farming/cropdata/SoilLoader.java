package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.api.ISoilList;
import com.synkra.cropsgtm.farming.registries.CompoundSoilList;
import com.synkra.cropsgtm.farming.registries.SoilRegistry;
import com.synkra.cropsgtm.farming.requirements.SubSoilRequirement;
import com.synkra.cropsgtm.integration.gt.GtIntegration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getArray;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getString;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.parseStringList;

/**
 * 从数据包重建土壤与下层土壤定义。
 */
final class SoilLoader {

    private SoilLoader() {}

    /**
     * 从数据包重建土壤列表。土壤要么列出方块与标签，要么包含其他土壤列表以构成复合土壤。
     */
    static void loadSoils(Map<ResourceLocation, JsonElement> objects) {
        if (objects.isEmpty()) return;
        SoilRegistry.instance.clear();
        Map<String, JsonObject> compoundSoils = new LinkedHashMap<>();
        Set<String> defined = new HashSet<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                String id = getString(json, "id", entry.getKey().getPath());
                JsonArray include = json.getAsJsonArray("include");
                if (include != null && include.size() > 0) {
                    compoundSoils.put(id, json);
                    continue;
                }
                ISoilList soil = SoilRegistry.instance.get(id);
                for (JsonElement blockId : getArray(json, "blocks")) {
                    Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId.getAsString()));
                    if (block == null) {
                        CropsGTM.LOGGER.warn("The soil {} refers to the unknown block {}", id, blockId.getAsString());
                        continue;
                    }
                    soil.register(block);
                }
                for (JsonElement tagId : getArray(json, "tags")) {
                    soil.register(TagKey.create(Registries.BLOCK, new ResourceLocation(tagId.getAsString())));
                }
                defined.add(id);
            } catch (Exception exception) {
                // 单个格式错误的文件不能中断整个重载，让注册表处于半填充状态
                CropsGTM.LOGGER.error("Could not load the soil definition {}", entry.getKey(), exception);
            }
        }
        // 复合土壤可以包含其他复合土壤，逐层解析直到为空
        boolean progress = true;
        while (!compoundSoils.isEmpty() && progress) {
            progress = false;
            Iterator<Map.Entry<String, JsonObject>> iterator = compoundSoils.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonObject> entry = iterator.next();
                List<ISoilList> members = new ArrayList<>();
                boolean complete = true;
                for (String include : parseStringList(entry.getValue().getAsJsonArray("include"))) {
                    if (!defined.contains(include)) {
                        complete = false;
                        break;
                    }
                    members.add(SoilRegistry.instance.get(include));
                }
                if (!complete) continue;
                SoilRegistry.instance.register(entry.getKey(),
                        new CompoundSoilList(entry.getKey(), members.toArray(new ISoilList[0])));
                defined.add(entry.getKey());
                iterator.remove();
                progress = true;
            }
        }
        for (String id : compoundSoils.keySet()) {
            CropsGTM.LOGGER.error("The soil {} includes an unknown soil, it will be skipped.", id);
        }
        CropsGTM.LOGGER.info("Loaded {} soil definitions from the data packs.", defined.size());
    }

    /**
     * 从数据包重建下层土壤需求，使用的 id 与作物文件中的一致。
     */
    static void loadSubSoils(Map<ResourceLocation, JsonElement> objects) {
        if (objects.isEmpty()) return;
        SubSoilRequirement.clearAll();
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                String id = getString(json, "id", entry.getKey().getPath());
                SubSoilRequirement requirement = SubSoilRequirement.get(id).clear();
                for (JsonElement blockId : getArray(json, "blocks")) {
                    Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockId.getAsString()));
                    if (block == null) {
                        CropsGTM.LOGGER.warn("The sub-soil {} refers to the unknown block {}", id,
                                blockId.getAsString());
                        continue;
                    }
                    requirement.addBlock(block);
                }
                for (JsonElement tagId : getArray(json, "tags")) {
                    requirement.addBlock(TagKey.create(Registries.BLOCK, new ResourceLocation(tagId.getAsString())));
                }
                if (CropsGTMPlatform.GREG_TECH_LOADED) {
                    List<String> materialIds = new ArrayList<>();
                    for (JsonElement materialId : getArray(json, "materials")) {
                        materialIds.add(materialId.getAsString());
                    }
                    if (!materialIds.isEmpty()) {
                        requirement.addMaterials(GtIntegration.resolveSubSoilMaterials(id, materialIds));
                    }
                }
                count++;
            } catch (Exception exception) {
                CropsGTM.LOGGER.error("Could not load the sub-soil definition {}", entry.getKey(), exception);
            }
        }
        CropsGTM.LOGGER.info("Loaded {} sub-soil definitions from the data packs.", count);
    }
}
