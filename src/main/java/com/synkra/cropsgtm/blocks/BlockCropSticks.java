package com.synkra.cropsgtm.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.synkra.cropsgtm.api.ICropLeftClickHandler;
import com.synkra.cropsgtm.farming.registries.SoilRegistry;
import com.synkra.cropsgtm.init.CropsGTMBlocks;

import javax.annotation.Nullable;

/**
 * 作物架方块，植物由其方块实体持有。
 */
public class BlockCropSticks extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(2.0d, 0.0d, 2.0d, 14.0d, 13.0d, 14.0d);

    public BlockCropSticks() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).noCollission().instabreak().sound(SoundType.WOOD)
                .noOcclusion().noLootTable().dynamicShape());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCropSticks(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, CropsGTMBlocks.cropSticksTE(),
                (tickLevel, tickPos, tickState, cropSticks) -> cropSticks.tick());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return SoilRegistry.instance.isRegistered(level.getBlockState(pos.below()));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCropSticks cropSticks)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return cropSticks.onRightClick(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityCropSticks cropSticks)) return;
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.getItem() instanceof ICropLeftClickHandler handler &&
                handler.onLeftClick(cropSticks, player, heldItem)) {
            return;
        }
        cropSticks.onLeftClick(player);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BlockEntityCropSticks cropSticks) {
            if (!player.isCreative()) {
                for (ItemStack drop : cropSticks.getBreakItems()) {
                    Block.popResource(level, pos, drop);
                }
            }
            cropSticks.clear();
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                boolean isMoving) {
        if (level.isClientSide) return;
        if (!canSurvive(state, level, pos)) {
            if (level.getBlockEntity(pos) instanceof BlockEntityCropSticks cropSticks) {
                cropSticks.breakCropStick(false);
            } else {
                level.removeBlock(pos, false);
            }
            return;
        }
        if (level.getBlockEntity(pos) instanceof BlockEntityCropSticks cropSticks) cropSticks.onNeighbourChange();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof BlockEntityCropSticks cropSticks) cropSticks.onEntityCollision(entity);
    }
}
