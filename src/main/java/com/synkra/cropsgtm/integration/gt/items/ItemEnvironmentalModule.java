package com.synkra.cropsgtm.integration.gt.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import com.synkra.cropsgtm.api.CropBiomeTags;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMIndustrialItems;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * 工业农场使用的环境模块，用于模拟额外的生物群系标签。
 * <p>
 * 原物品用物品损伤值区分变体。1.20.1 的物品不支持损伤值，因此变体改存于自定义模型数据标签，同时也用它选择对应的物品模型。
 * </p>
 */
public class ItemEnvironmentalModule extends Item {

    /** 原版模型谓词选择变体模型所用的 NBT 键。 */
    private static final String CUSTOM_MODEL_DATA = "CustomModelData";

    /** 单个生物群系标签模块变体。 */
    public record Variant(TagKey<Biome> tag, String name) {

        public String langKey() {
            return "cropsgtm_tooltip.biomeTag." + this.name.toUpperCase(java.util.Locale.ROOT);
        }
    }

    private static final List<Variant> VARIANTS = new ArrayList<>();

    static {
        // 顺序必须与原物品损伤值一致
        register(CropBiomeTags.HOT, "hot");
        register(CropBiomeTags.COLD, "cold");
        register(CropBiomeTags.SPARSE, "sparse");
        register(CropBiomeTags.DENSE, "dense");
        register(CropBiomeTags.WET, "wet");
        register(CropBiomeTags.DRY, "dry");
        register(CropBiomeTags.SAVANNA, "savanna");
        register(forgeTag("is_coniferous"), "coniferous");
        register(CropBiomeTags.JUNGLE, "jungle");
        register(CropBiomeTags.SPOOKY, "spooky");
        register(CropBiomeTags.DEAD, "dead");
        register(CropBiomeTags.LUSH, "lush");
        register(CropBiomeTags.NETHER, "nether");
        register(CropBiomeTags.END, "end");
        register(CropBiomeTags.MUSHROOM, "mushroom");
        register(CropBiomeTags.MAGICAL, "magical");
        register(CropBiomeTags.OCEAN, "ocean");
        register(CropBiomeTags.RIVER, "river");
        register(CropBiomeTags.MESA, "mesa");
        register(CropBiomeTags.FOREST, "forest");
        register(CropBiomeTags.PLAINS, "plains");
        register(CropBiomeTags.MOUNTAIN, "mountain");
        register(CropBiomeTags.HILLS, "hills");
        register(CropBiomeTags.SWAMP, "swamp");
        register(CropBiomeTags.SANDY, "sandy");
        register(CropBiomeTags.SNOWY, "snowy");
        register(CropBiomeTags.WASTELAND, "wasteland");
        register(CropBiomeTags.BEACH, "beach");
    }

    private static void register(TagKey<Biome> tag, String name) {
        VARIANTS.add(new Variant(tag, name));
    }

    private static TagKey<Biome> forgeTag(String name) {
        return TagKey.create(Registries.BIOME, new ResourceLocation("forge", name));
    }

    public ItemEnvironmentalModule() {
        super(new Item.Properties());
    }

    public static List<Variant> getVariants() {
        return VARIANTS;
    }

    /** @return 不含任何生物群系标签的空白模块。 */
    public static ItemStack createBlank() {
        return new ItemStack(CropsGTMIndustrialItems.environmentalModule.get());
    }

    /**
     * @param variantIndex {@link #getVariants()} 中的下标
     * @return 为该变体配置好的模块物品
     */
    public static ItemStack createVariant(int variantIndex) {
        ItemStack stack = createBlank();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(CUSTOM_MODEL_DATA, variantIndex + 1);
        return stack;
    }

    /** @return 指定模块物品的生物群系标签；物品为空白时返回 null。 */
    public static @Nullable TagKey<Biome> getBiomeTag(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemEnvironmentalModule)) return null;
        int modelData = stack.getOrCreateTag().getInt(CUSTOM_MODEL_DATA);
        if (modelData <= 0 || modelData > VARIANTS.size()) return null;
        return VARIANTS.get(modelData - 1).tag();
    }

    @Override
    public Component getName(ItemStack stack) {
        TagKey<Biome> tag = getBiomeTag(stack);
        if (tag == null) {
            return Component.translatable("cropsgtm_tooltip.environmentalModule.blank");
        }
        int modelData = stack.getOrCreateTag().getInt(CUSTOM_MODEL_DATA);
        return Component.translatable("cropsgtm_tooltip.environmentalModule.template",
                Component.translatable(VARIANTS.get(modelData - 1).langKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TagKey<Biome> tag = getBiomeTag(stack);
        if (tag == null) {
            tooltip.add(Component.translatable("cropsgtm_tooltip.environmentalModule.blank.info")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("cropsgtm_tooltip.environmentalModule.info").withStyle(ChatFormatting.GRAY));
    }
}
