package com.synkra.cropsgtm.farming.registries;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.synkra.cropsgtm.api.ISoilList;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 由其他土壤列表组合而成的土壤列表。
 */
public class CompoundSoilList implements ISoilList {

    private final String name;
    private final List<ISoilList> lists = new ArrayList<>();

    public CompoundSoilList(String name, ISoilList... lists) {
        this.name = name;
        this.lists.addAll(Arrays.asList(lists));
    }

    @Override
    public String getId() {
        return this.name;
    }

    @Override
    public boolean isRegistered(BlockState state) {
        for (ISoilList list : this.lists) {
            if (list.isRegistered(state)) return true;
        }
        return false;
    }

    @Override
    public ISoilList register(TagKey<Block> tag) {
        for (ISoilList list : this.lists) {
            list.register(tag);
        }
        return this;
    }

    @Override
    public ISoilList register(Block... blocks) {
        for (ISoilList list : this.lists) {
            list.register(blocks);
        }
        return this;
    }

    @Override
    public List<ItemStack> getDisplayStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (ISoilList list : this.lists) {
            for (ItemStack stack : list.getDisplayStacks()) {
                boolean duplicate = false;
                for (ItemStack existing : stacks) {
                    if (ItemStack.isSameItem(existing, stack)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) stacks.add(stack);
            }
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
