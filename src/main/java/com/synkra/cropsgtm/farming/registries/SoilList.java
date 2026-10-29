package com.synkra.cropsgtm.farming.registries;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.synkra.cropsgtm.api.ISoilList;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SoilList implements ISoilList {

    private final String name;
    private final SoilRegistry registry;
    private final boolean isGlobal;
    private final Set<Block> validBlocks = new LinkedHashSet<>();
    private final Set<TagKey<Block>> validTags = new LinkedHashSet<>();

    SoilList(String name, SoilRegistry registry, boolean isGlobal) {
        this.name = name;
        this.registry = registry;
        this.isGlobal = isGlobal;
    }

    /** 清除该列表的所有方块与标签，在定义重载时使用。 */
    void clear() {
        this.validBlocks.clear();
        this.validTags.clear();
    }

    @Override
    public String getId() {
        return this.name;
    }

    @Override
    public boolean isRegistered(BlockState state) {
        if (state == null) return false;
        if (this.validBlocks.contains(state.getBlock())) return true;
        for (TagKey<Block> tag : this.validTags) {
            if (state.is(tag)) return true;
        }
        return false;
    }

    @Override
    public ISoilList register(TagKey<Block> tag) {
        if (tag == null) return this;
        // 任何位置注册的土壤对杂草与全局土壤列表同样有效。
        if (!this.isGlobal) this.registry.registerGlobalTag(tag);
        this.validTags.add(tag);
        return this;
    }

    @Override
    public ISoilList register(Block... blocks) {
        if (blocks == null) return this;
        if (!this.isGlobal) this.registry.registerGlobalBlocks(blocks);
        for (Block block : blocks) {
            if (block == null) continue;
            this.validBlocks.add(block);
        }
        return this;
    }

    @Override
    public List<ItemStack> getDisplayStacks() {
        List<ItemStack> stacks = new ArrayList<>(this.validBlocks.size());
        for (Block block : this.validBlocks) {
            stacks.add(new ItemStack(block.asItem()));
        }
        return stacks;
    }

    @Override
    public String getUnlocalizedItemTooltip() {
        return Names.MOD_ID + "_soilList." + this.name + ".tooltip";
    }

    @Override
    public String getUnlocalizedWrongSoilMessage() {
        return Names.MOD_ID + "_soilList." + this.name + ".wrongSoil";
    }
}
