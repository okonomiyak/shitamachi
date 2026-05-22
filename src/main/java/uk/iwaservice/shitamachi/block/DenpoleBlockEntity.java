package uk.iwaservice.shitamachi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import uk.iwaservice.shitamachi.Shitamachi;

public class DenpoleBlockEntity extends CableBlockEntity {
    public DenpoleBlockEntity(BlockPos pos, BlockState state) {
        super(Shitamachi.DENPOLE_BE.get(), pos, state);
    }
}
