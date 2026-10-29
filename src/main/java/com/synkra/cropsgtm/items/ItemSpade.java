package com.synkra.cropsgtm.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

import com.synkra.cropsgtm.api.ICropLeftClickHandler;
import com.synkra.cropsgtm.api.ICropRightClickHandler;
import com.synkra.cropsgtm.api.ICropSeedDropOverride;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IHarvestDropModifier;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 铲子可清除作物架上的杂草，并让玩家从无法收获的作物中收集种子。
 * <p>
 * 它也能像锄头一样使用，把草方块、泥土与菌丝体变成耕地，也能把耕地变回泥土。强化版作物抗性越高，掉落的种子越多。
 * </p>
 */
public class ItemSpade extends ShovelItem implements ICropRightClickHandler, ICropLeftClickHandler,
                       ICropSeedDropOverride, IHarvestDropModifier {

    /** 铲子能变成耕地的方块，以及耕地能变回的泥土。 */
    private static final Map<Block, Block> TILLABLE_BLOCKS = Map.of(Blocks.GRASS_BLOCK, Blocks.FARMLAND, Blocks.DIRT,
            Blocks.FARMLAND, Blocks.FARMLAND, Blocks.DIRT, Blocks.MYCELIUM, Blocks.FARMLAND);

    /** 该铲子提示翻译键的后缀。 */
    private final String tooltipName;
    /** 强化铲子根据作物抗性掉落更多种子。 */
    private final boolean reinforced;

    public ItemSpade(Tier tier, String tooltipName, boolean reinforced) {
        super(tier, 1.0f, -3.0f, new Item.Properties().stacksTo(1));
        this.tooltipName = tooltipName;
        this.reinforced = reinforced;
    }

    /** 铲子永不磨损，因此不需要耐久条。 */
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    /** 作物架总是会响应交互，即使玩家正潜行。 */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return level.getBlockState(pos).is(CropsGTMBlocks.cropSticks.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        if (player == null || context.getClickedFace() == Direction.DOWN) return InteractionResult.PASS;

        Block transformTo = TILLABLE_BLOCKS.get(level.getBlockState(pos).getBlock());
        // 上方方块必须为空，与使用锄头时相同
        if (transformTo == null || !level.isEmptyBlock(pos.above())) return InteractionResult.PASS;
        if (!player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, transformTo.defaultBlockState());
            SoundType sound = Blocks.FARMLAND.defaultBlockState().getSoundType();
            level.playSound(player, pos, sound.getStepSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f,
                    sound.getPitch() * 0.8f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean onRightClick(ICropStickTile tile, Player player, ItemStack heldItem) {
        if (tile.getLevel().isClientSide) return true;
        return this.doWork(tile, false);
    }

    @Override
    public boolean onLeftClick(ICropStickTile tile, Player player, ItemStack heldItem) {
        if (tile.getLevel().isClientSide) return true;
        return this.doWork(tile, true);
    }

    @Override
    public boolean overrideSeedDrop(Player player, ItemStack heldItem, Collection<ItemStack> dropTracker,
                                    ICropStickTile tile, boolean hasHarvested) {
        // 铲子已在作物收获时添加了它的种子掉落
        return hasHarvested;
    }

    @Override
    public void modifyCropDrops(Player player, ItemStack heldItem, Collection<ItemStack> dropTracker,
                                ICropStickTile tile) {
        this.addSeedDrop(tile, dropTracker);
    }

    /**
     * 移除杂草，或对无法收获的成熟作物给予种子掉落。
     * 
     * @param clearAfter true 表示彻底清除作物，false 表示仅重置生长进度
     * @return 铲子执行了任何操作时返回 true
     */
    private boolean doWork(ICropStickTile tile, boolean clearAfter) {
        if (tile.isCrossCrop()) return false;
        if (tile.hasWeed()) {
            // 成熟的杂草掉落高草
            if (tile.isMature()) tile.dropItem(new ItemStack(Items.GRASS));
            tile.clear();
            tile.playCropRemovalSound();
            return true;
        }

        // 无法收获的成熟作物仍能让玩家收集种子
        if (tile.isMature() && !tile.canHarvest() && tile.areGrowthRequirementsMet()) {
            List<ItemStack> drops = new ArrayList<>(1);
            this.addSeedDrop(tile, drops);
            drops.forEach(tile::dropItem);
            if (clearAfter) {
                tile.clear();
            } else {
                tile.setGrowthProgress(0);
            }
            return true;
        }
        return false;
    }

    private void addSeedDrop(ICropStickTile tile, Collection<ItemStack> dropTracker) {
        if (!this.reinforced) {
            ItemStack drop = tile.getSeedDrop();
            if (drop != null && !drop.isEmpty()) dropTracker.add(drop);
            return;
        }
        // 每 10 点抗性给予一颗种子，余数作为再多一颗的概率
        ItemStack seedStack = tile.getSeedStack();
        if (seedStack == null || seedStack.isEmpty() || tile.getSeed() == null) return;
        int resistance = tile.getSeed().getStats().getResistance();
        int count = resistance / 10;
        if (tile.getLevel().random.nextInt(10) < resistance % 10) count++;
        if (count <= 0) return;
        seedStack.setCount(count);
        dropTracker.add(seedStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(Names.MOD_ID + "_tooltip.item." + this.tooltipName)
                .withStyle(ChatFormatting.GRAY));
    }
}
