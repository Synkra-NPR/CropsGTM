package com.synkra.cropsgtm.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.synkra.cropsgtm.blocks.BlockCropSticks;
import com.synkra.cropsgtm.blocks.BlockEntityCropSticks;
import com.synkra.cropsgtm.reference.Names;

public final class CropsGTMBlocks {

    private CropsGTMBlocks() {}

    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = DeferredRegister
            .create(ForgeRegistries.BLOCKS, Names.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, Names.MOD_ID);

    public static final RegistryObject<BlockCropSticks> cropSticks = BLOCKS.register("crop_sticks",
            BlockCropSticks::new);

    public static final RegistryObject<BlockEntityType<BlockEntityCropSticks>> cropSticksTE = BLOCK_ENTITIES.register(
            "crop_sticks", () -> BlockEntityType.Builder.of(BlockEntityCropSticks::new, cropSticks.get()).build(null));

    public static BlockCropSticks cropSticks() {
        return cropSticks.get();
    }

    public static BlockEntityType<BlockEntityCropSticks> cropSticksTE() {
        return cropSticksTE.get();
    }
}
