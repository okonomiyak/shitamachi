package uk.iwaservice.shitamachi.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DenpoleBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(6, 0, 6, 10, 16, 10);

    public static final BooleanProperty HAS_ARM     = BooleanProperty.create("has_arm");
    public static final BooleanProperty HAS_WARNING = BooleanProperty.create("has_warning");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final TagKey<Block> DENPOLE_ARMS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("shitamachi", "denpole_arms"));

    public DenpoleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_ARM, false)
                .setValue(HAS_WARNING, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_ARM, HAS_WARNING, FACING);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() == Direction.Axis.Y) return state;

        // 腕金検出
        if (neighborState.is(DENPOLE_ARMS)) {
            return state.setValue(HAS_ARM, true).setValue(FACING, direction);
        }
        boolean armFound = false;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir == direction) continue;
            if (level.getBlockState(pos.relative(dir)).is(DENPOLE_ARMS)) {
                state = state.setValue(HAS_ARM, true).setValue(FACING, dir);
                armFound = true;
                break;
            }
        }
        if (!armFound) state = state.setValue(HAS_ARM, false);
        return state;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !state.getValue(HAS_ARM)) return InteractionResult.PASS;

        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), 3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(DenpoleBlock::new);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DenpoleBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!stack.is(Items.SHEARS)) return super.useItemOn(stack, state, level, pos, player, hand, hit);

        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof DenpoleBlockEntity be) {
                List<ConnectionData> connected = be.getConnections();
                connected.forEach(conn -> {
                    BlockPos otherPos = conn.pos();
                    be.removeConnection(otherPos);
                    if (level.getBlockEntity(otherPos) instanceof DenpoleBlockEntity otherBe) {
                        otherBe.removeConnection(pos);
                        level.sendBlockUpdated(otherPos, level.getBlockState(otherPos), level.getBlockState(otherPos), 3);
                    }
                });
                level.sendBlockUpdated(pos, state, state, 3);
                player.sendSystemMessage(Component.literal("ケーブルをすべて切断しました。"));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
