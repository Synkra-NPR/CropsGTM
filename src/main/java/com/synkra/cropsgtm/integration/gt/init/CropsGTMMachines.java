package com.synkra.cropsgtm.integration.gt.init;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.client.model.machine.overlays.WorkableOverlays;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.models.GTMachineModels;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;

import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock.ComponentType;
import com.synkra.cropsgtm.integration.gt.client.renderer.IndustrialFarmPartRender;
import com.synkra.cropsgtm.integration.gt.machines.CropBreederMachine;
import com.synkra.cropsgtm.integration.gt.machines.CropGeneExtractorMachine;
import com.synkra.cropsgtm.integration.gt.machines.CropManagerMachine;
import com.synkra.cropsgtm.integration.gt.machines.CropSynthesizerMachine;
import com.synkra.cropsgtm.integration.gt.machines.IndustrialFarmMachine;
import com.synkra.cropsgtm.integration.gt.machines.SeedGeneratorMachine;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.List;

import static com.gregtechceu.gtceu.api.machine.multiblock.PartAbility.*;
import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;

/**
 * CropsGTM 添加的全部 GregTech 机器定义。
 */
public final class CropsGTMMachines {

    private CropsGTMMachines() {}

    public static final GTRegistrate REGISTRATE = GTRegistrate.create(Names.MOD_ID);

    /**
     * 机器 id 的电压等级后缀，按电压等级索引。
     * <p>
     * GregTech CEu 没有 MAX 机器，电力机器止步于 OpV，因此不注册 MAX 等级。
     * </p>
     */
    private static final String[] TIER_NAMES = { "", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv", "uhv", "uev",
            "uiv", "uxv", "opv" };

    public static final MachineDefinition[] CROP_MANAGERS = new MachineDefinition[TIER_NAMES.length];
    public static final MachineDefinition[] CROP_BREEDERS = new MachineDefinition[TIER_NAMES.length];
    public static final MachineDefinition[] SEED_GENERATORS = new MachineDefinition[TIER_NAMES.length];
    public static final MachineDefinition[] CROP_GENE_EXTRACTORS = new MachineDefinition[TIER_NAMES.length];
    public static final MachineDefinition[] CROP_SYNTHESIZERS = new MachineDefinition[TIER_NAMES.length];

    /** 在 GregTech 触发其机器注册事件时填充。 */
    public static MultiblockMachineDefinition INDUSTRIAL_FARM;

