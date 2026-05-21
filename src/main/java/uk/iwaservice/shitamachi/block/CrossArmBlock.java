package uk.iwaservice.shitamachi.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CrossArmBlock extends Block {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST  = BlockStateProperties.EAST;
    public static final BooleanProperty WEST  = BlockStateProperties.WEST;

    private static final VoxelShape CENTER    = box(5.80653, 6, 5.80653, 10.19347, 10, 10.19347);
    private static final VoxelShape ARM_NORTH = box(5.80653, 6, 0,        10.19347, 10,  5.80653);
    private static final VoxelShape ARM_SOUTH = box(5.80653, 6, 10.19347, 10.19347, 10, 16);
    private static final VoxelShape ARM_EAST  = box(10.19347, 6, 5.80653, 16,       10, 10.19347);
    private static final VoxelShape ARM_WEST  = box(0,        6, 5.80653,  5.80653, 10, 10.19347);

    public CrossArmBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST,  false)
                .setValue(WEST,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST);
    }

    private boolean canConnect(BlockState state) {
        Block b = state.getBlock();
        return b instanceof CrossArmBlock || b instanceof DenpoleBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, canConnect(level.getBlockState(pos.north())))
                .setValue(SOUTH, canConnect(level.getBlockState(pos.south())))
                .setValue(EAST,  canConnect(level.getBlockState(pos.east())))
                .setValue(WEST,  canConnect(level.getBlockState(pos.west())));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return switch (direction) {
            case NORTH -> state.setValue(NORTH, canConnect(neighborState));
            case SOUTH -> state.setValue(SOUTH, canConnect(neighborState));
            case EAST  -> state.setValue(EAST,  canConnect(neighborState));
            case WEST  -> state.setValue(WEST,  canConnect(neighborState));
            default    -> state;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH);
        if (state.getValue(EAST))  shape = Shapes.or(shape, ARM_EAST);
        if (state.getValue(WEST))  shape = Shapes.or(shape, ARM_WEST);
        return shape;
    }
}
