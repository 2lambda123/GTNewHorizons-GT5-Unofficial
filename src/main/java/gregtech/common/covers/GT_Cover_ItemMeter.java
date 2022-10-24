package gregtech.common.covers;

import com.google.common.io.ByteArrayDataInput;
import com.gtnewhorizons.modularui.api.math.MathExpression;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.BaseTextFieldWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;
import gregtech.api.GregTech_API;
import gregtech.api.enums.GT_Values;
import gregtech.api.gui.GT_GUICover;
import gregtech.api.gui.modularui.GT_CoverUIBuildContext;
import gregtech.api.gui.widgets.GT_GuiFakeItemButton;
import gregtech.api.gui.widgets.GT_GuiIcon;
import gregtech.api.gui.widgets.GT_GuiIconCheckButton;
import gregtech.api.gui.widgets.GT_GuiIntegerTextBox;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICoverable;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.net.GT_Packet_TileEntityCoverNew;
import gregtech.api.util.GT_CoverBehaviorBase;
import gregtech.api.util.GT_Utility;
import gregtech.api.util.ISerializableObject;
import gregtech.common.gui.modularui.CoverDataControllerWidget;
import gregtech.common.gui.modularui.CoverDataFollower_TextFieldWidget;
import gregtech.common.gui.modularui.CoverDataFollower_ToggleButtonWidget;
import gregtech.common.gui.modularui.ItemWatcherSlotWidget;
import gregtech.common.tileentities.machines.GT_MetaTileEntity_Hatch_OutputBus_ME;
import gregtech.common.tileentities.storage.GT_MetaTileEntity_DigitalChestBase;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

public class GT_Cover_ItemMeter extends GT_CoverBehaviorBase<GT_Cover_ItemMeter.ItemMeterData> {

    // Legacy data format
    private static final int SLOT_MASK = 0x3FFFFFFF; // 0 = all, 1 = 0 ...
    private static final int CONVERTED_BIT = 0x80000000;
    private static final int INVERT_BIT = 0x40000000;

    /**
     * @deprecated use {@link #GT_Cover_ItemMeter(ITexture coverTexture)} instead
     */
    @Deprecated
    public GT_Cover_ItemMeter() {
        this(null);
    }

    public GT_Cover_ItemMeter(ITexture coverTexture) {
        super(ItemMeterData.class, coverTexture);
    }

    @Override
    public ItemMeterData createDataObject(int aLegacyData) {
        // Convert from ver. 5.09.33.50
        if ((CONVERTED_BIT & aLegacyData) == 0)
            if (aLegacyData == 0) aLegacyData = CONVERTED_BIT;
            else if (aLegacyData == 1) aLegacyData = CONVERTED_BIT | INVERT_BIT;
            else if (aLegacyData > 1) aLegacyData = CONVERTED_BIT | Math.min((aLegacyData - 2), SLOT_MASK);

        boolean invert = (aLegacyData & INVERT_BIT) == INVERT_BIT;
        int slot = (aLegacyData & SLOT_MASK) - 1;

        return new ItemMeterData(invert, slot, 0);
    }

    @Override
    public ItemMeterData createDataObject() {
        return new ItemMeterData();
    }

    @Override
    protected boolean isRedstoneSensitiveImpl(
            byte aSide, int aCoverID, ItemMeterData aCoverVariable, ICoverable aTileEntity, long aTimer) {
        return false;
    }

    public static byte computeSignalBasedOnItems(
            ICoverable tileEntity, boolean inverted, int threshold, int slot, int side) {
        long max = 0;
        long used = 0;
        IMetaTileEntity mte = ((IGregTechTileEntity) tileEntity).getMetaTileEntity();
        if (mte instanceof GT_MetaTileEntity_DigitalChestBase) {
            GT_MetaTileEntity_DigitalChestBase dc = (GT_MetaTileEntity_DigitalChestBase) mte;
            max = dc.getMaxItemCount();
            used = dc.getProgresstime();
        } else if (GregTech_API.mAE2 && mte instanceof GT_MetaTileEntity_Hatch_OutputBus_ME) {
            if (((GT_MetaTileEntity_Hatch_OutputBus_ME) mte).isLastOutputFailed()) {
                max = 64;
                used = 64;
            }
        } else {
            int[] slots = slot >= 0 ? new int[] {slot} : tileEntity.getAccessibleSlotsFromSide(side);

            for (int i : slots) {
                if (i >= 0 && i < tileEntity.getSizeInventory()) {
                    max += 64;
                    ItemStack stack = tileEntity.getStackInSlot(i);
                    if (stack != null) used += ((long) stack.stackSize << 6) / stack.getMaxStackSize();
                }
            }
        }

        return GT_Utility.convertRatioToRedstone(used, max, threshold, inverted);
    }

