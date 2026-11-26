package com.modularmc.ten.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * 探测「世界实际有内容」的垂直范围：采样区块的生态段从两端向中间找首个非空气段，
 * 反推最低 / 最高有方块的层。
 * <p>
 * 不读维度声明的构建高度——模组维度常把 min / max 声明得比实际地形宽松得多，
 * 照声明画框会在虚空里空转、扫描会白走几十层空气。声明值只在两处出现：区块生态段索引的基准，
 * 以及结果钳位（方块本来就不可能在构建高度之外）。
 * <p>
 * 无副作用：区块查询带 {@code requireChunk = false}，不为探测加载区块；采样区没有已加载区块或
 * 全是空气时返回 {@code null}，由调用方决定兜底与重试节奏。
 */
public final class VerticalExtent {

    /** 含端点的垂直区间。 */
    public record Extent(int minY, int maxY) {}

    /** 默认采样半径（区块）：1 → 3×3 = 9 个区块，覆盖机器周边 48×48 格。 */
    public static final int DEFAULT_CHUNK_RADIUS = 1;
    /** 采样半径上限（区块）：3×3 = 9 个区块，再多也换不来多少精度，白多扫区块。 */
    public static final int MAX_CHUNK_RADIUS = 2;
    /** 重探周期：附近挖 / 建会改变内容范围，5 秒对齐一次足够。 */
    public static final int REFRESH_TICKS = 100;
    /** 采样区还没加载好时的重试间隔。 */
    public static final int RETRY_TICKS = 10;

    private VerticalExtent() {}

    /**
     * 以默认采样半径探测 center 周边的内容垂直范围。
     *
     * @return 含端点的范围；采样区没有任何已加载区块，或已加载区块全是空气时为 {@code null}
     */
    public static Extent probe(Level level, BlockPos center) {
        return probe(level, center, DEFAULT_CHUNK_RADIUS);
    }

    /**
     * 探测 center 周边的内容垂直范围。
     *
     * @param chunkRadius 采样半径（区块），按工作盒宽度给，内部钳到
     *                    {@link #DEFAULT_CHUNK_RADIUS}..{@link #MAX_CHUNK_RADIUS}
     * @return 含端点的范围；采样区没有任何已加载区块，或已加载区块全是空气时为 {@code null}
     */
    public static Extent probe(Level level, BlockPos center, int chunkRadius) {
        int radius = Math.max(DEFAULT_CHUNK_RADIUS, Math.min(MAX_CHUNK_RADIUS, chunkRadius));
        int chunkX = center.getX() >> 4;
        int chunkZ = center.getZ() >> 4;
        int lowestSection = Integer.MAX_VALUE;
        int highestSection = Integer.MIN_VALUE;
        boolean sampled = false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // requireChunk=false：只取已加载区块，绝不为探测拉加载
                // （不能用 ChunkSource#getChunkNow：客户端未覆写它，到客户端恒为 null）
                ChunkAccess access = level.getChunkSource().getChunk(chunkX + dx, chunkZ + dz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) {
                    continue;
                }
                sampled = true;
                LevelChunkSection[] sections = chunk.getSections();
                int baseSectionY = chunk.getMinBuildHeight() >> 4;
                for (int i = 0; i < sections.length; i++) {
                    if (!sections[i].hasOnlyAir()) {
                        lowestSection = Math.min(lowestSection, baseSectionY + i);
                        break;
                    }
                }
                for (int i = sections.length - 1; i >= 0; i--) {
                    if (!sections[i].hasOnlyAir()) {
                        highestSection = Math.max(highestSection, baseSectionY + i);
                        break;
                    }
                }
            }
        }
        // 一个区块都没加载（刚放置 / 区块被卸载）或采样区全空：没有可推断的内容，交给调用方兜底
        if (!sampled || lowestSection == Integer.MAX_VALUE) {
            return null;
        }
        int minY = Math.max(level.getMinBuildHeight(), lowestSection << 4);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, (highestSection << 4) + 15);
        return new Extent(minY, maxY);
    }
}
