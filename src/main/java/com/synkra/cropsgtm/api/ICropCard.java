package com.synkra.cropsgtm.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;

import com.synkra.cropsgtm.farming.SeedShapeDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作物的静态定义。
 */
public interface ICropCard {

    /**
     * @return 该作物的唯一 id，格式为“modId:cropName”。
     */
    String getId();

    /**
     * 设置用于繁殖配方排序的数字 id。由作物登记表分配。
     */
    void setNumericId(int numericId);

    int getNumericId();

    /**
     * @return 该作物的翻译键。
     */
    String getUnlocalizedName();

    String getCreator();

    /**
     * @return 该作物风味文本的翻译键，可能为 null。
     */
    String getFlavourText();

    int getPrimarySeedColor();

    int getSecondarySeedColor();

    SeedShapeDefinition getSeedShape();

    boolean isSeedEnchanted();

    int getTier();

    /**
     * @return 在工业农场中种植该作物所需的最低种子床等级；没有限制时返回低于最低种子床等级的值。
     */
    default int getMinSeedBedTier() {
        return -1;
    }

    /**
     * @return 繁殖该作物时作物繁殖机所需的电压等级。
     */
    default int getMachineBreedingRecipeTier() {
        // LV，繁殖机器从该等级开始
        return 1;
    }

    default Rarity getRarity() {
        int tier = this.getTier();
        if (tier > 12) return Rarity.EPIC;
        if (tier > 8) return Rarity.RARE;
        if (tier > 4) return Rarity.UNCOMMON;
        return Rarity.COMMON;
    }

    /**
     * @return 达到成熟所需的生长点数。
     */
    int getGrowthDuration();

    /**
     * @return 穿过该作物时受到的伤害，返回小于等于 0 的值表示无伤害。
     */
    float getEntityDamage();

    /**
     * @return 该作物生长更快的生物群系标签。
     */
    Set<TagKey<Biome>> getLikedBiomeTags();

    ISoilList getSoilTypes();

    List<IGrowthRequirement> getGrowthRequirements();

    /**
     * @return 该作物的掉落表，概率以万分之一为单位。
     */
    Map<ItemStack, Integer> getDropTable();

    List<ItemStack> getAlternateSeeds();

    boolean hideFromNEI();

    /**
     * @return 该作物是杂草时返回 true，杂草有特殊的生长与渲染规则。
     */
    default boolean isWeed() {
        return false;
    }

    /**
     * @return 掉落轮数的基础倍率。
     */
    float getBaseDropRounds();

    /**
     * @return 渲染该作物用到的全部贴图，按生长阶段排序。
     */
    List<ResourceLocation> getSprites();

    /**
     * @return 该作物架应显示的贴图下标。
     */
    int getSpriteIndex(ICropStickTile tile);

    PlantRenderShape getRenderShape();

    /**
     * @return 该作物蔓延到相邻作物架所需的生长百分比，负值表示禁止蔓延。
     */
    float getCrossingThreshold();

    /**
     * @return 该作物与相邻作物繁殖所需的生长百分比，负值表示禁止繁殖。
     */
    float getBreedingThreshold();

    int startsSpreadingWeedsAt();

    boolean spreadsWeeds(ICropStickTile tile);

    Component getDisplayName();

    /**
     * @return 该作物的全部突变都只能在作物繁殖机中繁殖时返回 true。
     */
    default boolean isMachineBreedingOnly() {
        return false;
    }

    ItemStack getSeedItem(ISeedStats stats);

    void onGrowthTick(ICropStickTile tile);

    boolean onRightClick(ICropStickTile tile, Player player);

    void onEntityCollision(ICropStickTile tile, Entity entity);

    void onHarvest(ICropStickTile tile);

    void onRemoved(ICropStickTile tile);

    void onNeighbourChange(ICropStickTile tile);
}
