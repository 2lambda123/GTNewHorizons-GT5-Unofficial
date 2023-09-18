package gregtech.api.multitileentity.multiblock.casing;

import net.minecraft.nbt.NBTTagCompound;

import gregtech.api.enums.GT_Values;
import gregtech.api.multitileentity.multiblock.base.MultiBlockPart;

public abstract class FunctionalCasing extends MultiBlockPart {

    private int tier = 0;

    @Override
    public int getPartTier() {
        return tier;
    }

    public abstract float getPartWeight();

    @Override
    public void readMultiTileNBT(NBTTagCompound nbt) {
        super.readMultiTileNBT(nbt);
        tier = nbt.getInteger(GT_Values.NBT.TIER);
    }

    @Override
    public void writeMultiTileNBT(NBTTagCompound nbt) {
        super.writeMultiTileNBT(nbt);
    }
}
