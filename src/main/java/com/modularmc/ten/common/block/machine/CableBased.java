package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.capability.CapabilityAdapters;
import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class CableBased extends HorizontalMachineBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, IntegerProperty> CONNECTION = Map.of(
            Direction.NORTH, IntegerProperty.create("north", 0, 2),
            Direction.EAST, IntegerProperty.create("east", 0, 2),
            Direction.SOUTH, IntegerProperty.create("south", 0, 2),
            Direction.WEST, IntegerProperty.create("west", 0, 2),
            Direction.UP, IntegerProperty.create("up", 0, 2),
            Direction.DOWN, IntegerProperty.create("down", 0, 2));

    protected static final VoxelShape CORE_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    // ── Shape helpers matching JSON model sizes ──────────────────────────
    // Type 1 (part) — 6×6×5 connector: core face → block edge
    private static final VoxelShape[] PART_SHAPES = new VoxelShape[6];
    // Type 2 (connect) — 6×6×4 inner + 8×8×2 flange at block edge
    private static final VoxelShape[] CONNECT_SHAPES = new VoxelShape[6];
    static {
        Direction[] dirs = Direction.values();
        for (int i = 0; i < 6; i++) {
            Direction d = dirs[i];
            VoxelShape part, connect;
            switch (d) {
                case NORTH -> {
                    part = Block.box(5, 5, 0, 11, 11, 5);
                    connect = Shapes.or(Block.box(5, 5, 1, 11, 11, 5), Block.box(4, 4, 0, 12, 12, 2));
                }
                case SOUTH -> {
                    part = Block.box(5, 5, 11, 11, 11, 16);
                    connect = Shapes.or(Block.box(5, 5, 11, 11, 11, 15), Block.box(4, 4, 14, 12, 12, 16));
                }
                case EAST -> {
                    part = Block.box(11, 5, 5, 16, 11, 11);
                    connect = Shapes.or(Block.box(11, 5, 5, 15, 11, 11), Block.box(14, 4, 4, 16, 12, 12));
                }
                case WEST -> {
                    part = Block.box(0, 5, 5, 5, 11, 11);
                    connect = Shapes.or(Block.box(1, 5, 5, 5, 11, 11), Block.box(0, 4, 4, 2, 12, 12));
                }
                case UP -> {
                    part = Block.box(5, 11, 5, 11, 16, 11);
                    connect = Shapes.or(Block.box(5, 11, 5, 11, 15, 11), Block.box(4, 14, 4, 12, 16, 12));
                }
                case DOWN -> {
                    part = Block.box(5, 0, 5, 11, 5, 11);
                    connect = Shapes.or(Block.box(5, 1, 5, 11, 5, 11), Block.box(4, 0, 4, 12, 2, 12));
                }
                default -> {
                    throw new AssertionError("unexpected direction: " + d);
                }
            }
            PART_SHAPES[i] = part;
            CONNECT_SHAPES[i] = connect;
        }
    }

    /**
     * Thread-safe shape cache shared across all Cable/Pipe block variants.
     * <p>
     * Key = base-3 encoding of the six connection property values (0..2 each),
     * read in {@link Direction#values()} order: DOWN, UP, NORTH, SOUTH, WEST, EAST.
     * Range [0, 728] covers all 3^6 = 729 connection combinations.
     * <p>
     * {@code ACTIVE}, {@code FACING}, and {@code WATERLOGGED} are deliberately
     * excluded from the key because {@link #buildShape(BlockState)} only reads
     * the six connection properties. Including them would inflate the cache to
     * 11,664 entries per block variant without any benefit.
     */
    private static final AtomicReferenceArray<VoxelShape> SHAPE_CACHE = new AtomicReferenceArray<>(729);

    public CableBased(Properties props) {
        super(props);
    }

    public CableBased() {
        this(Properties.of()
                .mapColor(MapColor.NONE)
                .strength(1, 3)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isViewBlocking((s, l, p) -> false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
        CONNECTION.values().forEach(builder::add);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return buildShape(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return buildShape(state);
    }

    private static VoxelShape buildShape(BlockState state) {
        // Cache key from the six connection properties only.
        // ACTIVE, FACING, WATERLOGGED are NOT read by this method,
        // so they are excluded — keeping the cache at 729 entries
        // instead of 11,664 per block variant across 7 blocks.
        Direction[] dirs = Direction.values(); // DOWN, UP, NORTH, SOUTH, WEST, EAST
        int key = 0;
        int mult = 1;
        for (int i = 0; i < 6; i++) {
            key += state.getValue(CONNECTION.get(dirs[i])) * mult;
            mult *= 3;
        }

        // Fast path: already cached (lock-free O(1) read)
        VoxelShape cached = SHAPE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        // Compute shape via original Shapes.or() logic — unchanged semantics
        VoxelShape shape = CORE_SHAPE;
        for (int i = 0; i < 6; i++) {
            int conn = state.getValue(CONNECTION.get(dirs[i]));
            if (conn == 1) {
                shape = Shapes.or(shape, PART_SHAPES[i]);
            } else if (conn == 2) {
                shape = Shapes.or(shape, CONNECT_SHAPES[i]);
            }
        }

        // Publish to cache atomically.
        // If another thread computed first, use the already-cached value.
        if (!SHAPE_CACHE.compareAndSet(key, null, shape)) {
            shape = SHAPE_CACHE.get(key);
        }

        return shape;
    }

    public void updateConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTION.get(dir), getConnectionType(level, pos, dir));
        }
        level.setBlock(pos, state, 3);
    }

    protected int getConnectionType(Level level, BlockPos pos, Direction dir) {
        BlockPos neighbor = pos.relative(dir);
        BlockEntity be = level.getBlockEntity(pos);
        BlockEntity neighborBe = level.getBlockEntity(neighbor);

        if (be == null || neighborBe == null) return 0;

        boolean isPipe = be instanceof PipeBlockEntity;
        boolean isCable = be instanceof CableBlockEntity;
        if (isPipe && neighborBe instanceof PipeBlockEntity) {
            return 1;
        }
        if (isCable && neighborBe instanceof CableBlockEntity) {
            return 1;
        }
        if (isPipe && CapabilityAdapters.getItems(level, neighbor, dir.getOpposite()) != null) {
            return 2;
        }
        if (isCable && CapabilityAdapters.getEnergy(level, neighbor, dir.getOpposite()) != null) {
            return 2;
        }
        if (neighborBe instanceof CmMachineBlockEntity) {
            return 2;
        }
        return 0;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = defaultBlockState();
        state = state.setValue(ACTIVE, false)
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTION.get(dir), 0);
        }
        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            updateConnections(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, net.minecraft.world.level.redstone.Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, moved);
        if (!level.isClientSide()) {
            updateConnections(level, pos);
        }
    }
}
