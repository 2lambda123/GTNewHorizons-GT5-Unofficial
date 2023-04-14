package gregtech.common.tileentities.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.enums.Mods.*;
import static gregtech.api.multitileentity.multiblock.base.MultiBlockPart.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizon.structurelib.util.Vec3Impl;
import com.gtnewhorizons.modularui.api.forge.ItemStackHandler;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.MultiChildWidget;
import com.gtnewhorizons.modularui.common.widget.SlotGroup;

import gregtech.api.GregTech_API;
import gregtech.api.enums.HeatingCoilLevel;
import gregtech.api.fluid.FluidTankGT;
import gregtech.api.gui.modularui.GT_UITextures;
import gregtech.api.logic.ComplexParallelProcessingLogic;
import gregtech.api.multitileentity.enums.GT_MultiTileCasing;
import gregtech.api.multitileentity.multiblock.base.ComplexController;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_StructureUtility;

public class AdvChemicalReactor extends ComplexController<AdvChemicalReactor> {

    private static IStructureDefinition<AdvChemicalReactor> STRUCTURE_DEFINITION = null;
    protected static final String STRUCTURE_PIECE_T1 = "T1";
    protected static final Vec3Impl STRUCTURE_OFFSET = new Vec3Impl(3, 1, 0);
    protected static final int PROCESS_WINDOW_BASE_ID = 100;
    protected static final int ITEM_WHITELIST_SLOTS = 8;
    protected static final int FLUID_WHITELIST_SLOTS = 8;
    protected static final int MAX_PROCESSES = 4;
    protected int numberOfProcessors = MAX_PROCESSES; // TODO: Set this value depending on structure
    protected HeatingCoilLevel coilTier;
    protected final List<ItemStack[]> processItemWhiteLists = new ArrayList<>();
    protected final List<ItemStackHandler> processInventoryHandlers = new ArrayList<>();
    protected final List<List<IFluidTank>> processFluidWhiteLists = new ArrayList<>();

    public AdvChemicalReactor() {
        super();
        for (int i = 0; i < MAX_PROCESSES; i++) {
            processItemWhiteLists.add(new ItemStack[ITEM_WHITELIST_SLOTS]);
            processInventoryHandlers.add(new ItemStackHandler(processItemWhiteLists.get(i)));
            List<IFluidTank> processFluidTanks = new ArrayList<>();
            for (int j = 0; j < FLUID_WHITELIST_SLOTS; j++) {
                processFluidTanks.add(new FluidTankGT());
            }
            processFluidWhiteLists.add(processFluidTanks);
        }
        processingLogic = new ComplexParallelProcessingLogic(
            GT_Recipe.GT_Recipe_Map_LargeChemicalReactor.sChemicalRecipes,
            MAX_PROCESSES);
    }

