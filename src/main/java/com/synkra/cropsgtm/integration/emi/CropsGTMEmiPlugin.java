package com.synkra.cropsgtm.integration.emi;

import net.minecraft.nbt.CompoundTag;

import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.reference.Names;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;

/**
 * 注册作物繁殖、互斥池与作物信息页面，EMI 通过 {@link EmiEntrypoint} 注解发现该类。
 */
@EmiEntrypoint
public class CropsGTMEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // 所有种子共用一个物品，因此 EMI 必须比较标签中的作物而不是物品
        registry.setDefaultComparison(CropsGTMItems.genericSeed(), Comparison.compareData(stack -> {
            CompoundTag tag = stack.getNbt();
            return tag == null ? "" : tag.getString(Names.NBT.crop);
        }));

        registry.addCategory(BreedingEmiCategory.CATEGORY);
        registry.addCategory(MutationPoolEmiCategory.CATEGORY);
        registry.addCategory(CropInfoEmiCategory.CATEGORY);
        registry.addCategory(AlternateSeedEmiCategory.CATEGORY);

        BreedingEmiCategory.registerDisplays(registry);
        MutationPoolEmiCategory.registerDisplays(registry);
        CropInfoEmiCategory.registerDisplays(registry);
        AlternateSeedEmiCategory.registerDisplays(registry);
    }
}
