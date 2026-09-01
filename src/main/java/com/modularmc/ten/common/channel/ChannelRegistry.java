package com.modularmc.ten.common.channel;

import com.modularmc.ten.TEN;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

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
 * {@link net.minecraft.world.level.storage.DimensionDataStorage}（SavedData 实现），
 * 文件位于存档 data 目录 {@code channel_registry.dat}。
 * 懒加载（{@link #get} 经 computeIfAbsent）+ 事件驱动保存（任何变更 setDirty，
 * 含共享存储内容变更的监听回调）。跨维度：所有维度统一经 {@code server.overworld()}
 * 访问同一实例，频道全局共享。
 * <p>
 * 成员跟踪：每个频道维护已接入成员集合（{@code 维度:坐标} 标识），join/leave 幂等；
 * 成员数=集合大小，驱动 {@link SharedStorage} 动态容量。
 * <p>
 * 1.21.1 适配：SavedDataType/Codec → SavedData.Factory + CompoundTag（loadAdditional/saveAdditional）。
 */
public final class ChannelRegistry extends SavedData {

    private static final SavedData.Factory<ChannelRegistry> FACTORY = new SavedData.Factory<>(
            ChannelRegistry::new,
            (tag, registries) -> {
                ChannelRegistry registry = new ChannelRegistry();
                registry.loadFromNbt(tag, registries);
                return registry;
            });

    private final Map<ChannelKey, SharedStorage> storages = new LinkedHashMap<>();
    private final Map<ChannelKey, Set<String>> members = new HashMap<>();

    /**
     * 获取（懒加载）全局频道注册表。任何维度均可调用；数据统一持久化在主世界。
     */
    public static ChannelRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, TEN.MOD_ID + "_channel_registry");
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

    // ───── 持久化（1.21.1 NBT）─────

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag channels = new ListTag();
        for (Map.Entry<ChannelKey, SharedStorage> e : storages.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("name", e.getKey().name());
            entryTag.putString("type", e.getKey().type().name());
            CompoundTag storageTag = new CompoundTag();
            e.getValue().saveToNbt(storageTag, registries);
            entryTag.put("storage", storageTag);
            ListTag memberList = new ListTag();
            for (String memberId : members.getOrDefault(e.getKey(), Set.of())) {
                memberList.add(StringTag.valueOf(memberId));
            }
            entryTag.put("members", memberList);
            channels.add(entryTag);
        }
        tag.put("channels", channels);
        return tag;
    }

    /**
     * 从 NBT 恢复（Factory deserializer 调用；1.21.1 的 SavedData 无 loadAdditional 钩子，
     * 加载逻辑全部在 deserializer 侧完成）。
     */
    private void loadFromNbt(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag channels = tag.getList("channels", Tag.TAG_COMPOUND);
        for (int i = 0; i < channels.size(); i++) {
            CompoundTag entryTag = channels.getCompound(i);
            String name = entryTag.getString("name");
            String typeName = entryTag.getString("type");
            ChannelType type;
            try {
                type = ChannelType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                continue; // 未知类型跳过（前向兼容）
            }
            ChannelKey key = new ChannelKey(name, type);
            SharedStorage storage = switch (type) {
                case ITEM -> SharedStorage.forItem();
                case FLUID -> SharedStorage.forFluid();
                case ENERGY -> SharedStorage.forEnergy();
            };
            storage.loadFromNbt(entryTag.getCompound("storage"), registries);
            storage.setChangeListener(this::setDirty);

            Set<String> memberSet = new HashSet<>();
            ListTag memberList = entryTag.getList("members", Tag.TAG_STRING);
            for (int m = 0; m < memberList.size(); m++) {
                memberSet.add(memberList.getString(m));
            }
            storage.refreshMemberCount(memberSet.size());

            storages.put(key, storage);
            members.put(key, memberSet);
        }
    }
}
