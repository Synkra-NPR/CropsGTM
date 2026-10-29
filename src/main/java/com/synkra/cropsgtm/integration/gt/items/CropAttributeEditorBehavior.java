package com.synkra.cropsgtm.integration.gt.items;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.synkra.cropsgtm.config.CropsGTMConfig;
import com.synkra.cropsgtm.farming.SeedStats;
import com.synkra.cropsgtm.farming.registries.CropRegistry;
import com.synkra.cropsgtm.items.ItemGenericSeed;
import com.synkra.cropsgtm.reference.Constants;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * 打开作物属性编辑器的界面并应用属性改动。
 * <p>
 * 每次点击加/减按钮即为一次编辑：数值立即写入种子，增加点数所需能量从设备中扣除。降低属性免费，因此一次编辑的费用等于三项属性正变化之和，与原公式一致。
 * </p>
 * <p>
 * 编辑只在服务端执行，客户端显示服务端写入种子槽的内容。若客户端也编辑，会显示服务端从未接受的值，玩家会看到种子似乎已被修改，直到下一次背包同步才被还原。
 * </p>
 */
public class CropAttributeEditorBehavior implements IItemUIFactory {

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 214;

    private static final int SEED_SLOT_X = 8;
    private static final int SEED_SLOT_Y = 18;

    private static final int INFO_X = 8;
    private static final int INFO_Y = 40;
    private static final int COST_Y = 51;

    private static final int FIRST_ROW_Y = 66;
    private static final int ROW_HEIGHT = 22;

    private static final int MINUS_BUTTON_X = 96;
    private static final int VALUE_X = 118;
    private static final int PLUS_BUTTON_X = 136;
    private static final int BUTTON_SIZE = 18;

    private static final int INVENTORY_Y = 132;

    /** 属性下标，按行的显示顺序排列。 */
    private static final int STAT_GROWTH = 0;
    private static final int STAT_GAIN = 1;
    private static final int STAT_RESISTANCE = 2;

    private static final String[] STAT_LANG_KEYS = { "cropsgtm_tooltip.cropAttributeEditor.growth",
            "cropsgtm_tooltip.cropAttributeEditor.gain", "cropsgtm_tooltip.cropAttributeEditor.resistance" };

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {
        boolean remote = entityPlayer.level().isClientSide;
        EditorSeedTransfer transfer = new EditorSeedTransfer(holder);

        SlotWidget seedSlot = new SlotWidget(transfer, 0, SEED_SLOT_X, SEED_SLOT_Y, true, true);
        seedSlot.setBackground(GuiTextures.SLOT);

        ModularUI ui = new ModularUI(GUI_WIDTH, GUI_HEIGHT, holder, entityPlayer).background(GuiTextures.BACKGROUND);
        // ldlib 会为任何非客户端组件的标签调用 textSupplier.get()，因此每个标签
        // 必须通过 supplier 创建，Component 构造器会把它留作 null 并导致崩溃
        // 服务端，在它打开界面时
        LabelWidget title = new LabelWidget(8, 6, "item.cropsgtm.crop_attribute_editor");
        title.setDropShadow(false);
        title.setColor(0x404040);
        ui.widget(title).widget(seedSlot);

        // 能量行是普通文本标签，因此可以绘制为黑色且不带阴影
        // GregTech 文本面板无法关闭
        LabelWidget energy = new LabelWidget(INFO_X, INFO_Y, () -> getEnergyText(holder));
        energy.setDropShadow(false);
        energy.setColor(0x000000);
        energy.setClientSideWidget();
        ui.widget(energy);

        LabelWidget cost = new LabelWidget(8, COST_Y,
                () -> Component.translatable("cropsgtm_tooltip.cropAttributeEditor.cost",
                        FormattingUtil.formatNumbers(ItemCropAttributeEditor.COST_PER_POINT)).getString());
        cost.setDropShadow(false);
        cost.setColor(0x000000);
        cost.setClientSideWidget();
        ui.widget(cost);

        for (int stat = STAT_GROWTH; stat <= STAT_RESISTANCE; stat++) {
            int rowY = FIRST_ROW_Y + stat * ROW_HEIGHT;
            addStatRow(ui, remote, holder, entityPlayer, transfer, seedSlot, stat, rowY);
        }

        ui.widget(UITemplate.bindPlayerInventory(entityPlayer.getInventory(), GuiTextures.SLOT, 7, INVENTORY_Y, true));
        // 种子存放在设备标签中，因此界面关闭时必须写回，否则会丢失
        ui.registerCloseListener(() -> {
            if (holder.isRemote()) return;
            ItemCropAttributeEditor.setSeed(holder.getHeld(), transfer.getStackInSlot(0));
        });
        return ui;
    }

