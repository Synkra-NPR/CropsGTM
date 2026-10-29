package com.synkra.cropsgtm.farming;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;

import com.synkra.cropsgtm.api.CropsGTMSoilTypes;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.api.ISoilList;
import com.synkra.cropsgtm.api.PlantRenderShape;
import com.synkra.cropsgtm.farming.registries.SeedShapeRegistry;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * 作物卡片的实现。
 * <p>
 * 作物由 {@code data/cropsgtm/crops} 下的数据包文件定义，因此该类既是 {@link ICropCard} 的唯一实现，也是一个定义的持有者。定义未声明的内容都会回退到 {@link Builder}
 * 的默认值。
 * </p>
 */
public class CropCard implements ICropCard {

    private final String id;
    private final String internalId;
    private int numericId = 0;
    private final int[] colors;

    private final String langKey;
    private final String creator;
    private final String flavourText;
    private final int tier;
    private final int growthDuration;
    private final int machineBreedingTier;
    private final int minSeedBedTier;
    private final float entityDamage;
    private final float dropRounds;
    private final float crossingThreshold;
    private final float breedingThreshold;
    private final int startsSpreadingWeedsAt;
    private final boolean hideFromNEI;
    private final SeedShapeDefinition seedShape;
    private final int seedModelData;
    private final boolean machineBreedingOnly;
    private final boolean seedEnchanted;
    private final PlantRenderShape renderShape;
    private final ISoilList soilTypes;
    private final List<ResourceLocation> sprites;
    private final boolean weed;
    private final List<String> mutationPools;

    protected final Map<ItemStack, Integer> dropTable = new HashMap<>();
    protected final List<IGrowthRequirement> growthRequirements = new ArrayList<>();
    protected final Set<TagKey<Biome>> likedBiomes = new HashSet<>();
    protected final List<ItemStack> alternateSeeds = new ArrayList<>();

    private CropCard(Builder builder) {
        this.id = builder.modId + ":" + builder.internalId;
        this.internalId = builder.internalId;
        this.colors = new int[] { builder.color1, builder.color2 };
        this.langKey = builder.langKey;
        this.creator = builder.creator;
        this.flavourText = builder.flavourText;
        this.tier = builder.tier;
        this.growthDuration = builder.growthDuration;
        this.machineBreedingTier = builder.machineBreedingTier;
        this.minSeedBedTier = builder.minSeedBedTier;
        this.entityDamage = builder.entityDamage;
        this.dropRounds = builder.dropRounds;
        this.crossingThreshold = builder.crossingThreshold;
        this.breedingThreshold = builder.breedingThreshold;
        this.startsSpreadingWeedsAt = builder.startsSpreadingWeedsAt;
        this.hideFromNEI = builder.hideFromNEI;
        this.seedShape = SeedShapeRegistry.instance.get(builder.seedShapeId);
        this.seedModelData = builder.seedModelData;
        this.machineBreedingOnly = builder.machineBreedingOnly;
        this.seedEnchanted = builder.seedEnchanted;
        this.renderShape = builder.renderShape;
        this.soilTypes = builder.soilTypes;
        this.sprites = List.copyOf(builder.sprites);
        this.weed = builder.weed;
        this.mutationPools = List.copyOf(builder.mutationPools);
    }

    public static Builder builder() {
        return new Builder();
    }

    // ---- 标识 ----

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setNumericId(int numericId) {
        if (this.numericId != 0) {
            throw new IllegalStateException("Attempted to re-initialize the numeric id of a crop.");
        }
        this.numericId = numericId;
    }

    @Override
    public int getNumericId() {
        return this.numericId;
    }

    // ---- 定义值 ----

    @Override
    public String getUnlocalizedName() {
        if (this.langKey != null && !this.langKey.isEmpty()) return this.langKey;
        return Names.MOD_ID + "_crops." + this.internalId;
    }

    @Override
    public String getCreator() {
        return this.creator;
    }

    @Override
    public @Nullable String getFlavourText() {
        return this.flavourText;
    }

    @Override
    public int getTier() {
        return this.tier;
    }

    @Override
    public int getGrowthDuration() {
        return this.growthDuration;
    }

    @Override
    public int getMachineBreedingRecipeTier() {
        return this.machineBreedingTier;
    }

    @Override
    public int getMinSeedBedTier() {
        return this.minSeedBedTier;
    }

    @Override
    public float getEntityDamage() {
        return this.entityDamage;
    }

    @Override
    public float getBaseDropRounds() {
        return this.dropRounds;
    }

    @Override
    public float getCrossingThreshold() {
        return this.crossingThreshold;
    }

    @Override
    public float getBreedingThreshold() {
        return this.breedingThreshold;
    }

