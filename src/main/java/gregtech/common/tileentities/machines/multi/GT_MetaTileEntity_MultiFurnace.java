package gregtech.common.tileentities.machines.multi;

import gregtech.api.GregTech_API;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.enums.Textures;
import gregtech.api.gui.GT_GUIContainer_MultiMachine;
import gregtech.api.interfaces.IHeatingCoil;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Energy;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;
import gregtech.api.objects.GT_RenderedTexture;
import gregtech.api.util.GT_ModHandler;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;

import static gregtech.api.enums.GT_Values.VN;

public class GT_MetaTileEntity_MultiFurnace
        extends GT_MetaTileEntity_AbstractMultiFurnace {
    private int mLevel = 0;
    private int mCostDiscount = 1;

    private static final int CASING_INDEX = 11;

    public GT_MetaTileEntity_MultiFurnace(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT_MetaTileEntity_MultiFurnace(String aName) {
        super(aName);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT_MetaTileEntity_MultiFurnace(this.mName);
    }

    @Override
    public String[] getDescription() {
    	final GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
		tt.addMachineType("Furnace")
		.addInfo("Controller Block for the Multi Smelter")
		.addInfo("Smelts up to 8-128 items at once")
		.addInfo("Items smelted increases with coil tier")
		.addPollutionAmount(20 * getPollutionPerTick(null))
		.addSeparator()
		.beginStructureBlock(3, 3, 3, true)
		.addController("Front bottom")
		.addCasingInfo("Heat Proof Machine Casing", 8)
		.addOtherStructurePart("Heating Coils", "Middle layer")
		.addEnergyHatch("Any bottom casing")
		.addMaintenanceHatch("Any bottom casing")
		.addMufflerHatch("Top Middle")
		.addInputBus("Any bottom casing")
		.addOutputBus("Any bottom casing")
		.toolTipFinisher("Gregtech");
		if (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
		    return tt.getInformation();
        return tt.getStructureInformation();
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing, byte aColorIndex, boolean aActive, boolean aRedstone) {
        if (aSide == aFacing)
            return new ITexture[]{Textures.BlockIcons.casingTexturePages[0][CASING_INDEX], new GT_RenderedTexture(aActive ? Textures.BlockIcons.OVERLAY_FRONT_MULTI_SMELTER_ACTIVE : Textures.BlockIcons.OVERLAY_FRONT_MULTI_SMELTER)};
        return new ITexture[]{Textures.BlockIcons.casingTexturePages[0][CASING_INDEX]};
    }

    @Override
    public Object getClientGUI(int aID, InventoryPlayer aPlayerInventory, IGregTechTileEntity aBaseMetaTileEntity) {
        return new GT_GUIContainer_MultiMachine(aPlayerInventory, aBaseMetaTileEntity, getLocalName(), "MultiFurnace.png");
    }

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        return GT_Recipe.GT_Recipe_Map.sFurnaceRecipes;
    }

    @Override
    public boolean checkRecipe(ItemStack aStack) {
        ArrayList<ItemStack> tInputList = getStoredInputs();
        if (tInputList.isEmpty())
            return false;

        int mVolatage = GT_Utility.safeInt(getMaxInputVoltage());
        int tMaxParrallel = 8 * this.mLevel;
        int tCurrenParrallel = 0;
        ItemStack tSmeltStack = tInputList.get(0);
        ItemStack tOutputStack = GT_ModHandler.getSmeltingOutput(tSmeltStack,false,null);
        if (tOutputStack == null)
                return false;
        for (ItemStack item : tInputList)
            if (tSmeltStack.isItemEqual(item)) if (item.stackSize < (tMaxParrallel - tCurrenParrallel)) {
                tCurrenParrallel += item.stackSize;
                item.stackSize = 0;
            } else {
                item.stackSize = (tCurrenParrallel + item.stackSize) - tMaxParrallel;
                tCurrenParrallel = tMaxParrallel;
                break;
            }
//            this.mOutputItems = new ItemStack[8 * this.mLevel];
//            for (int i = 0; (i < 256) && (j < this.mOutputItems.length); i++) {
//                if (null != (this.mOutputItems[j] = GT_ModHandler.getSmeltingOutput((ItemStack) tInputList.get(i % tInputList.size()), true, null))) {
//                    j++;
//                }
//            }
        tCurrenParrallel *= tOutputStack.stackSize;
        this.mOutputItems = new ItemStack[(tCurrenParrallel/64)+1];
        for (int i = 0; i < this.mOutputItems.length; i++) {
            ItemStack tNewStack = tOutputStack.copy();
            int size = Math.min(tCurrenParrallel, 64);
            tNewStack.stackSize = size;
            tCurrenParrallel -= size;
            this.mOutputItems[i] = tNewStack;
        }

        if (this.mOutputItems.length > 0) {
            this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
            this.mEfficiencyIncrease = 10000;
            calculateOverclockedNessMulti(4, 512, 1, mVolatage);
            //In case recipe is too OP for that machine
            if (mMaxProgresstime == Integer.MAX_VALUE - 1 && mEUt == Integer.MAX_VALUE - 1)
                return false;

            this.mEUt = GT_Utility.safeInt(((long)mEUt) * this.mLevel / (long)this.mCostDiscount,1);
            if (mEUt == Integer.MAX_VALUE - 1)
                return false;

            if (this.mEUt > 0)
                this.mEUt = (-this.mEUt);
        }
        updateSlots();
        return true;
    }

    private boolean checkMachineFunction(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;

        this.mLevel = 0;
        this.mCostDiscount = 1;

        replaceDeprecatedCoils(aBaseMetaTileEntity);
        HeatingCoilLevel heatingCap = getInitialHeatLevel(aBaseMetaTileEntity, xDir, zDir);
        if (heatingCap == null)
            return false;

        if (!checkStructure(heatingCap, xDir, zDir, aBaseMetaTileEntity))
            return false;

        this.mLevel = heatingCap.getLevel();
        this.mCostDiscount = heatingCap.getCostDiscount();
        return true;
    }

    @Override
    protected boolean checkCoils(HeatingCoilLevel heatingCap, int i, int j, int xDir, int zDir, IGregTechTileEntity aBaseMetaTileEntity) {
        if ((i == 0) && (j == 0))
            return aBaseMetaTileEntity.getAirOffset(xDir, 1, zDir);

        Block coilM = aBaseMetaTileEntity.getBlockOffset(xDir + i, 1, zDir + j);
        if (!(coilM instanceof IHeatingCoil))
            return false;
        byte usedMetaM = aBaseMetaTileEntity.getMetaIDOffset(xDir + i, 1, zDir + j);

        IHeatingCoil heatingCoilM = (IHeatingCoil) coilM;
        HeatingCoilLevel heatingLevelM = heatingCoilM.getCoilHeat(usedMetaM);

        return heatingLevelM == heatingCap;
    }

    @Override
    protected boolean checkTopLayer(int i, int j, int xDir, int zDir, IGregTechTileEntity aBaseMetaTileEntity) {
        if ((i == 0) && (j == 0)) {
            return addMufflerToMachineList(aBaseMetaTileEntity.getIGregTechTileEntityOffset(xDir, 2, zDir), CASING_INDEX);
        }
        if (aBaseMetaTileEntity.getBlockOffset(xDir + i, 2, zDir + j) != GregTech_API.sBlockCasings1)
            return false;
        return aBaseMetaTileEntity.getMetaIDOffset(xDir + i, 2, zDir + j) == CASING_INDEX;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack){
        return this.checkMachineFunction(aBaseMetaTileEntity,aStack);
    }

    private void replaceDeprecatedCoils(IGregTechTileEntity aBaseMetaTileEntity) {
        int xDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetX;
        int zDir = ForgeDirection.getOrientation(aBaseMetaTileEntity.getBackFacing()).offsetZ;
        int tX = aBaseMetaTileEntity.getXCoord() + xDir;
        int tY = aBaseMetaTileEntity.getYCoord();
        int tZ = aBaseMetaTileEntity.getZCoord() + zDir;
        int tUsedMeta;
        for (int xPos = tX - 1; xPos <= tX + 1; xPos++)
            for (int zPos = tZ - 1; zPos <= tZ + 1; zPos++) {
                if ((xPos == tX) && (zPos == tZ)) continue;
                tUsedMeta = aBaseMetaTileEntity.getMetaID(xPos, tY + 1, zPos);
                if (tUsedMeta >= 12 && tUsedMeta <= 14 && aBaseMetaTileEntity.getBlock(xPos, tY + 1, zPos) == GregTech_API.sBlockCasings1)
                    aBaseMetaTileEntity.getWorld().setBlock(xPos, tY + 1, zPos, GregTech_API.sBlockCasings5, tUsedMeta - 12, 3);
            }
    }


    @Override
    public String[] getInfoData() {
        int mPollutionReduction=0;
        for (GT_MetaTileEntity_Hatch_Muffler tHatch : mMufflerHatches)
            if (isValidMetaTileEntity(tHatch))
                mPollutionReduction = Math.max(tHatch.calculatePollutionReduction(100), mPollutionReduction);

        long storedEnergy=0;
        long maxEnergy=0;
        for(GT_MetaTileEntity_Hatch_Energy tHatch : mEnergyHatches)
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }

        return new String[]{
        		StatCollector.translateToLocal("GT5U.multiblock.Progress")+": "+
                EnumChatFormatting.GREEN + Integer.toString(mProgresstime/20) + EnumChatFormatting.RESET +" s / "+
                        EnumChatFormatting.YELLOW + Integer.toString(mMaxProgresstime/20) + EnumChatFormatting.RESET +" s",
                StatCollector.translateToLocal("GT5U.multiblock.energy")+": "+
                EnumChatFormatting.GREEN + Long.toString(storedEnergy) + EnumChatFormatting.RESET +" EU / "+
                        EnumChatFormatting.YELLOW + Long.toString(maxEnergy) + EnumChatFormatting.RESET +" EU",
                StatCollector.translateToLocal("GT5U.multiblock.usage")+": "+
                        EnumChatFormatting.RED + Integer.toString(-mEUt) + EnumChatFormatting.RESET + " EU/t",
                StatCollector.translateToLocal("GT5U.multiblock.mei")+": "+
                        EnumChatFormatting.YELLOW+Long.toString(getMaxInputVoltage())+EnumChatFormatting.RESET+" EU/t(*2A) "+StatCollector.translateToLocal("GT5U.machines.tier")+": "+
                        EnumChatFormatting.YELLOW+VN[GT_Utility.getTier(getMaxInputVoltage())]+ EnumChatFormatting.RESET,
                StatCollector.translateToLocal("GT5U.multiblock.problems")+": "+
                        EnumChatFormatting.RED+ (getIdealStatus() - getRepairStatus())+EnumChatFormatting.RESET+
                        " "+StatCollector.translateToLocal("GT5U.multiblock.efficiency")+": "+
                        EnumChatFormatting.YELLOW+Float.toString(mEfficiency / 100.0F)+EnumChatFormatting.RESET + " %",
                StatCollector.translateToLocal("GT5U.MS.multismelting")+": "+
                        EnumChatFormatting.GREEN+mLevel*8+EnumChatFormatting.RESET+" Discount: (EU/t) / "+EnumChatFormatting.GREEN+mCostDiscount+EnumChatFormatting.RESET,
                StatCollector.translateToLocal("GT5U.multiblock.pollution")+": "+ EnumChatFormatting.GREEN + mPollutionReduction+ EnumChatFormatting.RESET+" %"
        };
    }

}
