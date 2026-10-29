package com.synkra.cropsgtm.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.integration.gt.GtIntegration;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 本模组可补充装喷雾罐的基类。
 * <p>
 * 罐子在其 NBT 中存储固定次数，与 GregTech 自家喷雾罐的做法一致。用完最后一次后罐子会变回空的 GregTech 喷雾罐；未安装 GregTech 时变回合成它所用的桶。
 * </p>
 */
public abstract class ItemSprayCan extends Item {

    /** 剩余次数的 NBT 键，与 GregTech 喷雾罐使用的键相同。 */
    public static final String NBT_USES_LEFT = "UsesLeft";
    /** 标记由药水合成的罐子，合成时会返还其玻璃瓶。 */
    public static final String NBT_RETURN_BOTTLE = "ReturnBottle";

    /** 满罐的可用次数。 */
    private final int maxUses;

    protected ItemSprayCan(int maxUses) {
        super(new Item.Properties().stacksTo(1));
        this.maxUses = maxUses;
    }

    public int getMaxUses() {
        return this.maxUses;
    }

    /** @return 剩余可用次数；没有 NBT 标签的罐子视为满。 */
    public int getUsesLeft(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_USES_LEFT, Tag.TAG_INT)) return this.maxUses;
        return Mth.clamp(tag.getInt(NBT_USES_LEFT), 0, this.maxUses);
    }

    public static void setUsesLeft(ItemStack stack, int usesLeft) {
        stack.getOrCreateTag().putInt(NBT_USES_LEFT, usesLeft);
    }

    /**
     * 消耗一次罐子用量。
     * <p>
     * 最后一次用尽后，玩家主手的罐子会被替换为空喷雾罐，避免玩家拿着一个无法使用的罐子。
     * </p>
     */
    protected static void consumeUse(Player player, ItemStack stack) {
        int usesLeft = stack.getItem() instanceof ItemSprayCan can ? can.getUsesLeft(stack) : 0;
        if (usesLeft <= 1) {
            // 没有 GregTech 时罐子由桶合成，因此会返还桶
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    CropsGTMPlatform.GREG_TECH_LOADED ? GtIntegration.emptySprayCan() : new ItemStack(Items.BUCKET));
            return;
        }
        setUsesLeft(stack, usesLeft - 1);
    }

    /**
     * 返回无 GregTech 配方中所用药水的玻璃瓶。
     * <p>
     * 配方会标记其产物，因此不使用药水的治疗与除草剂配方不会给出瓶子。每个合成出的物品各给一个瓶子。
     * </p>
     */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide && !CropsGTMPlatform.GREG_TECH_LOADED) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean(NBT_RETURN_BOTTLE)) {
                int count = stack.getCount();
                tag.remove(NBT_RETURN_BOTTLE);
                ItemStack bottles = new ItemStack(Items.GLASS_BOTTLE, count);
                if (!player.getInventory().add(bottles)) {
                    player.drop(bottles, false);
                }
            }
        }
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * getUsesLeft(stack) / this.maxUses);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) getUsesLeft(stack) / this.maxUses / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.sprayCan.uses", getUsesLeft(stack), this.maxUses)
                .withStyle(ChatFormatting.GRAY));
    }
}
