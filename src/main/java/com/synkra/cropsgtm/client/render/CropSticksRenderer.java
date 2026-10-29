package com.synkra.cropsgtm.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ISeedData;
import com.synkra.cropsgtm.api.PlantRenderShape;
import com.synkra.cropsgtm.blocks.BlockEntityCropSticks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 渲染生长在作物架方块上的植物。
 * <p>
 * 几何形状对应原模组的四平面十字、双十字与四平面花朵造型，全部按方块像素单位缩放。
 * </p>
 */
public class CropSticksRenderer implements BlockEntityRenderer<BlockEntityCropSticks> {

    private static final float PIXEL = 1.0f / 16.0f;

    /** 作物架贴图，方块模型的四根角柱使用同一 sprite。 */
    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath(CropsGTM.MOD_ID,
            "block/cropsticks");

    /** 四根横杆，仅在作物架为双作物架时绘制。 */
    private static final float[][] CROSS_BAR_BOXES = { { 0, 10, 2, 16, 11, 3 }, { 0, 10, 13, 16, 11, 14 },
            { 2, 10, 0, 3, 11, 16 }, { 13, 10, 0, 14, 11, 16 } };

    /** 从方块贴图集解析出的 sprite，{@code null} 表示该位置未被缝合进图集。 */
    private static final Map<ResourceLocation, TextureAtlasSprite> SPRITE_CACHE = new HashMap<>();
    private static final Set<ResourceLocation> REPORTED_MISSING = new HashSet<>();

    /** 每个顶点：x、y、z、u 标志（0 表示最小，1 表示最大）、v 标志（0 表示最小，1 表示最大）。 */
    private static final float[] HASH_SHAPE = { // 第 1 层，z = 4
            0, 0, 4, 1, 1, 0, 16, 4, 1, 0, 16, 16, 4, 0, 0, 16, 0, 4, 0, 1,
            // 第 2 层，x = 4
            4, 0, 0, 0, 1, 4, 0, 16, 1, 1, 4, 16, 16, 1, 0, 4, 16, 0, 0, 0,
            // 第 3 层，z = 12
            0, 0, 12, 0, 1, 16, 0, 12, 1, 1, 16, 16, 12, 1, 0, 0, 16, 12, 0, 0,
            // 第 4 层，x = 12
            12, 0, 16, 0, 1, 12, 0, 0, 1, 1, 12, 16, 0, 1, 0, 12, 16, 16, 0, 0 };

    private static final float[] X_SHAPE = { // 第 1 层
            2, 0, 2, 1, 1, 2, 12, 2, 1, 0, 14, 12, 14, 0, 0, 14, 0, 14, 0, 1,
            // 第 2 层
            2, 0, 14, 0, 1, 14, 0, 2, 1, 1, 14, 12, 2, 1, 0, 2, 12, 14, 0, 0 };

    private static final float[] FLOWER_SHAPE = { // 第 1 层，z = 4
            6, 0, 4.001f, 1, 1, 6, 12, 4.001f, 1, 0, 18, 12, 4.001f, 0, 0, 18, 0, 4.001f, 0, 1,
            // 第 2 层，x = 4
            3.999f, 0, 6, 0, 1, 3.999f, 0, 18, 1, 1, 3.999f, 12, 18, 1, 0, 3.999f, 12, 6, 0, 0,
            // 第 3 层，z = 12
            6, 0, 11.999f, 0, 1, 18, 0, 11.999f, 1, 1, 18, 12, 11.999f, 1, 0, 6, 12, 11.999f, 0, 0,
            // 第 4 层，x = 12
            11.999f, 0, 18, 0, 1, 11.999f, 0, 6, 1, 1, 11.999f, 12, 6, 1, 0, 11.999f, 12, 18, 0, 0 };

    public CropSticksRenderer(BlockEntityRendererProvider.Context context) {}

