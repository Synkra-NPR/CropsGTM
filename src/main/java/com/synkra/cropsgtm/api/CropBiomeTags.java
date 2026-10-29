package com.synkra.cropsgtm.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import com.synkra.cropsgtm.CropsGTM;

/**
 * 作物用来描述偏好气候的生物群系标签。Forge 提供对应旧 BiomeDictionary 的生物群系类型标签，其余由原版提供。
 */
public final class CropBiomeTags {

    private CropBiomeTags() {}

    private static TagKey<Biome> forge(String name) {
        return TagKey.create(Registries.BIOME, new ResourceLocation("forge", name));
    }

    private static TagKey<Biome> vanilla(String name) {
        return TagKey.create(Registries.BIOME, new ResourceLocation("minecraft", name));
    }

    /**
     * Forge 1.20 不再提供生物群系字典，因此它没有提供的标签由本模组定义，见 {@code data/cropsgtm/tags/worldgen/biome}。
     */
    private static TagKey<Biome> mod(String name) {
        return TagKey.create(Registries.BIOME, new ResourceLocation(CropsGTM.MOD_ID, name));
    }

    public static final TagKey<Biome> HOT = forge("is_hot");
    public static final TagKey<Biome> COLD = forge("is_cold");
    public static final TagKey<Biome> DRY = forge("is_dry");
    public static final TagKey<Biome> WET = forge("is_wet");
    public static final TagKey<Biome> SANDY = forge("is_sandy");
    public static final TagKey<Biome> SNOWY = forge("is_snowy");
    public static final TagKey<Biome> SWAMP = forge("is_swamp");
    public static final TagKey<Biome> MUSHROOM = forge("is_mushroom");
    public static final TagKey<Biome> MAGICAL = mod("is_magical");
    public static final TagKey<Biome> DEAD = mod("is_dead");
    public static final TagKey<Biome> LUSH = forge("is_lush");
    public static final TagKey<Biome> SPARSE = forge("is_sparse");
    public static final TagKey<Biome> DENSE = forge("is_dense");
    public static final TagKey<Biome> WASTELAND = forge("is_wasteland");
    public static final TagKey<Biome> PLAINS = forge("is_plains");
    public static final TagKey<Biome> SPOOKY = forge("is_spooky");

    public static final TagKey<Biome> FOREST = vanilla("is_forest");
    public static final TagKey<Biome> JUNGLE = vanilla("is_jungle");
    public static final TagKey<Biome> HILLS = vanilla("is_hill");
    public static final TagKey<Biome> MOUNTAIN = vanilla("is_mountain");
    public static final TagKey<Biome> MESA = vanilla("is_badlands");
    public static final TagKey<Biome> SAVANNA = vanilla("is_savanna");
    public static final TagKey<Biome> BEACH = vanilla("is_beach");
    public static final TagKey<Biome> OCEAN = vanilla("is_ocean");
    public static final TagKey<Biome> RIVER = vanilla("is_river");
    public static final TagKey<Biome> NETHER = vanilla("is_nether");
    public static final TagKey<Biome> END = vanilla("is_end");
}