    @Override
    protected ItemMeterData doCoverThingsImpl(
            byte aSide,
            byte aInputRedstone,
            int aCoverID,
            ItemMeterData aCoverVariable,
            ICoverable aTileEntity,
            long aTimer) {
        byte signal = computeSignalBasedOnItems(
                aTileEntity, aCoverVariable.inverted, aCoverVariable.threshold, aCoverVariable.slot, aSide);
        aTileEntity.setOutputRedstoneSignal(aSide, signal);

        return aCoverVariable;
    }

    @Override
    protected ItemMeterData onCoverScrewdriverClickImpl(
            byte aSide,
            int aCoverID,
            ItemMeterData aCoverVariable,
            ICoverable aTileEntity,
            EntityPlayer aPlayer,
            float aX,
            float aY,
            float aZ) {
        if (aPlayer.isSneaking()) {
            if (aCoverVariable.inverted) {
                aCoverVariable.inverted = false;
                GT_Utility.sendChatToPlayer(aPlayer, GT_Utility.trans("055", "Normal"));
            } else {
                aCoverVariable.inverted = true;
                GT_Utility.sendChatToPlayer(aPlayer, GT_Utility.trans("054", "Inverted"));
            }
        } else {
            aCoverVariable.slot++;
            if (aCoverVariable.slot > aTileEntity.getSizeInventory()) aCoverVariable.slot = -1;

            if (aCoverVariable.slot == -1)
                GT_Utility.sendChatToPlayer(
                        aPlayer, GT_Utility.trans("053", "Slot: ") + GT_Utility.trans("ALL", "All"));
            else GT_Utility.sendChatToPlayer(aPlayer, GT_Utility.trans("053", "Slot: ") + aCoverVariable.slot);
        }

        return aCoverVariable;
    }

    @Override
    protected boolean letsEnergyInImpl(byte aSide, int aCoverID, ItemMeterData aCoverVariable, ICoverable aTileEntity) {
        return true;
    }

    @Override
    protected boolean letsEnergyOutImpl(
            byte aSide, int aCoverID, ItemMeterData aCoverVariable, ICoverable aTileEntity) {
        return true;
    }

    @Override
    protected boolean letsFluidInImpl(
            byte aSide, int aCoverID, ItemMeterData aCoverVariable, Fluid aFluid, ICoverable aTileEntity) {
        return true;
    }

    @Override
    protected boolean letsFluidOutImpl(
            byte aSide, int aCoverID, ItemMeterData aCoverVariable, Fluid aFluid, ICoverable aTileEntity) {
        return true;
    }

    @Override
    protected boolean letsItemsInImpl(
            byte aSide, int aCoverID, ItemMeterData aCoverVariable, int aSlot, ICoverable aTileEntity) {
        return true;
    }

    @Override
    protected boolean letsItemsOutImpl(
            byte aSide, int aCoverID, ItemMeterData aCoverVariable, int aSlot, ICoverable aTileEntity) {
        return true;
    }

    @Override
    protected boolean manipulatesSidedRedstoneOutputImpl(
            byte aSide, int aCoverID, ItemMeterData aCoverVariable, ICoverable aTileEntity) {
        return true;
    }

    @Override
    protected int getTickRateImpl(byte aSide, int aCoverID, ItemMeterData aCoverVariable, ICoverable aTileEntity) {
        return 5;
    }

    // GUI stuff

    @Override
    public boolean hasCoverGUI() {
        return true;
    }

    @Override
    public boolean useModularUI() {
        return true;
    }

    @Override
    public ModularWindow createWindow(GT_CoverUIBuildContext buildContext) {
        return new ItemMeterUIFactory(buildContext).createWindow();
    }

    private class ItemMeterUIFactory extends UIFactory {

        private static final int startX = 10;
        private static final int startY = 25;
        private static final int spaceX = 18;
        private static final int spaceY = 18;
        private static final String ALL_TEXT = "All";

        private int maxSlot;

        public ItemMeterUIFactory(GT_CoverUIBuildContext buildContext) {
            super(buildContext);
        }