    /** 丢弃缓存的贴图 sprite，资源重载后贴图集会自动重新解析。 */
    public static void clearSpriteCache() {
        SPRITE_CACHE.clear();
        REPORTED_MISSING.clear();
    }

    private static TextureAtlasSprite getSprite(ResourceLocation location) {
        if (SPRITE_CACHE.containsKey(location)) return SPRITE_CACHE.get(location);
        TextureAtlasSprite sprite = null;
        try {
            sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
        } catch (IllegalArgumentException e) {
            // 该 sprite 未被缝合进方块贴图集，跳过它而不是每帧崩溃
            if (REPORTED_MISSING.add(location)) {
                CropsGTM.LOGGER.warn("Missing block atlas sprite {}, the crop plant will not be rendered", location);
            }
        }
        SPRITE_CACHE.put(location, sprite);
        return sprite;
    }

    @Override
    public void render(BlockEntityCropSticks cropSticks, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PoseStack.Pose pose = poseStack.last();

        // 角柱来自方块模型，十字横杆是动态的，在此绘制
        if (cropSticks.isCrossCrop()) {
            TextureAtlasSprite frame = getSprite(FRAME_TEXTURE);
            if (frame != null) {
                renderCrossBars(bufferSource.getBuffer(RenderType.cutout()), pose, frame, packedLight, packedOverlay);
            }
        }

        ISeedData seed = cropSticks.getSeed();
        if (seed == null) return;
        ICropCard crop = seed.getCrop();
        List<ResourceLocation> sprites = crop.getSprites();
        if (sprites.isEmpty()) return;

        int index = Math.max(0, Math.min(sprites.size() - 1, crop.getSpriteIndex(cropSticks)));
        TextureAtlasSprite sprite = getSprite(sprites.get(index));
        if (sprite == null) return;

        // 植物 sprite 位于方块贴图集，因此必须用 cutout 方块渲染类型绘制
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());

