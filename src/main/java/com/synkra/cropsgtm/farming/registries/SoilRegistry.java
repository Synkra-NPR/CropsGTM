package com.synkra.cropsgtm.farming.registries;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.synkra.cropsgtm.api.ISoilList;

import java.util.HashMap;
import java.util.Map;

public class SoilRegistry {

    public static final SoilRegistry instance = new SoilRegistry();

    private final Map<String, ISoilList> soilTypes = new HashMap<>();

    /** 包含所有已注册土壤的列表，杂草与通用检查使用。 */
    private final SoilList allSoils = new SoilList("global", this, true);

    private SoilRegistry() {}

    public SoilList allSoils() {
        return this.allSoils;
    }

    /**
     * 获取具名土壤列表，不存在时创建。
     */
    public ISoilList get(String type) {
        return this.soilTypes.computeIfAbsent(type, name -> new SoilList(name, this, false));
    }

    /** @return 指定 id 的土壤列表已定义时返回 true。 */
    public boolean contains(String type) {
        return this.soilTypes.containsKey(type);
    }

    /** 注册一个已准备好的土壤列表，用于由其他列表构建的复合列表。 */
    public void register(String type, ISoilList list) {
        this.soilTypes.put(type, list);
    }

    /** 清除所有定义，使土壤列表可以从数据包重建。 */
    public void clear() {
        this.allSoils.clear();
        this.soilTypes.clear();
    }

    public boolean isRegistered(BlockState state) {
        return this.allSoils.isRegistered(state);
    }

    void registerGlobalBlocks(Block... blocks) {
        this.allSoils.register(blocks);
    }

    void registerGlobalTag(TagKey<Block> tag) {
        this.allSoils.register(tag);
    }
}
