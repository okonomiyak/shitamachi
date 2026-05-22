package uk.iwaservice.shitamachi.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import uk.iwaservice.shitamachi.item.InsulatorItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrossArmBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH           = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH           = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST            = BlockStateProperties.EAST;
    public static final BooleanProperty WEST            = BlockStateProperties.WEST;
    public static final BooleanProperty ARM_NE          = BooleanProperty.create("arm_ne");
    public static final BooleanProperty ARM_NW          = BooleanProperty.create("arm_nw");
    public static final BooleanProperty ARM_SE          = BooleanProperty.create("arm_se");
    public static final BooleanProperty ARM_SW          = BooleanProperty.create("arm_sw");
    public static final BooleanProperty ARM_UP          = BlockStateProperties.UP;
    public static final BooleanProperty ARM_DOWN        = BlockStateProperties.DOWN;
    public static final BooleanProperty INSULATOR_NORTH = BooleanProperty.create("insulator_north");
    public static final BooleanProperty INSULATOR_SOUTH = BooleanProperty.create("insulator_south");
    public static final BooleanProperty INSULATOR_EAST  = BooleanProperty.create("insulator_east");
    public static final BooleanProperty INSULATOR_WEST  = BooleanProperty.create("insulator_west");

    private static final VoxelShape CENTER       = box(5.80653, 6, 5.80653, 10.19347, 10, 10.19347);
    private static final VoxelShape ARM_NORTH_SH = box(5.80653, 6, 0,        10.19347, 10,  5.80653);
    private static final VoxelShape ARM_SOUTH_SH = box(5.80653, 6, 10.19347, 10.19347, 10, 16);
    private static final VoxelShape ARM_EAST_SH  = box(10.19347, 6, 5.80653, 16,       10, 10.19347);
    private static final VoxelShape ARM_WEST_SH  = box(0,        6, 5.80653,  5.80653, 10, 10.19347);
    private static final VoxelShape ARM_UP_SH    = box(5.80653,  8, 5.80653, 10.19347, 16, 10.19347);
    private static final VoxelShape ARM_DOWN_SH  = box(5.80653,  0, 5.80653, 10.19347,  8, 10.19347);

    public CrossArmBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(ARM_NE, false).setValue(ARM_NW, false)
                .setValue(ARM_SE, false).setValue(ARM_SW, false)
                .setValue(ARM_UP, false).setValue(ARM_DOWN, false)
                .setValue(INSULATOR_NORTH, false).setValue(INSULATOR_SOUTH, false)
                .setValue(INSULATOR_EAST,  false).setValue(INSULATOR_WEST,  false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CrossArmBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST,
                ARM_NE, ARM_NW, ARM_SE, ARM_SW, ARM_UP, ARM_DOWN,
                INSULATOR_NORTH, INSULATOR_SOUTH, INSULATOR_EAST, INSULATOR_WEST);
    }

    private static boolean canConnect(BlockState state) {
        Block b = state.getBlock();
        return b instanceof CrossArmBlock || b instanceof DenpoleBlock;
    }

    private static boolean isDisabledAt(LevelAccessor level, BlockPos pos, String key) {
        return level.getBlockEntity(pos) instanceof CrossArmBlockEntity be && be.isDisabled(key);
    }

    private static BooleanProperty armKeyToProp(String key) {
        return switch (key) {
            case "ne"   -> ARM_NE;
            case "nw"   -> ARM_NW;
            case "se"   -> ARM_SE;
            case "sw"   -> ARM_SW;
            case "up"   -> ARM_UP;
            case "down" -> ARM_DOWN;
            default     -> null;
        };
    }

    private static String getTargetArmKey(BlockHitResult hit, BlockPos pos) {
        Direction face = hit.getDirection();
        if (face == Direction.UP)   return "up";
        if (face == Direction.DOWN) return "down";
        Vec3 loc = hit.getLocation();
        double relX = loc.x - pos.getX() - 0.5;
        double relZ = loc.z - pos.getZ() - 0.5;
        if (Math.abs(relX) < 0.1 || Math.abs(relZ) < 0.1) return null;
        if (relX > 0 && relZ < 0) return "ne";
        if (relX < 0 && relZ < 0) return "nw";
        if (relX > 0)              return "se";
        return "sw";
    }

    private static BlockState neighborForKey(String key, LevelAccessor level, BlockPos pos) {
        return switch (key) {
            case "ne"   -> level.getBlockState(pos.north().east());
            case "nw"   -> level.getBlockState(pos.north().west());
            case "se"   -> level.getBlockState(pos.south().east());
            case "sw"   -> level.getBlockState(pos.south().west());
            case "up"   -> level.getBlockState(pos.above());
            case "down" -> level.getBlockState(pos.below());
            default     -> null;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH,    canConnect(level.getBlockState(pos.north())))
                .setValue(SOUTH,    canConnect(level.getBlockState(pos.south())))
                .setValue(EAST,     canConnect(level.getBlockState(pos.east())))
                .setValue(WEST,     canConnect(level.getBlockState(pos.west())))
                .setValue(ARM_NE,   canConnect(level.getBlockState(pos.north().east())))
                .setValue(ARM_NW,   canConnect(level.getBlockState(pos.north().west())))
                .setValue(ARM_SE,   canConnect(level.getBlockState(pos.south().east())))
                .setValue(ARM_SW,   canConnect(level.getBlockState(pos.south().west())))
                .setValue(ARM_UP,   canConnect(level.getBlockState(pos.above())))
                .setValue(ARM_DOWN, canConnect(level.getBlockState(pos.below())));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return switch (direction) {
            case NORTH -> state.setValue(NORTH, canConnect(neighborState))
                               .setValue(ARM_NE, !isDisabledAt(level, pos, "ne") && canConnect(level.getBlockState(pos.north().east())))
                               .setValue(ARM_NW, !isDisabledAt(level, pos, "nw") && canConnect(level.getBlockState(pos.north().west())));
            case SOUTH -> state.setValue(SOUTH, canConnect(neighborState))
                               .setValue(ARM_SE, !isDisabledAt(level, pos, "se") && canConnect(level.getBlockState(pos.south().east())))
                               .setValue(ARM_SW, !isDisabledAt(level, pos, "sw") && canConnect(level.getBlockState(pos.south().west())));
            case EAST  -> state.setValue(EAST,  canConnect(neighborState))
                               .setValue(ARM_NE, !isDisabledAt(level, pos, "ne") && canConnect(level.getBlockState(pos.north().east())))
                               .setValue(ARM_SE, !isDisabledAt(level, pos, "se") && canConnect(level.getBlockState(pos.south().east())));
            case WEST  -> state.setValue(WEST,  canConnect(neighborState))
                               .setValue(ARM_NW, !isDisabledAt(level, pos, "nw") && canConnect(level.getBlockState(pos.north().west())))
                               .setValue(ARM_SW, !isDisabledAt(level, pos, "sw") && canConnect(level.getBlockState(pos.south().west())));
            case UP    -> state.setValue(ARM_UP,   !isDisabledAt(level, pos, "up")   && canConnect(neighborState));
            case DOWN  -> state.setValue(ARM_DOWN, !isDisabledAt(level, pos, "down") && canConnect(neighborState));
            default    -> state;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        String key = getTargetArmKey(hit, pos);
        if (key == null) return InteractionResult.PASS;

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrossArmBlockEntity be) {
            boolean nowDisabled = be.toggleDisabled(key);
            BooleanProperty prop = armKeyToProp(key);
            if (prop != null) {
                BlockState ns = neighborForKey(key, level, pos);
                boolean armOn = !nowDisabled && ns != null && canConnect(ns);
                level.setBlock(pos, state.setValue(prop, armOn), 3);
            }
            player.sendSystemMessage(Component.literal(nowDisabled ? "腕を無効化しました。" : "腕を有効化しました。"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH))    shape = Shapes.or(shape, ARM_NORTH_SH);
        if (state.getValue(SOUTH))    shape = Shapes.or(shape, ARM_SOUTH_SH);
        if (state.getValue(EAST))     shape = Shapes.or(shape, ARM_EAST_SH);
        if (state.getValue(WEST))     shape = Shapes.or(shape, ARM_WEST_SH);
        if (state.getValue(ARM_UP))   shape = Shapes.or(shape, ARM_UP_SH);
        if (state.getValue(ARM_DOWN)) shape = Shapes.or(shape, ARM_DOWN_SH);
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrossArmBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.getItem() instanceof InsulatorItem) {
            Vec3 loc = hit.getLocation();
            double relX = loc.x - pos.getX() - 0.5;
            double relZ = loc.z - pos.getZ() - 0.5;
            Direction dir = Math.abs(relX) > Math.abs(relZ)
                    ? (relX > 0 ? Direction.EAST : Direction.WEST)
                    : (relZ > 0 ? Direction.SOUTH : Direction.NORTH);

            BooleanProperty armProp = switch (dir) {
                case NORTH -> NORTH; case SOUTH -> SOUTH;
                case EAST  -> EAST;  case WEST  -> WEST;
                default    -> null;
            };
            BooleanProperty insProp = switch (dir) {
                case NORTH -> INSULATOR_NORTH; case SOUTH -> INSULATOR_SOUTH;
                case EAST  -> INSULATOR_EAST;  case WEST  -> INSULATOR_WEST;
                default    -> null;
            };
            if (armProp == null || insProp == null || !state.getValue(armProp)) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }

            if (player.isShiftKeyDown()) {
                if (!state.getValue(insProp)) return ItemInteractionResult.sidedSuccess(level.isClientSide);
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(insProp, false), 3);
                    if (!player.isCreative()) Block.popResource(level, pos, new ItemStack(stack.getItem()));
                    player.sendSystemMessage(Component.literal("絶縁碍子を取り外しました。"));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }

            if (state.getValue(insProp)) return ItemInteractionResult.sidedSuccess(level.isClientSide);
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(insProp, true), 3);
                if (!player.isCreative()) stack.shrink(1);
                player.sendSystemMessage(Component.literal("絶縁碍子を取り付けました。"));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!stack.is(Items.SHEARS)) return super.useItemOn(stack, state, level, pos, player, hand, hit);

        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof CrossArmBlockEntity be) {
                List<ConnectionData> connected = be.getConnections();
                if (!connected.isEmpty()) {
                    BlockPos otherPos = connected.get(0).pos();
                    be.removeConnection(otherPos);
                    if (level.getBlockEntity(otherPos) instanceof CableBlockEntity otherBe) {
                        otherBe.removeConnection(pos);
                        level.sendBlockUpdated(otherPos, level.getBlockState(otherPos), level.getBlockState(otherPos), 3);
                    }
                    level.sendBlockUpdated(pos, state, state, 3);
                    int remaining = be.getConnections().size();
                    player.sendSystemMessage(Component.literal("ケーブルを切断しました。" + (remaining > 0 ? "残り" + remaining + "本" : "")));
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
