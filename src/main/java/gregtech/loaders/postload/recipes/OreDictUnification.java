package gregtech.loaders.postload.recipes;

import static gregtech.api.util.GT_ModHandler.getModItem;

import cpw.mods.fml.common.Loader;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.util.GT_OreDictUnificator;

public class OreDictUnification implements Runnable {
    @Override
    public void run() {
        if (Loader.isModLoaded("HardcoreEnderExpansion")) {
            GT_OreDictUnificator.set(
                    OrePrefixes.ingot,
                    Materials.HeeEndium,
                    getModItem("HardcoreEnderExpansion", "endium_ingot", 1),
                    true,
                    true);
        }
    }
}
