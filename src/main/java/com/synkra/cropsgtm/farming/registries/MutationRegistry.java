package com.synkra.cropsgtm.farming.registries;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropMutation;
import com.synkra.cropsgtm.api.IMutationPool;
import com.synkra.cropsgtm.farming.mutation.CropMutation;
import com.synkra.cropsgtm.farming.mutation.MutationMap;
import com.synkra.cropsgtm.farming.mutation.MutationPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class MutationRegistry {

    public static final MutationRegistry instance = new MutationRegistry();

    private final MutationMap mutationMap = new MutationMap();
    private final Map<String, MutationPool> mutationPools = new java.util.LinkedHashMap<>();
    private final Map<ICropCard, Collection<CropMutation>> deterministicMutations = new IdentityHashMap<>();
    private final List<CropMutation> allMutations = new ArrayList<>();

    private MutationRegistry() {}

    public void register(CropMutation mutation) {
        List<ICropCard> parents = createLookupQueue(mutation.getParents());
        if (parents.size() < 2) {
            throw new IllegalArgumentException("Crop mutations should not have less than 2 distinct parents");
        }
        this.mutationMap.register(parents, 0, mutation);
        this.deterministicMutations.computeIfAbsent(mutation.getOutput(), key -> new ArrayList<>(1)).add(mutation);
        this.allMutations.add(mutation);
    }

    /**
     * 把作物注册进一个互斥池。
     */
    public void register(String poolId, ICropCard... crops) {
        MutationPool pool = this.mutationPools.computeIfAbsent(poolId, MutationPool::new);
        pool.register(crops);
    }

    /**
     * 把单个作物注册进给定的每个互斥池。
     */
    public void register(ICropCard crop, String... poolIds) {
        for (String poolId : poolIds) {
            this.register(poolId, crop);
        }
    }

    /**
     * 用作物声明的池替换互斥池，并清除所有突变。
     */
    public void reloadPools(Map<String, ? extends Collection<ICropCard>> pools) {
        this.mutationPools.clear();
        this.clearMutations();
        for (Map.Entry<String, ? extends Collection<ICropCard>> entry : pools.entrySet()) {
            for (ICropCard crop : entry.getValue()) {
                this.register(entry.getKey(), crop);
            }
        }
    }

    /**
     * 清除所有已注册的突变，互斥池保留。
     */
    public void clearMutations() {
        this.mutationMap.clear();
        this.deterministicMutations.clear();
        this.allMutations.clear();
    }

    /** @return 所有已注册的突变，用于构建配方查看器页面。 */
    public List<CropMutation> getAllMutations() {
        return List.copyOf(this.allMutations);
    }

    public @Nullable List<ICropMutation> getPossibleDeterministicMutations(Collection<ICropCard> parents) {
        if (parents == null || parents.size() < 2) return null;
        List<ICropCard> sortedParents = createLookupQueue(parents);
        if (sortedParents.size() < 2) return null;
        HashSet<ICropMutation> accumulator = new HashSet<>();
        this.mutationMap.findMatches(sortedParents, 0, accumulator);
        if (accumulator.isEmpty()) return null;
        return new ArrayList<>(accumulator);
    }

    public @Nullable Collection<CropMutation> getDeterministicMutationsForCrop(ICropCard crop) {
        return this.deterministicMutations.get(crop);
    }

    public @Nullable List<IMutationPool> getPossiblePoolMutations(Collection<ICropCard> parents) {
        if (parents == null || parents.size() < 2) return null;
        List<ICropCard> sortedParents = createPoolQueue(parents);
        if (sortedParents.size() < 2) return null;
        List<IMutationPool> validPools = this.mutationPools.values().stream()
                .filter(pool -> pool.isMatch(sortedParents)).collect(Collectors.toList());
        if (validPools.isEmpty()) return null;
        return validPools;
    }

    public Collection<IMutationPool> getMutationPools() {
        return new ArrayList<>(this.mutationPools.values());
    }

    /**
     * 移除成员少于两个的池，它们参与繁殖与普通蔓延没有区别。
     */
    public void pruneMutationPools() {
        this.mutationPools.entrySet().removeIf(entry -> entry.getValue().getMembers().size() < 2);
    }

    private static List<ICropCard> createLookupQueue(Collection<ICropCard> parents) {
        return parents.stream().sorted(Comparator.comparingInt(ICropCard::getNumericId)).distinct()
                .collect(Collectors.toList());
    }

    private static List<ICropCard> createPoolQueue(Collection<ICropCard> parents) {
        return parents.stream().sorted(Comparator.comparingInt(ICropCard::getNumericId)).collect(Collectors.toList());
    }
}
