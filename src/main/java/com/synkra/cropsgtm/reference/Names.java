package com.synkra.cropsgtm.reference;

/**
 * Crops NH 使用的共享标识符。
 */
public final class Names {

    private Names() {}

    public static final String MOD_ID = "cropsgtm";

    /** 种子、种子数据与作物架使用的 NBT 键。 */
    public static final class NBT {

        private NBT() {}

        public static final String crop = "crop";
        public static final String amount = "amount";
        public static final String growth = "gr";
        public static final String gain = "ga";
        public static final String resistance = "re";
        public static final String analyzed = "scan";
        public static final String crossCrop = "crossCrop";
        public static final String sick = "sick";
        public static final String progress = "progress";
        public static final String extra = "extra";
        public static final String water = "water";
        public static final String fertilizer = "fert";
        public static final String weedEx = "weedex";
        public static final String hasSoilChanged = "hasSoilChanged";
        /** 基因数据宝珠的 NBT 键，类型为 specimen（样本）、growth、gain 或 resistance 之一。 */
        public static final String geneType = "cropsgtm_gene_type";
        public static final String geneValue = "cropsgtm_gene_value";
    }

    /** 物品与作物共用的翻译键。 */
    public static final class L10N {

        private L10N() {}

        public static final String genericSeedFormat = MOD_ID + "_genericSeeds.format";
        public static final String unknownSeed = MOD_ID + "_genericSeeds.unknown";
        public static final String invalidSeed = MOD_ID + "_genericSeeds.invalid";
        public static final String anySeed = MOD_ID + "_genericSeeds.any";
    }
}
