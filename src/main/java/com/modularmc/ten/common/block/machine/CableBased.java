package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.blockentity.CmMachineBlockEntity;
import com.modularmc.ten.api.transmission.ConnectionType;
import com.modularmc.ten.api.transmission.ITransmitterProvider;
import com.modularmc.ten.api.transmission.Transmitter;
import com.modularmc.ten.common.blockentity.CableBlockEntity;
import com.modularmc.ten.common.blockentity.PipeBlockEntity;
import com.modularmc.ten.common.data.TENTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Map;

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

    /**
     * 从点击落点解析连接边方向：管道非满方块（核心 + 连接臂），hit.getDirection() 返回的是被击中
     * box 面的法线（点东臂顶面会得到 UP），不能代表连接方位；改按落点落在哪个臂内判定。
     *
     * @return 被点击的连接边方向；点击核心本体（非任何连接臂）时返回 null（不响应切换）
     */
    private static Direction resolveClickedEdge(BlockHitResult hit, BlockPos pos) {
        Vec3 rel = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        for (Direction d : Direction.values()) {
            int i = d.ordinal();
            // 容差 0.02：点击臂表面时落点在 box 边界上，需外扩才能命中
            for (AABB bb : PART_SHAPES[i].toAabbs()) {
                if (bb.inflate(0.02).contains(rel)) {
                    return d;
                }
            }
            for (AABB bb : CONNECT_SHAPES[i].toAabbs()) {
                if (bb.inflate(0.02).contains(rel)) {
                    return d;
                }
            }
        }
        return null;
    }

    private static VoxelShape buildShape(BlockState state) {
        VoxelShape shape = CORE_SHAPE;
        Direction[] dirs = Direction.values();
        for (int i = 0; i < 6; i++) {
            int conn = state.getValue(CONNECTION.get(dirs[i]));
            if (conn == 1) {
                shape = Shapes.or(shape, PART_SHAPES[i]);
            } else if (conn == 2) {
                shape = Shapes.or(shape, CONNECT_SHAPES[i]);
            }
        }
        return shape;
    }

    /**
     * 连接位/模式变化后的外观同步：仅值变化时 setBlock。
     * 直接持有 transmitter 引用，不查 BlockEntity——在 BE 注册流程（clearRemoved）中
     * 查询自身会再次创建 BE 导致无限递归（GameTest StackOverflow 已验证）。
     * vanilla 对相同 state 引用短路，不触发邻居链。
     */
    public void updateConnectionState(Level level, BlockPos pos, Transmitter<?, ?, ?> transmitter) {
        BlockState state = level.getBlockState(pos);
        BlockState target = state;
        for (Direction dir : Direction.values()) {
            int conn;
            if (Transmitter.connectionBit((byte) (transmitter.getTransmitterConnections() | transmitter.getAcceptorConnections()), dir)) {
                conn = Transmitter.connectionBit(transmitter.getTransmitterConnections(), dir) ? 1 : 2;
            } else {
                conn = 0;
            }
            target = target.setValue(CONNECTION.get(dir), conn);
        }
        if (target != state) {
            level.setBlock(pos, target, 3);
        }
    }

    public void updateConnections(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTION.get(dir), getConnectionType(level, pos, dir));
        }
        level.setBlock(pos, state, 3);
    }

    /**
     * 连接外观取值（0=无，1=管道间 part，2=设备 connect）：
     * BE 已接入传输网络时以 transmitter 连接位为权威源；
     * 放置/加载瞬间 BE 尚未组网，回退到旧版能力探测保证首帧外观正确。
     * PULL 输出面信息不经 blockstate（state 爆炸），由 BE 的 @DescSynced pullFacesMask 同步给客户端。
     */
    protected int getConnectionType(Level level, BlockPos pos, Direction dir) {
        if (level.getBlockEntity(pos) instanceof ITransmitterProvider provider && provider.getTransmitter() != null && (provider.getTransmitter().getTransmitterConnections() | provider.getTransmitter().getAcceptorConnections()) != 0) {
            Transmitter<?, ?, ?> t = provider.getTransmitter();
            return Transmitter.connectionBit((byte) (t.getTransmitterConnections() | t.getAcceptorConnections()), dir) ? (Transmitter.connectionBit(t.getTransmitterConnections(), dir) ? 1 : 2) : 0;
        }
        return legacyConnectionType(level, pos, dir);
    }

    /** 旧版能力探测（BE 未组网时的回退路径）。 */
    private int legacyConnectionType(Level level, BlockPos pos, Direction dir) {
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
        if (isPipe && level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, dir.getOpposite()) != null) {
            return 2;
        }
        if (isCable && level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, dir.getOpposite()) != null) {
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
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moved);
        if (!level.isClientSide()) {
            // 先刷新 transmitter 连接位（设备增删后位图权威更新），再同步 blockstate 外观
            if (level.getBlockEntity(pos) instanceof ITransmitterProvider provider) {
                provider.getTransmitter().refreshConnections();
            }
            updateConnections(level, pos);
        }
    }

    /**
     * 扳手右键：切换连接模式（移植自 TE4-New DuctInteractions）。
     * 管道↔设备边 PULL（抽取）↔ NORMAL（被动）二态轮询：ker 体系管道对设备只有两种有效语义
     * （PULL=主动抽取，NORMAL=被动接收投放，PUSH/NONE 对设备边无意义且会让轮询偏离预期）；
     * 管道↔管道边 NONE↔NORMAL（断开/连接）。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(TENTags.SPANNER)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 潜行+右键：放行到 Item.useOn → SpannerItem 拆解分支（管道/线缆均在 wrench_dismantleable tag 内）。
        // 必须在客户端分支之前：客户端同样需放行，否则拆解包不发出。
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ITransmitterProvider provider) || provider.getTransmitter() == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Transmitter<?, ?, ?> transmitter = provider.getTransmitter();
        // 按落点所在连接臂解析方向（hit.getDirection() 是击中面法线，非连接方位，见 resolveClickedEdge）
        Direction clicked = resolveClickedEdge(hit, pos);
        if (clicked == null) {
            // 点击管道核心本体（非任何连接臂）：不属于任何边，不切换模式
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ConnectionType current = transmitter.getConnectionType(clicked);
        boolean isDeviceEdge = !(level.getBlockEntity(pos.relative(clicked)) instanceof ITransmitterProvider);
        ConnectionType next;
        if (!isDeviceEdge) {
            // 管道间边：NONE↔NORMAL 二态
            next = current == ConnectionType.NONE ? ConnectionType.NORMAL : ConnectionType.NONE;
        } else {
            // 管道↔设备边：抽取(PULL)↔被动(NORMAL) 二态轮询
            next = current == ConnectionType.PULL ? ConnectionType.NORMAL : ConnectionType.PULL;
        }
        transmitter.setConnectionTypeRaw(clicked, next);
        transmitter.onModeChange(clicked);
        transmitter.refreshConnections();
        transmitter.rebuild();
        updateConnections(level, pos);
        // 显示键按边类型分流：设备边 pull/device_normal（输出/输入），管道间边 none/pipe_normal（断开/连接）
        String modeKey = (next == ConnectionType.PULL) ? "pull" : (next == ConnectionType.NORMAL && isDeviceEdge ? "device_normal" : (next == ConnectionType.NORMAL ? "pipe_normal" : next.getSerializedName()));
        player.displayClientMessage(Component.translatable(TEN.MOD_ID + ".connection_mode")
                .append(": ")
                .append(Component.translatable(TEN.MOD_ID + ".connection_mode." + modeKey)), true);
        return ItemInteractionResult.CONSUME;
    }
}
