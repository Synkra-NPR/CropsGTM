package com.synkra.cropsgtm.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.synkra.cropsgtm.api.BiomeClimate;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropRightClickHandler;
import com.synkra.cropsgtm.api.ICropSeedDropOverride;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.IHarvestDropModifier;
import com.synkra.cropsgtm.api.ISeedData;
import com.synkra.cropsgtm.api.ISeedStats;
import com.synkra.cropsgtm.api.IWorldGrowthRequirement;
import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.CropGrowthMath;
import com.synkra.cropsgtm.farming.PotencyData;
import com.synkra.cropsgtm.farming.SeedData;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.farming.registries.FertilizerRegistry;
import com.synkra.cropsgtm.init.CropsGTMBlocks;
import com.synkra.cropsgtm.init.CropsGTMItems;
import com.synkra.cropsgtm.reference.Constants;
import com.synkra.cropsgtm.reference.Names;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * 作物架方块背后的方块实体，负责持有植物并处理全部种植逻辑。
 */
public class BlockEntityCropSticks extends BlockEntity implements ICropStickTile {

    /** 杂草作物的 id，由内置数据包定义。 */
    public static final String WEED_CROP_ID = Names.MOD_ID + ":weed";

    // 种子状态
    private ISeedData seed = null;
    private boolean crossCrop = false;

    // 作物状态
    private boolean sick = false;
    private int growthProgress = 0;
    private int waterStorage = 0;
    private int weedExStorage = 0;
    private int fertilizerStorage = 0;

    // 渲染与刻更新
    private int ticker = -1;
    private int spriteIndex = 0;
    private boolean dirty = true;

    /**
     * 最近一次检查中未通过的生长条件。该列表被复用而非重建，使长期未通过检查的作物不会在每次生长刻分配内存。
     */
    private final List<IGrowthRequirement> failedChecks = new ArrayList<>(4);

    public BlockEntityCropSticks(BlockPos pos, BlockState state) {
        super(CropsGTMBlocks.cropSticksTE(), pos, state);
    }

    // ---- 状态检查 ----

    @Override
    public boolean hasCrop() {
        return this.seed != null;
    }

    @Override
    public boolean hasWeed() {
        return this.hasCrop() && this.seed.getCrop().isWeed();
    }

    @Override
    public boolean isSick() {
        return this.hasCrop() && !this.hasWeed() && this.sick;
    }

    @Override
    public boolean isCrossCrop() {
        return this.crossCrop;
    }

    @Override
    public boolean canPlantSeed() {
        return this.seed == null && !this.crossCrop;
    }

    @Override
    public boolean isMature() {
        return this.hasCrop() && this.seed.getCrop().getGrowthDuration() <= this.growthProgress;
    }

    @Override
    public @Nullable ISeedData getSeed() {
        return this.seed;
    }

    @Override
    public int getGrowthProgress() {
        return this.seed != null ? this.growthProgress : 0;
    }

    @Override
    public void setGrowthProgress(int progress) {
        if (!this.hasCrop()) return;
        this.growthProgress = Math.max(0, Math.min(this.seed.getCrop().getGrowthDuration(), progress));
        this.dirty = true;
    }

    @Override
    public float getGrowthPercent() {
        return this.hasCrop() ?
                Math.max(0.0f,
                        Math.min(1.0f, (float) this.growthProgress / (float) this.seed.getCrop().getGrowthDuration())) :
                0.0f;
    }

    @Override
    public boolean canGrow() {
        if (!this.hasCrop() || this.sick) {
            this.failedChecks.clear();
            return false;
        }
        // 杂草不关心周围环境。
        if (this.hasWeed()) {
            this.failedChecks.clear();
            return true;
        }
        // 成熟作物不再生长，但我们仍希望偶尔检查一次需求，以便
        // 一旦不再满足，收获就会被阻止。
        if (this.isMature()) {
            if (this.getLevel() != null && this.getLevel().random.nextBoolean()) this.areGrowthRequirementsMet();
            return false;
        }
        return this.areGrowthRequirementsMet();
    }

