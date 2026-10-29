package com.synkra.cropsgtm.api;

import java.util.Collection;

/**
 * 一组可以随机繁殖出组内其他成员的作物。
 */
public interface IMutationPool {

    String getName();

    Collection<ICropCard> getMembers();

    boolean contains(ICropCard crop);

    /**
     * @return 该互斥池至少包含两个不同亲本时返回 true。
     */
    boolean isMatch(Collection<ICropCard> parents);
}