    // 动态渲染类型必须在数据生成器写入机器模型之前已知
    // 且在客户端加载之前，因此它与机器定义一起注册
    static {
        DynamicRenderManager.register(
                ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "industrial_farm_part_casing"),
                IndustrialFarmPartRender.TYPE);
    }

    /**
     * 在 GregTech 的机器注册事件中注册所有机器定义；该事件在 GregTech 初始化完注册表之后、注册表冻结之前触发。
     */
    public static void register(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        INDUSTRIAL_FARM = REGISTRATE.multiblock("industrial_farm", IndustrialFarmMachine::new)
                // 农场只朝水平方向，扩展朝向会多出朝上的状态
                // 该状态在 GT 7.5.3 中会渲染出多余的 z 轴旋转
                .allowExtendedFacing(false).rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.DUMMY_RECIPES).appearanceBlock(CropsGTMIndustrialBlocks::industrialFarmCasing)
                .pattern(definition -> createIndustrialFarmPattern(definition, -1)).shapeInfos(definition -> {
                    List<MultiblockShapeInfo> shapes = new ArrayList<>();
                    for (int tier = IndustrialFarmMachine.MIN_TIER; tier <= IndustrialFarmMachine.MAX_TIER; tier++) {
                        int slices = IndustrialFarmComponentBlock.getMultiLength(tier);
                        // 形状的排列顺序与样板一致，预览组件按此放置
                        // 控制器朝北。
                        MultiblockShapeInfo.ShapeInfoBuilder builder = MultiblockShapeInfo.builder()
                                // 带控制器与全部舱口的前壁，只有 'C' 位置接受它们
                                .aisle("c   c", "cI~Oc", "cFEMc", " cCc ");
                        // 带种子床的可重复中间层
                        for (int slice = 0; slice < slices; slice++) {
                            builder.aisle("     ", "csssc", "g   g", " gUg ");
                        }
                        // 后壁，它在与前面壁相同的位置接受舱口，只有
                        // 前壁的控制器位置在这里是普通机壳
                        builder.aisle("c   c", "cCcCc", "cCCCc", " cCc ")
                                .where('c', CropsGTMIndustrialBlocks.industrialFarmCasing.get())
                                .where('C', CropsGTMIndustrialBlocks.industrialFarmCasing.get())
                                .where('s',
                                        CropsGTMIndustrialBlocks.industrialFarmComponent(ComponentType.SEED_BED, tier)
                                                .defaultBlockState())
                                .where('U',
                                        CropsGTMIndustrialBlocks
                                                .industrialFarmComponent(ComponentType.FERTILIZER_UNIT, tier)
                                                .defaultBlockState())
                                // 预览使用该电压下仍允许的最弱玻璃
                                .where('g', IndustrialFarmMachine.getAllowedGlass(tier).get(0))
                                .where('I', GTMachines.ITEM_IMPORT_BUS[tier], Direction.NORTH)
                                .where('O', GTMachines.ITEM_EXPORT_BUS[tier], Direction.NORTH)
                                .where('F', GTMachines.FLUID_IMPORT_HATCH[tier], Direction.NORTH)
                                .where('M', GTMachines.MAINTENANCE_HATCH, Direction.NORTH)
                                .where('E', GTMachines.ENERGY_INPUT_HATCH[tier], Direction.NORTH)
                                .where('~', previewController(definition, tier));
                        shapes.add(builder.build());
                    }
                    return shapes;
                }).modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                // 与 GregTech 电力高炉相同的模型模式：单张机壳贴图覆盖
                // 每个面，加上 workable 覆盖层，使机壳在
                // 多方块被旋转，成型部件重新贴上机壳
                .model(workableCasingModel(
                        ResourceLocation.fromNamespaceAndPath(Names.MOD_ID,
                                "block/industrialfarm/brickedagriculturalcasing"),
                        ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "block/machine/industrial_farm")))
                .tooltipBuilder((stack, tooltips) -> {
                    tooltips.add(gray("cropsgtm_tooltip.industrialFarm.0"));
                    tooltips.add(gray("cropsgtm_tooltip.industrialFarm.tooltip.0"));
                    tooltips.add(gray("cropsgtm_tooltip.industrialFarm.tooltip.1"));
                    tooltips.add(gray("cropsgtm_tooltip.industrialFarm.tooltip.2"));
                    tooltips.add(gray("cropsgtm_tooltip.industrialFarm.tooltip.3"));
                }).register();

        for (int tier = GTValues.LV; tier < TIER_NAMES.length; tier++) {
            final int machineTier = tier;
            CROP_MANAGERS[tier] = REGISTRATE
                    .machine("crop_manager_" + TIER_NAMES[tier], info -> new CropManagerMachine(info, machineTier))
                    .tier(tier).rotationState(RotationState.NON_Y_AXIS)
                    .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                    .model(workableHullModel(
                            ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "block/machines/crop_manager")))
                    .tooltips(
                            gray("gtceu.universal.tooltip.voltage_in",
                                    FormattingUtil.formatNumbers(GTValues.V[machineTier]), GTValues.VNF[machineTier]),
                            gray("gtceu.universal.tooltip.energy_storage_capacity",
                                    FormattingUtil.formatNumbers(CropManagerMachine.getEnergyCapacity(machineTier))),
                            gray("cropsgtm_tooltip.cropManager.description"),
                            gray("cropsgtm_tooltip.cropManager.tooltip.0"),
                            gray("cropsgtm_tooltip.cropManager.tooltip.4",
                                    CropManagerMachine.getHorizontalRadius(machineTier) * 2 + 1,
                                    CropManagerMachine.getVerticalRadius() * 2 + 1),
                            gray("cropsgtm_tooltip.cropManager.tooltip.1", GTValues.V[machineTier] / 8),
                            gray("cropsgtm_tooltip.cropManager.tooltip.2", GTValues.V[machineTier] / 32),
                            gray("cropsgtm_tooltip.cropManager.tooltip.5", (int) (0.05d * machineTier * 100) + "%"),
                            gray("cropsgtm_tooltip.cropManager.tooltip.6",
                                    CropManagerMachine.getWaterCapacity(machineTier)),
                            gray("cropsgtm_tooltip.cropManager.tooltip.8", machineTier * 1500))
                    .register();

            CROP_BREEDERS[tier] = REGISTRATE
                    .machine("crop_breeder_" + TIER_NAMES[tier], info -> new CropBreederMachine(info, machineTier))
                    .tier(tier).recipeType(CropsGTMRecipeTypes.CROP_BREEDER_RECIPES)
                    .rotationState(RotationState.NON_Y_AXIS)
                    .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                    .model(workableHullModel(
                            ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "block/machines/crop_breeder")))
                    .tooltips(
                            gray("gtceu.universal.tooltip.voltage_in",
                                    FormattingUtil.formatNumbers(GTValues.V[machineTier]), GTValues.VNF[machineTier]),
                            gray("gtceu.universal.tooltip.energy_storage_capacity",
                                    FormattingUtil.formatNumbers(GTValues.V[machineTier] * 64L)),
                            gray("cropsgtm_tooltip.cropBreeder.0"),
                            gray("cropsgtm_tooltip.cropBreeder.1", CropBreederMachine.FERTILIZER_PER_TIER),
                            gray("cropsgtm_tooltip.cropBreeder.2", CropBreederMachine.FERTILIZER_PER_STAT),
                            gray("cropsgtm_tooltip.cropBreeder.3"),
                            gray("cropsgtm_tooltip.cropBreeder.4", CropBreederMachine.getOutputChance(machineTier)),
                            gray("cropsgtm_tooltip.cropBreeder.5",
                                    CropBreederMachine.getCustomFluidCapacity(machineTier)),
                            gray("cropsgtm_tooltip.cropBreeder.6", CropBreederMachine.getInputSlotCount(machineTier)))
                    .register();

            SEED_GENERATORS[tier] = REGISTRATE
                    .machine("seed_generator_" + TIER_NAMES[tier], info -> new SeedGeneratorMachine(info, machineTier))
                    .tier(tier).recipeType(CropsGTMRecipeTypes.SEED_GENERATOR_RECIPES)
                    .rotationState(RotationState.NON_Y_AXIS)
                    .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                    .model(workableHullModel(
                            ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "block/machines/seed_generator")))
                    .tooltips(
                            gray("gtceu.universal.tooltip.voltage_in",
                                    FormattingUtil.formatNumbers(GTValues.V[machineTier]), GTValues.VNF[machineTier]),
                            gray("gtceu.universal.tooltip.energy_storage_capacity",
                                    FormattingUtil.formatNumbers(GTValues.V[machineTier] * 64L)),
                            gray("cropsgtm_tooltip.seedGenerator.0"),
                            gray("cropsgtm_tooltip.seedGenerator.1", SeedGeneratorMachine.FERTILIZER_PER_STAT),
                            gray("cropsgtm_tooltip.seedGenerator.2"), gray("cropsgtm_tooltip.seedGenerator.3",
                                    SeedGeneratorMachine.getCustomFluidCapacity(machineTier)))
                    .register();

            // 基因提取机与合成机仅从 EV 起提供，电压
            // 机器等级限制了它们能处理的作物
            if (tier >= GTValues.EV) {
                CROP_GENE_EXTRACTORS[tier] = REGISTRATE
                        .machine("crop_gene_extractor_" + TIER_NAMES[tier],
                                info -> new CropGeneExtractorMachine(info, machineTier))
                        .tier(tier).recipeType(CropsGTMRecipeTypes.CROP_GENE_EXTRACTOR_RECIPES)
                        .rotationState(RotationState.NON_Y_AXIS)
                        .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                        .model(workableHullModel(ResourceLocation.fromNamespaceAndPath(Names.MOD_ID,
                                "block/machines/crop_gene_extractor")))
                        .tooltips(gray("gtceu.universal.tooltip.voltage_in",
                                FormattingUtil.formatNumbers(GTValues.V[machineTier]), GTValues.VNF[machineTier]),
                                gray("gtceu.universal.tooltip.energy_storage_capacity",
                                        FormattingUtil.formatNumbers(GTValues.V[machineTier] * 64L)),
                                gray("cropsgtm_tooltip.cropGeneExtractor.0"),
                                gray("cropsgtm_tooltip.cropGeneExtractor.1"),
                                gray("cropsgtm_tooltip.cropGeneExtractor.2"),
                                gray("cropsgtm_tooltip.cropGeneExtractor.3"))
                        .register();

                CROP_SYNTHESIZERS[tier] = REGISTRATE
                        .machine("crop_synthesizer_" + TIER_NAMES[tier],
                                info -> new CropSynthesizerMachine(info, machineTier))
                        .tier(tier).recipeType(CropsGTMRecipeTypes.CROP_SYNTHESIZER_RECIPES)
                        .rotationState(RotationState.NON_Y_AXIS)
                        .modelProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                        .model(workableHullModel(
                                ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "block/machines/crop_synthesizer")))
                        .tooltips(gray("gtceu.universal.tooltip.voltage_in",
                                FormattingUtil.formatNumbers(GTValues.V[machineTier]), GTValues.VNF[machineTier]),
                                gray("gtceu.universal.tooltip.energy_storage_capacity",
                                        FormattingUtil.formatNumbers(GTValues.V[machineTier] * 64L)),
                                gray("cropsgtm_tooltip.cropSynthesizer.0"),
                                gray("cropsgtm_tooltip.cropSynthesizer.1", CropSynthesizerMachine.UUM_PER_TIER),
                                gray("cropsgtm_tooltip.cropSynthesizer.2", CropSynthesizerMachine.UUM_PER_STAT),
                                gray("cropsgtm_tooltip.cropSynthesizer.3", CropSynthesizerMachine.AMPERAGE),
                                gray("cropsgtm_tooltip.cropSynthesizer.4",
                                        CropSynthesizerMachine.getCustomFluidCapacity(machineTier)))
                        .register();
            }
        }
    }

    /** 强制该类的静态初始化在模组构造阶段运行。 */
    public static void init() {}

    /** 一行提示文本，样式与 GregTech 自家机器提示一致。 */
    private static Component gray(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    /**
     * 构建工业农场的多方块样板。
     * 
     * @param tier 结构预览页的电压；传入负值表示用于校验已放置结构的通用样板。预览样板只认识自身电压的方块，因此配方查看器会列出对应的候选项而非全部等级。
     */
    public static BlockPattern createIndustrialFarmPattern(MultiblockMachineDefinition definition, int tier) {
        boolean preview = tier >= IndustrialFarmMachine.MIN_TIER && tier <= IndustrialFarmMachine.MAX_TIER;
        List<Block> seedBeds = new ArrayList<>();
        List<Block> upgrades = new ArrayList<>();
        if (preview) {
            seedBeds.add(CropsGTMIndustrialBlocks.industrialFarmComponent(ComponentType.SEED_BED, tier));
            addUpgradeBlocks(upgrades, tier);
        } else {
            for (int value = IndustrialFarmMachine.MIN_TIER; value <= IndustrialFarmMachine.MAX_TIER; value++) {
                seedBeds.add(CropsGTMIndustrialBlocks.industrialFarmComponent(ComponentType.SEED_BED, value));
                addUpgradeBlocks(upgrades, value);
            }
        }

        // 前壁与后壁在相同位置接受舱口，其余每个机壳
        // 位置只能由机壳方块本身构成。
        TraceabilityPredicate hatchCasing = blocks(CropsGTMIndustrialBlocks.industrialFarmCasing.get())
                .or(abilities(INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(abilities(IMPORT_ITEMS).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(abilities(EXPORT_ITEMS).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(abilities(IMPORT_FLUIDS).setMinGlobalLimited(1).setMaxGlobalLimited(2))
                .or(autoAbilities(true, false, false));
        TraceabilityPredicate casing = blocks(CropsGTMIndustrialBlocks.industrialFarmCasing.get());

        TraceabilityPredicate glass;
        if (preview) {
            // 只使用该电压支持的玻璃，使预览候选项与该页一致
            glass = blocks(IndustrialFarmMachine.getAllowedGlass(tier).toArray(new Block[0]));
        } else {
            // 已放置的结构接受任意玻璃，电压检查在成型后进行
            List<Block> glasses = new ArrayList<>(
                    IndustrialFarmMachine.getAllowedGlass(IndustrialFarmMachine.MIN_TIER));
            glasses.add(GTBlocks.CLEANROOM_GLASS.get());
            glass = Predicates.custom(
                    state -> state.getBlockState().is(BlockTags.IMPERMEABLE) ||
                            state.getBlockState().getBlock() instanceof GlassBlock,
                    () -> glasses.stream().map(block -> BlockInfo.fromBlockState(block.defaultBlockState()))
                            .toArray(BlockInfo[]::new));
        }
        TraceabilityPredicate upgrade = blocks(upgrades.toArray(new Block[0])).or(frames(GTMaterials.Wood));

        // 通道轴指向后方，使控制器保持在前表面，而
        // 种子床切片向其后延伸，与原 CropNH 布局一致。
        FactoryBlockPattern pattern = FactoryBlockPattern.start(RIGHT, UP, BACK)
                // 前切片，控制器位于第二层中间
                .aisle("c   c", "cC~Cc", "cCCCc", " cCc ");
        if (preview) {
            // 结构的长度只取决于电压
            int slices = IndustrialFarmComponentBlock.getMultiLength(tier);
            pattern.aisleRepeatable(slices, slices, "     ", "csssc", "g   g", " gUg ");
        } else {
            pattern.aisleRepeatable(IndustrialFarmMachine.MIN_SLICES, IndustrialFarmMachine.MAX_SLICES, "     ",
                    "csssc", "g   g", " gUg ");
        }
        return pattern.aisle("c   c", "cCcCc", "cCCCc", " cCc ").where('~', controller(blocks(definition.getBlock())))
                .where('c', casing).where('C', hatchCasing)
                .where('s', blocks(seedBeds.toArray(new Block[0])).setMinGlobalLimited(3)).where('U', upgrade)
                .where('g', glass).build();
    }

    /** 把指定电压的全部升级方块加入列表。 */
    private static void addUpgradeBlocks(List<Block> upgrades, int tier) {
        upgrades.add(CropsGTMIndustrialBlocks.industrialFarmComponent(ComponentType.FERTILIZER_UNIT, tier));
        upgrades.add(CropsGTMIndustrialBlocks.industrialFarmComponent(ComponentType.GROWTH_ACCELERATION_UNIT, tier));
        upgrades.add(CropsGTMIndustrialBlocks.industrialFarmComponent(ComponentType.ADVANCED_HARVESTING_UNIT, tier));
        upgrades.add(
                CropsGTMIndustrialBlocks.industrialFarmComponent(ComponentType.ENVIRONMENTAL_ENHANCEMENT_UNIT, tier));
        if (tier >= GTValues.ZPM) {
            upgrades.add(CropsGTMIndustrialBlocks
                    .industrialFarmComponent(ComponentType.OVERCLOCKED_GROWTH_ACCELERATION_UNIT, tier));
        }
    }

    /**
     * @return 结构预览页对应的控制器方块：它告知预览控制器该页属于哪个电压等级，以便使用匹配的样板。
     */
    private static BlockInfo previewController(MultiblockMachineDefinition definition, int tier) {
        BlockState state = definition.getBlock().defaultBlockState().setValue(definition.getRotationState().property,
                Direction.NORTH);
        return new BlockInfo(state, blockEntity -> {
            if (blockEntity instanceof IMachineBlockEntity holder &&
                    holder.getMetaMachine() instanceof IndustrialFarmMachine farm) {
                farm.setPreviewTier(tier);
            }
        });
    }

    /**
     * 与 GregTech 电力高炉相同的模型布局，在此重新实现，因为 {@link MachineBuilder#workableCasingModel(ResourceLocation, ResourceLocation)}
     * 通过数据生成器的既有文件助手解析模型，而它看不到 GregTech 自身的资源。
     * <p>
     * 单一机壳贴图覆盖所有面，因此多方块旋转时机壳朝向不变；成型部件通过贴图覆盖被重新贴上机壳。
     * </p>
     */
    private static MachineBuilder.ModelInitializer workableCasingModel(ResourceLocation casingTexture,
                                                                       ResourceLocation overlayDir) {
        return (ctx, prov, builder) -> {
            WorkableOverlays overlays = WorkableOverlays.get(overlayDir, prov.getExistingFileHelper());
            builder.forAllStates(state -> {
                RecipeLogic.Status status = state.getValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS);
                BlockModelBuilder model = prov.models().nested()
                        .parent(new ModelFile.UncheckedModelFile(GTCEu.id("block/machine/template/cube_all/sided")))
                        .texture("all", casingTexture);
                return GTMachineModels.addWorkableOverlays(overlays, status, model);
            });
            builder.addTextureOverride("all", casingTexture);
            // 成型多方块的部件借用该机壳贴图。GregTech 只会替换它们的
            // sprite，这会随部件朝向旋转方向性砖纹，因此
            // 改为在它们的基座立方体上绘制机壳方块自身的模型，只有端口覆盖层
            // 仍会跟随扳手旋转。
            builder.addDynamicRenderer(() -> new IndustrialFarmPartRender(
                    CropsGTMIndustrialBlocks.industrialFarmCasing.get().defaultBlockState()));
            // 机壳贴图带有方向性，锁定其 UV 可使砖纹无论
            // 控制器朝向的水平方向，与 GregTech 处理此类机壳的方式一致
            builder.replaceForAllStates((state, models) -> {
                for (int i = 0; i < models.length; i++) {
                    models[i] = ConfiguredModel.builder().modelFile(models[i].model).uvLock(true).buildLast();
                }
                return models;
            });
        };
    }

    /**
     * 构建标准的 GregTech 带等级机壳模型，并在其上叠加本模组的覆盖贴图。
     * <p>
     * GT 自带的 {@link MachineBuilder#workableTieredHullModel(ResourceLocation)}
     * 要求生成的带等级机壳模型文件对数据生成器可见，而附属模组不满足这一点。在这里自行构建机壳模型可以保留标准 GT 机壳（及其随电压变化的颜色），只替换叠加在上面的覆盖贴图。
     * </p>
     */
    private static MachineBuilder.ModelInitializer workableHullModel(ResourceLocation overlayDir) {
        return (ctx, prov, builder) -> {
            WorkableOverlays overlays = WorkableOverlays.get(overlayDir, prov.getExistingFileHelper());
            // GT 机器使用带等级电压机壳作为外壳，贴图已镜像到本模组资源中
            // 使数据生成器无需依赖 GregTech 生成的资源即可解析它们。
            ResourceLocation casing = ResourceLocation.fromNamespaceAndPath(Names.MOD_ID, "block/machine/casing/" +
                    GTValues.VN[builder.getOwner().getTier()].toLowerCase(java.util.Locale.ROOT));
            builder.forAllStates(state -> {
                RecipeLogic.Status status = state.getValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS);
                BlockModelBuilder model = prov.models().nested()
                        .parent(new ModelFile.UncheckedModelFile(GTCEu.id("block/machine/template/sided/sided")));
                model.texture("bottom", casing.withSuffix("/bottom"));
                model.texture("side", casing.withSuffix("/side"));
                model.texture("top", casing.withSuffix("/top"));
                return GTMachineModels.addWorkableOverlays(overlays, status, model);
            });
            builder.addReplaceableTextures("bottom", "top", "side");
        };
    }
}
