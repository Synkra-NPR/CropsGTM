package com.synkra.cropsgtm.farming.registries;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.CropsGTM;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.items.ItemGenericSeed;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class CropRegistry {

    public static final CropRegistry instance = new CropRegistry();

    // 从 1 开始，使 0 可被繁殖检查视为"无亲本"。
    private int cropIdCounter = 1;

    /** 每次重载都会自增，使缓存的作物数据能感知定义已被替换。 */
    private int generation = 0;

    private final Map<String, ICropCard> cropRegistry = new HashMap<>();
    private final List<ICropCard> registrationOrder = new ArrayList<>();
    private final Map<Item, ICropCard> alternateSeedList = new HashMap<>();

    private ICropCard defaultCrop = null;

    private CropRegistry() {}

    /** @return 按注册顺序排列的已注册作物，只读，防止登记表被修改。 */
    public List<ICropCard> getAllInRegistrationOrder() {
        return Collections.unmodifiableList(this.registrationOrder);
    }

    public @Nullable ICropCard get(String id) {
        if (id == null) return null;
        return this.cropRegistry.get(id);
    }

    public @Nullable ICropCard get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        // 通用种子把作物 id 存在自己的标签中。
        if (stack.getItem() instanceof ItemGenericSeed && stack.hasTag() &&
                stack.getTag().contains(Names.NBT.crop, 8)) {
            return this.cropRegistry.get(stack.getTag().getString(Names.NBT.crop));
        }
        return this.alternateSeedList.get(stack.getItem());
    }

    public boolean isAlternateSeed(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() instanceof ItemGenericSeed) return false;
        return this.alternateSeedList.containsKey(stack.getItem());
    }

    public void register(ICropCard crop) {
        if (this.cropRegistry.containsKey(crop.getId())) {
            CropsGTM.LOGGER.warn("Duplicate crop id, the definition {} is skipped.", crop.getId());
            return;
        }
        crop.setNumericId(this.cropIdCounter++);
        this.cropRegistry.put(crop.getId(), crop);
        this.registrationOrder.add(crop);
        if (this.defaultCrop == null) this.defaultCrop = crop;
        for (ItemStack alternateSeed : crop.getAlternateSeeds()) {
            if (!alternateSeed.isEmpty()) {
                this.alternateSeedList.put(alternateSeed.getItem(), crop);
            }
        }
    }

    /**
     * 用给定定义替换所有已注册作物，在数据包加载时使用。
     * <p>
     * 替代种子查找由 {@link #register(ICropCard)} 自身填充，定义加入后无需额外的重建步骤。
     * </p>
     */
    public void reload(Collection<? extends ICropCard> crops) {
        this.generation++;
        this.cropRegistry.clear();
        this.registrationOrder.clear();
        this.alternateSeedList.clear();
        this.defaultCrop = null;
        this.cropIdCounter = 1;
        for (ICropCard crop : crops) {
            this.register(crop);
        }
    }

    public ICropCard getDefaultCrop() {
        if (this.defaultCrop == null) {
            throw new IllegalStateException("No crops have been registered yet.");
        }
        return this.defaultCrop;
    }

    /** @return 每次重载都会变化的计数器，用于让基于定义的缓存失效。 */
    public int getGeneration() {
        return this.generation;
    }

    /**
     * @return 为指定作物按基础属性新建的、已完成分析的种子物品。
     */
    public ItemStack getDefaultSeedStack(ICropCard crop) {
        return crop.getSeedItem(SeedStats.DEFAULT_ANALYZED);
    }
}
