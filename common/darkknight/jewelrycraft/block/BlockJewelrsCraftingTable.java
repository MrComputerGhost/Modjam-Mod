package darkknight.jewelrycraft.block;

import java.util.Random;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import darkknight.jewelrycraft.config.ConfigHandler;
import darkknight.jewelrycraft.item.ItemList;
import darkknight.jewelrycraft.item.ItemRing;
import darkknight.jewelrycraft.tileentity.TileEntityJewelrsCraftingTable;

public class BlockJewelrsCraftingTable extends BlockContainer
{
    Random rand        = new Random();
    int    modifiers[] = new int[] { Item.blazePowder.itemID };
    int    effects[]   = new int[] { 12 };

    protected BlockJewelrsCraftingTable(int par1, Material par2Material)
    {
        super(par1, par2Material);
        this.setBlockBounds(0.0F, 0F, 0.0F, 1.0F, 0.8F, 1.0F);
    }

    @Override
    public TileEntity createNewTileEntity(World world)
    {
        return new TileEntityJewelrsCraftingTable();
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    @Override
    public boolean onBlockActivated(World world, int i, int j, int k, EntityPlayer entityPlayer, int par6, float par7, float par8, float par9)
    {
        TileEntityJewelrsCraftingTable te = (TileEntityJewelrsCraftingTable) world.getBlockTileEntity(i, j, k);
        ItemStack item = entityPlayer.inventory.getCurrentItem();
        if (te != null && !world.isRemote)
        {
            te.isDirty = true;
            if (!te.hasEndItem && !te.hasJewel && item != null && item.getItem().itemID == ItemList.ring.itemID)
            {
                te.jewel = item.copy();
                te.hasJewel = true;
                if (!entityPlayer.capabilities.isCreativeMode) --item.stackSize;
                entityPlayer.inventory.onInventoryChanged();
            }
            if (!te.hasEndItem && !te.hasModifier && item != null && item.getItem().itemID == modifiers[0])
            {
                te.modifier = item.copy();
                te.modifier.stackSize = 1;
                te.hasModifier = true;
                if (!entityPlayer.capabilities.isCreativeMode) --item.stackSize;
                entityPlayer.inventory.onInventoryChanged();
            }
            if(te.timer == 0 && !te.hasEndItem && te.hasJewel && te.hasModifier) te.timer = ConfigHandler.jewelryCraftingTime;
            if(te.hasEndItem && item != null) entityPlayer.addChatMessage(StatCollector.translateToLocal("chatmessage.jewelrycraft.table.hasenditem"));

            if (te.hasModifier && entityPlayer.isSneaking())
            {
                dropItem( world, (double)te.xCoord, (double)te.yCoord, (double)te.zCoord, te.modifier.copy());
                te.modifier = new ItemStack(0, 0, 0);
                te.hasModifier = false;
            }
            if (te.hasJewel && entityPlayer.isSneaking())
            {
                dropItem(world, (double)te.xCoord, (double)te.yCoord, (double)te.zCoord, te.jewel.copy());
                te.jewel = new ItemStack(0, 0, 0);
                te.hasJewel = false;
            }
            te.isDirty = true;
            world.setBlockTileEntity(i, j, k, te);
        }
        return true;
    }

    public void dropItem(World world, double x, double y, double z, ItemStack stack)
    {
        EntityItem entityitem = new EntityItem(world, x + 0.5D, y + 1D, z + 0.5D, stack);
        entityitem.motionX = 0;
        entityitem.motionZ = 0;
        entityitem.motionY = 0.21000000298023224D;
        world.spawnEntityInWorld(entityitem);
    }

    public void breakBlock(World world, int i, int j, int k, int par5, int par6)
    {
        TileEntityJewelrsCraftingTable te = (TileEntityJewelrsCraftingTable) world.getBlockTileEntity(i, j, k);
        if (te != null)
        {
            if(te.hasModifier) dropItem(world, (double)te.xCoord, (double)te.yCoord, (double)te.zCoord, te.modifier.copy());
            if(te.hasJewel) dropItem(world, (double)te.xCoord, (double)te.yCoord, (double)te.zCoord, te.jewel.copy());
            if(te.hasEndItem) giveJewelToPlayer(te, te.endItem, te.modifier);
        }
        super.breakBlock(world, i, j, k, par5, par6);
    }

    public void giveJewelToPlayer(TileEntityJewelrsCraftingTable cf, ItemStack item, ItemStack modifier)
    {
        if (item != null)
        {
            ItemRing.addEffect(item, Potion.fireResistance.id);
            dropItem(cf.worldObj, (double)cf.xCoord, (double)cf.yCoord, (double)cf.zCoord, item.copy());
        }
    }

    @Override
    public void onBlockPlacedBy(World world, int i, int j, int k, EntityLivingBase entityLiving, ItemStack par6ItemStack)
    {
        int rotation = MathHelper.floor_double(entityLiving.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        world.setBlockMetadataWithNotify(i, j, k, rotation, 2);
    }

    @Override
    public void onBlockClicked(World world, int i, int j, int k, EntityPlayer player)
    {
        TileEntityJewelrsCraftingTable te = (TileEntityJewelrsCraftingTable) world.getBlockTileEntity(i, j, k);
        if (te != null && !world.isRemote)
        {
            if (te.hasEndItem)
            {
                giveJewelToPlayer(te, te.endItem, te.modifier);
                te.endItem = new ItemStack(0, 0, 0);
                te.hasEndItem = false;
            }
            else if (te.hasJewel && te.hasModifier && te.timer > 0 && te.jewel != null)
                player.addChatMessage(StatCollector.translateToLocalFormatted("chatmessage.jewelrycraft.table.iscrafting", te.jewel.getDisplayName()) + " (" + ((ConfigHandler.jewelryCraftingTime - te.timer)*100/ConfigHandler.jewelryCraftingTime) + "%)");
            else if (!te.hasModifier && !te.hasJewel)
                player.addChatMessage(StatCollector.translateToLocal("chatmessage.jewelrycraft.table.missingjewelryandmodifier"));
            else if (!te.hasJewel)
                player.addChatMessage(StatCollector.translateToLocal("chatmessage.jewelrycraft.table.misingjewelry"));
            else if (!te.hasModifier)
                player.addChatMessage(StatCollector.translateToLocal("chatmessage.jewelrycraft.table.missingmodifier"));
            te.isDirty = true;
        }
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess iblockaccess, int i, int j, int k, int l)
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public int getRenderType()
    {
        return -1;
    }

    @Override
    public void registerIcons(IconRegister icon)
    {
        this.blockIcon = icon.registerIcon("jewelrycraft:jewelrsCraftingTable");
    }
}