        @Override
        protected void addUIWidgets(ModularWindow.Builder builder) {
            final String INVERTED = GT_Utility.trans("INVERTED", "Inverted");
            final String NORMAL = GT_Utility.trans("NORMAL", "Normal");

            maxSlot = getMaxSlot();

            builder.widget(new CoverDataControllerWidget<>(
                                    this::getCoverData, this::setCoverData, GT_Cover_ItemMeter.this)
                            .addFollower(
                                    CoverDataFollower_ToggleButtonWidget.ofRedstone(),
                                    coverData -> coverData.inverted,
                                    (coverData, state) -> {
                                        coverData.inverted = state;
                                        return coverData;
                                    },
                                    widget -> widget.addTooltip(0, NORMAL)
                                            .addTooltip(1, INVERTED)
                                            .setPos(0, 0))
                            .addFollower(
                                    new CoverDataFollower_TextFieldWidget<>(),
                                    coverData -> getSlotTextFieldContent(coverData.slot),
                                    (coverData, state) -> {
                                        coverData.slot = getIntFromText(state);
                                        return coverData;
                                    },
                                    widget -> widget.setOnScrollText()
                                            .setValidator(val -> {
                                                final int valSlot = getIntFromText(val);
                                                if (valSlot > -1) {
                                                    return TextFieldWidget.format.format(Math.min(valSlot, maxSlot));
                                                } else {
                                                    return ALL_TEXT;
                                                }
                                            })
                                            .setPattern(BaseTextFieldWidget.NATURAL_NUMS)
                                            .setFocusOnGuiOpen(true)
                                            .setPos(0, spaceY + 2)
                                            .setSize(spaceX * 2 + 5, 12))
                            .addFollower(
                                    new CoverDataFollower_TextFieldWidget<>(),
                                    coverData -> String.valueOf(coverData.threshold),
                                    (coverData, state) -> {
                                        coverData.threshold = (int) MathExpression.parseMathExpression(state);
                                        return coverData;
                                    },
                                    widget -> widget.setOnScrollNumbers(1, 10, 64)
                                            .setNumbers(0, getUpperBound())
                                            .setPos(0, spaceY * 2 + 2)
                                            .setSize(spaceX * 2 + 5, 12))
                            .setPos(startX, startY))
                    .widget(new ItemWatcherSlotWidget()
                            .setGetter(this::getTargetItem)
                            .setPos(startX + spaceX * 8 - 4, startY + spaceY))
                    .widget(TextWidget.dynamicString(
                                    () -> getCoverData() != null ? getCoverData().inverted ? INVERTED : NORMAL : "")
                            .setSynced(false)
                            .setDefaultColor(COLOR_TEXT_GRAY.get())
                            .setPos(startX + spaceX * 3, 4 + startY))
                    .widget(new TextWidget(GT_Utility.trans("254", "Detect slot#"))
                            .setDefaultColor(COLOR_TEXT_GRAY.get())
                            .setPos(startX + spaceX * 3, 4 + startY + spaceY))
                    .widget(new TextWidget(GT_Utility.trans("221", "Item threshold"))
                            .setDefaultColor(COLOR_TEXT_GRAY.get())
                            .setPos(startX + spaceX * 3, startY + spaceY * 2 + 4));
        }

        private int getMaxSlot() {
            final ICoverable tile = getUIBuildContext().getTile();
            if (tile instanceof TileEntity
                    && !tile.isDead()
                    && tile instanceof IGregTechTileEntity
                    && !(((IGregTechTileEntity) tile).getMetaTileEntity()
                            instanceof GT_MetaTileEntity_DigitalChestBase))
                return Math.min(tile.getSizeInventory() - 1, SLOT_MASK - 1);
            else return -1;
        }

        private int getIntFromText(String text) {
            try {
                return (int) MathExpression.parseMathExpression(text, -1);
            } catch (Exception e) {
                return -1;
            }
        }

        private String getSlotTextFieldContent(int val) {
            return val < 0 ? ALL_TEXT : String.valueOf(val);
        }

        private int getUpperBound() {
            return maxSlot > 0 ? maxSlot * 64 : 999_999;
        }

        private ItemStack getTargetItem() {
            ItemMeterData coverVariable = getCoverData();
            if (coverVariable == null || coverVariable.slot < 0) {
                return null;
            }
            ICoverable tile = getUIBuildContext().getTile();
            if (tile instanceof TileEntity && !tile.isDead()) {
                if (tile.getSizeInventory() >= coverVariable.slot) {
                    return tile.getStackInSlot(coverVariable.slot);
                }
            }
            return null;
        }
    }

