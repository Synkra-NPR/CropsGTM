package com.synkra.cropsgtm.farming.mutation;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropMutation;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用于查找确定性突变的亲本前缀树。
 */
public class MutationMap {

    private final List<ICropMutation> list = new ArrayList<>(1);
    private final Map<ICropCard, MutationMap> map = new IdentityHashMap<>();

    /**
     * 清空该映射中的所有突变。
     */
    public void clear() {
        this.list.clear();
        this.map.clear();
    }

    /**
     * 为按注册顺序排序的亲本列表注册突变。
     */
    public void register(List<ICropCard> parents, int index, ICropMutation mutation) {
        if (index < 0) throw new IllegalArgumentException("parent index cannot be negative");
        if (parents.size() <= index) {
            if (index < 2) {
                throw new IllegalArgumentException("Mutation recipes should always at least specify 2 parents");
            }
            this.list.add(mutation);
            return;
        }
        ICropCard parent = parents.get(index);
        this.map.computeIfAbsent(parent, key -> new MutationMap()).register(parents, index + 1, mutation);
    }

    /**
     * 找出所有匹配给定亲本列表的突变，按注册顺序排序。
     */
    public void findMatches(List<ICropCard> parents, int index, Set<ICropMutation> matches) {
        if (index < 0) throw new IllegalArgumentException("parent index cannot be negative");
        // 确定性突变不应少于两个不同亲本。
        if (index >= 2) matches.addAll(this.list);
        if (parents.size() <= index) return;
        MutationMap next = this.map.get(parents.get(index));
        if (next != null) next.findMatches(parents, index + 1, matches);
    }
}