    @Override
    public int startsSpreadingWeedsAt() {
        return this.startsSpreadingWeedsAt;
    }

    @Override
    public boolean hideFromNEI() {
        return this.hideFromNEI;
    }

    @Override
    public SeedShapeDefinition getSeedShape() {
        return this.seedShape;
    }

    /** @return 种子物品的自定义模型数据，{@code 0} 表示由种子形状决定。 */
    public int getSeedModelData() {
        return this.seedModelData;
    }

    @Override
    public boolean isMachineBreedingOnly() {
        return this.machineBreedingOnly;
    }

    @Override
    public boolean isSeedEnchanted() {
        return this.seedEnchanted;
    }

    @Override
    public PlantRenderShape getRenderShape() {
        return this.renderShape;
    }

    @Override
    public ISoilList getSoilTypes() {
        return this.soilTypes;
    }

    @Override
    public boolean isWeed() {
        return this.weed;
    }

    public List<String> getMutationPools() {
        return this.mutationPools;
    }

    @Override
    public int getPrimarySeedColor() {
        return this.colors[0];
    }

    @Override
    public int getSecondarySeedColor() {
        return this.colors[1];
    }

    // ---- 内容 ----

    @Override
    public Set<TagKey<Biome>> getLikedBiomeTags() {
        return this.likedBiomes;
    }

    @Override
    public List<IGrowthRequirement> getGrowthRequirements() {
        return this.growthRequirements;
    }

    @Override
    public Map<ItemStack, Integer> getDropTable() {
        return this.dropTable;
    }

    @Override
    public List<ItemStack> getAlternateSeeds() {
        return this.alternateSeeds;
    }

    @Override
    public List<ResourceLocation> getSprites() {
        return this.sprites;
    }

    @Override
    public int getSpriteIndex(ICropStickTile tile) {
        int max = this.sprites.size() - 1;
        if (max < 0) return 0;
        if (tile.isMature()) return max;
        float percent = tile.getGrowthPercent();
        // 三段贴图：40% 之前显示幼苗，40% 之后显示中间阶段，成熟时才切到最后一张
        if (max == 2) return percent < 0.4f ? 0 : 1;
        int index = (int) Math.floor(percent * max);
        return Math.max(0, Math.min(max, index));
    }

    // ---- 行为 ----

