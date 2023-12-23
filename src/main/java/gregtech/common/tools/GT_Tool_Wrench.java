package gregtech.common.tools;

import static gregtech.api.items.GT_MetaGenerated_Tool.getPrimaryMaterial;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.IToolStats;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.util.GT_ToolHarvestHelper;
import gregtech.common.items.behaviors.Behaviour_Switch_Mode;
import gregtech.common.items.behaviors.Behaviour_Wrench;
import ic2.api.tile.IWrenchable;

public class GT_Tool_Wrench extends GT_Tool {

    public static final List<String> mEffectiveList = Arrays
        .asList(EntityIronGolem.class.getName(), "EntityTowerGuardian");

    @Override
    public float getNormalDamageAgainstEntity(float aOriginalDamage, Entity aEntity, ItemStack aStack,
        EntityPlayer aPlayer) {
        String tName = aEntity.getClass()
            .getName();
        tName = tName.substring(tName.lastIndexOf('.') + 1);
        return (mEffectiveList.contains(tName)) || (tName.contains("Golem")) ? aOriginalDamage * 2.0F : aOriginalDamage;
    }

    @Override
    public int getToolDamagePerBlockBreak() {
        return 50;
    }

    @Override
    public int getToolDamagePerDropConversion() {
        return 100;
    }

    @Override
    public int getToolDamagePerContainerCraft() {
        return 800;
    }

    @Override
    public int getToolDamagePerEntityAttack() {
        return 200;
    }

    @Override
    public int getBaseQuality() {
        return 0;
    }

    @Override
    public float getBaseDamage() {
        return 3.0F;
    }

    @Override
    public int getHurtResistanceTime(int aOriginalHurtResistance, Entity aEntity) {
        return aOriginalHurtResistance * 2;
    }

    @Override
    public float getSpeedMultiplier() {
        return 1.0F;
    }

    @Override
    public float getMaxDurabilityMultiplier() {
        return 1.0F;
    }

    @Override
    public String getCraftingSound() {
        return SoundResource.IC2_TOOLS_WRENCH.toString();
    }

    @Override
    public String getEntityHitSound() {
        return null;
    }

    @Override
    public String getMiningSound() {
        return SoundResource.IC2_TOOLS_WRENCH.toString();
    }

    @Override
    public boolean canBlock() {
        return false;
    }

    @Override
    public boolean isCrowbar() {
        return false;
    }

    @Override
    public boolean isWrench() {
        return true;
    }

    @Override
    public boolean isMinableBlock(Block aBlock, byte aMetaData) {
        return GT_ToolHarvestHelper.isAppropriateTool(aBlock, aMetaData, "wrench")
            || GT_ToolHarvestHelper.isAppropriateMaterial(aBlock, Material.piston)
            || GT_ToolHarvestHelper.isSpecialBlock(aBlock, Blocks.hopper, Blocks.dispenser, Blocks.dropper);
    }

    @Override
    public ItemStack getBrokenItem(ItemStack aStack) {
        return null;
    }

    @Override
    public IIconContainer getIcon(boolean aIsToolHead, ItemStack aStack) {
        return aIsToolHead ? Textures.ItemIcons.WRENCH : null;
    }

    @Override
    public short[] getRGBa(boolean aIsToolHead, ItemStack aStack) {
        return aIsToolHead ? getPrimaryMaterial(aStack).mRGBa : null;
    }

    @Override
    public void onStatsAddedToTool(GT_MetaGenerated_Tool aItem, int aID) {
        aItem.addItemBehavior(aID, new Behaviour_Switch_Mode());
        aItem.addItemBehavior(aID, new Behaviour_Wrench(100));
    }

    @Override
    public IChatComponent getDeathMessage(EntityLivingBase aPlayer, EntityLivingBase aEntity) {
        return new ChatComponentText(
            EnumChatFormatting.GREEN + aPlayer.getCommandSenderName()
                + EnumChatFormatting.WHITE
                + " threw a Monkey Wrench into the Plans of "
                + EnumChatFormatting.RED
                + aEntity.getCommandSenderName()
                + EnumChatFormatting.WHITE);
    }

    @Override
    public float getMiningSpeed(Block block, byte metadata, float mineSpeed, EntityPlayer player, World world, int x,
        int y, int z) {
        ItemStack holding = player.getCurrentEquippedItem();
        if (holding == null || !(holding.getItem() instanceof GT_MetaGenerated_Tool tool)) return mineSpeed;

        IToolStats stats = tool.getToolStats(holding);
        if (stats == null) return mineSpeed;

        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) return mineSpeed;

        float newSpeed = Math.max(Float.MIN_NORMAL, getSpeedMultiplier() * getPrimaryMaterial(holding).mToolSpeed);

        if (tile instanceof IWrenchable) return newSpeed;

        return mineSpeed;
    }

    @Override
    public void onBreakBlock(@Nonnull EntityPlayer player, int x, int y, int z, @Nonnull Block block, byte metadata,
        TileEntity tile, @Nonnull BlockEvent.BreakEvent event) {
        final World world = player.worldObj;
        if (tile instanceof IWrenchable wrenchable) {
            ItemStack drop = wrenchable.getWrenchDrop(player);
            world.setBlockToAir(x, y, z);
            world.spawnEntityInWorld(new EntityItem(world, x, y, z, drop));
        }
    }

    @Override
    public String getToolTypeName() {
        return "wrench";
    }

    @Override
    public int getMaxMode() {
        return 2;
    }
}
