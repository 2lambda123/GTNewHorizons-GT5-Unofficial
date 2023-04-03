package gregtech.api.enums;

import cpw.mods.fml.common.Loader;

public enum ModIDs {

    AdvancedSolarPanel("AdvancedSolarPanel"),
    AdventureBackpack("adventurebackpack"),
    AE2FluidCraft("ae2fc"),
    AFSU("AFSU"),
    AppleCore("AppleCore"),
    AppliedEnergistics2("appliedenergistics2"),
    ArchitectureCraft("ArchitectureCraft"),
    Aroma1997Core("Aroma1997Core"),
    Avaritia("Avaritia"),
    AvaritiaAddons("avaritiaddons"),
    Backpack("Backpack"),
    BartWorks("bartworks"),
    Baubles("Baubles"),
    BetterLoadingScreen("betterloadingscreen"),
    BetterQuesting("betterquesting"),
    BiblioCraft("BiblioCraft"),
    BiblioWoodsBoPEdition("BiblioWoodsBoP"),
    BiblioWoodsForestryEdition("BiblioWoodsForestry"),
    BiblioWoodsNaturaEdition("BiblioWoodsNatura"),
    BiomesOPlanty("BiomesOPlenty"),
    BloodArsenal("BloodArsenal"),
    BloodMagic("AWWayofTime"),
    Botania("Botania"),
    Botany("Botany"),
    BuildCraftCore("BuildCraft|Core"),
    BuildCraftFactory("BuildCraft|Factory"),
    BuildCraftSilicon("BuildCraft|Silicon"),
    BuildCraftTransport("BuildCraft|Transport"),
    Chisel("chisel"),
    Computronics("computronics"),
    CraftTweaker("MineTweaker3"),
    DraconicEvolution("DraconicEvolution"),
    EnderIO("EnderIO"),
    EnderStorage("EnderStorage"),
    EnderZoo("EnderZoo"),
    EternalSingularity("eternalsingularity"),
    ExtraBees("ExtraBees"),
    ExtraCells2("extracells"),
    ExtraTrees("ExtraTrees"),
    ExtraUtilities("ExtraUtilities"),
    FloodLights("FloodLights"),
    ForbiddenMagic("ForbiddenMagic"),
    Forestry("Forestry"),
    ForgeMicroblocks("ForgeMicroblock"),
    ForgeRelocation("ForgeRelocation"),
    Gadomancy("gadomancy"),
    GalacticraftCore("GalacticraftCore"),
    GalacticraftMars("GalacticraftMars"),
    GalactiGreg("galacticgreg"),
    GalaxySpace("GalaxySpace"),
    Gendustry("gendustry"),
    Genetics("Genetics"),
    GoodGenerator("GoodGenerator"),
    GraviSuite("GraviSuite"),
    GregTech("gregtech"),
    GTNHLanthanides("gtnhlanth"),
    GTPlusPlus("miscutils"),
    HardcoreEnderExpansion("HardcoreEnderExpansion"),
    HodgePodge("hodgepodge"),
    HoloInventory("holoinventory"),
    IC2CropPlugin("Ic2Nei"),
    IC2NuclearControl("IC2NuclearControl"),
    IguanaTweaksTinkerConstruct("IguanaTweaksTConstruct"),
    IndustrialCraft2("IC2"),
    IronChests("IronChest"),
    IronTanks("irontank"),
    JABBA("JABBA"),
    MalisisDoors("malisisdoors"),
    Mantle("Mantle"),
    MineAndBladeBattleGear2("battlegear2"),
    Minecraft("minecraft"),
    Natura("Natura"),
    NaturesCompass("naturescompass"),
    NEICustomDiagrams("neicustomdiagram"),
    NEIOrePlugin("gtneioreplugin"),
    NewHorizonsCoreMod("dreamcraft"),
    NotEnoughItems("NotEnoughItems"),
    OpenBlocks("OpenBlocks"),
    OpenComputers("OpenComputers"),
    OpenGlasses("openglasses"),
    OpenModularTurrets("openmodularturrets"),
    OpenPrinters("openprinter"),
    OpenSecurity("opensecurity"),
    PamsHarvestCraft("harvestcraft"),
    PamsHarvestTheNether("harvestthenether"),
    ProjectRedCore("ProjRed|Core"),
    ProjectRedIllumination("ProjRed|Illumination"),
    Railcraft("Railcraft"),
    RandomThings("RandomThings"),
    SGCraft("SGCraft"),
    SleepingBags("sleepingbag"),
    SpiceOfLife("SpiceOfLife"),
    StevesCarts2("StevesCarts"),
    SuperSolarPanels("supersolarpanel"),
    TaintedMagic("TaintedMagic"),
    Thaumcraft("Thaumcraft"),
    ThaumicBases("thaumicbases"),
    ThaumicEnergistics("thaumicenergistics"),
    ThaumicHorizons("ThaumicHorizons"),
    ThaumicMachina("ThaumicMachina"),
    ThaumicTinkerer("ThaumicTinkerer"),
    TinkerConstruct("TConstruct"),
    TinkersGregworks("TGregworks"),
    Translocator("Translocator"),
    TravellersGear("TravellersGear"),
    TwilightForest("TwilightForest"),
    Waila("Waila"),
    WarpTheory("WarpTheory"),
    Witchery("witchery"),
    ZTones("Ztones"),

    // Do we keep compat of those?
    ArsMagica2("arsmagica2"),
    IndustrialCraft2Classic("IC2-Classic-Spmod"),
    Metallurgy("Metallurgy"),
    RotaryCraft("RotaryCraft"),
    ThermalExpansion("ThermalExpansion"),
    ThermalFondation("ThermalFoundation"),
    UndergroundBiomes("UndergroundBiomes");

    public final String modID;
    private Boolean modLoaded;

    ModIDs(String modID) {
        this.modID = modID;
    }

    public boolean isModLoaded() {
        if (this.modLoaded == null) {
            this.modLoaded = Loader.isModLoaded(modID);
        }
        return this.modLoaded;
    }
}
