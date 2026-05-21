package uk.iwaservice.shitamachi.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import uk.iwaservice.shitamachi.block.DenpoleBlock;

public class WarningCoverItem extends Item {

    public WarningCoverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof DenpoleBlock)) return InteractionResult.PASS;

        if (!level.isClientSide) {
            boolean hasWarning = state.getValue(DenpoleBlock.HAS_WARNING);
            if (!hasWarning) {
                Direction playerFacing = context.getHorizontalDirection();
                level.setBlock(pos, state.setValue(DenpoleBlock.HAS_WARNING, true).setValue(DenpoleBlock.FACING, playerFacing), 3);
                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            } else {
                level.setBlock(pos, state.setValue(DenpoleBlock.HAS_WARNING, false), 3);
                if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                    context.getPlayer().getInventory().add(new ItemStack(this));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