        PlantRenderShape shape = crop.getRenderShape();
        float[] geometry = switch (shape) {
            case HASH -> HASH_SHAPE;
            case X -> X_SHAPE;
            case FLOWER -> FLOWER_SHAPE;
        };
        // 患病植物使用原模组应用于植物图层的同一绿色覆盖
        renderQuads(consumer, pose, geometry, sprite, packedLight, packedOverlay, cropSticks.isSick());
    }

    private static void renderCrossBars(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                        int packedLight, int packedOverlay) {
        for (float[] box : CROSS_BAR_BOXES) {
            addBox(consumer, pose, box[0], box[1], box[2], box[3], box[4], box[5], sprite, packedLight, packedOverlay);
        }
    }

    /**
     * 追加一个轴对齐盒体；cutout 渲染类型不做面剔除，因此每个面一个四边形即可。
     */
    private static void addBox(VertexConsumer consumer, PoseStack.Pose pose, float minX, float minY, float minZ,
                               float maxX, float maxY, float maxZ, TextureAtlasSprite sprite, int packedLight,
                               int packedOverlay) {
        // 向下
        addBoxQuad(consumer, pose, 0, -1, 0, sprite, packedLight, packedOverlay, minX, minY, minZ, maxX, minY, minZ,
                maxX, minY, maxZ, minX, minY, maxZ);
        // 向上
        addBoxQuad(consumer, pose, 0, 1, 0, sprite, packedLight, packedOverlay, minX, maxY, minZ, minX, maxY, maxZ,
                maxX, maxY, maxZ, maxX, maxY, minZ);
        // 向北
        addBoxQuad(consumer, pose, 0, 0, -1, sprite, packedLight, packedOverlay, minX, minY, minZ, minX, maxY, minZ,
                maxX, maxY, minZ, maxX, minY, minZ);
        // 向南
        addBoxQuad(consumer, pose, 0, 0, 1, sprite, packedLight, packedOverlay, minX, minY, maxZ, maxX, minY, maxZ,
                maxX, maxY, maxZ, minX, maxY, maxZ);
        // 向西
        addBoxQuad(consumer, pose, -1, 0, 0, sprite, packedLight, packedOverlay, minX, minY, minZ, minX, minY, maxZ,
                minX, maxY, maxZ, minX, maxY, minZ);
        // 向东
        addBoxQuad(consumer, pose, 1, 0, 0, sprite, packedLight, packedOverlay, maxX, minY, minZ, maxX, maxY, minZ,
                maxX, maxY, maxZ, maxX, minY, maxZ);
    }

    private static void addBoxQuad(VertexConsumer consumer, PoseStack.Pose pose, float normalX, float normalY,
                                   float normalZ, TextureAtlasSprite sprite, int packedLight, int packedOverlay,
                                   float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2,
                                   float z2, float x3, float y3, float z3) {
        // 方块实体不会被方块模型渲染器着色，因此面亮度在此处施加
        int shade = faceShade(normalX, normalY, normalZ);
        addVertex(consumer, pose, x0, y0, z0, sprite.getU0(), sprite.getV1(), normalX, normalY, normalZ, shade, shade,
                shade, packedLight, packedOverlay);
        addVertex(consumer, pose, x1, y1, z1, sprite.getU1(), sprite.getV1(), normalX, normalY, normalZ, shade, shade,
                shade, packedLight, packedOverlay);
        addVertex(consumer, pose, x2, y2, z2, sprite.getU1(), sprite.getV0(), normalX, normalY, normalZ, shade, shade,
                shade, packedLight, packedOverlay);
        addVertex(consumer, pose, x3, y3, z3, sprite.getU0(), sprite.getV0(), normalX, normalY, normalZ, shade, shade,
                shade, packedLight, packedOverlay);
    }

    /**
     * 方块模型渲染器为每个面施加的亮度，方块实体渲染器必须自行施加。
     */
    private static int faceShade(float normalX, float normalY, float normalZ) {
        if (normalY > 0.0f) return 255;
        if (normalY < 0.0f) return 128;
        return normalZ != 0.0f ? 204 : 153;
    }

    private static void renderQuads(VertexConsumer consumer, PoseStack.Pose pose, float[] geometry,
                                    TextureAtlasSprite sprite, int packedLight, int packedOverlay, boolean sick) {
        for (int quad = 0; quad < geometry.length; quad += 20) {
            addQuad(consumer, pose, geometry, quad, false, sprite, packedLight, packedOverlay, sick);
            addQuad(consumer, pose, geometry, quad, true, sprite, packedLight, packedOverlay, sick);
        }
    }

    /**
     * 追加一个植物四边形，背面使用顶点顺序相反的同一四边形。
     */
    private static void addQuad(VertexConsumer consumer, PoseStack.Pose pose, float[] geometry, int offset,
                                boolean reversed, TextureAtlasSprite sprite, int packedLight, int packedOverlay,
                                boolean sick) {
        // 患病植物染成绿色，与原模组给植物图层上色的方式一致
        int red = sick ? 0 : 255;
        int green = 255;
        int blue = sick ? 0 : 255;
        for (int i = 0; i < 4; i++) {
            int vertexIndex = reversed ? 3 - i : i;
            int o = offset + vertexIndex * 5;
            addVertex(consumer, pose, geometry[o], geometry[o + 1], geometry[o + 2],
                    geometry[o + 3] == 0.0f ? sprite.getU0() : sprite.getU1(),
                    geometry[o + 4] == 0.0f ? sprite.getV0() : sprite.getV1(), 0, 1, 0, red, green, blue, packedLight,
                    packedOverlay);
        }
    }

    private static void addVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u,
                                  float v, float normalX, float normalY, float normalZ, int red, int green, int blue,
                                  int packedLight, int packedOverlay) {
        consumer.vertex(pose.pose(), x * PIXEL, y * PIXEL, z * PIXEL).color(red, green, blue, 255).uv(u, v)
                .overlayCoords(packedOverlay).uv2(packedLight).normal(pose.normal(), normalX, normalY, normalZ)
                .endVertex();
    }
}
