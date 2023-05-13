package gregtech.api.multitileentity.multiblock.casing;

import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.enums.GT_Values;
import gregtech.api.multitileentity.interfaces.IMultiBlockController;
import gregtech.api.multitileentity.multiblock.base.MultiBlockPart;

public abstract class UpgradeCasing extends MultiBlockPart {

    protected int tier = 0;

    @Override
    public int getPartTier() {
        return tier;
    }

    @Override
    public void setTarget(IMultiBlockController newTarget, int aAllowedModes) {
        super.setTarget(newTarget, aAllowedModes);

        if (newTarget != null) customWork(newTarget);
    }

    @Override
    public void readMultiTileNBT(NBTTagCompound aNBT) {
        super.readMultiTileNBT(aNBT);
        tier = aNBT.getInteger(GT_Values.NBT.TIER);
    }

    protected abstract void customWork(IMultiBlockController aTarget);
}