    @Override
    public void readMultiTileNBT(NBTTagCompound nbt) {
        super.readMultiTileNBT(nbt);
        final NBTTagCompound processWhiteLists = nbt.getCompoundTag("whiteLists");
        if (processWhiteLists != null) {
            for (int i = 0; i < MAX_PROCESSES; i++) {
                final NBTTagList itemList = processWhiteLists.getTagList("items" + i, Constants.NBT.TAG_COMPOUND);
                if (itemList != null) {
                    for (int j = 0; j < itemList.tagCount(); j++) {
                        final NBTTagCompound item = itemList.getCompoundTagAt(j);
                        if (item != null) {
                            short index = item.getShort("s");
                            ItemStack itemStack = ItemStack.loadItemStackFromNBT(item);
                            if (itemStack != null) {
                                processItemWhiteLists.get(i)[index] = itemStack;
                            }
                        }
                    }
                }
                final NBTTagList fluidList = processWhiteLists.getTagList("fluids" + i, Constants.NBT.TAG_COMPOUND);
                if (fluidList != null) {
                    for (int j = 0; j < fluidList.tagCount(); j++) {
                        final NBTTagCompound fluid = fluidList.getCompoundTagAt(j);
                        if (fluid != null) {
                            short index = fluid.getShort("s");
                            FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(fluid);
                            if (fluidStack != null) {
                                processFluidWhiteLists.get(i)
                                    .get(index)
                                    .fill(fluidStack, true);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void writeMultiTileNBT(NBTTagCompound nbt) {
        super.writeMultiTileNBT(nbt);
        final NBTTagCompound processWhiteLists = new NBTTagCompound();
        for (int i = 0; i < MAX_PROCESSES; i++) {
            final NBTTagList itemList = new NBTTagList();
            final NBTTagList fluidList = new NBTTagList();
            for (int j = 0; j < ITEM_WHITELIST_SLOTS; j++) {
                final ItemStack itemStack = processItemWhiteLists.get(i)[j];
                if (itemStack != null) {
                    final NBTTagCompound tag = new NBTTagCompound();
                    tag.setByte("s", (byte) j);
                    itemStack.writeToNBT(tag);
                    itemList.appendTag(tag);
                }
            }
            processWhiteLists.setTag("items" + i, itemList);
            for (int j = 0; j < FLUID_WHITELIST_SLOTS; j++) {
                final FluidStack fluidStack = processFluidWhiteLists.get(i)
                    .get(j)
                    .getFluid();
                if (fluidStack != null) {
                    final NBTTagCompound tag = new NBTTagCompound();
                    tag.setByte("s", (byte) j);
                    fluidStack.writeToNBT(tag);
                    fluidList.appendTag(tag);
                }
            }
            processWhiteLists.setTag("fluids" + i, fluidList);
        }
        nbt.setTag("whiteLists", processWhiteLists);
    }

    @Override
    public short getCasingRegistryID() {
        return 0;
    }

    @Override
    public short getCasingMeta() {
        return GT_MultiTileCasing.Chemical.getId();
    }

    @Override
    public GT_Multiblock_Tooltip_Builder createTooltip() {
        final GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType("Chemical Reactor")
            .addInfo("Controller block for the Advanced Chemical Reactor")
            .addSeparator()
            .beginStructureBlock(3, 3, 3, false)
            .addController("Front center")
            .toolTipFinisher("Gregtech");
        return tt;
    }

    @Override
    public Vec3Impl getStartingStructureOffset() {
        return STRUCTURE_OFFSET;
    }

    @Override
    public boolean checkMachine() {
        setCoilTier(HeatingCoilLevel.None);
        setMaxComplexParallels(MAX_PROCESSES);
        buildState.startBuilding(getStartingStructureOffset());
        return checkPiece(STRUCTURE_PIECE_T1, buildState.stopBuilding());
    }

    @Override
    public void construct(ItemStack trigger, boolean hintsOnly) {
        buildState.startBuilding(getStartingStructureOffset());
        buildPiece(STRUCTURE_PIECE_T1, trigger, hintsOnly, buildState.stopBuilding());
    }

    @Override
    public IStructureDefinition<AdvChemicalReactor> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<AdvChemicalReactor>builder()
                .addShape(
                    STRUCTURE_PIECE_T1,
                    transpose(
                        new String[][] { { "CPCPC", "CCCCC", "CPCPC" }, { "CGC~C", "GWWWC", "CGCCC" },
                            { "CPCPC", "CCCCC", "CPCPC" } }))
                .addElement(
                    'C',
                    ofChain(
                        addMultiTileCasing(
                            "gt.multitileentity.casings",
                            getCasingMeta(),
                            FLUID_IN | ITEM_IN | FLUID_OUT | ITEM_OUT | ENERGY_IN)))
                .addElement('P', ofBlock(GregTech_API.sBlockCasings8, 1))
                .addElement(
                    'W',
                    GT_StructureUtility.ofCoil(AdvChemicalReactor::setCoilTier, AdvChemicalReactor::getCoilTier))
                .addElement(
                    'G',
                    ofChain(
                        ofBlockUnlocalizedName(IndustrialCraft2.ID, "blockAlloyGlass", 0, true),
                        ofBlockUnlocalizedName(BartWorks.ID, "BW_GlasBlocks", 0, true),
                        ofBlockUnlocalizedName(BartWorks.ID, "BW_GlasBlocks2", 0, true),
                        ofBlockUnlocalizedName(Thaumcraft.ID, "blockCosmeticOpaque", 2, false)))
                .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    protected MultiChildWidget createMainPage() {
        MultiChildWidget child = super.createMainPage();
        for (int i = 0; i < MAX_PROCESSES; i++) {
            final int processIndex = i;
            child.addChild(
                new ButtonWidget().setPlayClickSound(true)
                    .setOnClick(
                        (clickData, widget) -> {
                            if (!widget.isClient()) widget.getContext()
                                .openSyncedWindow(PROCESS_WINDOW_BASE_ID + processIndex);
                        })
                    .setBackground(GT_UITextures.OVERLAY_BUTTON_BATCH_MODE_ON)
                    .setSize(18, 18)
                    .setPos(20 * i, 18));
        }
        return child;
    }

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        super.addUIWidgets(builder, buildContext);
        for (int i = 0; i < MAX_PROCESSES; i++) {
            final int processIndex = i;
            buildContext.addSyncedWindow(
                PROCESS_WINDOW_BASE_ID + i,
                (player) -> createProcessConfigWindow(player, processIndex));
        }
    }

    protected ModularWindow createProcessConfigWindow(final EntityPlayer player, final int processIndex) {
        ModularWindow.Builder builder = ModularWindow.builder(86, 90);
        builder.setBackground(GT_UITextures.BACKGROUND_SINGLEBLOCK_DEFAULT);
        builder.widget(
            SlotGroup.ofItemHandler(processInventoryHandlers.get(processIndex), 4)
                .startFromSlot(0)
                .endAtSlot(ITEM_WHITELIST_SLOTS - 1)
                .phantom(true)
                .background(getGUITextureSet().getItemSlot())
                .build()
                .setPos(7, 9));
        builder.widget(
            SlotGroup.ofFluidTanks(processFluidWhiteLists.get(processIndex), 4)
                .startFromSlot(0)
                .endAtSlot(FLUID_WHITELIST_SLOTS - 1)
                .phantom(true)
                .build()
                .setPos(7, 45));
        return builder.build();
    }

    @Override
    public String getTileEntityName() {
        return "gt.multitileentity.multiblock.advchemicalreactor";
    }

    @Override
    public String getLocalName() {
        return "Advanced Chemical Reactor";
    }

    public void setCoilTier(HeatingCoilLevel coilTier) {
        this.coilTier = coilTier;
    }

    public HeatingCoilLevel getCoilTier() {
        return coilTier;
    }

    @Override
    protected boolean hasPerfectOverclock() {
        return true;
    }
}