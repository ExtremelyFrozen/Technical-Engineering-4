package com.modularmc.ten.common.block.machine;

import com.modularmc.ten.TEN;
import com.modularmc.ten.api.transmission.CableRenderState;
import com.modularmc.ten.api.transmission.ConnectionType;
import com.modularmc.ten.api.transmission.ITransmitterProvider;
import com.modularmc.ten.api.transmission.Transmitter;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 线缆/管道的公共方块基类。
 * <p>
 * 继承 {@link BaseMachineBlock} 而不是 {@code HorizontalMachineBlock}：后者多带一个用不到的 {@code FACING}。
 * 连接外观与 active 也不再进 BlockState，改由 {@link CableRenderState} 经 {@code ModelData} 驱动——
 * 原先六个方向各一个 0..2 属性让单个方块有 3^6 × 2 × 2 = 2916 个状态，7 个方块合计 2 万余个，
 * 注册表冻结时逐个建 Cache、模型烘焙时逐状态匹配 multipart 条件，是启动期最大的一笔固定开销。
 * 现在只剩 {@code ACTIVE}（父类约束，恒为 false）与 {@code WATERLOGGED}，共 4 个状态/方块。
 */
public class CableBased extends BaseMachineBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected static final VoxelShape CORE_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    // ── Shape helpers matching JSON model sizes ──────────────────────────
    // Type 1 (part) — 6×6×5 connector: core face → block edge
    private static final VoxelShape[] PART_SHAPES = new VoxelShape[6];
    // Type 2 (connect) — 6×6×4 inner + 8×8×2 flange at block edge
    private static final VoxelShape[] CONNECT_SHAPES = new VoxelShape[6];
    /** 固定遍历顺序，下标与 PART_SHAPES / CONNECT_SHAPES 一致（{@code Direction.values()} 每次调用都会克隆数组）。 */
    private static final Direction[] DIRECTION_ORDER = Direction.values();
    /**
     * 连接位组合到合并形状的惰性缓存：6 个方向各取 0/1/2，共 3^6 = 729 种。
     * <p>
     * 形状只取决于 {@link CableRenderState} 的连接位，与具体是哪种线缆无关，
     * 7 个 {@link CableBased} 实例共用这张表；运行期 {@code BlockCollisions} 走三参数
     * {@code getCollisionShape} 绕开原版 Cache，实体每 tick 移动都会查一次，必须缓存。
     * <p>
     * 用 {@link AtomicReferenceArray} 而不是普通数组：渲染线程与服务端线程会并发查同一张表，
     * 普通数组的惰性写入没有安全发布保证，另一线程可能看到未初始化完的 {@code VoxelShape}。
     */
    private static final AtomicReferenceArray<VoxelShape> SHAPE_CACHE = new AtomicReferenceArray<>(729);
    static {
        for (int i = 0; i < 6; i++) {
            Direction d = DIRECTION_ORDER[i];
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
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return buildShape(renderPackedAt(level, pos));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return buildShape(renderPackedAt(level, pos));
    }

    /** 碰撞箱是服务端逻辑，从 BE 的权威渲染态取；BE 尚未就位时退化为纯核心形状。 */
    private static int renderPackedAt(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return 0;
        }
        if (level.getBlockEntity(pos) instanceof ITransmitterProvider provider) {
            return provider.getRenderPacked();
        }
        return 0;
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

    private static VoxelShape buildShape(int packed) {
        CableRenderState render = new CableRenderState(packed);
        int idx = 0;
        for (int i = 0; i < 6; i++) {
            idx = idx * 3 + render.connection(i);
        }
        VoxelShape cached = SHAPE_CACHE.get(idx);
        if (cached == null) {
            cached = computeShape(render);
            SHAPE_CACHE.set(idx, cached);
        }
        return cached;
    }

    /** 缓存未命中时的实际合并；同一连接组合只会走到这里一次。 */
    private static VoxelShape computeShape(CableRenderState render) {
        VoxelShape shape = CORE_SHAPE;
        for (int i = 0; i < 6; i++) {
            int conn = render.connection(i);
            if (conn == 1) {
                shape = Shapes.or(shape, PART_SHAPES[i]);
            } else if (conn == 2) {
                shape = Shapes.or(shape, CONNECT_SHAPES[i]);
            }
        }
        return shape;
    }

    /**
     * 连接位/模式变化后只刷新 BE 的渲染态，不再 setBlock。
     * <p>
     * 旧实现把六个连接值写进 BlockState：每次连接变化都是一次 setBlock(flags=3)，
     * 连带邻居更新、光照重算、区块重绘与网络同步。现在渲染态存在 BE 的 {@code renderPacked}，
     * 由 BE 覆写的更新标签随方块更新下发，客户端落值后再请求重绘。
     */
    public void updateConnections(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ITransmitterProvider provider) {
            provider.refreshRenderState();
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(ACTIVE, false)
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
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
            // 先刷新 transmitter 连接位（设备增删后位图权威更新），再同步渲染态
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
