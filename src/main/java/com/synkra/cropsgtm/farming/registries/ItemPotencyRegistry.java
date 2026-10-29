package com.synkra.cropsgtm.farming.registries;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.farming.PotencyData;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 把物品映射到其提供的资源量。
 */
public class ItemPotencyRegistry {

    private final Map<Item, PotencyData> registry = new HashMap<>();

    public void register(Item item, PotencyData data) {
        validate(data);
        this.registry.putIfAbsent(item, data);
    }

    /** 清空所有条目，在加载数据包定义之前调用。 */
    public void clear() {
        this.registry.clear();
    }

    public boolean isRegistered(ItemStack stack) {
        return stack != null && !stack.isEmpty() && this.registry.containsKey(stack.getItem());
    }

    public @Nullable PotencyData getPotency(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return this.registry.get(stack.getItem());
    }

    private static void validate(PotencyData data) {
        if (data.getPotency() <= 0) throw new IllegalArgumentException("potency must be greater than 0");
        if (data.getUnitsConsumedPerApplication() <= 0) {
            throw new IllegalArgumentException("units consumed per application must be greater than 0");
        }
    }
}
