package com.synkra.cropsgtm.integration.gt.init;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import com.synkra.cropsgtm.CropsGTM;

/**
 * CropsGTM 机器的配方类型。
 * <p>
 * 机器使用自己的作物逻辑，配方类型只是映射该逻辑，以便配方查看器展示机器的繁殖与复制配方。
 * </p>
 */
public final class CropsGTMRecipeTypes {

    private CropsGTMRecipeTypes() {}

    public static GTRecipeType CROP_BREEDER_RECIPES;
    public static GTRecipeType SEED_GENERATOR_RECIPES;
    public static GTRecipeType CROP_GENE_EXTRACTOR_RECIPES;
    public static GTRecipeType CROP_SYNTHESIZER_RECIPES;

    private static GTRecipeType createBreeder() {
        return register("crop_breeder", "crop").setMaxIOSize(6, 1, 1, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                // 与 GregTech 太阳能蒸汽锅炉相同的工作音效
                .setSound(GTSoundEntries.FURNACE);
    }

    private static GTRecipeType createSeedGenerator() {
        return register("seed_generator", "crop").setMaxIOSize(2, 1, 1, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                // 与 GregTech 太阳能蒸汽锅炉相同的工作音效
                .setSound(GTSoundEntries.FURNACE);
    }

    private static GTRecipeType createGeneExtractor() {
        return register("crop_gene_extractor", "crop").setMaxIOSize(2, 1, 0, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                // 原机器循环播放 GregTech 复制机的音效
                .setSound(GTSoundEntries.REPLICATOR);
    }

    private static GTRecipeType createSynthesizer() {
        GTRecipeType type = register("crop_synthesizer", "crop").setMaxIOSize(4, 1, 1, 0).setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                // 原机器循环播放 GregTech 复制机的音效
                .setSound(GTSoundEntries.REPLICATOR);
        // 四个基因宝珠显示为 2x2 网格，而不是每行三个
        type.setRecipeUI(new TwoByTwoInputRecipeUI(type));
        return type;
    }

    /** 把四个物品输入排成 2x2 网格的配方查看界面。 */
    private static final class TwoByTwoInputRecipeUI extends GTRecipeTypeUI {

        TwoByTwoInputRecipeUI(GTRecipeType recipeType) {
            super(recipeType);
        }

        @Override
        protected WidgetGroup addInventorySlotGroup(boolean isOutputs, boolean isSteam, boolean isHighPressure) {
            if (isOutputs) return super.addInventorySlotGroup(isOutputs, isSteam, isHighPressure);

            WidgetGroup group = new WidgetGroup(0, 0, 2 * 18 + 8, 3 * 18 + 8);
            for (int i = 0; i < 4; i++) {
                SlotWidget slot = new SlotWidget();
                slot.initTemplate();
                slot.setSelfPosition(new Position((i % 2) * 18 + 4, (i / 2) * 18 + 4));
                slot.setBackground(
                        getOverlaysForSlot(false, ItemRecipeCapability.CAP, i == 3, isSteam, isHighPressure));
                slot.setId(ItemRecipeCapability.CAP.slotName(IO.IN, i));
                group.addWidget(slot);
            }

            TankWidget tank = new TankWidget();
            tank.initTemplate();
            tank.setFillDirection(ProgressTexture.FillDirection.ALWAYS_FULL);
            tank.setSelfPosition(new Position(4, 2 * 18 + 4));
            tank.setBackground(getOverlaysForSlot(false, FluidRecipeCapability.CAP, true, isSteam, isHighPressure));
            tank.setId(FluidRecipeCapability.CAP.slotName(IO.IN, 0));
            group.addWidget(tank);
            return group;
        }
    }

    /**
     * 在 GregTech 的配方类型注册事件中注册配方类型；它们必须在注册表冻结前存在，且后续注册的机器定义会用到它们。
     */
    public static void register(GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {
        if (CROP_BREEDER_RECIPES != null) return;
        CROP_BREEDER_RECIPES = createBreeder();
        SEED_GENERATOR_RECIPES = createSeedGenerator();
        CROP_GENE_EXTRACTOR_RECIPES = createGeneExtractor();
        CROP_SYNTHESIZER_RECIPES = createSynthesizer();
    }

    private static GTRecipeType register(String name, String group) {
        GTRecipeType recipeType = new GTRecipeType(new ResourceLocation(CropsGTM.MOD_ID, name), group);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, recipeType.registryName, recipeType);
        GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER, recipeType.registryName, new GTRecipeSerializer());
        GTRegistries.RECIPE_TYPES.register(recipeType.registryName, recipeType);
        return recipeType;
    }
}
