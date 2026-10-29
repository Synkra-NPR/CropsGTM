package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.PotencyData;
import com.synkra.cropsgtm.farming.registries.FertilizerRegistry;
import com.synkra.cropsgtm.farming.registries.HydrationRegistry;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock;
import com.synkra.cropsgtm.integration.gt.init.CropsGTMMaterials;

import java.util.List;

/**
 * 工业农场的水与肥料效力处理。
 * <p>
 * 储罐作为参数传入，使算术不落在机器类中，可以在没有多方块的情况下单独校验。
 * </p>
 */
final class IndustrialFarmPotency {

    private IndustrialFarmPotency() {}

    /**
     * 单个富集肥料单位的效力，做成共享值是因为肥料模块路径每刻都会被查询，否则会反复分配同一条目。
     */
    private static final PotencyData SINGLE_FERTILIZER_POTENCY = new PotencyData(1);

    static int waterNeededPerCycle(int tier, int ocs) {
        return (int) Math.ceil(IndustrialFarmComponentBlock.getWaterConsumption(tier) * ocPotencyMultiplier(ocs) *
                CropsGTMConfig.waterConsumptionMultiplier);
    }

    static int fertilizerNeededPerCycle(int tier, int ocs) {
        return (int) Math.ceil(IndustrialFarmComponentBlock.getFertilizerConsumption(tier) * ocPotencyMultiplier(ocs) *
                CropsGTMConfig.fertilizerConsumptionMultiplier);
    }

    /** @return 给定储罐中存储的水效力。 */
    static int availableWater(List<NotifiableFluidTank> tanks) {
        int potency = 0;
        for (NotifiableFluidTank tank : tanks) {
            for (int slot = 0; slot < tank.getTanks(); slot++) {
                FluidStack fluid = tank.getFluidInTank(slot);
                PotencyData data = HydrationRegistry.instance.getPotency(fluid);
                if (data == null) continue;
                int unitPotency = CropsGTMConfig.scalePotency(data.getPotency(), CropsGTMConfig.waterPotencyMultiplier);
                potency += (fluid.getAmount() / data.getUnitsConsumedPerApplication()) * unitPotency;
            }
        }
        return potency;
    }

    static void consumeWater(List<NotifiableFluidTank> tanks, int amount) {
        int remaining = amount;
        for (NotifiableFluidTank tank : tanks) {
            for (int slot = 0; slot < tank.getTanks() && remaining > 0; slot++) {
                FluidStack fluid = tank.getFluidInTank(slot);
                PotencyData data = HydrationRegistry.instance.getPotency(fluid);
                if (data == null) continue;

                int unitSize = data.getUnitsConsumedPerApplication();
                int unitPotency = CropsGTMConfig.scalePotency(data.getPotency(), CropsGTMConfig.waterPotencyMultiplier);
                int unitsNeeded = ceilDiv(remaining, unitPotency);
                int unitsAvailable = fluid.getAmount() / unitSize;
                int toDrain = Math.min(unitsNeeded, unitsAvailable) * unitSize;
                if (toDrain <= 0) continue;

                FluidStack drained = fluid.copy();
                drained.setAmount(toDrain);
                tank.drainInternal(drained, FluidAction.EXECUTE);
                remaining -= (toDrain / unitSize) * unitPotency;
            }
            if (remaining <= 0) break;
        }
    }

    /** @return 给定储罐中存储的肥料效力。 */
    static int availableFertilizer(List<NotifiableFluidTank> tanks, int fertilizerUnitCount) {
        int potency = 0;
        for (NotifiableFluidTank tank : tanks) {
            for (int slot = 0; slot < tank.getTanks(); slot++) {
                FluidStack fluid = tank.getFluidInTank(slot);
                int value = fertilizerPotencyPerUnit(fluid, fertilizerUnitCount);
                if (value <= 0) continue;
                PotencyData data = fertilizerPotencyData(fluid, fertilizerUnitCount);
                potency += (fluid.getAmount() / data.getUnitsConsumedPerApplication()) * value;
            }
        }
        return potency;
    }

    static void consumeFertilizer(List<NotifiableFluidTank> tanks, int amount, int fertilizerUnitCount) {
        int remaining = amount;
        for (NotifiableFluidTank tank : tanks) {
            for (int slot = 0; slot < tank.getTanks() && remaining > 0; slot++) {
                FluidStack fluid = tank.getFluidInTank(slot);
                int value = fertilizerPotencyPerUnit(fluid, fertilizerUnitCount);
                if (value <= 0) continue;

                PotencyData data = fertilizerPotencyData(fluid, fertilizerUnitCount);
                int unitSize = data.getUnitsConsumedPerApplication();
                int unitsNeeded = ceilDiv(remaining, value);
                int unitsAvailable = fluid.getAmount() / unitSize;
                int toDrain = Math.min(unitsNeeded, unitsAvailable) * unitSize;
                if (toDrain <= 0) continue;

                FluidStack drained = fluid.copy();
                drained.setAmount(toDrain);
                tank.drainInternal(drained, FluidAction.EXECUTE);
                remaining -= (toDrain / unitSize) * value;
            }
            if (remaining <= 0) break;
        }
    }

    /**
     * @return 指定肥料流体的效力；肥料模块忽略富集肥料的效力加成，并且只接受富集肥料。
     */
    private static int fertilizerPotencyPerUnit(FluidStack fluid, int fertilizerUnitCount) {
        if (fluid == null) return 0;
        if (fertilizerUnitCount > 0) {
            return fluid.getFluid() == CropsGTMMaterials.enrichedFertilizer.getFluid() ? 1 : 0;
        }
        PotencyData data = FertilizerRegistry.instance.getPotency(fluid);
        if (data == null) return 0;
        return CropsGTMConfig.scalePotency(data.getPotency(), CropsGTMConfig.fertilizerPotencyMultiplier);
    }

    private static PotencyData fertilizerPotencyData(FluidStack fluid, int fertilizerUnitCount) {
        if (fertilizerUnitCount > 0) return SINGLE_FERTILIZER_POTENCY;
        PotencyData data = FertilizerRegistry.instance.getPotency(fluid);
        return data == null ? SINGLE_FERTILIZER_POTENCY : data;
    }

    private static double ocPotencyMultiplier(int ocs) {
        return ocs <= 63 ? 1L << ocs : Math.pow(2.0d, ocs);
    }

    private static int ceilDiv(int dividend, int divisor) {
        return dividend / divisor + (dividend % divisor == 0 ? 0 : 1);
    }
}
