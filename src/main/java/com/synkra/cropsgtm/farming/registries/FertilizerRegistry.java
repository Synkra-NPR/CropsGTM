package com.synkra.cropsgtm.farming.registries;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import com.synkra.cropsgtm.farming.PotencyData;

import javax.annotation.Nullable;

/**
 * 全部肥料来源，包括物品与流体。
 */
public class FertilizerRegistry {

    public static final FertilizerRegistry instance = new FertilizerRegistry();

    private final ItemPotencyRegistry itemRegistry = new ItemPotencyRegistry();
    private final FluidPotencyRegistry fluidRegistry = new FluidPotencyRegistry();

    private FertilizerRegistry() {}

    public void register(Item item, PotencyData potency) {
        this.itemRegistry.register(item, potency);
    }

    public void register(Fluid fluid, PotencyData potency) {
        this.fluidRegistry.register(fluid, potency);
    }

    public @Nullable PotencyData getPotency(ItemStack stack) {
        return this.itemRegistry.getPotency(stack);
    }

    public @Nullable PotencyData getPotency(FluidStack stack) {
        return this.fluidRegistry.getPotency(stack);
    }

    public boolean isRegistered(ItemStack stack) {
        return this.itemRegistry.isRegistered(stack);
    }

    /** 清空所有条目，在加载数据包定义之前调用。 */
    public void clear() {
        this.itemRegistry.clear();
        this.fluidRegistry.clear();
    }
}
