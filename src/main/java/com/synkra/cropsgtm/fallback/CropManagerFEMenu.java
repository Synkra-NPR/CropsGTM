package com.synkra.cropsgtm.fallback;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 备用作物管理器的容器。
 * <p>
 * 机器槽位排在前面，便于 Shift 点击区分：六个输入槽、两个回收空容器的槽位、电池槽和十五个输出槽，其后是玩家背包。
 * </p>
 */
public class CropManagerFEMenu extends AbstractContainerMenu {

    public static final int INPUT_SLOTS = BlockEntityCropManagerFE.INPUT_SLOTS;
    public static final int CONTAINER_SLOTS = BlockEntityCropManagerFE.CONTAINER_SLOTS;
    public static final int CONTAINER_START = INPUT_SLOTS;
    public static final int ENERGY_SLOT = CONTAINER_START + CONTAINER_SLOTS;
    public static final int OUTPUT_START = ENERGY_SLOT + 1;
    public static final int MACHINE_SLOTS = OUTPUT_START + BlockEntityCropManagerFE.OUTPUT_SLOTS;

    private static final int INPUT_X = 30;
    private static final int INPUT_Y = 18;
    private static final int CONTAINER_Y = 78;
    private static final int ENERGY_X = 78;
    private static final int ENERGY_Y = 78;
    private static final int OUTPUT_X = 150;
    private static final int OUTPUT_Y = 18;
    private static final int INVENTORY_X = 48;
    private static final int INVENTORY_Y = 150;
    private static final int HOTBAR_Y = 204;

    private static final int DATA_WATER = 0;
    private static final int DATA_FERTILIZER = 1;
    private static final int DATA_WEED_EX = 2;
    private static final int DATA_ENERGY = 3;
    private static final int DATA_AUTO_OUTPUT = 4;
    private static final int DATA_WORKING = 5;
    private static final int DATA_HARVEST = 6;
    private static final int DATA_WATER_ENABLED = 7;
    private static final int DATA_FERTILIZER_ENABLED = 8;
    private static final int DATA_WEED_EX_ENABLED = 9;

    private final BlockEntityCropManagerFE machine;
    private final SimpleContainerData data = new SimpleContainerData(10);

    /** 服务端构造器。 */
    public CropManagerFEMenu(int windowId, Inventory inventory, BlockEntityCropManagerFE machine) {
        super(FallbackContent.cropManagerMenu.get(), windowId);
        this.machine = machine;
        this.addMachineSlots(machine);
        this.addPlayerSlots(inventory);
        this.addDataSlots(this.data);
    }

    /** 客户端构造器，打开界面时会发送方块坐标。 */
    public CropManagerFEMenu(int windowId, Inventory inventory, FriendlyByteBuf buffer) {
        this(windowId, inventory, resolveMachine(inventory, buffer));
    }

    private static BlockEntityCropManagerFE resolveMachine(Inventory inventory, FriendlyByteBuf buffer) {
        BlockEntityCropManagerFE machine = inventory.player.level()
                .getBlockEntity(buffer.readBlockPos()) instanceof BlockEntityCropManagerFE found ? found : null;
        return machine;
    }

    private void addMachineSlots(BlockEntityCropManagerFE machine) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 2; column++) {
                this.addSlot(new SlotItemHandler(machine.getInput(), row * 2 + column, INPUT_X + column * 18,
                        INPUT_Y + row * 18));
            }
        }
        for (int slot = 0; slot < CONTAINER_SLOTS; slot++) {
            this.addSlot(new SlotItemHandler(machine.getContainerOutput(), slot, INPUT_X + slot * 18, CONTAINER_Y));
        }
        this.addSlot(new SlotItemHandler(machine.getEnergySlot(), 0, ENERGY_X, ENERGY_Y));
        // 产出横向排布：五列三行
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 5; column++) {
                this.addSlot(new SlotItemHandler(machine.getOutput(), row * 5 + column, OUTPUT_X + column * 18,
                        OUTPUT_Y + row * 18));
            }
        }
    }

    private void addPlayerSlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(
                        new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    @Override
    public void broadcastChanges() {
        if (this.machine != null && this.machine.getLevel() != null && !this.machine.getLevel().isClientSide) {
            this.data.set(DATA_WATER, this.machine.getWaterMb());
            this.data.set(DATA_FERTILIZER, this.machine.getFertilizerMb());
            this.data.set(DATA_WEED_EX, this.machine.getWeedExMb());
            this.data.set(DATA_ENERGY, this.machine.getEnergyStored());
            this.data.set(DATA_AUTO_OUTPUT, this.machine.isAutoOutput() ? 1 : 0);
            this.data.set(DATA_WORKING, this.machine.isWorkingEnabled() ? 1 : 0);
            this.data.set(DATA_HARVEST, this.machine.isHarvestEnabled() ? 1 : 0);
            this.data.set(DATA_WATER_ENABLED, this.machine.isWaterEnabled() ? 1 : 0);
            this.data.set(DATA_FERTILIZER_ENABLED, this.machine.isFertilizerEnabled() ? 1 : 0);
            this.data.set(DATA_WEED_EX_ENABLED, this.machine.isWeedExEnabled() ? 1 : 0);
        }
        super.broadcastChanges();
    }

    /** @return 机器正在把输出槽推送到相邻容器时返回 true。 */
    public boolean isAutoOutput() {
        return this.data.get(DATA_AUTO_OUTPUT) != 0;
    }

    public boolean isWorkingEnabled() {
        return this.data.get(DATA_WORKING) != 0;
    }

    public boolean isHarvestEnabled() {
        return this.data.get(DATA_HARVEST) != 0;
    }

    public boolean isWaterEnabled() {
        return this.data.get(DATA_WATER_ENABLED) != 0;
    }

    public boolean isFertilizerEnabled() {
        return this.data.get(DATA_FERTILIZER_ENABLED) != 0;
    }

    public boolean isWeedExEnabled() {
        return this.data.get(DATA_WEED_EX_ENABLED) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id <= 5 && this.machine != null) {
            this.machine.toggleFeature(id);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    public int getWater() {
        return this.data.get(DATA_WATER);
    }

    public int getFertilizer() {
        return this.data.get(DATA_FERTILIZER);
    }

    public int getWeedEx() {
        return this.data.get(DATA_WEED_EX);
    }

    public int getEnergy() {
        return this.data.get(DATA_ENERGY);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, INPUT_SLOTS, false) &&
                !this.moveItemStackTo(stack, ENERGY_SLOT, ENERGY_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.machine == null) return false;
        return player.distanceToSqr(this.machine.getBlockPos().getX() + 0.5d, this.machine.getBlockPos().getY() + 0.5d,
                this.machine.getBlockPos().getZ() + 0.5d) <= 64.0d;
    }
}