    /**
     * 构建在客户端本地重新计算的文本面板，使显示的数值跟随种子槽变化而无需服务端往返。槽位本身仍只由服务端填充。
     */
    private static ComponentPanelWidget createTextPanel(int x, int y, boolean remote,
                                                        Consumer<List<Component>> textSupplier) {
        ComponentPanelWidget panel = new ComponentPanelWidget(x, y, textSupplier);
        panel.setClientSideWidget();
        if (!remote) {
            // 服务端无需推送文本，客户端会自行计算同一个 supplier
            panel.textSupplier(null);
        }
        return panel;
    }

    private void addStatRow(ModularUI ui, boolean remote, HeldItemUIFactory.HeldItemHolder holder, Player player,
                            EditorSeedTransfer transfer, SlotWidget seedSlot, int stat, int rowY) {
        String langKey = STAT_LANG_KEYS[stat];

        LabelWidget label = new LabelWidget(8, rowY + 4, langKey);
        label.setDropShadow(false);
        label.setColor(0x404040);

        ui.widget(label);
        ui.widget(new ButtonWidget(MINUS_BUTTON_X, rowY, BUTTON_SIZE, BUTTON_SIZE,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("-").setDropShadow(false)),
                data -> adjustStat(player, holder, transfer, seedSlot, stat, -1)));
        ui.widget(createTextPanel(VALUE_X, rowY + 4, remote, list -> addValueText(list, transfer, stat)));
        ui.widget(new ButtonWidget(PLUS_BUTTON_X, rowY, BUTTON_SIZE, BUTTON_SIZE,
                new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("+").setDropShadow(false)),
                data -> adjustStat(player, holder, transfer, seedSlot, stat, 1)));
    }

    private String getEnergyText(HeldItemUIFactory.HeldItemHolder holder) {
        // 背包同步会替换手持物品对象，因此必须在此读取实时物品，
        // holder.getHeld() 会一直显示打开界面那一刻的电量
        ItemStack editor = holder.player.getItemInHand(holder.hand);
        long charge = ItemCropAttributeEditor.getStoredEnergy(editor);
        long capacity = ItemCropAttributeEditor.getEnergyCapacity(editor);
        return Component.translatable("cropsgtm_tooltip.cropAttributeEditor.energyStored", formatMillion(charge),
                formatMillion(capacity)).getString();
    }

    /** @return 以 M EU 表示的能量，例如 {@code 2937.0M}。 */
    private static String formatMillion(long energy) {
        return String.format(Locale.ROOT, "%.1fM", energy / 1_000_000.0d);
    }

    private void addValueText(List<Component> textList, EditorSeedTransfer transfer, int stat) {
        SeedStats stats = SeedStats.getStatsFromStack(transfer.getStackInSlot(0));
        if (stats == null) {
            textList.add(Component.literal("-").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        textList.add(Component.literal(Integer.toString(getStatValue(stats, stat))).withStyle(ChatFormatting.YELLOW));
    }

    private static int getStatValue(SeedStats stats, int stat) {
        return switch (stat) {
            case STAT_GROWTH -> stats.getGrowth();
            case STAT_GAIN -> stats.getGain();
            default -> stats.getResistance();
        };
    }

    /** @return 按配置，该属性可达到的最高值。 */
    private static int getMaxStat(int stat) {
        return switch (stat) {
            case STAT_GROWTH -> CropsGTMConfig.maxGrowthStat;
            case STAT_GAIN -> CropsGTMConfig.maxGainStat;
            default -> CropsGTMConfig.maxResistanceStat;
        };
    }

    /**
     * 应用一次属性编辑。只有服务端执行编辑，客户端通过同步槽位获得新的种子。
     * <p>
     * 可以整组编辑种子：属性存放在物品标签中，费用按组内每颗种子各收一次。
     * </p>
     */
    private void adjustStat(Player player, HeldItemUIFactory.HeldItemHolder holder, EditorSeedTransfer transfer,
                            SlotWidget seedSlot, int stat, int delta) {
        if (player.level().isClientSide) return;

        ItemStack editor = holder.getHeld();
        // 种子存放在设备本身，因此即使同步槽位落后，编辑也能生效
        ItemStack seed = transfer.getStackInSlot(0);
        if (seed.isEmpty()) seed = ItemCropAttributeEditor.getSeed(editor);
        seed = seed.copy();
        SeedStats stats = SeedStats.getStatsFromStack(seed);
        if (stats == null) {
            player.sendSystemMessage(Component.translatable("cropsgtm_tooltip.cropAttributeEditor.noSeed"));
            return;
        }

        int oldValue = getStatValue(stats, stat);
        int newValue = Mth.clamp(oldValue + delta, Constants.MIN_SEED_STAT, getMaxStat(stat));
        if (newValue == oldValue) return;

        long cost = Math.max(0, newValue - oldValue) * ItemCropAttributeEditor.COST_PER_POINT * seed.getCount();
        if (!player.isCreative() && cost > 0) {
            long charge = ItemCropAttributeEditor.getStoredEnergy(editor);
            if (charge < cost) {
                player.sendSystemMessage(
                        Component.translatable("cropsgtm_tooltip.cropAttributeEditor.notEnoughEnergy"));
                return;
            }
            // 电量只是物品标签中的一个数字，不需要电力物品能力
            ItemCropAttributeEditor.setStoredEnergy(editor, charge - cost);
        }

        int growth = stats.getGrowth();
        int gain = stats.getGain();
        int resistance = stats.getResistance();
        switch (stat) {
            case STAT_GROWTH -> growth = newValue;
            case STAT_GAIN -> gain = newValue;
            default -> resistance = newValue;
        }

        // 编辑种子属性意味着玩家已了解其属性，因此种子变为已分析
        CompoundTag tag = seed.getOrCreateTag();
        new SeedStats((byte) growth, (byte) gain, (byte) resistance, true).writeToNBT(tag);
        // 先把种子存入设备，再让槽位通知菜单，使客户端拿到它
        ItemCropAttributeEditor.setSeed(editor, seed);
        seedSlot.setItem(seed);
    }

    /**
     * 界面中种子槽的物品处理器，使设备的物品标签与槽位内容保持同步，并接受整组种子以便一次批量编辑。
     */
    private static final class EditorSeedTransfer extends ItemStackTransfer {

        private final HeldItemUIFactory.HeldItemHolder holder;

        private EditorSeedTransfer(HeldItemUIFactory.HeldItemHolder holder) {
            super(1);
            this.holder = holder;
            setFilter(this::isValidSeed);
            setStackInSlot(0, ItemCropAttributeEditor.getSeed(holder.getHeld()));
        }

        @Override
        public void onContentsChanged() {
            super.onContentsChanged();
            // 物品标签只由服务端持有，客户端写入会留下一个服务端
            // 并不知道，背包同步之后会把值还原
            if (holder.isRemote()) return;
            ItemCropAttributeEditor.setSeed(holder.getHeld(), getStackInSlot(0));
        }

        private boolean isValidSeed(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof ItemGenericSeed &&
                    CropRegistry.instance.get(stack) != null;
        }
    }
}