    @Override
    public boolean spreadsWeeds(ICropStickTile tile) {
        // 杂草长到至少一半时会开始蔓延
        if (this.weed) return tile.getGrowthPercent() >= 0.5f;
        if (!tile.isMature()) return false;
        ISeedStats stats = tile.getSeed().getStats();
        if (stats.getResistance() >= stats.getGrowth()) return false;
        return stats.getGrowth() > this.startsSpreadingWeedsAt;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getUnlocalizedName());
    }

    @Override
    public ItemStack getSeedItem(ISeedStats stats) {
        ItemStack seed = new ItemStack(CropsGTMItems.genericSeed());
        seed.getOrCreateTag().putString(Names.NBT.crop, this.id);
        // 形状决定物品模型显示哪个种子剪影，作物可以声明自己的模型
        int modelData = this.seedModelData;
        seed.getOrCreateTag().putInt("CustomModelData", modelData > 0 ? modelData : this.seedShape.modelData());
        if (stats != null) stats.writeToNBT(seed.getOrCreateTag());
        return seed;
    }

    // ---- 构建辅助 ----

    public CropCard addLikedBiomes(TagKey<Biome>... tags) {
        this.likedBiomes.addAll(Arrays.asList(tags));
        return this;
    }

    public CropCard addDrop(ItemStack stack, int chance) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Attempted to add an empty drop to " + this.id);
        }
        this.dropTable.put(stack, chance);
        return this;
    }

    public CropCard addAlternateSeed(ItemStack alternateSeed) {
        if (alternateSeed == null || alternateSeed.isEmpty()) {
            throw new IllegalArgumentException("Attempted to add an empty alternate seed to " + this.id);
        }
        ItemStack copy = alternateSeed.copy();
        copy.setCount(1);
        this.alternateSeeds.add(copy);
        return this;
    }

    public CropCard addGrowthRequirement(IGrowthRequirement requirement) {
        this.growthRequirements.add(requirement);
        return this;
    }

    // ---- 事件 ----

    @Override
    public void onGrowthTick(ICropStickTile tile) {}

    @Override
    public boolean onRightClick(ICropStickTile tile, Player player) {
        return false;
    }

    @Override
    public void onEntityCollision(ICropStickTile tile, Entity entity) {}

    @Override
    public void onHarvest(ICropStickTile tile) {}

    @Override
    public void onRemoved(ICropStickTile tile) {}

    @Override
    public void onNeighbourChange(ICropStickTile tile) {}

    /** 由一条数据包定义构建作物卡片。 */
    public static final class Builder {

        private String modId = Names.MOD_ID;
        private String internalId = "";
        private int color1 = 0xFFFFFF;
        private int color2 = 0xFFFFFF;

        private String langKey;
        private String creator = "CropsGTM Team";
        private String flavourText;
        private int tier = 1;
        private int growthDuration = -1;
        private int machineBreedingTier = 1;
        private int minSeedBedTier = -1;
        private float entityDamage = -1.0f;
        private float dropRounds = -1.0f;
        private float crossingThreshold = 0.8f;
        private float breedingThreshold = 0.8f;
        private int startsSpreadingWeedsAt = Constants.SPREAD_WEED_WHEN_GROWTH_AT_DEFAULT;
        private boolean hideFromNEI = false;
        private String seedShapeId = "vanilla";
        private int seedModelData = 0;
        private boolean machineBreedingOnly = false;
        private boolean seedEnchanted = false;
        private PlantRenderShape renderShape = PlantRenderShape.HASH;
        private ISoilList soilTypes;
        private List<ResourceLocation> sprites = List.of();
        private boolean weed = false;
        private List<String> mutationPools = List.of();

        private Builder() {}

        public Builder id(String modId, String internalId) {
            this.modId = modId;
            this.internalId = internalId;
            return this;
        }

        public Builder colors(int color1, int color2) {
            this.color1 = color1;
            this.color2 = color2;
            return this;
        }

        public Builder langKey(@Nullable String langKey) {
            this.langKey = langKey;
            return this;
        }

        public Builder creator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder flavourText(@Nullable String flavourText) {
            this.flavourText = flavourText;
            return this;
        }

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder growthDuration(int growthDuration) {
            this.growthDuration = growthDuration;
            return this;
        }

        public Builder machineBreedingTier(int machineBreedingTier) {
            this.machineBreedingTier = machineBreedingTier;
            return this;
        }

        public Builder minSeedBedTier(int minSeedBedTier) {
            this.minSeedBedTier = minSeedBedTier;
            return this;
        }

        public Builder entityDamage(float entityDamage) {
            this.entityDamage = entityDamage;
            return this;
        }

        public Builder dropRounds(float dropRounds) {
            this.dropRounds = dropRounds;
            return this;
        }

        public Builder crossingThreshold(float crossingThreshold) {
            this.crossingThreshold = crossingThreshold;
            return this;
        }

        public Builder breedingThreshold(float breedingThreshold) {
            this.breedingThreshold = breedingThreshold;
            return this;
        }

        public Builder startsSpreadingWeedsAt(int startsSpreadingWeedsAt) {
            this.startsSpreadingWeedsAt = startsSpreadingWeedsAt;
            return this;
        }

        public Builder hideFromNEI(boolean hideFromNEI) {
            this.hideFromNEI = hideFromNEI;
            return this;
        }

        public Builder seedShape(String seedShapeId) {
            this.seedShapeId = seedShapeId;
            return this;
        }

        /** 覆盖种子的自定义模型数据，用于让资源包添加种子模型。 */
        public Builder seedModelData(int seedModelData) {
            this.seedModelData = seedModelData;
            return this;
        }

        /** 使该作物的所有突变只能在作物繁殖机中进行。 */
        public Builder machineBreedingOnly(boolean machineBreedingOnly) {
            this.machineBreedingOnly = machineBreedingOnly;
            return this;
        }

        public Builder seedEnchanted(boolean seedEnchanted) {
            this.seedEnchanted = seedEnchanted;
            return this;
        }

        public Builder renderShape(PlantRenderShape renderShape) {
            this.renderShape = renderShape;
            return this;
        }

        public Builder soilTypes(ISoilList soilTypes) {
            this.soilTypes = soilTypes;
            return this;
        }

        public Builder sprites(List<ResourceLocation> sprites) {
            this.sprites = sprites;
            return this;
        }

        public Builder weed(boolean weed) {
            this.weed = weed;
            return this;
        }

        public Builder mutationPools(List<String> mutationPools) {
            this.mutationPools = mutationPools;
            return this;
        }

        public CropCard build() {
            if (this.growthDuration < 0) this.growthDuration = 600 * this.tier;
            if (this.dropRounds < 0.0f) this.dropRounds = (float) Math.pow(0.95, this.tier);
            if (this.soilTypes == null) {
                this.soilTypes = CropsGTMSoilTypes.getOrDefault(CropsGTMSoilTypes.DEFAULT);
            }
            return new CropCard(this);
        }
    }
}
