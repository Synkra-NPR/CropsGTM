package com.synkra.cropsgtm.integration.gt;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMCoreRecipes;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMachineRecipes;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMachines;

import java.util.function.Consumer;

/**
 * GregTech 附属模组入口。
 * <p>
 * GregTech 会把附属配方收集进其动态配方包。产物配方不在此处添加，而是生成到 {@code data/cropsgtm/recipes}，使数据包能按所属作物对其设条件。
 * </p>
 */
@GTAddon
public class CropsGTMAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return CropsGTMMachines.REGISTRATE;
    }

    @Override
    public void initializeAddon() {}

    @Override
    public String addonModId() {
        return CropsGTM.MOD_ID;
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        CropsGTMCoreRecipes.init(provider);
        CropsGTMCoreRecipes.initPoisonPowder(provider);
        CropsGTMCoreRecipes.initTools(provider);
        CropsGTMCoreRecipes.initMachineRecipes(provider);
        CropsGTMMachineRecipes.init(provider);
        // 产物配方以数据包文件形式提供，见 CropsGTMRecipeProvider
    }
}
