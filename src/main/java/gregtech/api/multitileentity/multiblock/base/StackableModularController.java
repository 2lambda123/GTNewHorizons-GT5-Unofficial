package gregtech.api.multitileentity.multiblock.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.gtnewhorizons.modularui.api.forge.IItemHandlerModifiable;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.logic.interfaces.ProcessingLogicHost;
import gregtech.api.util.GT_StructureUtilityMuTE;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import gregtech.api.multitileentity.interfaces.UpgradableModularMuTE;
import gregtech.api.util.GT_StructureUtilityMuTE.UpgradeCasings;

public abstract class StackableModularController<T extends StackableModularController<T>> extends StackableController<T>
    implements UpgradableModularMuTE {
    protected double durationMultiplier = 1;
    protected double euTickMultiplier = 1;

    private Map<UpgradeCasings, int[]> mucMap;

    protected @NotNull Map<UpgradeCasings, int[]> getMucMap() {
        if (mucMap == null) {
            mucMap = createMucMap();
        }
        return mucMap;
    }

    protected static @NotNull Map<UpgradeCasings, int[]> createMucMap() {
        Map<UpgradeCasings, int[]> mucCount = new HashMap<>();
        mucCount.put(UpgradeCasings.Heater, new int[] { 0, 0, 0, 0, 0 });
        mucCount.put(UpgradeCasings.Insulator, new int[] { 0, 0, 0, 0, 0 });
        return mucCount;
    }

    @Override
    public void increaseMucCount(UpgradeCasings casingType, int tier) {
        Map<UpgradeCasings, int[]> mucCounters = getMucMap();
        int[] casingCount = mucCounters.get(casingType);

        switch (tier) {
            case 0, 1, 2 -> casingCount[0] += 1;
            case 3, 4, 5 -> casingCount[1] += 1;
            case 6, 7, 8 -> casingCount[2] += 1;
            case 9, 10, 11 -> casingCount[3] += 1;
            default -> casingCount[4] += 1;
        }
    }

    @Override
    public void resetMucCount() {
        Map<UpgradeCasings, int[]> mucCounters = getMucMap();
        mucCounters.forEach((type, casingCount) -> { Arrays.fill(casingCount, 0); });
    }

    // Returns the cheapest MUC that is possible for the multi, which gets the minimum bonuses.
    protected abstract UpgradeCasings getBaseMucType();

    // Minimum parallel bonus per MUC. Higher tier MUCs multiply with this value for even more parallels.
    protected abstract int getParallelFactor();

    protected void calculateParallels() {
        int parallelCount = 0;
        int parallelFactor = getParallelFactor();
        int[] parallelCasingList = mucMap.get(getBaseMucType());

        for (int i = 0; i < 5; i++) {
            // (i * 3 + 1) -> Convert MUC tier into minimum GT tier, in groups of 3 (LV, EV, LuV, UHV, UMV)
            // If higher than multi tier, upgrade casing has no effect
            if (i * 3 + 1 <= tier) {
                parallelCount += parallelCasingList[i] * (i + 1) * parallelFactor;
            }
        }
        maxParallel = parallelCount == 0 ? 1 : parallelCount;
    }

    protected abstract boolean calculateMucMultipliers();

    @Override
    protected boolean checkRecipe() {
        if (!(this instanceof ProcessingLogicHost)) {
            return false;
        }
        ProcessingLogic logic = ((ProcessingLogicHost) this).getProcessingLogic();
        logic.clear();
        boolean result = false;
        if (isSeparateInputs()) {
            // TODO: Add separation with fluids
            for (Pair<ItemStack[], String> inventory : getItemInputsForEachInventory()) {
                IItemHandlerModifiable outputInventory = multiBlockOutputInventory
                    .getOrDefault(inventory.getLeft(), null);
                result = logic.setInputItems(inventory.getLeft())
                    .setCurrentOutputItems(getOutputItems())
                    .process();
                if (result) {
                    inventoryName = inventory.getRight();
                    break;
                }
                logic.clear();
            }
        } else {
            result = logic.setInputItems(getInputItems())
                .setCurrentOutputItems(getOutputItems())
                .setInputFluids(getInputFluids())
                .setCurrentOutputFluids(getOutputFluids())
                .setVoltage(power.getVoltage())
                .setAmperage(amperage)
                .setMaxParallel(maxParallel)
                .setPerfectOverclock(hasPerfectOverclock())
                .setIsCleanroom(isCleanroom)
                .process();
        }
        setDuration((long) (logic.getDuration() * durationMultiplier));
        setEut((long) (logic.getEut() * euTickMultiplier));
        setItemOutputs(logic.getOutputItems());
        setFluidOutputs(logic.getOutputFluids());
        return result;
    }
}
