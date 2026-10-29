package com.synkra.cropsgtm.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

import java.util.List;

import javax.annotation.Nullable;

/**
 * 通用种子物品，所属作物与属性存于物品标签中。
 */
public class ItemGenericSeed extends Item {

    public ItemGenericSeed() {
        super(new Item.Properties());
    }

    @Override
    public Component getName(ItemStack stack) {
        ICropCard crop = CropRegistry.instance.get(stack);
        if (crop == null) return Component.translatable(Names.L10N.invalidSeed);
        ISeedStats stats = SeedStats.getStatsFromStack(stack);
        if (stats == null || !stats.isAnalyzed()) return Component.translatable(Names.L10N.unknownSeed);
        return Component.translatable(Names.L10N.genericSeedFormat, crop.getDisplayName());
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        ICropCard crop = CropRegistry.instance.get(stack);
        return crop == null ? super.getRarity(stack) : crop.getRarity();
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        ICropCard crop = CropRegistry.instance.get(stack);
        return crop != null && crop.isSeedEnchanted();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ICropCard crop = CropRegistry.instance.get(stack);
        ISeedStats stats = SeedStats.getStatsFromStack(stack);
        if (stats == null || !stats.isAnalyzed()) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.scanToViewStats")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        if (crop != null && crop.getFlavourText() != null) {
            tooltip.add(Component.translatable(crop.getFlavourText()).withStyle(ChatFormatting.ITALIC,
                    ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.growth", stats.getGrowth())
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.gain", stats.getGain())
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.resistance", stats.getResistance())
                .withStyle(ChatFormatting.GREEN));
        if (crop == null) return;
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.tier", crop.getTier()));
        tooltip.add(Component.translatable(crop.getSoilTypes().getUnlocalizedItemTooltip()));
        if (crop.getBreedingThreshold() < 0.0f) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.cannotBreedOnSticks")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (MutationRegistry.instance.areAllMutationsMachineOnly(crop)) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_breedingReq.lockout.machineOnly")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (crop.getMinSeedBedTier() >= 0) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.minSeedBedTier",
                    Constants.VOLTAGE_NAMES[crop.getMinSeedBedTier()]).withStyle(ChatFormatting.GRAY));
        }
        if (crop.getCrossingThreshold() < 0.0f) {
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.cannotSpreadToOtherSticks.0")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.genericSeed.cannotSpreadToOtherSticks.1")
                    .withStyle(ChatFormatting.GRAY));
        }
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            tooltip.add(requirement.getDescription());
        }
    }
}