    @Override
    protected Object getClientGUIImpl(
            byte aSide,
            int aCoverID,
            ItemMeterData coverData,
            ICoverable aTileEntity,
            EntityPlayer aPlayer,
            World aWorld) {
        return new GUI(aSide, aCoverID, coverData, aTileEntity);
    }

    public static class ItemMeterData implements ISerializableObject {
        private boolean inverted;
        /** The special value {@code -1} means all slots. */
        private int slot;
        /** The special value {@code 0} means threshold check is disabled. */
        private int threshold;

        public ItemMeterData() {
            inverted = false;
            slot = -1;
            threshold = 0;
        }

        public ItemMeterData(boolean inverted, int slot, int threshold) {
            this.inverted = inverted;
            this.slot = slot;
            this.threshold = threshold;
        }

        @Nonnull
        @Override
        public ISerializableObject copy() {
            return new ItemMeterData(inverted, slot, threshold);
        }

        @Nonnull
        @Override
        public NBTBase saveDataToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("invert", inverted);
            tag.setInteger("slot", slot);
            tag.setInteger("threshold", threshold);
            return tag;
        }

        @Override
        public void writeToByteBuf(ByteBuf aBuf) {
            aBuf.writeBoolean(inverted);
            aBuf.writeInt(slot);
            aBuf.writeInt(threshold);
        }

        @Override
        public void loadDataFromNBT(NBTBase aNBT) {
            NBTTagCompound tag = (NBTTagCompound) aNBT;
            inverted = tag.getBoolean("invert");
            slot = tag.getInteger("slot");
            threshold = tag.getInteger("threshold");
        }