    /**
     * @return 作物能在当前位置继续生长时返回 true；只阻止收获的条件会被记录，但不会中断生长。
     */
    @Override
    public boolean areGrowthRequirementsMet() {
        if (this.seed == null) {
            this.failedChecks.clear();
            return true;
        }
        List<IGrowthRequirement> reqs = this.seed.getCrop().getGrowthRequirements();
        if (reqs.isEmpty()) {
            this.failedChecks.clear();
            return true;
        }
        this.failedChecks.clear();
        boolean success = true;
        for (IGrowthRequirement req : reqs) {
            if (!(req instanceof IWorldGrowthRequirement worldRequirement)) continue;
            if (this.getLevel() == null || this.getBlockPos() == null) break;
            if (worldRequirement.canGrow(this.getLevel(), this.getBlockPos(), this)) continue;
            success &= worldRequirement.onlyPreventsHarvest();
            this.failedChecks.add(req);
        }
        return success;
    }

    /**
     * @return 指定作物能在该作物架所在位置生长时返回 true。
     */
    public boolean wouldCropBeAbleToGrow(ICropCard crop) {
        if (this.getLevel() == null) return true;
        for (IGrowthRequirement req : crop.getGrowthRequirements()) {
            if (!(req instanceof IWorldGrowthRequirement worldRequirement)) continue;
            if (!worldRequirement.canGrow(this.getLevel(), this.getBlockPos(), this) &&
                    !worldRequirement.onlyPreventsHarvest()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isValidSoilForCrop(ICropCard crop) {
        if (this.getLevel() == null) return false;
        return crop.getSoilTypes().isRegistered(this.getLevel().getBlockState(this.getBlockPos().below()));
    }

    /**
     * @return 按作物抗性属性抽取成功时返回 true。
     */
    public boolean passesResistanceCheck() {
        if (this.seed == null || this.getLevel() == null) return false;
        return this.seed.getStats().getResistance() > this.getLevel().random.nextInt(CropsGTMConfig.maxResistanceStat);
    }

    /**
     * 把已种下的种子标记为已分析，使植物透镜能读出其属性。
     */
    public void analyzeSeed() {
        if (this.seed == null) return;
        this.seed.setAnalyzed(true);
        this.dirty = true;
    }

    /**
     * 收集植物透镜输出到聊天栏的内容，顺序为：已种作物、属性、土壤、问题。
     */
    public void getPlantLensStatus(List<Component> information) {
        Component soilLine = Component.translatable(Names.MOD_ID + "_tooltip.plantLens.soil", this.fertilizerStorage,
                this.waterStorage, this.weedExStorage);
        if (!this.hasCrop()) {
            information.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.empty"));
            information.add(soilLine);
            return;
        }
        if (this.hasWeed()) {
            information.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.weeds"));
            information.add(soilLine);
            return;
        }

        ICropCard crop = this.seed.getCrop();
        information.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.seed",
                Component.translatable(crop.getUnlocalizedName()).withStyle(rarityColor(crop.getRarity()))));

        if (this.seed.getStats().isAnalyzed()) {
            ISeedStats stats = this.seed.getStats();
            information.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.stats", (int) stats.getGrowth(),
                    (int) stats.getGain(), (int) stats.getResistance()));
        }

        information.add(soilLine);

        if (this.isSick()) {
            information.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.isSick"));
        }

