package com.synkra.cropsgtm.integration.gt.items;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.ElectricStats;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMIndustrialItems;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 用于编辑已存种子属性的电力设备。
 * <p>
 * 种子不在容器中，而是存放在物品标签里，因此该物品可以像本模组的其他工具一样随身携带。能量由 GregTech 的电力物品能力处理，这也使该物品能在常规 GregTech 充电器中充能。
 * </p>
 */
public class ItemCropAttributeEditor extends ComponentItem {

    /** 设备可存储的能量，3000 MEU。 */
    public static final long CAPACITY = 3_000_000_000L;
    /** 每提升一点属性所需的能量。 */
    public static final long COST_PER_POINT = 4_000_000L;

    /** 已存种子的标签键。 */
    public static final String SEED_TAG = "Seed";

    public ItemCropAttributeEditor() {
        // 电子设备不可堆叠，多物品编辑发生在其中的种子物品上
        // 改为在设备界面中
        super(new Properties().stacksTo(1));
        attachComponents(ElectricStats.createElectricItem(CAPACITY, GTValues.UV), new CropAttributeEditorBehavior());
    }

    /** @return 创造模式标签页使用的空设备。 */
    public static ItemStack createEmpty() {
        return new ItemStack(CropsGTMIndustrialItems.cropAttributeEditor.get());
    }

    /** @return 能量缓存已满的设备，用于创造模式标签页。 */
    public static ItemStack createCharged() {
        ItemStack stack = createEmpty();
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
        if (electricItem != null) {
            electricItem.charge(CAPACITY, GTValues.UV, true, false);
        }
        return stack;
    }

    /** @return 设备中已存种子的副本；为空时返回空物品。 */
    public static ItemStack getSeed(ItemStack editor) {
        CompoundTag tag = editor.getTag();
        if (tag == null || !tag.contains(SEED_TAG, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        return ItemStack.of(tag.getCompound(SEED_TAG));
    }

    /** 把给定种子存入设备；传空物品则清除已存种子。 */
    public static void setSeed(ItemStack editor, ItemStack seed) {
        CompoundTag tag = editor.getOrCreateTag();
        if (seed.isEmpty()) {
            tag.remove(SEED_TAG);
        } else {
            tag.put(SEED_TAG, seed.save(new CompoundTag()));
        }
        if (tag.isEmpty()) {
            editor.setTag(null);
        }
    }

    /** @return 设备当前存储的能量，直接从 GregTech 充能标签读取。 */
    public static long getStoredEnergy(ItemStack editor) {
        CompoundTag tag = editor.getTag();
        if (tag == null) return 0L;
        long capacity = getEnergyCapacity(editor);
        if (tag.getBoolean("Infinite")) return capacity;
        return Math.min(tag.getLong("Charge"), capacity);
    }

    /** @return 物品中存储的能量容量；没有时返回默认容量。 */
    public static long getEnergyCapacity(ItemStack editor) {
        CompoundTag tag = editor.getTag();
        if (tag != null && tag.contains("MaxCharge", Tag.TAG_LONG)) {
            return tag.getLong("MaxCharge");
        }
        return CAPACITY;
    }

    /** 写入已存能量，与 GregTech 电力物品读写的是同一个标签。 */
    public static void setStoredEnergy(ItemStack editor, long charge) {
        editor.getOrCreateTag().putLong("Charge", Math.max(0L, charge));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(
                Component.translatable("cropsgtm_tooltip.cropAttributeEditor.hint.0").withStyle(ChatFormatting.GRAY));
        ItemStack seed = getSeed(stack);
        if (!seed.isEmpty()) {
            tooltip.add(Component.translatable("cropsgtm_tooltip.cropAttributeEditor.storedSeed", seed.getHoverName())
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
