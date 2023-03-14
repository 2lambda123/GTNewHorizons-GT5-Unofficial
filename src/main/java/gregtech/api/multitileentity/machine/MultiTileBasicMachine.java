package gregtech.api.multitileentity.machine;

import static com.google.common.primitives.Ints.saturatedCast;
import static gregtech.api.enums.GT_Values.B;
import static gregtech.api.enums.GT_Values.emptyIconContainerArray;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import com.gtnewhorizons.modularui.api.forge.IItemHandlerModifiable;
import com.gtnewhorizons.modularui.api.forge.ItemStackHandler;

import gregtech.api.enums.GT_Values;
import gregtech.api.enums.GT_Values.NBT;
import gregtech.api.enums.Textures;
import gregtech.api.enums.TickTime;
import gregtech.api.fluid.FluidTankGT;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IMachineProgress;
import gregtech.api.multitileentity.MultiTileEntityRegistry;
import gregtech.api.multitileentity.base.TickableMultiTileEntity;
import gregtech.api.multitileentity.interfaces.IMultiTileMachine;
import gregtech.api.net.GT_Packet_MultiTileEntity;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GT_Util;
import gregtech.api.util.GT_Utility;

public abstract class MultiTileBasicMachine extends TickableMultiTileEntity
        implements IMachineProgress, IMultiTileMachine {

    protected static final int ACTIVE = B[0];
    protected static final int TICKS_BETWEEN_RECIPE_CHECKS = 5 * TickTime.SECOND;

    protected static final IItemHandlerModifiable EMPTY_INVENTORY = new ItemStackHandler(0);

    private static final String TEXTURE_LOCATION = "multitileentity/machines/";
    public IIconContainer[] texturesInactive = emptyIconContainerArray;
    public IIconContainer[] texturesActive = emptyIconContainerArray;

    protected int maxParallel = 1;
    protected boolean active = false;
    protected long storedEnergy = 0;
    protected long maximumEnergyStored = 0;
    protected long voltage = 0;
    protected long amperage = 2;
    protected long eut = 0;
    protected int tier = 0;
    protected int maxProgressTime = 0;
    protected int progressTime = 0;
    protected long burnTime = 0;
    protected long totalBurnTime = 0;
    protected FluidTankGT[] inputTanks = GT_Values.emptyFluidTankGT;
    protected FluidTankGT[] outputTanks = GT_Values.emptyFluidTankGT;
    protected FluidStack[] fluidToOutput = GT_Values.emptyFluidStack;
    protected ItemStack[] itemsToOutput = GT_Values.emptyItemStackArray;

    protected IItemHandlerModifiable inputInventory = EMPTY_INVENTORY;
    protected IItemHandlerModifiable outputInventory = EMPTY_INVENTORY;
    protected boolean outputInventoryChanged = false;
    private boolean powerShutDown = false;
    private boolean wasEnabled = false;
    private boolean canWork = true;
    private boolean isElectric = true;
    private boolean isSteam = false;
    private boolean acceptsFuel = false;
    private boolean isWireless = false;

    @Override
    public String getTileEntityName() {
        return "gt.multitileentity.machine.basic";
    }

    @Override
    public void writeMultiTileNBT(NBTTagCompound nbt) {
        super.writeMultiTileNBT(nbt);
        if (maxParallel > 0) {
            nbt.setInteger(NBT.PARALLEL, maxParallel);
        }

        if (active) {
            nbt.setBoolean(NBT.ACTIVE, active);
        }

        if (inputInventory != null && inputInventory.getSlots() > 0) {
            writeInventory(nbt, inputInventory, NBT.INV_INPUT_LIST);
        }

        if (outputInventory != null && outputInventory.getSlots() > 0) {
            writeInventory(nbt, outputInventory, NBT.INV_OUTPUT_LIST);
        }

        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i].writeToNBT(nbt, NBT.TANK_IN + i);
        }

        for (int i = 0; i < outputTanks.length; i++) {
            outputTanks[i].writeToNBT(nbt, NBT.TANK_OUT + i);
        }

        if (fluidToOutput != null && fluidToOutput.length > 0) {
            writeFluids(nbt, fluidToOutput, NBT.FLUID_OUT);
        }

        nbt.setInteger(NBT.TIER, tier);
        nbt.setLong(NBT.VOLTAGE, voltage);
        nbt.setLong(NBT.AMPERAGE, amperage);
        nbt.setLong(NBT.EUT_CONSUMPTION, eut);
        nbt.setLong(NBT.STORED_ENERGY, storedEnergy);
        nbt.setLong(NBT.MAXIMUM_ENERGY, maximumEnergyStored);
        nbt.setLong(NBT.BURN_TIME_LEFT, burnTime);
        nbt.setLong(NBT.TOTAL_BURN_TIME, totalBurnTime);
        nbt.setBoolean(NBT.ALLOWED_WORK, canWork);
        nbt.setBoolean(NBT.ACTIVE, active);
    }

    protected void writeFluids(NBTTagCompound nbt, FluidStack[] fluids, String fluidListTag) {
        if (fluids != null && fluids.length > 0) {
            final NBTTagList tList = new NBTTagList();
            for (final FluidStack tFluid : fluids) {
                if (tFluid != null) {
                    final NBTTagCompound tag = new NBTTagCompound();
                    tFluid.writeToNBT(tag);
                    tList.appendTag(tag);
                }
            }
            nbt.setTag(fluidListTag, tList);
        }
    }

    protected void writeInventory(NBTTagCompound nbt, IItemHandlerModifiable inv, String invListTag) {
        if (inv != null && inv.getSlots() > 0) {
            final NBTTagList tList = new NBTTagList();
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                final ItemStack tStack = inv.getStackInSlot(slot);
                if (tStack != null) {
                    final NBTTagCompound tag = new NBTTagCompound();
                    tag.setByte("s", (byte) slot);
                    tStack.writeToNBT(tag);
                    tList.appendTag(tag);
                }
            }
            nbt.setTag(invListTag, tList);
        }
    }

    @Override
    public void readMultiTileNBT(NBTTagCompound nbt) {
        super.readMultiTileNBT(nbt);
        if (nbt.hasKey(NBT.PARALLEL)) {
            maxParallel = Math.max(1, nbt.getInteger(NBT.PARALLEL));
        }

        if (nbt.hasKey(NBT.ACTIVE)) {
            active = nbt.getBoolean(NBT.ACTIVE);
        }

        /* Inventories */
        inputInventory = new ItemStackHandler(Math.max(nbt.getInteger(NBT.INV_INPUT_SIZE), 0));
        outputInventory = new ItemStackHandler(Math.max(nbt.getInteger(NBT.INV_OUTPUT_SIZE), 0));
        loadInventory(nbt, inputInventory, NBT.INV_INPUT_LIST);
        loadInventory(nbt, outputInventory, NBT.INV_OUTPUT_LIST);

        /* Tanks */
        long capacity = 1000;
        if (nbt.hasKey(NBT.TANK_CAPACITY)) {
            capacity = saturatedCast(nbt.getLong(NBT.TANK_CAPACITY));
        }

        inputTanks = new FluidTankGT[getFluidInputCount()];
        outputTanks = new FluidTankGT[getFluidOutputCount()];
        fluidToOutput = new FluidStack[getFluidOutputCount()];

        // TODO: See if we need the adjustable map here `.setCapacity(mRecipes, mParallel * 2L)` in place of the
        // `setCapacityMultiplier`
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i] = new FluidTankGT(capacity).setCapacityMultiplier(maxParallel * 2L)
                    .readFromNBT(nbt, NBT.TANK_IN + i);
        }
        for (int i = 0; i < outputTanks.length; i++) {
            outputTanks[i] = new FluidTankGT().readFromNBT(nbt, NBT.TANK_OUT + i);
        }

        for (int i = 0; i < fluidToOutput.length; i++) {
            fluidToOutput[i] = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag(NBT.FLUID_OUT + "." + i));
        }

        tier = nbt.getInteger(NBT.TIER);
        voltage = nbt.getLong(NBT.VOLTAGE);
        amperage = nbt.getLong(NBT.AMPERAGE);
        eut = nbt.getLong(NBT.EUT_CONSUMPTION);
        storedEnergy = nbt.getLong(NBT.STORED_ENERGY);
        maximumEnergyStored = nbt.getLong(NBT.MAXIMUM_ENERGY);
        burnTime = nbt.getLong(NBT.BURN_TIME_LEFT);
        totalBurnTime = nbt.getLong(NBT.TOTAL_BURN_TIME);
        canWork = nbt.getBoolean(NBT.ALLOWED_WORK);
        active = nbt.getBoolean(NBT.ACTIVE);
    }

    protected void loadInventory(NBTTagCompound aNBT, IItemHandlerModifiable inv, String invListTag) {
        final NBTTagList tList = aNBT.getTagList(invListTag, 10);
        for (int i = 0; i < tList.tagCount(); i++) {
            final NBTTagCompound tNBT = tList.getCompoundTagAt(i);
            final int tSlot = tNBT.getShort("s");
            if (tSlot >= 0 && tSlot < inv.getSlots()) inv.setStackInSlot(tSlot, GT_Utility.loadItem(tNBT));
        }
    }

    @Override
    public void loadTextureNBT(NBTTagCompound aNBT) {
        // Loading the registry
        final String textureName = aNBT.getString(NBT.TEXTURE);
        textures = new IIconContainer[] {
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/bottom"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/top"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/left"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/front"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/right"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/side") };
        texturesInactive = new IIconContainer[] {
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/inactive/bottom"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/inactive/top"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/inactive/left"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/inactive/front"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/inactive/right"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/inactive/back") };
        texturesActive = new IIconContainer[] {
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/active/bottom"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/active/top"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/active/left"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/active/front"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/active/right"),
                new Textures.BlockIcons.CustomIcon(TEXTURE_LOCATION + textureName + "/overlay/active/back") };
    }

    @Override
    public void copyTextures() {
        // Loading an instance
        final TileEntity tCanonicalTileEntity = MultiTileEntityRegistry
                .getCanonicalTileEntity(getMultiTileEntityRegistryID(), getMultiTileEntityID());
        if (tCanonicalTileEntity instanceof MultiTileBasicMachine) {
            textures = ((MultiTileBasicMachine) tCanonicalTileEntity).textures;
            texturesInactive = ((MultiTileBasicMachine) tCanonicalTileEntity).texturesInactive;
            texturesActive = ((MultiTileBasicMachine) tCanonicalTileEntity).texturesActive;
        } else {
            textures = texturesInactive = texturesActive = emptyIconContainerArray;
        }
    }

    @Override
    public ITexture[] getTexture(Block aBlock, byte aSide, boolean isActive, int aRenderPass) {
        if (aSide != facing) {
            return new ITexture[] { TextureFactory
                    .of(textures[GT_Values.FACING_ROTATIONS[facing][aSide]], GT_Util.getRGBaArray(rgba)) };
        }
        return new ITexture[] {
                TextureFactory.of(textures[GT_Values.FACING_ROTATIONS[facing][aSide]], GT_Util.getRGBaArray(rgba)),
                TextureFactory
                        .of((active ? texturesActive : texturesInactive)[GT_Values.FACING_ROTATIONS[facing][aSide]]) };
    }

    @Override
    public GT_Packet_MultiTileEntity getClientDataPacket() {
        final GT_Packet_MultiTileEntity packet = super.getClientDataPacket();
        int booleans = getBooleans();
        packet.setBooleans(booleans);
        return packet;
    }

    /*
     * Fluids
     */

    /**
     * The number of fluid (input) slots available for this machine
     */
    public int getFluidInputCount() {
        return 7;
    }

    /**
     * The number of fluid (output) slots available for this machine
     */
    public int getFluidOutputCount() {
        return 3;
    }

    @Override
    public void setLightValue(byte aLightValue) {}

    @Override
    public String getInventoryName() {
        final String name = getCustomName();
        if (name != null) return name;
        final MultiTileEntityRegistry tRegistry = MultiTileEntityRegistry.getRegistry(getMultiTileEntityRegistryID());
        return tRegistry == null ? getClass().getName() : tRegistry.getLocal(getMultiTileEntityID());
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer aPlayer) {
        return playerOwnsThis(aPlayer, false) && mTickTimer > 40
                && getTileEntityOffset(0, 0, 0) == this
                && aPlayer.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) < 64
                && allowInteraction(aPlayer);
    }

    @Override
    public boolean isLiquidInput(byte aSide) {
        return aSide != facing;
    }

    @Override
    public boolean isLiquidOutput(byte aSide) {
        return aSide != facing;
    }

    @Override
    protected IFluidTank[] getFluidTanks(byte aSide) {
        final boolean fluidInput = isLiquidInput(aSide);
        final boolean fluidOutput = isLiquidOutput(aSide);

        if (fluidInput && fluidOutput) {
            final IFluidTank[] rTanks = new IFluidTank[inputTanks.length + outputTanks.length];
            System.arraycopy(inputTanks, 0, rTanks, 0, inputTanks.length);
            System.arraycopy(outputTanks, 0, rTanks, inputTanks.length, outputTanks.length);
            return rTanks;
        } else if (fluidInput) {
            return inputTanks;
        } else if (fluidOutput) {
            return outputTanks;
        }
        return GT_Values.emptyFluidTank;
    }

    @Override
    public IFluidTank getFluidTankFillable(byte aSide, FluidStack aFluidToFill) {
        if (!isLiquidInput(aSide)) return null;
        for (FluidTankGT tankGT : inputTanks) if (tankGT.contains(aFluidToFill)) return tankGT;
        // if (!mRecipes.containsInput(aFluidToFill, this, slot(mRecipes.mInputItemsCount +
        // mRecipes.mOutputItemsCount))) return null;
        for (FluidTankGT fluidTankGT : inputTanks) if (fluidTankGT.isEmpty()) return fluidTankGT;
        return null;
    }

    @Override
    protected IFluidTank getFluidTankDrainable(byte aSide, FluidStack aFluidToDrain) {
        if (!isLiquidOutput(aSide)) return null;
        for (FluidTankGT fluidTankGT : outputTanks)
            if (aFluidToDrain == null ? fluidTankGT.has() : fluidTankGT.contains(aFluidToDrain)) return fluidTankGT;

        return null;
    }

    /*
     * Inventory
     */

    @Override
    public boolean hasInventoryBeenModified() {
        // True if the input inventory has changed
        return hasInventoryChanged;
    }

    public void markOutputInventoryBeenModified() {
        outputInventoryChanged = true;
    }

    public boolean hasOutputInventoryBeenModified() {
        // True if the output inventory has changed
        return outputInventoryChanged;
    }

    public void markInputInventoryBeenModified() {
        hasInventoryChanged = true;
    }

    @Override
    public boolean isItemValidForSlot(int aSlot, ItemStack aStack) {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    // #region Machine

    @Override
    public void onPostTick(long tick, boolean isServerSide) {
        if (isServerSide) {
            runMachine(tick);
        }
    }

    protected void runMachine(long aTick) {
        if (acceptsFuel() && isActive()) {
            if (!consumeFuel()) {
                stopMachine();
                return;
            }
        }

        if (hasThingsToDo()) {
            markDirty();
            if (isElectric()) {
                if (!isGenerator() && !drainEut(eut)) {
                    stopMachine();
                    return;
                }
                if (isGenerator()) {
                    generateEut(eut);
                }
            }

            if (isSteam()) {
                return;
            }

            if (maxProgressTime > 0 && ++progressTime >= maxProgressTime) {
                progressTime = 0;
                maxProgressTime = 0;
                outputItems();
                if (isAllowedToWork()) {
                    if (!checkRecipe()) {
                        setActive(false);
                        issueClientUpdate();
                    }
                }
                updateSlots();
            }
        } else {
            if (aTick % TICKS_BETWEEN_RECIPE_CHECKS == 0 || hasWorkJustBeenEnabled() || hasInventoryBeenModified()) {
                if (isAllowedToWork()) {
                    wasEnabled = false;
                    if (checkRecipe()) {
                        setActive(true);
                        updateSlots();
                        markDirty();
                        issueClientUpdate();
                    }
                }
            }
        }
    }

    protected boolean checkRecipe() {
        return false;
    }

    protected void outputItems() {
        int index = 0;
        if (itemsToOutput == null) {
            return;
        }
        for (ItemStack item : itemsToOutput) {
            outputInventory.insertItem(index++, item, false);
        }
        itemsToOutput = null;
    }

    @Override
    public int getProgress() {
        return progressTime;
    }

    @Override
    public int getMaxProgress() {
        return maxProgressTime;
    }

    @Override
    public boolean increaseProgress(int aProgressAmountInTicks) {
        progressTime += aProgressAmountInTicks;
        return true;
    }

    @Override
    public boolean hasThingsToDo() {
        return getMaxProgress() > 0;
    }

    @Override
    public boolean hasWorkJustBeenEnabled() {
        return wasEnabled;
    }

    @Override
    public void enableWorking() {
        wasEnabled = true;
        canWork = true;
    }

    @Override
    public void disableWorking() {
        canWork = false;
    }

    @Override
    public boolean wasShutdown() {
        return powerShutDown;
    }

    @Override
    public boolean isAllowedToWork() {
        return canWork;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    protected boolean isElectric() {
        return isElectric;
    }

    protected void setElectric(boolean isElectric) {
        this.isElectric = isElectric;
    }

    protected boolean isSteam() {
        return isSteam;
    }

    protected void setSteam(boolean isSteam) {
        this.isSteam = isSteam;
    }

    protected boolean acceptsFuel() {
        return acceptsFuel;
    }

    protected void setFuel(boolean acceptsFuel) {
        this.acceptsFuel = acceptsFuel;
    }

    protected boolean isWireless() {
        return isWireless;
    }

    protected void setWireless(boolean isWireless) {
        this.isWireless = isWireless;
    }

    protected boolean drainEut(long eut) {
        return decreaseStoredEnergyUnits(eut, false);
    }

    protected boolean generateEut(long eut) {
        return increaseStoredEnergyUnits(eut, true);
    }

    protected boolean isGenerator() {
        return false;
    }

    protected boolean consumeFuel() {
        if (isActive() && burnTime <= 0) {
            for (int i = 0; i < inputInventory.getSlots(); i++) {
                if (inputInventory.getStackInSlot(i) != null) {
                    int checkBurnTime = TileEntityFurnace.getItemBurnTime(inputInventory.getStackInSlot(i)) / 10;
                    if (checkBurnTime <= 0) continue;
                    inputInventory.getStackInSlot(i).stackSize--;
                    burnTime = checkBurnTime;
                    totalBurnTime = checkBurnTime;
                    break;
                }
            }
            updateSlots();
        }

        if (--burnTime < 0) {
            burnTime = 0;
            totalBurnTime = 0;
            return false;
        }

        return true;
    }

    @Override
    protected void addDebugInfo(EntityPlayer player, int logLevel, ArrayList<String> list) {
        if (isElectric()) {
            list.add(
                    "Energy: " + EnumChatFormatting.GOLD
                            + getUniversalEnergyStored()
                            + "/"
                            + getUniversalEnergyCapacity());
        }

        if (acceptsFuel()) {
            list.add("Fuel: " + EnumChatFormatting.GOLD + burnTime + "/" + totalBurnTime);
        }
    }

    protected void stopMachine() {
        progressTime = 0;
        setActive(false);
        disableWorking();
    }

    protected void updateSlots() {
        for (int i = 0; i < inputInventory.getSlots(); i++) {
            ItemStack item = inputInventory.getStackInSlot(i);
            if (item != null && item.stackSize <= 0) {
                inputInventory.setStackInSlot(i, null);
            }
        }
    }

    /**
     * Must always be a positive. If the multi generates Eu/t isGenerator() should be overridden to true
     */
    protected void setEut(long eut) {
        if (eut < 0) {
            eut = -eut;
        }

        this.eut = eut;
    }

    protected void setDuration(int duration) {
        if (duration < 0) {
            duration = -duration;
        }

        maxProgressTime = duration;
    }

    @Override
    public int getBooleans() {
        int booleans = 0;
        if (isActive()) {
            booleans |= ACTIVE;
        }
        return booleans;
    }

    @Override
    public void setBooleans(int booleans) {
        if ((booleans & ACTIVE) == ACTIVE) {
            setActive(true);
        }
    }

    protected boolean hasItemInput() {
        return true;
    }

    protected boolean hasItemOutput() {
        return true;
    }

    protected boolean hasFluidInput() {
        return true;
    }

    protected boolean hasFluidOutput() {
        return true;
    }

    protected void setItemOutputs(ItemStack... outputs) {
        itemsToOutput = outputs;
    }
}
