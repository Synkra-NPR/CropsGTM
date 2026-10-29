package com.synkra.cropsgtm.farming.requirements;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.synkra.cropsgtm.CropsGTMPlatform;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.IWorldBreedingRequirement;
import com.synkra.cropsgtm.api.IWorldGrowthRequirement;
import com.synkra.cropsgtm.integration.gt.GtSubSoilMaterials;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 若土壤正下方不是指定类型的方块，则阻止作物生长。
 * <p>
 * 方块可直接匹配、通过方块标签匹配，或通过 GregTech 材质系统匹配（已注册材质的矿石、粗矿与存储方块）。
 * </p>
 */
public class SubSoilRequirement implements IWorldGrowthRequirement, IWorldBreedingRequirement {

    private static final Map<String, SubSoilRequirement> REGISTRATIONS = new HashMap<>();

    public static SubSoilRequirement get(String name) {
        return REGISTRATIONS.computeIfAbsent(name, SubSoilRequirement::new);
    }

    private final String name;
    /** GregTech 材质名称，仅在安装 GregTech 时解析。 */
    private final Set<String> materialIds = new HashSet<>();
    private final Set<Block> blocks = new HashSet<>();
    private final Set<TagKey<Block>> blockTags = new HashSet<>();
    /** 满足该需求的物品缓存，定义变化时重置。 */
    private List<ItemStack> validSubSoilItems;

    private SubSoilRequirement(String name) {
        this.name = name;
    }

    public SubSoilRequirement addMaterials(Collection<String> materialIds) {
        this.materialIds.addAll(materialIds);
        this.validSubSoilItems = null;
        return this;
    }

    public SubSoilRequirement addBlock(Block... blocks) {
        this.blocks.addAll(Arrays.asList(blocks));
        this.validSubSoilItems = null;
        return this;
    }

    public SubSoilRequirement addBlock(TagKey<Block>... tags) {
        this.blockTags.addAll(Arrays.asList(tags));
        this.validSubSoilItems = null;
        return this;
    }

    /** 清除所有方块、标签与材质，使定义可以从数据包重建。 */
    public SubSoilRequirement clear() {
        this.materialIds.clear();
        this.blocks.clear();
        this.blockTags.clear();
        this.validSubSoilItems = null;
        return this;
    }

    /** 清除所有已知的下层土壤定义，在加载数据包定义之前调用。 */
    public static void clearAll() {
        for (SubSoilRequirement requirement : REGISTRATIONS.values()) {
            requirement.clear();
        }
    }

    @Override
    public boolean canGrow(Level level, BlockPos pos, ICropStickTile tile) {
        BlockPos subSoilPos = pos.below(2);
        if (subSoilPos.getY() < level.getMinBuildHeight()) return false;
        return this.isValidSubSoil(level.getBlockState(subSoilPos));
    }

    @Override
    public boolean canBreed(Collection<ICropCard> parents, Level level, ICropStickTile tile) {
        return this.canGrow(level, tile.getBlockPos(), tile);
    }

    /**
     * @return 给定方块状态满足该下层土壤需求时返回 true。
     */
    public boolean isValidSubSoil(BlockState state) {
        if (state == null || state.isAir()) return false;
        if (this.blocks.contains(state.getBlock())) return true;
        for (TagKey<Block> tag : this.blockTags) {
            if (state.is(tag)) return true;
        }
        // 基于材质的方块只在安装 GregTech 时存在
        if (this.materialIds.isEmpty() || !CropsGTMPlatform.GREG_TECH_LOADED) return false;
        return GtSubSoilMaterials.matches(this.materialIds, state);
    }

    /**
     * @return 满足该下层土壤需求的全部物品，用作繁殖机与复制机的催化剂，并显示在配方查看器中。
     */
    public List<ItemStack> getValidSubSoilItems() {
        if (this.validSubSoilItems == null) {
            List<ItemStack> items = new ArrayList<>();
            for (Block block : this.blocks) {
                items.add(new ItemStack(block));
            }
            for (TagKey<Block> tag : this.blockTags) {
                for (Block block : net.minecraftforge.registries.ForgeRegistries.BLOCKS) {
                    if (block.defaultBlockState().is(tag)) items.add(new ItemStack(block));
                }
            }
            if (!this.materialIds.isEmpty() && CropsGTMPlatform.GREG_TECH_LOADED) {
                items.addAll(GtSubSoilMaterials.getItems(this.materialIds));
            }
            this.validSubSoilItems = items;
        }
        return this.validSubSoilItems;
    }

    /**
     * @return 能作为该作物催化剂的全部物品；机器周期使用的催化剂就是其下层土壤需求里的物品。
     */
    public static List<ItemStack> collectCatalysts(ICropCard crop) {
        List<ItemStack> items = new ArrayList<>();
        for (IGrowthRequirement requirement : crop.getGrowthRequirements()) {
            if (requirement instanceof SubSoilRequirement subSoil) {
                items.addAll(subSoil.getValidSubSoilItems());
            }
        }
        return items;
    }

    @Override
    public Component getDescription() {
        return Component.translatable(Names.MOD_ID + "_growthReq.subSoil." + this.name).withStyle(ChatFormatting.GRAY);
    }
}
