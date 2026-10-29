package com.synkra.cropsgtm.integration.jei;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.client.JeiRecipeBridge;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.reference.Names;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

/**
 * 为 JEI 注册作物繁殖、互斥池、作物信息与替代种子页面，JEI 通过 {@link JeiPlugin} 注解发现该类。
 * <p>
 * EMI 通过其 JEMI 兼容层加载 JEI 插件，而本模组已有原生 EMI 分类，因此仅当 EMI 不是当前配方查看器时才注册这些页面。GregTech 对其自家 JEI 插件也采用同一规则。
 * </p>
 */
@JeiPlugin
public class CropsGTMJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(CropsGTM.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // 所有种子共用一个物品，因此 JEI 必须比较标签中的作物而不是物品
        registration.registerSubtypeInterpreter(CropsGTMItems.genericSeed(), (stack, context) -> {
            CompoundTag tag = stack.getTag();
            return tag == null ? IIngredientSubtypeInterpreter.NONE : tag.getString(Names.NBT.crop);
        });
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (isDisabled()) return;
        registration.addRecipeCategories(new BreedingJeiCategory(registration.getJeiHelpers()),
                new MutationPoolJeiCategory(registration.getJeiHelpers()),
                new CropInfoJeiCategory(registration.getJeiHelpers()),
                new AlternateSeedJeiCategory(registration.getJeiHelpers()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (isDisabled()) return;
        BreedingJeiCategory.registerRecipes(registration);
        MutationPoolJeiCategory.registerRecipes(registration);
        CropInfoJeiCategory.registerRecipes(registration);
        AlternateSeedJeiCategory.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (isDisabled()) return;
        ItemStack sticks = new ItemStack(CropsGTMBlocks.cropSticks());
        registration.addRecipeCatalyst(sticks, BreedingJeiCategory.RECIPE_TYPE, MutationPoolJeiCategory.RECIPE_TYPE,
                CropInfoJeiCategory.RECIPE_TYPE, AlternateSeedJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        JeiRecipeBridge.onRuntimeAvailable(jeiRuntime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiRecipeBridge.onRuntimeUnavailable();
    }

    /**
     * @return 其他配方查看器已接管本模组页面、JEI 不应重复注册时返回 true。
     */
    private static boolean isDisabled() {
        ModList modList = ModList.get();
        return modList.isLoaded("rei") || modList.isLoaded("emi");
    }
}
