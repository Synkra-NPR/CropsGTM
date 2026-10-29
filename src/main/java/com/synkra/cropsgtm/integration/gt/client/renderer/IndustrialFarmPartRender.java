package com.synkra.cropsgtm.integration.gt.client.renderer;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.client.model.machine.IControllerModelRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.util.ModelUtils;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 在已成型工业农场部件的基座立方体上绘制机壳方块自身的模型。
 * <p>
 * GregTech 会用控制器机壳贴图重新贴图成型多方块的部件，但它只替换部件已烘焙四边形的贴图。这些四边形按部件的朝向烘焙，因此砖墙这类方向性贴图会随扳手旋转一起转动。改为绘制机壳方块自身的模型可以让砖纹保持世界朝向，与
 * GregTech 替换锅炉部件的做法一致，只有部件自身的端口覆盖层仍随之旋转。
 * </p>
 * <p>
 * 部件自身的基底贴图已被控制器模型的贴图覆盖置空，因此这里追加的四边形是唯一被绘制的基座几何体。
 * </p>
 */
public class IndustrialFarmPartRender extends DynamicRender<MultiblockControllerMachine, IndustrialFarmPartRender>
                                      implements IControllerModelRenderer {

    // spotless:off
    public static final Codec<IndustrialFarmPartRender> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("casing").forGetter(IndustrialFarmPartRender::getCasing)
    ).apply(instance, IndustrialFarmPartRender::new));
    public static final DynamicRenderType<MultiblockControllerMachine, IndustrialFarmPartRender> TYPE =
            new DynamicRenderType<>(IndustrialFarmPartRender.CODEC);
    // spotless:on

    private final BlockState casing;
    private BakedModel casingModel;

    public IndustrialFarmPartRender(BlockState casing) {
        this.casing = casing;
    }

    public BlockState getCasing() {
        return this.casing;
    }

    @Override
    public DynamicRenderType<MultiblockControllerMachine, IndustrialFarmPartRender> getType() {
        return TYPE;
    }

    @Override
    public void render(MultiblockControllerMachine machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {}

    @Override
    public boolean shouldRender(MultiblockControllerMachine machine, Vec3 cameraPos) {
        return false;
    }

    @Override
    public boolean isBlockEntityRenderer() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderPartModel(List<BakedQuad> quads, IMultiController controller, IMultiPart part,
                                Direction frontFacing, @Nullable Direction side, RandomSource rand,
                                @NotNull ModelData modelData, @Nullable RenderType renderType) {
        if (this.casingModel == null) {
            this.casingModel = ModelUtils.getModelForState(this.casing);
        }
        if (this.casingModel == null) return;

        BlockAndTintGetter level = controller.self().getLevel();
        BlockPos pos = part.self().getPos();
        ModelData data = this.casingModel.getModelData(level, pos, this.casing, modelData);
        quads.addAll(this.casingModel.getQuads(this.casing, side, rand, data, renderType));
    }
}
