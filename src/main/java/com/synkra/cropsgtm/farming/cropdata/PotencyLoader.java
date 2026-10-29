package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.farming.PotencyData;
import com.synkra.cropsgtm.farming.registries.FertilizerRegistry;
import com.synkra.cropsgtm.farming.registries.HydrationRegistry;
import com.synkra.cropsgtm.farming.registries.WeedEXRegistry;
import com.synkra.cropsgtm.reference.Constants;

import java.util.Map;

import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getArray;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getInt;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.getString;
import static com.synkra.cropsgtm.farming.cropdata.DefinitionJson.isModLoaded;

/**
 * 从数据包重建肥料、水分与除草剂来源。
 */
final class PotencyLoader {

    private PotencyLoader() {}

    static void loadPotency(Map<ResourceLocation, JsonElement> objects) {
        if (objects.isEmpty()) return;
        FertilizerRegistry.instance.clear();
        HydrationRegistry.instance.clear();
        WeedEXRegistry.instance.clear();
        int count = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                String type = getString(json, "type", "");
                for (JsonElement element : getArray(json, "items")) {
                    JsonObject itemJson = element.getAsJsonObject();
                    if (!isDefinitionAvailable(itemJson)) continue;
                    String itemId = getString(itemJson, "item", "");
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                    if (item == null) {
                        CropsGTM.LOGGER.warn("The potency definition refers to the unknown item {}", itemId);
                        continue;
                    }
                    if (!"fertilizer".equals(type)) {
                        CropsGTM.LOGGER.warn("The potency type {} does not accept items, {} ignored.", type, itemId);
                        continue;
                    }
                    FertilizerRegistry.instance.register(item, parsePotency(itemJson));
                    count++;
                }
                for (JsonElement element : getArray(json, "fluids")) {
                    JsonObject fluidJson = element.getAsJsonObject();
                    if (!isDefinitionAvailable(fluidJson)) continue;
                    String fluidId = getString(fluidJson, "fluid", "");
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidId));
                    // 未注册的 id 会解析为空流体而不是 null，注册它会
                    // 静默吞掉所有查找该目标流体的调用
                    if (fluid == null || fluid == Fluids.EMPTY) {
                        CropsGTM.LOGGER.warn("The potency definition refers to the unknown fluid {}", fluidId);
                        continue;
                    }
                    PotencyData data = parsePotency(fluidJson);
                    switch (type) {
                        case "fertilizer" -> FertilizerRegistry.instance.register(fluid, data);
                        case "hydration" -> HydrationRegistry.instance.register(fluid, data);
                        case "weed_ex" -> WeedEXRegistry.instance.register(fluid, data);
                        default -> {
                            CropsGTM.LOGGER.warn("The potency type {} is unknown, {} ignored.", type, fluidId);
                            continue;
                        }
                    }
                    count++;
                }
            } catch (Exception exception) {
                CropsGTM.LOGGER.error("Could not load the potency definition {}", entry.getKey(), exception);
            }
        }
        CropsGTM.LOGGER.info("Loaded {} potency definitions from the data packs.", count);

        // 骨粉在 GT 环境里被 GT 肥料取代，效力下调到 5；非 GT 环境沿用数据包里的 25
        if (CropsGTMPlatform.GREG_TECH_LOADED) {
            FertilizerRegistry.instance.register(Items.BONE_MEAL,
                    new PotencyData(5, Constants.MANUAL_FERTILIZER_MAX_STORAGE));
        }
    }

    /**
     * @return 效力条目中可选的 {@code mod} 条件满足时返回 true。
     */
    private static boolean isDefinitionAvailable(JsonObject json) {
        String mod = getString(json, "mod", null);
        return mod == null || isModLoaded(mod);
    }

    private static PotencyData parsePotency(JsonObject json) {
        return new PotencyData(Math.max(1, getInt(json, "potency", 1)), getInt(json, "max_storage", 0),
                Math.max(1, getInt(json, "units", 1)));
    }
}
