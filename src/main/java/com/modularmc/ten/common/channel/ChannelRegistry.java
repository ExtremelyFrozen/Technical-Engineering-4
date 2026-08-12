package com.modularmc.ten.common.channel;

import com.modularmc.ten.TEN;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 频道注册表：存档级持久化的频道 → 共享存储映射（末影箱模式数据层）。
 * <p>
 * 持久化：挂载到主世界 {@link net.minecraft.server.level.ServerLevel} 的
 * {@link net.minecraft.world.level.storage.SavedDataStorage}（EnderStorage 模式
 * 的 SavedData 实现），文件位于存档 data 目录 {@code channel_registry.dat}。
 * 懒加载（{@link #get} 经 computeIfAbsent）+ 事件驱动保存（任何变更 setDirty，
 * 含共享存储内容变更的监听回调）。跨维度：所有维度统一经 {@code server.overworld()}
 * 访问同一实例，频道全局共享。
 * <p>
 * 成员跟踪：每个频道维护已接入成员集合（{@code 维度:坐标} 标识），join/leave 幂等；
 * 成员数=集合大小，驱动 {@link SharedStorage} 动态容量。
 */
public final class ChannelRegistry extends SavedData {

    // ───── 持久化（Codec）─────
    // 注意：CODEC 必须先于 TYPE 声明。Java 静态字段按声明顺序初始化，
    // 若 TYPE 先引用 CODEC，捕获到的将是 null，导致保存时 encodeUnchecked NPE。

    private record Entry(ChannelKey key, SharedStorage storage, Set<String> memberIds) {

        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ChannelKey.CODEC.fieldOf("key").forGetter(Entry::key),
                SharedStorage.codec().fieldOf("storage").forGetter(Entry::storage),
                Codec.STRING.listOf().xmap(list -> (Set<String>) new HashSet<>(list), list -> new ArrayList<>(list)).fieldOf("members").forGetter(Entry::memberIds)).apply(instance, Entry::new));
    }

    public static final Codec<ChannelRegistry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().fieldOf("channels").forGetter(ChannelRegistry::encodeEntries)).apply(instance, ChannelRegistry::decodeEntries));

    private static final SavedDataType<ChannelRegistry> TYPE = new SavedDataType<>(
            TEN.id("channel_registry"),
            ChannelRegistry::new,
            ChannelRegistry.CODEC);

    private final Map<ChannelKey, SharedStorage> storages = new LinkedHashMap<>();
    private final Map<ChannelKey, Set<String>> members = new HashMap<>();

    /**
     * 获取（懒加载）全局频道注册表。任何维度均可调用；数据统一持久化在主世界。
     */
    public static ChannelRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public SharedStorage getOrCreate(ChannelKey key) {
        return storages.computeIfAbsent(key, k -> {
            SharedStorage storage = switch (k.type()) {
                case ITEM -> SharedStorage.forItem();
                case FLUID -> SharedStorage.forFluid();
                case ENERGY -> SharedStorage.forEnergy();
            };
            // 共享存储内容变化 → 存档脏标记（事件驱动保存）
            storage.setChangeListener(this::setDirty);
            return storage;
        });
    }

    public SharedStorage get(ChannelKey key) {
        return storages.get(key);
    }

    public int memberCount(ChannelKey key) {
        return members.getOrDefault(key, Set.of()).size();
    }

    /** 按类型列出全部频道（目录同步用，保持创建顺序）。 */
    public List<ChannelKey> listChannels(ChannelType type) {
        List<ChannelKey> result = new ArrayList<>();
        for (ChannelKey key : storages.keySet()) {
            if (key.type() == type) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * 接入频道。幂等：同一成员重复 join 返回 false 且不重复计数。
     * 新频道自动创建共享存储；容量按新成员数重算。
     *
     * @return true 表示本次实际新增成员
     */
    public boolean join(ChannelKey key, String memberId) {
        SharedStorage storage = getOrCreate(key);
        Set<String> set = members.computeIfAbsent(key, k -> new HashSet<>());
        if (!set.add(memberId)) {
            return false;
        }
        storage.refreshMemberCount(set.size());
        setDirty();
        return true;
    }

    /**
     * 退出频道。成员数减少后容量重算（拒绝缩容由 {@link SharedStorage} 保证）。
     * 空频道（成员 0）保留存储与内容，可随时重新接入。
     *
     * @return true 表示本次确实移除了成员
     */
    public boolean leave(ChannelKey key, String memberId) {
        SharedStorage storage = storages.get(key);
        if (storage == null) {
            return false;
        }
        Set<String> set = members.get(key);
        if (set == null || !set.remove(memberId)) {
            return false;
        }
        storage.refreshMemberCount(set.size());
        setDirty();
        return true;
    }

    /**
     * 删除频道。仅空频道可删：共享存储内容全空且成员集为空；两 map 同步清理并打存档脏标记。
     * 非空（内容或成员残留）不执行、返回 false——内容不丢硬约束。
     *
     * @return true 表示删除成功
     */
    public boolean remove(ChannelKey key) {
        SharedStorage storage = storages.get(key);
        if (storage == null) {
            return false;
        }
        if (!storage.isEmpty() || !members.getOrDefault(key, Set.of()).isEmpty()) {
            return false;
        }
        storages.remove(key);
        members.remove(key);
        setDirty();
        return true;
    }

    private List<Entry> encodeEntries() {
        List<Entry> entries = new ArrayList<>(storages.size());
        for (Map.Entry<ChannelKey, SharedStorage> e : storages.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue(), members.getOrDefault(e.getKey(), Set.of())));
        }
        return entries;
    }

    private static ChannelRegistry decodeEntries(List<Entry> entries) {
        ChannelRegistry registry = new ChannelRegistry();
        for (Entry entry : entries) {
            registry.storages.put(entry.key(), entry.storage());
            registry.members.put(entry.key(), new HashSet<>(entry.memberIds()));
            entry.storage().refreshMemberCount(entry.memberIds().size());
            entry.storage().setChangeListener(registry::setDirty);
        }
        return registry;
    }
}
