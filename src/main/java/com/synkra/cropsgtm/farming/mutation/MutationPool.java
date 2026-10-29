package com.synkra.cropsgtm.farming.mutation;

import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.IMutationPool;
import com.synkra.cropsgtm.reference.Names;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class MutationPool implements IMutationPool {

    private final String name;
    private final Set<ICropCard> members = new LinkedHashSet<>();

    public MutationPool(String name) {
        this.name = name;
    }

    public String getUnlocalizedName() {
        return Names.MOD_ID + "_mutationPool." + this.name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void register(ICropCard... newMembers) {
        this.members.addAll(Arrays.asList(newMembers));
    }

    @Override
    public Collection<ICropCard> getMembers() {
        return this.members;
    }

    @Override
    public boolean contains(ICropCard crop) {
        return this.members.contains(crop);
    }

    /**
     * @return 该互斥池至少包含两个不同亲本时返回 true。
     */
    @Override
    public boolean isMatch(Collection<ICropCard> parents) {
        int match = 0;
        for (ICropCard parent : parents) {
            if (this.members.contains(parent) && ++match >= 2) return true;
        }
        return false;
    }
}
