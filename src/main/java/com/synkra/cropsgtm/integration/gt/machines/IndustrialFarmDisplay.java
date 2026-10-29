package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.api.ISeedData;
import com.synkra.cropsgtm.integration.gt.blocks.IndustrialFarmComponentBlock;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 工业农场显示界面上展示的文本。
 * <p>
 * 从 {@link IndustrialFarmMachine} 中拆出，使机器类专注于工作逻辑；这里的一切只通过访问器读取机器状态。
 * </p>
 */
public final class IndustrialFarmDisplay {

    private IndustrialFarmDisplay() {}

    public static void addDisplayText(IndustrialFarmMachine machine, List<Component> textList) {
        MultiblockDisplayText.builder(textList, machine.isFormed())
                .setWorkingStatus(machine.isWorkingEnabled(), machine.getCycleProgress() > 0).addWorkingStatusLine();

        if (machine.isFormed()) {
            textList.add(
                    Component
                            .translatable("cropsgtm_tooltip.industrialFarm.tier",
                                    Component.literal(GTValues.VNF[machine.getUpgradeTier()]))
                            .withStyle(ChatFormatting.GRAY));
            textList.add(Component.translatable("cropsgtm_tooltip.industrialFarm.capacity", machine.getSeedCapacity())
                    .withStyle(ChatFormatting.GRAY));
            // 空闲与运行状态已由上方的工作状态行描述，只
            // 解释机器在做什么或缺少什么的状态显示在这里
            IndustrialFarmMachine.Status status = machine.getStatus();
            if (status != IndustrialFarmMachine.Status.IDLE && status != IndustrialFarmMachine.Status.OK) {
                textList.add(getStatusName(status));
            }
            textList.add(Component
                    .translatable("cropsgtm_tooltip.industrialFarm.progress",
                            (int) (Math.min(1.0d,
                                    (double) machine.getCycleProgress() /
                                            IndustrialFarmComponentBlock.getCycleDuration()) *
                                    100))
                    .withStyle(ChatFormatting.WHITE));
            addExpectedOutputLine(machine, textList);
        }
        addStructureHints(machine, textList);
    }

    /**
     * 展示农场运行时单周期的平均产出。
     * <p>
     * 数值是周期所用掉落计算的期望值，因此不会像随机掉落那样跳动。
     * </p>
     */
    private static void addExpectedOutputLine(IndustrialFarmMachine machine, List<Component> textList) {
        if (!machine.isWorkingEnabled() || machine.getMachineMode() != IndustrialFarmMachine.MODE_FARM) return;

        ItemStack seedStack = machine.inventory.getStackInSlot(IndustrialFarmMachine.SLOT_SEED);
        ISeedData seedData = machine.getCachedSeedData();
        if (seedData == null || !machine.areGrowthRequirementsMet(seedData)) return;

        int seedCount = Math.min(seedStack.getCount(), machine.getSeedCapacity());
        List<Map.Entry<ItemStack, Double>> drops = machine.getExpectedDrops(seedData, seedCount);
        if (drops.isEmpty()) return;

        textList.add(Component.translatable("cropsgtm_tooltip.industrialFarm.outputPerCycle")
                .withStyle(ChatFormatting.GRAY));
        // 每条掉落一行，使有多个产出的作物仍然易读
        for (Map.Entry<ItemStack, Double> entry : drops) {
            textList.add(Component.literal("  ").withStyle(ChatFormatting.GRAY)
                    .append(entry.getKey().getHoverName().copy().withStyle(ChatFormatting.WHITE)).append(Component
                            .literal(" x " + formatOutputAmount(entry.getValue())).withStyle(ChatFormatting.GRAY)));
        }
    }

    /** @return 保留两位小数的数值；若两位小数会为零，则保留两位有效数字。 */
    private static String formatOutputAmount(double amount) {
        if (amount >= 0.005d) return String.format(Locale.ROOT, "%.2f", amount);
        if (amount <= 0.0d) return "0";
        return BigDecimal.valueOf(amount).round(new MathContext(2)).stripTrailingZeros().toPlainString();
    }

    /**
     * 说明结构为何没有成型。
     * <p>
     * 方块样板接受每个组件的任意电压，因此电压混用、中间层数不对或升级模块电压不同的结构虽然会成型，但随后会被判为无效。这里只显示结构检查停止时的原因，避免一次给出多个提示。
     * </p>
     */
    private static void addStructureHints(IndustrialFarmMachine machine, List<Component> textList) {
        if (machine.isFormed()) return;
        IndustrialFarmMachine.Status status = machine.getStatus();
        if (!status.isError()) return;
        int detectedTier = machine.getDetectedTier();
        if (status == IndustrialFarmMachine.Status.STRUCTURE_LENGTH_MISMATCH &&
                detectedTier >= IndustrialFarmMachine.MIN_TIER && detectedTier <= IndustrialFarmMachine.MAX_TIER) {
            textList.add(getLengthRequirement(detectedTier).withStyle(ChatFormatting.RED));
        } else {
            textList.add(getStatusName(status));
        }
    }

    public static Component getModeName(int mode) {
        return switch (mode) {
            case IndustrialFarmMachine.MODE_FARM -> Component.translatable("cropsgtm_tooltip.industrialFarm.mode.farm");
            case IndustrialFarmMachine.MODE_OUTPUT -> Component
                    .translatable("cropsgtm_tooltip.industrialFarm.mode.output");
            default -> Component.translatable("cropsgtm_tooltip.industrialFarm.mode.input");
        };
    }

    public static Component getStatusName(IndustrialFarmMachine.Status status) {
        return Component.translatable("cropsgtm_tooltip.industrialFarm.status." + status.code())
                .withStyle(status.isError() ? ChatFormatting.RED : ChatFormatting.GRAY);
    }

    /** @return 说明指定种子床电压下结构需要多长的提示。 */
    public static MutableComponent getLengthRequirement(int tier) {
        return Component.translatable(
                "cropsgtm_tooltip.industrialFarm.status." +
                        IndustrialFarmMachine.Status.STRUCTURE_LENGTH_MISMATCH.code(),
                IndustrialFarmComponentBlock.getMultiLength(tier));
    }
}
