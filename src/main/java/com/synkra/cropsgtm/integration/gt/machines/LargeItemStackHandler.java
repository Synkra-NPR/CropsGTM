package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 槽位可容纳超过一组物品的物品堆处理器。
 * <p>
 * 工业农场的种子槽与下层土壤槽最多存放数百个物品，超出原版物品格式的表达能力：原版把数量写成 byte，超过 127 会回绕，数量在存档与网络同步中都会丢失。这里在 {@code count} 旁边额外以 int
 * 写入数量并据此读回，其余部分仍用原版格式。
 * </p>
 */
class LargeItemStackHandler extends CustomItemStackHandler {

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_SLOT = "Slot";
    private static final String TAG_COUNT = "Count";
    private static final String TAG_STORED_COUNT = "StoredCount";

    LargeItemStackHandler(int size) {
        super(size);
    }

    @Override
    public CompoundTag serializeNBT() {
        ListTag items = new ListTag();
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            CompoundTag tag = stack.save(new CompoundTag());
            tag.putInt(TAG_SLOT, slot);
            // 原版数量是 byte，超过 127 会回绕，因此保留合法的物品数量
            // 那里，真实数量以 int 形式写在旁边
            tag.putByte(TAG_COUNT, (byte) Math.min(stack.getMaxStackSize(), stack.getCount()));
            tag.putInt(TAG_STORED_COUNT, stack.getCount());
            items.add(tag);
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put(TAG_ITEMS, items);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (int slot = 0; slot < getSlots(); slot++) {
            this.stacks.set(slot, ItemStack.EMPTY);
        }
        ListTag items = nbt.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag tag = items.getCompound(index);
            int slot = tag.getInt(TAG_SLOT);
            if (slot < 0 || slot >= getSlots()) continue;
            ItemStack stack = ItemStack.of(tag);
            // 绝不触碰共享的空物品，恢复其数量会破坏每个槽位
            if (stack == ItemStack.EMPTY) continue;
            if (tag.contains(TAG_STORED_COUNT)) {
                stack.setCount(tag.getInt(TAG_STORED_COUNT));
            }
            if (stack.isEmpty()) continue;
            this.stacks.set(slot, stack);
        }
    }
}
