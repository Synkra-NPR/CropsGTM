package com.synkra.cropsgtm.integration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

import com.synkra.cropsgtm.api.IBreedingRequirement;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.IWorldGrowthRequirement;
import com.synkra.cropsgtm.farming.mutation.CropMutation;
import com.synkra.cropsgtm.farming.requirements.SubSoilRequirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 构建 EMI 与 JEI 作物页共用的信息行与原料列表，使两个配方查看器始终显示相同内容。
 */
public final class CropRecipeText {

    /** 常规信息行的颜色，与原作物页面一致。 */
    public static final int TEXT_COLOR = 0x404040;
    /** 繁殖页的文本颜色，繁殖条件自带灰色样式，需要统一改成黑色。 */
    public static final int BREEDING_TEXT_COLOR = 0x000000;

    private CropRecipeText() {}

    /**
     * @return 这些文本行在给定宽度下换行后需要的高度。
     */
    public static int textHeight(List<Component> lines, int width) {
        Font font = Minecraft.getInstance().font;
        int height = 0;
        for (Component line : lines) {
            height += Math.max(1, font.split(line, width).size()) * 10;
        }
        return height;
    }

    /**
     * @return 作物信息页的信息行。
     */
    public static List<Component> cropInfoLines(ICropCard crop) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("cropsgtm_tooltip.viewer.tier", crop.getTier()));
        lines.add(Component.translatable("cropsgtm_tooltip.viewer.growthPoints", crop.getGrowthDuration()));
        lines.add(Component.translatable("cropsgtm_tooltip.viewer.dropMultiplier",
                String.format(Locale.ROOT, "%.2f", crop.getBaseDropRounds())));
        lines.add(Component.translatable("cropsgtm_tooltip.viewer.likes", likesLine(crop)));
        // 下层土壤已由自己的槽位展示，这里只列出其他条件
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (requirement instanceof SubSoilRequirement) continue;
            // 需求自带灰色样式，强制把它改成与上方各行相同的颜色
            MutableComponent description = requirement.getDescription().copy()
                    .withStyle(style -> style.withColor(TextColor.fromRgb(TEXT_COLOR)));
            lines.add(description);
        }
        return lines;
    }

    /**
     * @return 指定突变繁殖页面的条件行。
     */
    public static List<Component> breedingLines(CropMutation mutation, ICropCard crop) {
        List<Component> lines = new ArrayList<>();
        // 下层土壤由自己的槽位展示，这里只列出其他条件
        for (IBreedingRequirement requirement : mutation.getRequirements()) {
            if (requirement instanceof SubSoilRequirement) continue;
            lines.add(blackened(requirement.getDescription()));
        }
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (requirement instanceof SubSoilRequirement) continue;
            if (!(requirement instanceof IWorldGrowthRequirement)) continue;
            lines.add(blackened(requirement.getDescription()));
        }
        return lines;
    }

    /**
     * @return 把需求自带的灰色样式改成繁殖页统一的黑色。
     */
    private static Component blackened(Component description) {
        return description.copy().withStyle(style -> style.withColor(TextColor.fromRgb(BREEDING_TEXT_COLOR)));
    }

    /**
     * @return 该作物可用的下层土壤物品；没有下层土壤需求时为空。
     */
    public static List<ItemStack> subSoilStacks(ICropCard crop) {
        return nonEmptyStacks(SubSoilRequirement.collectCatalysts(crop));
    }

    /**
     * @return 去掉空物品后的列表副本。
     */
    public static List<ItemStack> nonEmptyStacks(List<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) result.add(stack);
        }
        return result;
    }

    /**
     * @return “Likes:”那一行，其中每个偏好的生物群系都换成了显示名。
     */
    public static Component likesLine(ICropCard crop) {
        Set<TagKey<Biome>> biomes = crop.getLikedBiomeTags();
        if (biomes.isEmpty()) return Component.translatable("cropsgtm_tooltip.viewer.likes.none");
        MutableComponent likes = Component.empty();
        boolean first = true;
        for (TagKey<Biome> biome : biomes) {
            if (!first) likes.append(", ");
            first = false;
            likes.append(Component.translatable("cropsgtm_tooltip.biomeTag." + biomeTagKey(biome)));
        }
        return likes;
    }

    /**
     * 把生物群系标签映射到其翻译键后缀，本模组沿用旧的生物群系类型名称。
     */
    private static String biomeTagKey(TagKey<Biome> biome) {
        String path = biome.location().getPath();
        if ("is_badlands".equals(path)) return "MESA";
        if ("is_hill".equals(path)) return "HILLS";
        String name = path.startsWith("is_") ? path.substring(3) : path;
        return name.toUpperCase(Locale.ROOT);
    }
}
