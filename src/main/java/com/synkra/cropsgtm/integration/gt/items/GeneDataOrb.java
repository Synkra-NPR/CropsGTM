package com.synkra.cropsgtm.integration.gt.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMIndustrialItems;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 携带单个作物基因的数据宝珠。
 * <p>
 * 原模组通过行为的数据标题与数据名称把基因存进 GregTech 的数据宝珠。现代 GregTech 仅为研究数据保留该 API，且不再允许附属模组写入标题，因此本移植改用带自有标签键的专属物品。
 * </p>
 */
public class GeneDataOrb extends Item {

    public static final String TYPE_SPECIMEN = "specimen";
    public static final String TYPE_GROWTH = "growth";
    public static final String TYPE_GAIN = "gain";
    public static final String TYPE_RESISTANCE = "resistance";

    public GeneDataOrb() {
        // 一个宝珠一次只携带一项基因
        super(new Item.Properties().stacksTo(1));
    }

    public static boolean isGeneOrb(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GeneDataOrb;
    }

    /**
     * @return 尚未写入基因的宝珠返回 true，基因提取机消耗的就是这种宝珠。
     */
    public static boolean isBlankOrb(ItemStack stack) {
        return isGeneOrb(stack) && getType(stack).isEmpty();
    }

    /**
     * @return 宝珠中存储的基因类型，空白或损坏的宝珠返回空字符串。
     */
    public static String getType(ItemStack stack) {
        if (!isGeneOrb(stack)) return "";
        CompoundTag tag = stack.getTag();
        if (tag == null) return "";
        return tag.getString(Names.NBT.geneType);
    }

    /**
     * @return 宝珠中存储的基因值：样本基因是作物 id，其余基因是属性值。
     */
    public static String getValue(ItemStack stack) {
        if (!isGeneOrb(stack)) return "";
        CompoundTag tag = stack.getTag();
        if (tag == null) return "";
        return tag.getString(Names.NBT.geneValue);
    }

    /** @return 宝珠中的属性值；未存数字时返回 0。 */
    public static int getStatValue(ItemStack stack) {
        try {
            return Integer.parseInt(getValue(stack));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * 创建一个已填充的宝珠。两者都传空字符串即可创建空白宝珠，它仍保留两个标签键，使配方能区分空白宝珠与已填充宝珠。
     */
    public static ItemStack create(String type, String value) {
        ItemStack stack = new ItemStack(CropsGTMIndustrialItems.geneDataOrb());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(Names.NBT.geneType, type);
        tag.putString(Names.NBT.geneValue, value);
        return stack;
    }

    /** @return 空白宝珠，可供基因提取机填充。 */
    public static ItemStack createEmpty() {
        return create("", "");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String value = getValue(stack);
        switch (getType(stack)) {
            case TYPE_SPECIMEN -> tooltip
                    .add(Component.translatable(Names.MOD_ID + "_tooltip.geneDataOrb.specimen", getCropName(value))
                            .withStyle(ChatFormatting.GRAY));
            case TYPE_GROWTH -> tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.geneDataOrb.growth", value)
                    .withStyle(ChatFormatting.GRAY));
            case TYPE_GAIN -> tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.geneDataOrb.gain", value)
                    .withStyle(ChatFormatting.GRAY));
            case TYPE_RESISTANCE -> tooltip
                    .add(Component.translatable(Names.MOD_ID + "_tooltip.geneDataOrb.resistance", value)
                            .withStyle(ChatFormatting.GRAY));
            default -> tooltip.add(
                    Component.translatable(Names.MOD_ID + "_tooltip.geneDataOrb.blank").withStyle(ChatFormatting.GRAY));
        }
    }

    /** @return 已存作物的显示名；未知时返回原始作物 id。 */
    private static Component getCropName(String cropId) {
        ICropCard crop = CropRegistry.instance.get(cropId);
        return crop == null ? Component.literal(cropId) : crop.getDisplayName();
    }
}