        @Nonnull
        @Override
        public ISerializableObject readFromPacket(ByteArrayDataInput aBuf, EntityPlayerMP aPlayer) {
            inverted = aBuf.readBoolean();
            slot = aBuf.readInt();
            threshold = aBuf.readInt();
            return this;
        }
    }

    private class GUI extends GT_GUICover {
        private final byte side;
        private final int coverID;
        private final GT_GuiIconCheckButton invertedButton;
        private final GT_GuiIntegerTextBox intSlot;
        private final GT_GuiFakeItemButton intSlotIcon;
        private final GT_GuiIntegerTextBox thresholdSlot;
        private final ItemMeterData coverVariable;

        private final int maxSlot;

        private static final int startX = 10;
        private static final int startY = 25;
        private static final int spaceX = 18;
        private static final int spaceY = 18;

        private final String ALL = GT_Utility.trans("ALL", "All");
        private final String INVERTED = GT_Utility.trans("INVERTED", "Inverted");
        private final String NORMAL = GT_Utility.trans("NORMAL", "Normal");

        private final int textColor = this.getTextColorOrDefault("text", 0xFF555555);

        public GUI(byte aSide, int aCoverID, ItemMeterData aCoverVariable, ICoverable aTileEntity) {
            super(aTileEntity, 176, 107, GT_Utility.intToStack(aCoverID));
            this.side = aSide;
            this.coverID = aCoverID;
            this.coverVariable = aCoverVariable;

            invertedButton = new GT_GuiIconCheckButton(
                    this, 0, startX, startY, GT_GuiIcon.REDSTONE_ON, GT_GuiIcon.REDSTONE_OFF, INVERTED, NORMAL);

            intSlot = new GT_GuiIntegerTextBox(this, 1, startX, startY + spaceY + 2, spaceX * 2 + 5, 12);
            intSlot.setMaxStringLength(6);

            // only shows if opened gui of block sadly, should've used container.
            intSlotIcon =
                    new GT_GuiFakeItemButton(this, startX + spaceX * 8 - 4, startY + spaceY, GT_GuiIcon.SLOT_GRAY);
            intSlotIcon.setMimicSlot(true);

            if (tile instanceof TileEntity
                    && !super.tile.isDead()
                    && tile instanceof IGregTechTileEntity
                    && !(((IGregTechTileEntity) tile).getMetaTileEntity()
                            instanceof GT_MetaTileEntity_DigitalChestBase))
                maxSlot = Math.min(tile.getSizeInventory() - 1, SLOT_MASK - 1);
            else maxSlot = -1;

            intSlot.setEnabled(maxSlot >= 0);

            thresholdSlot = new GT_GuiIntegerTextBox(this, 2, startX, startY + spaceY * 2 + 2, spaceX * 2 + 5, 12);
            thresholdSlot.setMaxStringLength(6);
        }

        @Override
        public void drawExtras(int mouseX, int mouseY, float parTicks) {
            super.drawExtras(mouseX, mouseY, parTicks);
            this.getFontRenderer()
                    .drawString(coverVariable.inverted ? INVERTED : NORMAL, startX + spaceX * 3, 4 + startY, textColor);
            this.getFontRenderer()
                    .drawString(
                            GT_Utility.trans("254", "Detect slot#"),
                            startX + spaceX * 3,
                            4 + startY + spaceY,
                            textColor);
            this.getFontRenderer()
                    .drawString(
                            GT_Utility.trans("221", "Item threshold"),
                            startX + spaceX * 3,
                            startY + spaceY * 2 + 4,
                            textColor);
        }

        @Override
        protected void onInitGui(int guiLeft, int guiTop, int gui_width, int gui_height) {
            update();
            if (intSlot.isEnabled()) intSlot.setFocused(true);
        }

        @Override
        public void buttonClicked(GuiButton btn) {
            coverVariable.inverted = !coverVariable.inverted;
            GT_Values.NW.sendToServer(new GT_Packet_TileEntityCoverNew(side, coverID, coverVariable, tile));
            update();
        }

        @Override
        public void onMouseWheel(int x, int y, int delta) {
            if (intSlot.isFocused()) {
                int step = Math.max(1, Math.abs(delta / 120));
                step = (isShiftKeyDown() ? 50 : isCtrlKeyDown() ? 5 : 1) * (delta > 0 ? step : -step);
                int val = parseTextBox(intSlot);

                if (val < 0) val = -1;

                val = val + step;

                if (val < 0) val = -1;
                else if (val > maxSlot) val = maxSlot;

                intSlot.setText(val < 0 ? ALL : Integer.toString(val));
            } else if (thresholdSlot.isFocused()) {
                int val = parseTextBox(thresholdSlot);

                int step = 1;
                if (isShiftKeyDown()) {
                    step *= 64;
                }
                if (isCtrlKeyDown()) {
                    step *= 10;
                }

                val += step * Integer.signum(delta);

                val = GT_Utility.clamp(val, 0, getUpperBound());
                thresholdSlot.setText(Integer.toString(val));
            }
        }

        @Override
        public void applyTextBox(GT_GuiIntegerTextBox box) {
            if (box == intSlot) {
                coverVariable.slot = parseTextBox(box);
            } else if (box == thresholdSlot) {
                coverVariable.threshold = parseTextBox(thresholdSlot);
            }

            GT_Values.NW.sendToServer(new GT_Packet_TileEntityCoverNew(side, coverID, coverVariable, tile));
            update();
        }

        @Override
        public void resetTextBox(GT_GuiIntegerTextBox box) {
            if (box == intSlot) {
                intSlot.setText(coverVariable.slot < 0 ? ALL : Integer.toString(coverVariable.slot));
            } else if (box == thresholdSlot) {
                thresholdSlot.setText(Integer.toString(coverVariable.threshold));
            }
        }

        private void update() {
            invertedButton.setChecked(coverVariable.inverted);
            resetTextBox(intSlot);
            resetTextBox(thresholdSlot);

            if (coverVariable.slot < 0) {
                intSlotIcon.setItem(null);
                return;
            }
            if (tile instanceof TileEntity && !super.tile.isDead()) {
                if (tile.getSizeInventory() >= coverVariable.slot) {
                    ItemStack item = tile.getStackInSlot(coverVariable.slot);
                    intSlotIcon.setItem(item);
                    return;
                }
            }
            intSlotIcon.setItem(null);
        }

        private int parseTextBox(GT_GuiIntegerTextBox box) {
            if (box == intSlot) {
                String text = box.getText();
                if (text == null) return -1;
                text = text.trim();
                if (text.startsWith(ALL)) text = text.substring(ALL.length());

                if (text.isEmpty()) return -1;

                int val;
                try {
                    val = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    return -1;
                }

                if (val < 0) return -1;
                else if (maxSlot < val) return maxSlot;
                return val;
            } else if (box == thresholdSlot) {
                String text = box.getText();
                if (text == null) {
                    return 0;
                }

                int val;
                try {
                    val = Integer.parseInt(text.trim());
                } catch (NumberFormatException e) {
                    return 0;
                }

                return GT_Utility.clamp(val, 0, getUpperBound());
            }

            throw new UnsupportedOperationException("Unknown text box: " + box);
        }

        private int getUpperBound() {
            return maxSlot > 0 ? maxSlot * 64 : 999_999;
        }
    }
}
