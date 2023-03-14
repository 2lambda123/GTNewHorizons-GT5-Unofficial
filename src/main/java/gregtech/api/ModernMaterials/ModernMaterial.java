package gregtech.api.ModernMaterials;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import gregtech.api.ModernMaterials.Fluids.FluidEnum;
import gregtech.api.ModernMaterials.PartsClasses.CustomPartInfo;
import gregtech.api.ModernMaterials.PartsClasses.PartsEnum;

public class ModernMaterial {

    private final HashMap<PartsEnum, CustomPartInfo> existingPartsForMaterial = new HashMap<>();
    public final Set<FluidEnum> existingFluidsForMaterial = new HashSet<>();
    private Color color;
    private int ID;
    private String name;
    private long materialTier;
    private double materialTimeMultiplier;

    public ModernMaterial() {}

    public ModernMaterial setColor(Color aColor) {
        color = aColor;
        return this;
    }

    public ModernMaterial setColor(int aRed, int aGreen, int aBlue, int aAlpha) {
        color = new Color(aRed, aGreen, aBlue, aAlpha);
        return this;
    }

    public ModernMaterial setColor(int aRed, int aGreen, int aBlue) {
        color = new Color(aRed, aGreen, aBlue);
        return this;
    }

    public ModernMaterial setName(String aName) {
        name = aName;
        return this;
    }

    public ModernMaterial setMaterialTimeMultiplier(double materialTimeMultiplier) {
        this.materialTimeMultiplier = materialTimeMultiplier;
        return this;
    }

    public ModernMaterial addParts(final PartsEnum... aParts) {
        for (PartsEnum aPart : aParts) {
            existingPartsForMaterial.put(aPart, new CustomPartInfo(aPart));
        }
        return this;
    }

    public ModernMaterial addPartsCustom(final CustomPartInfo... customParts) {
        for (CustomPartInfo customPartInfo : customParts) {
            existingPartsForMaterial.put(customPartInfo.mPart, customPartInfo);
        }
        return this;
    }

    public ModernMaterial addFluids(final FluidEnum... fluids) {
        existingFluidsForMaterial.addAll(Arrays.asList(fluids));
        return this;
    }

    public ModernMaterial build() {
        return this;
    }

    public void setID(int aID) {
        ID = aID;
    }

    public Color getColor() {
        return color;
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public double getMaterialTimeMultiplier() {
        return materialTimeMultiplier;
    }

    public long getMaterialTier() {
        return materialTier;
    }

    public ModernMaterial addPart(final PartsEnum aPart) {
        existingPartsForMaterial.put(aPart, new CustomPartInfo(aPart));
        return this;
    }

    // This will override all existing parts settings and enable ALL possible parts. Be careful!
    public ModernMaterial addAllParts() {
        for (PartsEnum tPart : PartsEnum.values()) {
            existingPartsForMaterial.put(tPart, new CustomPartInfo(tPart));
        }
        return this;
    }

    public ModernMaterial setMaterialTier(final long tier) {
        this.materialTier = tier;
        return this;
    }

    public ModernMaterial addPart(final CustomPartInfo aCustomPartInfo) {
        existingPartsForMaterial.put(aCustomPartInfo.mPart, aCustomPartInfo);
        return this;
    }

    public CustomPartInfo getCustomPartInfo(final PartsEnum aPart) {
        return existingPartsForMaterial.get(aPart);
    }

    public boolean doesPartExist(PartsEnum aPart) {
        return existingPartsForMaterial.containsKey(aPart);
    }
}
