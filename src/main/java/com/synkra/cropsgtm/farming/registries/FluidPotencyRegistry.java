package com.synkra.cropsgtm.farming.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.synkra.cropsgtm.farming.PotencyData;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 把流体映射到其提供的资源量。
 * <p>
 * 定义以流体的注册名而非实例为键：GregTech 惰性注册材质流体，材料返回的实例可能与流体注册表中的实例不同，用实例查找会静默失败。
 * </p>
 */
public class FluidPotencyRegistry {

    protected final Map<ResourceLocation, PotencyData> registry = new HashMap<>();

    public void register(Fluid fluid, PotencyData data) {
        validate(data);
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
        if (key == null) return;
        this.registry.putIfAbsent(key, data);
    }

    /** 清空所有条目，在加载数据包定义之前调用。 */
    public void clear() {
        this.registry.clear();
    }

    public boolean isRegistered(FluidStack stack) {
        return getPotency(stack) != null;
    }

    public @Nullable PotencyData getPotency(FluidStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return getPotency(stack.getFluid());
    }

    public @Nullable PotencyData getPotency(Fluid fluid) {
        if (fluid == null) return null;
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
        return key == null ? null : this.registry.get(key);
    }

    private static void validate(PotencyData data) {
        if (data.getPotency() <= 0) throw new IllegalArgumentException("potency must be greater than 0");
        if (data.getUnitsConsumedPerApplication() <= 0) {
            throw new IllegalArgumentException("units consumed per application must be greater than 0");
        }
    }
}
