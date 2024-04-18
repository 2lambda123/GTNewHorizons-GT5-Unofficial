package gregtech.api.multitileentity.interfaces;

import gregtech.api.logic.interfaces.FluidInventoryLogicHost;
import gregtech.api.logic.interfaces.ItemInventoryLogicHost;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.util.ChunkCoordinates;

public interface IMultiBlockPart
    extends IMultiTileEntity, ItemInventoryLogicHost, FluidInventoryLogicHost {

  @Nonnull ChunkCoordinates getTargetPos();

  void setTargetPos(@Nonnull final ChunkCoordinates targetPos);

  @Nullable UUID getLockedInventory();

  boolean shouldTick(final long tickTimer);
}