        if (!this.failedChecks.isEmpty()) {
            for (IGrowthRequirement requirement : this.failedChecks) {
                information.add(requirement.getDescription().copy().withStyle(ChatFormatting.RED));
            }
        }
    }

    /**
     * @return 上次检查中未通过的生长条件，全部通过时为空。
     */
    public List<IGrowthRequirement> getFailedChecks() {
        return this.failedChecks;
    }

    private static ChatFormatting rarityColor(Rarity rarity) {
        return switch (rarity) {
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case RARE -> ChatFormatting.AQUA;
            case UNCOMMON -> ChatFormatting.YELLOW;
            default -> ChatFormatting.WHITE;
        };
    }

    // ---- 资源存储 ----

    @Override
    public int getFertilizerStorage() {
        return this.fertilizerStorage;
    }

    @Override
    public boolean addFertilizer(int amount, int threshold, int maxStorage, boolean simulate) {
        if (this.fertilizerStorage > threshold) return false;
        if (!simulate) {
            this.fertilizerStorage = Math.min(maxStorage, this.fertilizerStorage + amount);
            this.dirty = true;
        }
        return true;
    }

    @Override
    public int getWaterStorage() {
        return this.waterStorage;
    }

    @Override
    public boolean addWater(int amount, int threshold, int maxStorage, boolean simulate) {
        if (this.waterStorage > threshold) return false;
        if (!simulate) {
            this.waterStorage = Math.min(maxStorage, this.waterStorage + amount);
            this.dirty = true;
        }
        return true;
    }

    @Override
    public int getWeedExStorage() {
        return this.weedExStorage;
    }

    @Override
    public boolean addWeedEx(int amount, int threshold, int maxStorage, boolean simulate) {
        if (this.weedExStorage > threshold) return false;
        if (!simulate) {
            this.weedExStorage = Math.min(maxStorage, this.weedExStorage + amount);
            this.dirty = true;
        }
        return true;
    }

    // ---- 播种 ----

    @Override
    public void plantSeed(ISeedData seedData) {
        this.clear();
        this.seed = seedData;
        this.dirty = true;
    }

    @Override
    public void clear() {
        if (this.seed != null && this.getLevel() != null && !this.getLevel().isClientSide) {
            this.seed.getCrop().onRemoved(this);
        }
        this.seed = null;
        this.crossCrop = false;
        this.sick = false;
        this.failedChecks.clear();
        this.growthProgress = 0;
        this.spriteIndex = 0;
        this.dirty = true;
    }

    @Override
    public void setCrossCrop(boolean status) {
        if (status != this.crossCrop) {
            this.clear();
            this.crossCrop = status;
            this.dirty = true;
        }
    }

    /**
     * 解析物品中的种子数据，通用种子与替代种子均支持。
     */
    public static @Nullable ISeedData getSeedData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ICropCard crop = CropRegistry.instance.get(stack);
        if (crop == null) return null;
        ISeedStats stats = CropRegistry.instance.isAlternateSeed(stack) ? SeedStats.DEFAULT_ANALYZED :
                SeedStats.getStatsFromStack(stack);
        if (stats == null) stats = SeedStats.DEFAULT_ANALYZED;
        return new SeedData(crop, stats);
    }

    // ---- 收获 ----

    @Override
    public boolean canHarvest() {
        if (this.getLevel() == null || this.getLevel().isClientSide) return false;
        if (!this.hasCrop() || this.hasWeed() || !this.isMature()) return false;
        if (!this.areGrowthRequirementsMet()) return false;
        // 只阻止收获的需求（例如仅限机器的作物）会作为未通过检查报告
        return this.failedChecks.isEmpty();
    }

    @Override
    public @Nullable List<ItemStack> harvest(double dropMultiplier) {
        if (!this.canHarvest()) return null;
        ICropCard crop = this.seed.getCrop();
        crop.onHarvest(this);
        this.growthProgress = 0;
        this.spriteIndex = 0;
        this.dirty = true;

        double avgDropRounds = CropGrowthMath.getAvgDropRounds(crop, this.seed.getStats().getGain()) * dropMultiplier *
                CropsGTMConfig.cropStickOutputMultiplier;
        int dropCount = (int) avgDropRounds;
        if (this.getLevel().random.nextDouble() <= avgDropRounds % 1.0d) {
            dropCount++;
        }
        if (dropCount <= 0) return null;

        List<ItemStack> drops = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> entry : crop.getDropTable().entrySet()) {
            int count = 0;
            int gainBonus = 0;
            for (int i = 0; i < dropCount; i++) {
                if (this.getLevel().random.nextInt(10000) < entry.getValue()) {
                    if (this.getLevel().random.nextInt(100) <= this.seed.getStats().getGain()) {
                        gainBonus++;
                    }
                    count++;
                }
            }
            if (count <= 0) continue;
            int remaining = count * entry.getKey().getCount() + gainBonus;
            while (remaining > 0) {
                ItemStack stack = entry.getKey().copy();
                int toDrop = Math.min(stack.getMaxStackSize(), remaining);
                remaining -= toDrop;
                stack.setCount(toDrop);
                drops.add(stack);
            }
        }
        return drops;
    }

    /**
     * 收获或移除作物，并把掉落物生成到世界中。
     * 
     * @return 发生了任何变化时返回 true。
     */
    public boolean doPlayerHarvest(Player player, boolean isRemovingCrop) {
        if (!this.hasCrop() || this.hasWeed()) return false;
        boolean willHarvest = this.canHarvest();
        if (!willHarvest && !isRemovingCrop) return false;

        ItemStack heldItem = player.getMainHandItem();
        List<ItemStack> drops = new ArrayList<>();
        if (willHarvest) {
            List<ItemStack> harvestDrops = this.harvest(1.0d);
            if (harvestDrops != null) drops.addAll(harvestDrops);
            // 让手持物品增加或改变收获掉落
            if (heldItem.getItem() instanceof IHarvestDropModifier modifier) {
                modifier.modifyCropDrops(player, heldItem, drops, this);
            }
        }
        if (this.hasCrop() && !this.hasWeed() && isRemovingCrop) {
            boolean skipSeedDrop = false;
            if (heldItem.getItem() instanceof ICropSeedDropOverride override) {
                skipSeedDrop = override.overrideSeedDrop(player, heldItem, drops, this, willHarvest);
            }
            if (!skipSeedDrop) {
                ItemStack seedDrop = this.getSeedDrop();
                if (seedDrop != null) drops.add(seedDrop);
            }
        }
        for (ItemStack drop : drops) {
            this.dropItem(drop);
        }
        if (isRemovingCrop) {
            this.clear();
            this.playSound(SoundEvents.CROP_BREAK, 1.0f);
        } else {
            this.playSound(SoundEvents.CROP_BREAK, 0.5f);
        }
        return true;
    }

    @Override
    public @Nullable ItemStack getSeedStack() {
        if (!this.hasCrop()) return null;
        return this.seed.getCropSeeds(1);
    }

    @Override
    public @Nullable ItemStack getSeedDrop() {
        if (this.hasCrop() && !this.hasWeed() && this.passesResistanceCheck()) {
            return this.getSeedStack();
        }
        return null;
    }

    /**
     * @return 破坏该作物架时应掉落的全部物品。
     */
    public List<ItemStack> getBreakItems() {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(CropsGTMItems.cropSticksItem(), this.crossCrop ? 2 : 1));
        if (this.hasCrop()) {
            ItemStack seedDrop = this.getSeedDrop();
            List<ItemStack> harvestDrops = this.harvest(1.0d);
            if (harvestDrops != null) drops.addAll(harvestDrops);
            if (seedDrop != null) drops.add(seedDrop);
        }
        return drops;
    }

    /**
     * 移除该作物架及其承载的一切。
     */
    public void breakCropStick(boolean isTrampling) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) return;
        List<ItemStack> drops = this.getBreakItems();
        if (this.hasCrop()) {
            this.clear();
            if (isTrampling && level.getBlockState(this.getBlockPos().below()).is(Blocks.FARMLAND)) {
                level.setBlock(this.getBlockPos().below(), Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        level.setBlock(this.getBlockPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        for (ItemStack drop : drops) {
            this.dropItem(drop);
        }
    }

    @Override
    public void dropItem(ItemStack stack) {
        if (this.getLevel() == null || this.getLevel().isClientSide || stack.isEmpty()) return;
        Block.popResource(this.getLevel(), this.getBlockPos(), stack);
    }

    // ---- 生长速率计算 ----

    @Override
    public int calcGrowthRate() {
        if (!this.hasCrop()) return 0;
        return CropGrowthMath.getGrowthRate(this.getNutrientScore(), this.seed.getCrop().getTier(),
                this.seed.getStats().getGrowth());
    }

    /**
     * @return 已种植作物当前获得的养分量，按来源拆分；未种植任何作物时返回 {@code null}。
     */
    public @Nullable CropGrowthMath.NutrientBreakdown getNutrientBreakdown() {
        Level level = this.getLevel();
        if (level == null || !this.hasCrop()) return null;
        Holder<Biome> biome = level.getBiome(this.getBlockPos());
        int likedBiomes = 0;
        Set<TagKey<Biome>> liked = this.seed.getCrop().getLikedBiomeTags();
        for (TagKey<Biome> tag : liked) {
            if (BiomeClimate.matches(biome, tag)) likedBiomes++;
        }
        boolean canSeeSky = level.canSeeSky(this.getBlockPos().above());
        float humidity = biome.value().getModifiedClimateSettings().downfall();
        return CropGrowthMath.NutrientBreakdown.of(likedBiomes, humidity, canSeeSky, this.waterStorage,
                this.fertilizerStorage);
    }

    public int getNutrientScore() {
        CropGrowthMath.NutrientBreakdown breakdown = this.getNutrientBreakdown();
        return breakdown == null ? 0 : breakdown.total();
    }

    /**
     * 收集便携式扫描仪输出到聊天栏的养分读数，包含当前养分分与各来源的贡献。
     */
    public void getNutrientStatus(List<Component> information) {
        CropGrowthMath.NutrientBreakdown breakdown = this.getNutrientBreakdown();
        if (breakdown == null) return;
        information.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.nutrient", breakdown.total(),
                CropGrowthMath.NutrientBreakdown.maxTotal()));
        String climateKey = breakdown.fromLikedBiomes() ? Names.MOD_ID + "_tooltip.plantLens.nutrient.likedBiomes" :
                Names.MOD_ID + "_tooltip.plantLens.nutrient.humidity";
        information.add(Component.translatable(Names.MOD_ID + "_tooltip.plantLens.nutrientSources", breakdown.base(),
                breakdown.water(), breakdown.fertilizer(), breakdown.sky(),
                Component.translatable(climateKey, breakdown.climate())));
    }

    // ---- 杂草与病害处理 ----

    @Override
    public void spawnWeed() {
        if (this.hasCrop() && !this.hasWeed() && (!Constants.WEEDS_WIPE_PLANTS ||
                this.seed.getStats().getResistance() >= CropsGTMConfig.maxResistanceStat)) {
            return;
        }
        if (this.weedExStorage > 0) {
            this.weedExStorage = Math.max(this.weedExStorage - 5, 0);
            this.dirty = true;
            return;
        }
        ICropCard weed = CropRegistry.instance.get(WEED_CROP_ID);
        if (weed == null) return;
        this.plantSeed(new SeedData(weed, SeedStats.DEFAULT_ANALYZED));
    }

    public void spreadWeed() {
        Level level = this.getLevel();
        if (level == null || !this.hasCrop()) return;
        if (!this.hasWeed() && this.seed.getStats().getResistance() >= this.seed.getStats().getGrowth()) {
            if (this.weedExStorage > 0) {
                this.weedExStorage = Math.max(this.weedExStorage - 5, 0);
                return;
            }
        }
        BlockPos target = this.getBlockPos();
        switch (level.random.nextInt(4)) {
            case 0 -> target = target.east();
            case 1 -> target = target.west();
            case 2 -> target = target.south();
            default -> target = target.north();
        }
        if (level.getBlockEntity(target) instanceof ICropStickTile neighbour) {
            if (neighbour.hasCrop()) {
                if (!neighbour.hasWeed() && !neighbour.isSick()) neighbour.transferDisease();
            } else {
                neighbour.spawnWeed();
            }
        } else if (level.isEmptyBlock(target)) {
            BlockPos below = target.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(Blocks.GRASS_BLOCK) || belowState.is(Blocks.DIRT) || belowState.is(Blocks.FARMLAND)) {
                level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(target, Blocks.TALL_GRASS.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void transferDisease() {
        if (CropsGTMConfig.disableDisease) return;
        if (!this.hasCrop() || this.sick || this.hasWeed()) return;
        if (this.passesResistanceCheck()) return;
        if (this.weedExStorage > 0) {
            this.weedExStorage = Math.max(this.weedExStorage - 2, 0);
            this.dirty = true;
            return;
        }
        this.sick = true;
        this.dirty = true;
    }

    @Override
    public boolean cureDisease() {
        if (!this.hasCrop() || !this.sick) return false;
        this.sick = false;
        this.dirty = true;
        return true;
    }

    public void spreadDisease() {
        List<ICropStickTile> neighbours = this.getNeighbours();
        neighbours.removeIf(neighbour -> neighbour == null || !neighbour.hasCrop());
        if (neighbours.isEmpty()) return;
        if (this.getLevel() == null) return;
        neighbours.get(this.getLevel().random.nextInt(neighbours.size())).transferDisease();
    }

    @Override
    public boolean canCross() {
        return this.hasCrop() && !this.hasWeed() && this.seed.getCrop().getCrossingThreshold() >= 0.0f &&
                this.getGrowthPercent() >= this.seed.getCrop().getCrossingThreshold();
    }

    @Override
    public boolean canBreed() {
        return this.hasCrop() && !this.hasWeed() && this.seed.getCrop().getBreedingThreshold() >= 0.0f &&
                this.getGrowthPercent() >= this.seed.getCrop().getBreedingThreshold();
    }

    // ---- 相邻检测 ----

    @Override
    public List<ICropStickTile> getNeighbours() {
        List<ICropStickTile> neighbours = new ArrayList<>(4);
        Level level = this.getLevel();
        if (level == null) return neighbours;
        BlockPos pos = this.getBlockPos();
        addNeighbour(neighbours, pos.north());
        addNeighbour(neighbours, pos.south());
        addNeighbour(neighbours, pos.east());
        addNeighbour(neighbours, pos.west());
        return neighbours;
    }

    private void addNeighbour(List<ICropStickTile> neighbours, BlockPos pos) {
        Level level = this.getLevel();
        if (level == null) return;
        if (level.getBlockEntity(pos) instanceof ICropStickTile tile) neighbours.add(tile);
    }

    // ---- 刻更新 ----

    public void tick() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) return;
        if (this.ticker < 0) this.ticker = level.random.nextInt(Constants.TICK_RATE);
        this.ticker = (this.ticker + 1) % Constants.TICK_RATE;
        if (this.ticker == 0) this.onGrowthTick();
        if (this.dirty) {
            this.dirty = false;
            this.setChanged();
            level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void onGrowthTick() {
        Level level = this.getLevel();
        if (level == null) return;

        if (level.isRainingAt(this.getBlockPos().above())) {
            // 取 101，使单点也能被周期消耗且不丢失完整加成。
            this.addWater(11, 100, 101, false);
        }

        if (this.hasCrop()) {
            boolean canGrow = this.canGrow();
            if (this.sick) {
                this.spreadDisease();
                canGrow = false;
            }
            if (canGrow) this.doGrowth();
            if (!CropsGTMConfig.disableWeeds && this.seed.getCrop().spreadsWeeds(this) &&
                    level.random.nextInt(Constants.WEED_SPREAD_CHANCE) - this.seed.getStats().getGrowth() <= 2) {
                this.spreadWeed();
            }
        } else {
            if (!this.crossCrop || !CropBreeding.attempt(this)) {
                if (!CropsGTMConfig.disableWeeds && level.random.nextInt(Constants.WEED_SPAWN_CHANCE) == 0) {
                    this.spawnWeed();
                }
            }
        }

        if (this.fertilizerStorage > 0) this.fertilizerStorage--;
        if (this.waterStorage > 0) this.waterStorage--;
    }

    private void doGrowth() {
        Level level = this.getLevel();
        if (level == null) return;
        ICropCard crop = this.seed.getCrop();
        if (this.growthProgress < crop.getGrowthDuration()) {
            int growthRate = this.calcGrowthRate();
            if (growthRate <= 0) {
                if (!CropsGTMConfig.disableDisease && !this.passesResistanceCheck()) {
                    this.sick = true;
                    this.dirty = true;
                }
                return;
            }
            crop.onGrowthTick(this);
            this.growthProgress += (int) (growthRate * CropsGTMConfig.cropStickGrowthMultiplier);
            if (this.growthProgress > crop.getGrowthDuration()) this.growthProgress = crop.getGrowthDuration();
            this.dirty = true;
        }
        int newSpriteIndex = crop.getSpriteIndex(this);
        if (newSpriteIndex != this.spriteIndex) {
            this.spriteIndex = newSpriteIndex;
            this.dirty = true;
        }
    }

    // ---- 玩家交互 ----

    /**
     * 处理对该作物架的右键点击。
     * 
     * @return 交互已被消耗时返回 true。
     */
    public boolean onRightClick(Player player) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) return true;
        ItemStack heldItem = player.getMainHandItem();

        if (!heldItem.isEmpty() && heldItem.getItem() instanceof ICropRightClickHandler handler) {
            if (handler.onRightClick(this, player, heldItem)) return true;
        }

        // 优先尝试施肥
        PotencyData fertilizerData = FertilizerRegistry.instance.getPotency(heldItem);
        if (fertilizerData != null && fertilizerData.getUnitsConsumedPerApplication() <= heldItem.getCount()) {
            int maxStorage = fertilizerData.getMaxStorage(Constants.MANUAL_FERTILIZER_MAX_STORAGE);
            int potency = CropsGTMConfig.scalePotency(fertilizerData.getPotency(),
                    CropsGTMConfig.fertilizerPotencyMultiplier);
            int units = CropsGTMConfig.scaleConsumption(fertilizerData.getUnitsConsumedPerApplication(),
                    CropsGTMConfig.fertilizerConsumptionMultiplier);
            if (this.addFertilizer(potency,
                    Math.max(maxStorage - Constants.MANUAL_FERTILIZER_MIN_THRESHOLD, maxStorage - potency), maxStorage,
                    false)) {
                if (!player.isCreative()) {
                    heldItem.shrink(units);
                }
                this.playSound(SoundEvents.BONE_MEAL_USE, 1.0f);
                return true;
            }
        }

        // 尝试种下手中种子
        if (this.canPlantSeed()) {
            ISeedData seedData = getSeedData(heldItem);
            if (seedData != null) {
                if (!this.isValidSoilForCrop(seedData.getCrop())) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component
                                .translatable(getWrongSoilMessage(seedData.getCrop())));
                    }
                    return true;
                }
                this.plantSeed(seedData);
                if (!player.isCreative()) heldItem.shrink(1);
                this.playSound(SoundEvents.CROP_PLANTED, 1.0f);
                this.areGrowthRequirementsMet();
                return true;
            }
        }

        // 让作物处理交互
        if (this.seed != null && this.seed.getCrop().onRightClick(this, player)) {
            return true;
        }

        // 重置双作物架
        if (this.seed == null && this.crossCrop) {
            this.setCrossCrop(false);
            this.playSound(SoundEvents.WOOD_PLACE, 0.5f);
            if (!player.isCreative()) this.dropItem(new ItemStack(CropsGTMItems.cropSticksItem(), 1));
            return true;
        }

        // 成熟时收获
        if (this.isMature()) this.doPlayerHarvest(player, false);
        return true;
    }

    private static String getWrongSoilMessage(ICropCard crop) {
        return crop.getSoilTypes().getUnlocalizedWrongSoilMessage();
    }

    /**
     * 处理对该作物架的左键点击，即移除作物。
     */
    public boolean onLeftClick(Player player) {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) return true;
        if (this.hasCrop() && !this.hasWeed()) this.doPlayerHarvest(player, true);
        if (this.seed == null && this.crossCrop) {
            this.setCrossCrop(false);
            this.playSound(SoundEvents.WOOD_PLACE, 0.5f);
            if (!player.isCreative()) this.dropItem(new ItemStack(CropsGTMItems.cropSticksItem(), 1));
        }
        return true;
    }

    public void onEntityCollision(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return;
        if (this.canTrample() && (this.shouldTrampleFromRunning(living) || this.shouldTrampleFromFalling(living))) {
            this.breakCropStick(true);
            return;
        }
        if (this.hasCrop() && !entity.isShiftKeyDown()) {
            float damage = this.seed.getCrop().getEntityDamage();
            if (damage > 0.0f) {
                entity.hurt(this.getLevel().damageSources().cactus(), damage);
            }
            this.seed.getCrop().onEntityCollision(this, entity);
        }
    }

    private boolean canTrample() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide) return false;
        if (!this.hasCrop() || this.hasWeed()) return false;
        return this.seed.getStats().getResistance() < CropsGTMConfig.maxResistanceStat;
    }

    private boolean shouldTrampleFromRunning(LivingEntity entity) {
        Level level = this.getLevel();
        if (level == null || !entity.isSprinting()) return false;
        double maxRoll = 100.0d * Math.pow(0.95d, this.seed.getCrop().getTier());
        if ((double) level.random.nextInt((int) maxRoll) > 0) return false;
        return level.random.nextInt(CropsGTMConfig.maxResistanceStat) > this.seed.getStats().getResistance();
    }

    private boolean shouldTrampleFromFalling(LivingEntity entity) {
        Level level = this.getLevel();
        if (level == null || !entity.onGround() || entity.fallDistance <= Constants.MIN_FALL_DISTANCE_FOR_TRAMPLING) {
            return false;
        }
        return level.random.nextFloat() < entity.fallDistance - Constants.MIN_FALL_DISTANCE_FOR_TRAMPLING;
    }

    public void onNeighbourChange() {
        if (!this.hasCrop()) return;
        this.seed.getCrop().onNeighbourChange(this);
    }

    /**
     * 作物所在土壤不再有效时调用。
     * 
     * @return 作物架被破坏时返回 true。
     */
    public boolean onInvalidSoilDetected() {
        if (!this.hasCrop()) return false;
        this.breakCropStick(false);
        return true;
    }

    // ---- 音效 ----

    private void playSound(net.minecraft.sounds.SoundEvent event, float volume) {
        Level level = this.getLevel();
        if (level == null) return;
        level.playSound(null, this.getBlockPos(), event, SoundSource.BLOCKS, volume,
                0.8f + level.random.nextFloat() * 0.1f);
    }

    @Override
    public void playCropRemovalSound() {
        this.playSound(SoundEvents.CROP_BREAK, 1.0f);
    }

    // ---- NBT 读写 ----

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.getBoolean(Names.NBT.crossCrop)) {
            this.seed = null;
            this.crossCrop = true;
        } else {
            this.crossCrop = false;
            if (tag.contains(Names.NBT.crop, Tag.TAG_COMPOUND)) {
                this.seed = new SeedData(tag.getCompound(Names.NBT.crop));
                if (tag.contains(Names.NBT.progress, Tag.TAG_INT)) {
                    this.growthProgress = tag.getInt(Names.NBT.progress);
                }
                this.sick = tag.getBoolean(Names.NBT.sick);
            } else {
                this.seed = null;
                this.growthProgress = 0;
            }
        }
        this.waterStorage = tag.getInt(Names.NBT.water);
        this.fertilizerStorage = tag.getInt(Names.NBT.fertilizer);
        this.weedExStorage = tag.getInt(Names.NBT.weedEx);
        this.dirty = true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.crossCrop) {
            tag.putBoolean(Names.NBT.crossCrop, true);
        } else if (this.seed != null) {
            tag.put(Names.NBT.crop, this.seed.writeToNBT());
            tag.putInt(Names.NBT.progress, this.growthProgress);
            if (this.sick) tag.putBoolean(Names.NBT.sick, true);
        }
        tag.putInt(Names.NBT.water, this.waterStorage);
        tag.putInt(Names.NBT.fertilizer, this.fertilizerStorage);
        tag.putInt(Names.NBT.weedEx, this.weedExStorage);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
