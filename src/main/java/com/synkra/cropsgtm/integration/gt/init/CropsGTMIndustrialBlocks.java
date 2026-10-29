package com.synkra.cropsgtm.integration.gt.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock.ComponentType;

import java.util.EnumMap;
import java.util.Locale;

/**
 * 工业农场方块，仅在安装 GregTech CEu Modern 时注册。
 * <p>
 * 该类在缺少 GregTech 时绝不会被加载，因此可以安全引用其组件所需的 GregTech 类。
 * </p>
 */
public final class CropsGTMIndustrialBlocks {

    private CropsGTMIndustrialBlocks() {}

    /** 强制静态初始化块在方块注册事件触发前运行。 */
    public static void init() {}

    /** 工业农场多方块的结构机壳。 */
    public static final RegistryObject<Block> industrialFarmCasing = CropsGTMBlocks.BLOCKS
            .register("industrial_farm_casing",
                    () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED)
                            // 硬度与爆炸抗性与 GregTech 机壳、加热线圈一致
                            .strength(5.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));

    /**
     * 全部带等级的工业农场组件，按电压等级索引。
     * <p>
     * 不在组件允许范围内的等级保持 {@code null}。
     * </p>
     */
    public static final EnumMap<ComponentType, RegistryObject<IndustrialFarmComponentBlock>[]> INDUSTRIAL_FARM_COMPONENTS = new EnumMap<>(
            ComponentType.class);

    /**
     * 组件注册 id 使用的电压名称后缀。
     * <p>
     * GregTech CEu 没有 MAX 机器，因此工业农场组件同样止步于 OpV。
     * </p>
     */
    public static final String[] VOLTAGE_NAMES = { "ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv",
            "uev", "uiv", "uxv", "opv" };

    static {
        for (ComponentType type : ComponentType.values()) {
            @SuppressWarnings("unchecked")
            RegistryObject<IndustrialFarmComponentBlock>[] blocks = new RegistryObject[VOLTAGE_NAMES.length];
            INDUSTRIAL_FARM_COMPONENTS.put(type, blocks);
            for (int tier = type.getMinTier(); tier <= type.getMaxTier(); tier++) {
                final int blockTier = tier;
                String id = type.getIdPrefix() + "_" + VOLTAGE_NAMES[tier].toLowerCase(Locale.ROOT);
                blocks[tier] = CropsGTMBlocks.BLOCKS.register(id,
                        () -> new IndustrialFarmComponentBlock(type, blockTier));
            }
        }
    }

    /**
     * @return 指定组件等级的注册 id，用于方块状态与物品模型路径。
     */
    public static String componentId(ComponentType type, int tier) {
        return type.getIdPrefix() + "_" + VOLTAGE_NAMES[tier].toLowerCase(Locale.ROOT);
    }

    public static IndustrialFarmComponentBlock industrialFarmComponent(ComponentType type, int tier) {
        return INDUSTRIAL_FARM_COMPONENTS.get(type)[tier].get();
    }

    public static Block industrialFarmCasing() {
        return industrialFarmCasing.get();
    }
}
