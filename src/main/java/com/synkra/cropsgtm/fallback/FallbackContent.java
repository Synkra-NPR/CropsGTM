package com.synkra.cropsgtm.fallback;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.reference.Names;

/**
 * 仅在未安装 GregTech CEu Modern 时存在的内容。
 * <p>
 * 该类只在缺少 GregTech 时加载，因此可以注册备用作物管理器及其菜单而不触碰其他内容。这里的一切只使用原版与 Forge API。
 * </p>
 */
public final class FallbackContent {

    private FallbackContent() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            Names.MOD_ID);

    public static final RegistryObject<BlockCropManagerFE> cropManager = CropsGTMBlocks.BLOCKS
            .register("crop_manager_fe", BlockCropManagerFE::new);

    public static final RegistryObject<BlockEntityType<BlockEntityCropManagerFE>> cropManagerTE = CropsGTMBlocks.BLOCK_ENTITIES
            .register("crop_manager_fe",
                    () -> BlockEntityType.Builder.of(BlockEntityCropManagerFE::new, cropManager.get()).build(null));

    public static final RegistryObject<BlockItem> cropManagerItem = CropsGTMItems.ITEMS.register("crop_manager_fe",
            () -> new ItemBlockCropManagerFE(cropManager.get(), new Item.Properties()));

    public static final RegistryObject<MenuType<CropManagerFEMenu>> cropManagerMenu = MENUS.register("crop_manager_fe",
            () -> IForgeMenuType
                    .create((windowId, inventory, buffer) -> new CropManagerFEMenu(windowId, inventory, buffer)));

    /** 强制静态初始化块运行，并注册菜单类型。 */
    public static void init(IEventBus modBus) {
        MENUS.register(modBus);
    }

    /** 把备用物品加入创造模式标签页。 */
    public static void addCreativeTabItems(CreativeModeTab.Output output) {
        output.accept(new ItemStack(cropManagerItem.get()));
    }
}
