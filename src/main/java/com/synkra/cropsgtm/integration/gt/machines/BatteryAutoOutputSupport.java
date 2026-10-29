package com.synkra.cropsgtm.integration.gt.machines;

import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import net.minecraft.core.Direction;

import javax.annotation.Nullable;

/**
 * 作物机器共用的电池槽与自动物品输出。
 * <p>
 * 机器保留其持久化字段，因为同步注解必须写在机器上；该辅助类负责刻订阅以及这些字段背后的行为。
 * </p>
 */
public final class BatteryAutoOutputSupport {

    /** 辅助类需要从所属机器获得的全部信息。 */
    public interface Host {

        TieredEnergyMachine machine();

        CustomItemStackHandler chargerInventory();

        NotifiableItemStackHandler outputHandler();

        boolean isAutoOutputEnabled();

        @Nullable
        Direction getOutputFacing();
    }

    private final Host host;
    @Nullable
    private TickableSubscription batterySubs;
    @Nullable
    private TickableSubscription autoOutputSubs;

    public BatteryAutoOutputSupport(Host host) {
        this.host = host;
    }

    public void onLoad() {
        updateBatterySubscription();
        updateAutoOutputSubscription();
    }

    public void onUnload() {
        if (this.batterySubs != null) {
            this.batterySubs.unsubscribe();
            this.batterySubs = null;
        }
        if (this.autoOutputSubs != null) {
            this.autoOutputSubs.unsubscribe();
            this.autoOutputSubs = null;
        }
    }

    /** 根据槽位中是否放有可充电物品决定启动或停止电池放电。 */
    public void updateBatterySubscription() {
        if (this.host.machine().energyContainer.dischargeOrRechargeEnergyContainers(this.host.chargerInventory(), 0,
                true)) {
            this.batterySubs = this.host.machine().subscribeServerTick(this.batterySubs, this::chargeBattery);
        } else if (this.batterySubs != null) {
            this.batterySubs.unsubscribe();
            this.batterySubs = null;
        }
    }

    private void chargeBattery() {
        if (!this.host.machine().energyContainer.dischargeOrRechargeEnergyContainers(this.host.chargerInventory(), 0,
                false)) {
            updateBatterySubscription();
        }
    }

    /** 根据配置与相邻容器决定启动或停止自动物品输出。 */
    public void updateAutoOutputSubscription() {
        TieredEnergyMachine machine = this.host.machine();
        Direction outputFacing = this.host.getOutputFacing();
        if (this.host.isAutoOutputEnabled() && !this.host.outputHandler().isEmpty() && outputFacing != null &&
                GTTransferUtils.hasAdjacentItemHandler(machine.getLevel(), machine.getPos(), outputFacing)) {
            this.autoOutputSubs = machine.subscribeServerTick(this.autoOutputSubs, this::autoOutput);
        } else if (this.autoOutputSubs != null) {
            this.autoOutputSubs.unsubscribe();
            this.autoOutputSubs = null;
        }
    }

    private void autoOutput() {
        TieredEnergyMachine machine = this.host.machine();
        if (machine.getOffsetTimer() % 5 == 0) {
            Direction outputFacing = this.host.getOutputFacing();
            if (this.host.isAutoOutputEnabled() && outputFacing != null) {
                this.host.outputHandler().exportToNearby(outputFacing);
            }
            updateAutoOutputSubscription();
        }
    }
}
