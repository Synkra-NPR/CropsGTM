package com.synkra.cropsgtm.farming.mutation;

import net.minecraft.world.level.Level;

import com.synkra.cropsgtm.api.IBreedingRequirement;
import com.synkra.cropsgtm.api.ICropCard;
import com.synkra.cropsgtm.api.ICropMutation;
import com.synkra.cropsgtm.api.ICropStickTile;
import com.synkra.cropsgtm.api.IGrowthRequirement;
import com.synkra.cropsgtm.api.IMachineBreedingRequirement;
import com.synkra.cropsgtm.api.IWorldBreedingRequirement;
import com.synkra.cropsgtm.farming.registries.MutationRegistry;
import com.synkra.cropsgtm.farming.requirements.breeding.MachineOnlyBreedingRequirement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class CropMutation implements ICropMutation {

    private final List<IBreedingRequirement> requirements = new ArrayList<>();
    private final ICropCard output;
    private final ICropCard parent1;
    private final ICropCard parent2;
    private final ICropCard parent3;
    private final ICropCard parent4;

    private long breedingMachineEUt;
    private int breedingMachineTier;
    private int breedingMachineDuration;

    public CropMutation(ICropCard output, ICropCard parent1, ICropCard parent2) {
        this(output, parent1, parent2, null, null);
    }

    public CropMutation(ICropCard output, ICropCard parent1, ICropCard parent2, ICropCard parent3) {
        this(output, parent1, parent2, parent3, null);
    }

    public CropMutation(ICropCard output, ICropCard parent1, ICropCard parent2, ICropCard parent3, ICropCard parent4) {
        if (output == null) throw new IllegalArgumentException("mutation output cannot be null");
        if (parent1 == null) throw new IllegalArgumentException("parent 1 cannot be null");
        if (parent2 == null) throw new IllegalArgumentException("parent 2 cannot be null");
        if (parent3 == null && parent4 != null) {
            throw new IllegalArgumentException("parent 3 is null but parent 4 isn't, something has gone wrong.");
        }
        HashSet<ICropCard> seen = new HashSet<>();
        seen.add(parent1);
        if (!seen.add(parent2)) {
            throw new IllegalArgumentException("duplicate mutation parent for " + output.getId() + " at parent 2");
        }
        if (parent3 != null && !seen.add(parent3)) {
            throw new IllegalArgumentException("duplicate mutation parent for " + output.getId() + " at parent 3");
        }
        if (parent4 != null && !seen.add(parent4)) {
            throw new IllegalArgumentException("duplicate mutation parent for " + output.getId() + " at parent 4");
        }
        this.output = output;
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.parent3 = parent3;
        this.parent4 = parent4;

        // 沿用产出作物中同样适用于繁殖的需求
        for (IGrowthRequirement requirement : output.getGrowthRequirements()) {
            if (requirement instanceof IBreedingRequirement breedingRequirement) {
                this.requirements.add(breedingRequirement);
            }
        }

        this.setBreedingMachineRecipeTier(this.output.getMachineBreedingRecipeTier());
    }

    private static final int MIN_BREEDING_TIER = 1;
    private static final int MAX_BREEDING_TIER = 13;

    /**
     * 设置作物繁殖机繁殖该突变所使用的能量等级。
     */
    public void setBreedingMachineRecipeTier(int tier) {
        int clamped = Math.max(MIN_BREEDING_TIER, Math.min(MAX_BREEDING_TIER, tier));
        this.breedingMachineTier = clamped;
        // ULV 下为 8 EU/t，每级翻倍，并手动应用 30/32 系数
        this.breedingMachineEUt = (8L << clamped) * 30L / 32L;
        this.breedingMachineDuration = (int) (400 * Math.pow(1.3, Math.max(0, clamped - MIN_BREEDING_TIER)));
    }

    @Override
    public long getBreedingMachineRecipeEUt() {
        return this.breedingMachineEUt;
    }

    @Override
    public int getBreedingMachineRecipeTier() {
        return this.breedingMachineTier;
    }

    @Override
    public int getBreedingMachineRecipeDuration() {
        return this.breedingMachineDuration;
    }

    @Override
    public ICropCard getOutput() {
        return this.output;
    }

    @Override
    public int getParentCount() {
        if (this.parent4 != null) return 4;
        if (this.parent3 != null) return 3;
        return 2;
    }

    @Override
    public Collection<ICropCard> getParents() {
        List<ICropCard> parents = new ArrayList<>(this.getParentCount());
        parents.add(this.parent1);
        parents.add(this.parent2);
        if (this.parent3 != null) parents.add(this.parent3);
        if (this.parent4 != null) parents.add(this.parent4);
        return parents;
    }

    @Override
    public boolean canBreed(Collection<ICropCard> parents, Level level, ICropStickTile tile) {
        if (!this.hasAllParents(parents)) return false;
        for (IBreedingRequirement requirement : this.requirements) {
            if (!(requirement instanceof IWorldBreedingRequirement worldRequirement)) continue;
            if (!worldRequirement.canBreed(parents, level, tile)) return false;
        }
        return true;
    }

    @Override
    public boolean canBreedInMachine(Collection<ICropCard> parents) {
        if (!this.hasAllParents(parents)) return false;
        for (IBreedingRequirement requirement : this.requirements) {
            if (!(requirement instanceof IMachineBreedingRequirement machineRequirement)) continue;
            if (!machineRequirement.canBreedInMachine(parents)) return false;
        }
        return true;
    }

    @Override
    public List<IBreedingRequirement> getRequirements() {
        return this.requirements;
    }

    private boolean hasAllParents(Collection<ICropCard> parents) {
        if (!parents.contains(this.parent1) || !parents.contains(this.parent2)) return false;
        if (this.parent3 != null && !parents.contains(this.parent3)) return false;
        if (this.parent4 != null && !parents.contains(this.parent4)) return false;
        return true;
    }

    /**
     * 使该突变只能在作物繁殖机中进行。
     */
    public CropMutation machineOnly() {
        this.requirements.add(new MachineOnlyBreedingRequirement());
        return this;
    }

    /**
     * 为该突变追加一条额外繁殖条件。
     */
    public CropMutation addRequirement(IBreedingRequirement requirement) {
        this.requirements.add(requirement);
        return this;
    }

    /**
     * 把该突变注册进突变登记表。
     */
    public CropMutation register() {
        MutationRegistry.instance.register(this);
        return this;
    }
}
