package com.synkra.cropsgtm.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.fallback.FallbackContent;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.integration.gt.GtIntegration;
import com.synkra.cropsgtm.items.ItemCropSticks;
import com.synkra.cropsgtm.items.ItemGenericSeed;
import com.synkra.cropsgtm.items.ItemPlantCure;
import com.synkra.cropsgtm.items.ItemPlantLens;
import com.synkra.cropsgtm.items.ItemSpade;
import com.synkra.cropsgtm.items.ItemWateringCan;
import com.synkra.cropsgtm.items.ItemWeedEx;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

public final class CropsGTMItems {

    private CropsGTMItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Names.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB,
            Names.MOD_ID);

    public static final RegistryObject<ItemGenericSeed> genericSeed = ITEMS.register("generic_seed",
            ItemGenericSeed::new);

    public static final RegistryObject<ItemCropSticks> cropSticks = ITEMS.register("crop_sticks",
            () -> new ItemCropSticks(CropsGTMBlocks.cropSticks(), new Item.Properties()));

    public static final RegistryObject<ItemWeedEx> weedEx = ITEMS.register("weed_ex", ItemWeedEx::new);

    public static final RegistryObject<ItemPlantCure> plantCure = ITEMS.register("plant_cure", ItemPlantCure::new);

    public static final RegistryObject<Item> poisonPowder = ITEMS.register("poison_powder",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<ItemPlantLens> plantLens = ITEMS.register("plant_lens", ItemPlantLens::new);

    public static final RegistryObject<ItemSpade> spade = ITEMS.register("spade",
            () -> new ItemSpade(Tiers.STONE, "spade", false));

    public static final RegistryObject<ItemSpade> reinforcedSpade = ITEMS.register("reinforced_spade",
            () -> new ItemSpade(Tiers.IRON, "reinforcedSpade", true));

    public static final RegistryObject<ItemWateringCan> wateringCan = ITEMS.register("watering_can",
            () -> new ItemWateringCan(Constants.WATERING_CAN_MAX_WATER, Constants.WATERING_CAN_RADIUS));

    public static final RegistryObject<ItemWateringCan> reinforcedWateringCan = ITEMS
            .register("watering_can_reinforced", () -> new ItemWateringCan(Constants.WATERING_CAN_REINFORCED_MAX_WATER,
                    Constants.WATERING_CAN_REINFORCED_RADIUS));

    public static final RegistryObject<ItemWateringCan> superReinforcedWateringCan = ITEMS.register(
            "watering_can_super_reinforced",
            () -> new ItemWateringCan(Constants.WATERING_CAN_SUPER_REINFORCED_MAX_WATER,
                    Constants.WATERING_CAN_SUPER_REINFORCED_RADIUS,
                    Constants.WATERING_CAN_SUPER_REINFORCED_WATER_PER_USE, false));

    public static final RegistryObject<ItemWateringCan> ultraReinforcedWateringCan = ITEMS.register(
            "watering_can_ultra_reinforced",
            () -> new ItemWateringCan(0, Constants.WATERING_CAN_ULTRA_REINFORCED_RADIUS,
                    Constants.WATERING_CAN_ULTRA_REINFORCED_WATER_PER_USE, true));

    /** 金鱼作物掉落的金鱼，与原版一样可食用。 */
    public static final RegistryObject<Item> goldfish = ITEMS.register("goldfish", () -> new Item(
            new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationMod(0.1f).build())));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("cropsgtm",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.cropsgtm"))
                    .icon(() -> new ItemStack(cropSticks.get())).displayItems((params, output) -> {
                        output.accept(new ItemStack(cropSticks.get()));
                        output.accept(new ItemStack(plantLens.get()));
                        output.accept(new ItemStack(spade.get()));
                        output.accept(new ItemStack(reinforcedSpade.get()));
                        output.accept(new ItemStack(weedEx.get()));
                        output.accept(new ItemStack(plantCure.get()));
                        output.accept(new ItemStack(poisonPowder.get()));
                        output.accept(new ItemStack(wateringCan.get()));
                        output.accept(new ItemStack(reinforcedWateringCan.get()));
                        output.accept(new ItemStack(superReinforcedWateringCan.get()));
                        output.accept(new ItemStack(ultraReinforcedWateringCan.get()));
                        output.accept(new ItemStack(goldfish.get()));
                        FallbackContent.addCreativeTabItems(output);
                        if (CropsGTMPlatform.GREG_TECH_LOADED) {
                            GtIntegration.addCreativeTabItems(output);
                        }
                    }).build());

    /** 所有作物种子放在独立标签页中，主标签页只放工具与机器。 */
    public static final RegistryObject<CreativeModeTab> SEED_TAB = TABS.register("cropsgtm_seeds",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.cropsgtm.seeds"))
                    .icon(() -> new ItemStack(genericSeed.get())).displayItems((params, output) -> {
                        for (ICropCard crop : CropRegistry.instance.getAllInRegistrationOrder()) {
                            if (crop.hideFromNEI()) continue;
                            ItemStack seed = CropRegistry.instance.getDefaultSeedStack(crop);
                            if (!seed.isEmpty()) output.accept(seed);
                        }
                    }).build());

    public static ItemGenericSeed genericSeed() {
        return genericSeed.get();
    }

    public static ItemCropSticks cropSticksItem() {
        return cropSticks.get();
    }

    public static ItemWeedEx weedExItem() {
        return weedEx.get();
    }
}
