package com.synkra.cropsgtm.farming.cropdata;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 配方条件：仅当所属作物全部注册时才加载配方。
 * <p>
 * 被配置禁用、或引用缺失模组物品的作物不会被注册，其配方也必须跳过。这样被禁用作物的配方不会出现在配方查看器中。
 * </p>
 */
public record CropExistsCondition(List<String> cropIds) implements ICondition {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "crop_exists");
    /** 已报告过的作物，避免日志为每个配方重复输出同一个作物。 */
    private static final Set<String> REPORTED = new HashSet<>();

    public static final Serializer SERIALIZER = new Serializer();

    public CropExistsCondition {
        cropIds = List.copyOf(cropIds);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        for (String cropId : this.cropIds) {
            if (CropRegistry.instance.get(cropId) == null) {
                if (REPORTED.add(cropId)) {
                    CropsGTM.LOGGER.info("Skipping the recipes of the unregistered crop {}.", cropId);
                }
                return false;
            }
        }
        return true;
    }

    public static final class Serializer implements IConditionSerializer<CropExistsCondition> {

        @Override
        public void write(JsonObject json, CropExistsCondition value) {
            List<String> cropIds = value.cropIds();
            if (cropIds.size() == 1) {
                json.addProperty("crop", cropIds.get(0));
                return;
            }
            JsonArray crops = new JsonArray();
            for (String cropId : cropIds) {
                crops.add(cropId);
            }
            json.add("crops", crops);
        }

        @Override
        public CropExistsCondition read(JsonObject json) {
            JsonArray crops = json.getAsJsonArray("crops");
            if (crops != null) {
                List<String> cropIds = new ArrayList<>(crops.size());
                for (JsonElement element : crops) {
                    cropIds.add(element.getAsString());
                }
                return new CropExistsCondition(cropIds);
            }
            return new CropExistsCondition(List.of(GsonHelper.getAsString(json, "crop")));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
