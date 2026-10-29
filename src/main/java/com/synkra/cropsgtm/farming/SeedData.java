package com.synkra.cropsgtm.farming;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ISeedData;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.reference.Names;
import org.jetbrains.annotations.NotNull;

public class SeedData implements ISeedData {

    private final ICropCard crop;
    private final ISeedStats stats;
    private ItemStack stack;

    public SeedData(@NotNull ICropCard crop, @NotNull ISeedStats stats) {
        this(crop, stats, crop.getSeedItem(stats));
    }

    public SeedData(@NotNull ICropCard crop, @NotNull ISeedStats stats, ItemStack stack) {
        this.crop = crop;
        this.stats = stats;
        this.stack = stack;
    }

    public SeedData(CompoundTag tag) {
        ICropCard loaded = CropRegistry.instance
                .get(tag.contains(Names.NBT.crop, Tag.TAG_STRING) ? tag.getString(Names.NBT.crop) : null);
        if (loaded == null) loaded = CropRegistry.instance.getDefaultCrop();
        this.crop = loaded;
        this.stats = SeedStats.readFromNBT(tag);
        this.stack = loaded.getSeedItem(this.stats);
        this.stack.setCount(tag.contains(Names.NBT.amount, Tag.TAG_INT) ? tag.getInt(Names.NBT.amount) : 1);
    }

    @Override
    public @NotNull ICropCard getCrop() {
        return this.crop;
    }

    @Override
    public @NotNull ISeedStats getStats() {
        return this.stats;
    }

    @Override
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setAnalyzed(boolean analyzed) {
        this.stats.setAnalyzed(analyzed);
        ItemStack newStack = this.crop.getSeedItem(this.stats);
        newStack.setCount(this.stack.getCount());
        this.stack = newStack;
    }

    @Override
    public @NotNull CompoundTag writeToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(Names.NBT.crop, this.crop.getId());
        tag.putInt(Names.NBT.amount, this.stack.getCount());
        this.stats.writeToNBT(tag);
        return tag;
    }

    @Override
    public @NotNull ItemStack getCropSeeds(int amount) {
        ItemStack ret = this.crop.getSeedItem(this.stats);
        ret.setCount(amount);
        return ret;
    }
}
