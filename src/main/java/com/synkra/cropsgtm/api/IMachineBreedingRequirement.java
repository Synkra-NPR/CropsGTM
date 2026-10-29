package com.synkra.cropsgtm.api;

import java.util.Collection;

/**
 * 作物繁殖机繁殖突变时检查的繁殖条件。
 */
public interface IMachineBreedingRequirement extends IBreedingRequirement {

    boolean canBreedInMachine(Collection<ICropCard> parents);
}
