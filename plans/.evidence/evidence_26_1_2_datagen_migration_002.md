# Evidence: TASK-002 — NeoForge 26.1.2 Datagen Provider API 确认

> **计划**: `plan_26_1_2_datagen_migration.md`
> **TASK**: TASK-002 — 确认 Gradle 坐标 → 检索对应 NeoForge datagen Provider API
> **执行日期**: 2026-07-10
> **执行者**: 猫娘文案师-芙蕾（证据整理）/ 猫娘检索员-诺雅（外部检索）
> **来源 URL**:
> - NeoForge 26.1.2 tag: https://github.com/neoforged/NeoForge/tree/26.1.2
> - GatherDataEvent: https://github.com/neoforged/NeoForge/blob/26.1.2/src/main/java/net/neoforged/neoforge/data/event/GatherDataEvent.java
> - BlockTagsProvider: https://github.com/neoforged/NeoForge/blob/26.1.2/src/main/java/net/neoforged/neoforge/common/data/BlockTagsProvider.java
> - ItemTagsProvider: https://github.com/neoforged/NeoForge/blob/26.1.2/src/main/java/net/neoforged/neoforge/common/data/ItemTagsProvider.java
> - LanguageProvider: https://github.com/neoforged/NeoForge/blob/26.1.2/src/main/java/net/neoforged/neoforge/common/data/LanguageProvider.java
> - FluidTagsProvider patch: https://github.com/neoforged/NeoForge/blob/26.1.2/patches/net/minecraft/data/tags/FluidTagsProvider.java.patch
> - LootTableProvider patch: https://github.com/neoforged/NeoForge/blob/26.1.2/patches/net/minecraft/data/loot/LootTableProvider.java.patch

---

## 1. 版本基线（来自 Gradle 配置）

| 属性 | 值 | 来源文件 |
|------|----|----------|
| Minecraft / NeoForge 版本 | `26.1.2` (即 MC 1.21.1) | `gradle/libs.versions.toml` → `minecraft = "26.1.2"` |
| NeoForge 精确版本 | `26.1.2.78` | `gradle/libs.versions.toml` → `neoForge = "26.1.2.78"` |
| modDevGradle | `2.0.141` | `gradle/libs.versions.toml` → `modDevGradle = "2.0.141"` |
| Java | `25` | `build.gradle` → `toolchain.languageVersion = JavaLanguageVersion.of(25)` |
| Loader | `4` (待确认) | `gradle/libs.versions.toml` → `loader = "4"`（含 TODO 标记） |
| Mod ID | `kenergyengineering` | `gradle.properties` |
| Mod 版本 | `4.1.0` | `gradle.properties` |
| Maven Group | `com.modularmc.ten` | `gradle.properties` |

### runClientData 配置（`gradle/scripts/moddevgradle.gradle`）

```groovy
clientData {
    clientData()
    sourceSet = sourceSets.main
    ideName = "Data Generation"
    gameDirectory.set(file('run/data'))
    programArguments.addAll('--mod', project.mod_id)
    programArguments.addAll('--all')
    programArguments.addAll('--output', file('src/generated/resources/').getAbsolutePath())
    programArguments.addAll('--existing', file('src/main/resources/').getAbsolutePath())
}
```

### sourceSets 配置（`build.gradle`）

```groovy
main.resources {
    srcDir 'src/generated/resources'
}
```

### processResources 策略

```groovy
processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
```

---

## 2. GatherDataEvent 注册方式

**全类名**: `net.neoforged.neoforge.data.event.GatherDataEvent`

### 2.1 事件类层次

| 类 | 说明 |
|----|------|
| `GatherDataEvent` | 基类，无 Client/Server 区分 |
| `GatherDataEvent.Client` | Client 侧数据（模型、blockstate、语言） |
| `GatherDataEvent.Server` | Server 侧数据（战利品表、标签、配方、advancements） |

### 2.2 Provider 注册方法

**推荐方式**（来自源码分析）:

```java
// 方式 1: 直接 addProvider
event.addProvider(new MyProvider(...));

// 方式 2: 通过 createProvider 工厂方法
event.createProvider(MyProvider::new);                     // DataProviderFromOutput (仅 PackOutput)
event.createProvider(MyProvider::new);                     // DataProviderFromOutputLookup (PackOutput + LookupProvider)

// 方式 3: 标签专用便利工厂
event.createBlockAndItemTags(
    (output, lookup) -> new ModBlockTagsProvider(output, lookup, modId),
    (output, lookup, blockTags) -> new ModItemTagsProvider(output, lookup, blockTags, modId)
);

// 方式 4: 动态注册（datapack registry）
event.createDatapackRegistryObjects(new RegistrySetBuilder()
    .add(Registries.DAMAGE_TYPE, bootstrap -> { ... }));
```

**内部实现**: `event.addProvider(provider)` → `dataGenerator.addProvider(true, provider)`。`true` 表示强制启用。

### 2.3 当前项目用法（现状）

```java
@EventBusSubscriber(modid = TEN.MOD_ID, value = Dist.CLIENT)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        generator.addProvider(true, new LanguageProvider(output, TEN.MOD_ID, "zh_cn") { ... });
        generator.addProvider(true, new TENRecipeGen(output, event.getLookupProvider()));
        generator.addProvider(true, new TENVanillaPackGen(output));
    }
}
```

**关键发现**: 当前使用老式 `generator.addProvider(true, ...)`。NeoForge 26.1.2 推荐改用 `event.addProvider(...)` 或 `event.createProvider(...)`。当前方式仍兼容但需迁移。

### 2.4 建议的迁移模式

```java
@EventBusSubscriber(modid = TEN.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        // Block states + item models (Client side)
        event.addProvider(new TENBlockModelProvider(event.getGenerator().getPackOutput()));
    }

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        var output = event.getGenerator().getPackOutput();
        var lookup = event.getLookupProvider();

        // Loot tables
        event.addProvider(new TENLootTableProvider(output, lookup));

        // Tags: Block → Item (copy) → Fluid
        var blockTags = event.createBlockAndItemTags(
            (out, lk) -> new TENBlockTagsProvider(out, lk, TEN.MOD_ID),
            (out, lk, bt) -> new TENItemTagsProvider(out, lk, bt, TEN.MOD_ID)
        );

        // Fluid tags
        event.addProvider(new TENFluidTagsProvider(output, lookup, TEN.MOD_ID));

        // Recipes (existing)
        event.addProvider(new TENRecipeGen(output, lookup));
        event.addProvider(new TENVanillaPackGen(output));
    }

    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent event) {
        if (!event.includeClient()) return;
        // Language (existing)
        event.addProvider(new LanguageProvider(event.getGenerator().getPackOutput(), TEN.MOD_ID, "zh_cn") {
            @Override protected void addTranslations() { ... }
        });
    }
}
```

> ⚠️ **注意**: `GatherDataEvent.Client` 和 `GatherDataEvent.Server` 是独立的事件类，不是子事件关系。需要各自有 `@SubscribeEvent` 方法，且 `value = Dist.CLIENT` 只在 Client 侧注册事件。

---

## 3. Provider 清单：包路径、构造器、使用方式

### 3.1 BlockModelGenerators（取代旧 BlockStateProvider）

| 项目 | 内容 |
|------|------|
| **全类名** | `net.minecraft.data.models.BlockModelGenerators` |
| **包** | Vanilla `net.minecraft.data.models`（NeoForge 无 patch） |
| **构造器** | `BlockModelGenerators(Consumer<BlockStateGenerator> output, ModelProvider modelProvider, List<BlockStateDefinition> stateDefinitions, ResourceManager resourceManager)` |
| **抽象方法** | 无（通过 Consumer 和 ModelProvider 协作） |
| **覆盖范围** | 方块状态 JSON + 方块模型 JSON |
| **状态** | ❌ **旧 BlockStateProvider 不存在**。需使用 Vanilla API。 |
| **复杂度** | 🔴 高 — Vanilla API 较底层，需大量样板代码 |

**代码骨架**:

```java
// TODO: TASK-003 需进一步探索如何封装 Vanilla BlockModelGenerators
// 现有已知方法: createSimpleBlock(Block), createRotatedVariant(Block, ...)
// blockstate 通过 Consumer<BlockStateGenerator> 输出
```

### 3.2 ItemModelGenerators（取代旧 ItemModelProvider）

| 项目 | 内容 |
|------|------|
| **全类名** | `net.minecraft.data.models.ItemModelGenerators` |
| **包** | Vanilla `net.minecraft.data.models`（NeoForge 无 patch） |
| **构造器** | `ItemModelGenerators(Consumer<ItemModelGenerator> output, ModelProvider modelProvider)` |
| **抽象方法** | 无 |
| **覆盖范围** | 物品模型 JSON |
| **状态** | ❌ **旧 ItemModelProvider 不存在**。需使用 Vanilla API。 |
| **复杂度** | 🔴 高 |

### 3.3 LootTableProvider

| 项目 | 内容 |
|------|------|
| **全类名** | `net.minecraft.data.loot.LootTableProvider` |
| **包** | Vanilla `net.minecraft.data.loot`（NeoForge 有 patch） |
| **构造器** | `LootTableProvider(PackOutput output, Set<ResourceKey<LootTable>> requiredTables, List<SubProviderEntry> subProviders)` |
| **抽象方法** | 无（通过 `SubProviderEntry` 列表配置子 provider） |
| **子 provider 基类** | `net.minecraft.data.loot.BlockLootSubProvider` |
| **覆盖范围** | 方块战利品表 JSON |
| **NeoForge patch** | `getTables()`、`validate()`、条件支持 |

**代码骨架**:

```java
// LootTableProvider — 主 Provider
public class TENLootTableProvider extends LootTableProvider {
    public TENLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, TENLootTableProvider.getRequiredTables(),
              List.of(new SubProviderEntry(TENBlockLoot::new, LootContextParamSets.BLOCK)));
    }

    private static Set<ResourceKey<LootTable>> getRequiredTables() {
        return TENBlocks.ALL_BLOCKS.stream()
            .map(block -> block.getLootTable())
            .collect(Collectors.toSet());
    }
}

// BlockLootSubProvider — 方块战利品
public class TENBlockLoot extends BlockLootSubProvider {
    public TENBlockLoot(HolderLookup.Provider registries) {
        super(Collections.emptySet(), List.of(), registries);
    }

    @Override
    protected void generate() {
        // 掉落自身: dropSelf(block)
        // 矿物掉落: add(oreBlock, createOreDrop(oreBlock, rawItem))
        // 流体: noLootTable(fluidBlock)
    }
}
```

### 3.4 BlockTagsProvider（NeoForge 包装版）

| 项目 | 内容 |
|------|------|
| **全类名** | `net.neoforged.neoforge.common.data.BlockTagsProvider` |
| **包** | `net.neoforged.neoforge.common.data` |
| **构造器** | `BlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId)` |
| **继承链** | → `IntrinsicHolderTagsProvider<Block>` → `TagsProvider<Block>` |
| **抽象方法** | `addTags(HolderLookup.Provider registries)` |
| **覆盖范围** | Block tag JSON |

**代码骨架**:

```java
public class TENBlockTagsProvider extends BlockTagsProvider {
    public TENBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(output, lookupProvider, modId);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(TENBlocks.TIN_ORE.get(), TENBlocks.NICKEL_ORE.get(), ...);
        tag(BlockTags.NEEDS_IRON_TOOL)
            .add(TENBlocks.TIN_ORE.get(), ...);
        tag(TENTags.MACHINES)
            .add(TENBlocks.MACHINE_SMELTER.get(), ...);
    }
}
```

### 3.5 ItemTagsProvider（NeoForge 包装版）

| 项目 | 内容 |
|------|------|
| **全类名** | `net.neoforged.neoforge.common.data.ItemTagsProvider` |
| **包** | `net.neoforged.neoforge.common.data` |
| **构造器** | `ItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId)` |
| | 备用: `ItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTags, String modId)` |
| **继承链** | → `IntrinsicHolderTagsProvider<Item>` → `TagsProvider<Item>` |
| **抽象方法** | `addTags(HolderLookup.Provider registries)` |
| **覆盖范围** | Item tag JSON |

**附加类** — `BlockTagCopyingItemTagProvider`:
- 自动从 block tag 复制到 item tag（适用于 `mineable/` 等 block tag 对应的 item tag）
- 构造器: `BlockTagCopyingItemTagProvider(PackOutput, CompletableFuture<HolderLookup.Provider>, CompletableFuture<TagLookup<Block>>, String modId)`

**代码骨架**:

```java
public class TENItemTagsProvider extends ItemTagsProvider {
    public TENItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               CompletableFuture<TagLookup<Block>> blockTags, String modId) {
        super(output, lookupProvider, blockTags, modId);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(ItemTags.STONE_TOOL_MATERIALS)
            .add(TENItems.TIN_INGOT.get(), ...);

        // 从 block tag 复制
        copy(BlockTags.MINEABLE_WITH_PICKAXE, ItemTags.STONE_TOOL_MATERIALS);
    }
}
```

### 3.6 FluidTagsProvider（Vanilla + NeoForge patch）

| 项目 | 内容 |
|------|------|
| **全类名** | `net.minecraft.data.tags.FluidTagsProvider` |
| **包** | Vanilla `net.minecraft.data.tags`（NeoForge 有 patch） |
| **构造器** | `FluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId)` **← 推荐** |
| | `FluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)` **← 已弃用** |
| **继承链** | → `IntrinsicHolderTagsProvider<Fluid>` → `TagsProvider<Fluid>` |
| **抽象方法** | `addTags(HolderLookup.Provider registries)` |
| **覆盖范围** | Fluid tag JSON |
| **NeoForge patch** | 新增了 `modId` 参数构造器 |

**代码骨架**:

```java
public class TENFluidTagsProvider extends FluidTagsProvider {
    public TENFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, String modId) {
        super(output, lookupProvider, modId);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(FluidTags.WATER)
            .add(TENFluids.LIQUID_XP.get(), TENFluids.LIQUID_BIZARRERIE.get());
    }
}
```

### 3.7 LanguageProvider（NeoForge 版）

| 项目 | 内容 |
|------|------|
| **全类名** | `net.neoforged.neoforge.common.data.LanguageProvider` |
| **包** | `net.neoforged.neoforge.common.data` |
| **构造器** | `LanguageProvider(PackOutput output, String modid, String locale)` |
| **抽象方法** | `addTranslations()` |
| **常用方法** | `addBlock(Block, String)`, `addItem(Item, String)`, `add(EntityType, String)`, `add(String, String)` |
| **状态** | ✅ 已在 `DataGenerators.java` 中使用，协议兼容 |

---

## 4. ExistingFileHelper 状态

| 项目 | 内容 |
|------|------|
| **状态** | ❌ **已移除** |
| **说明** | NeoForge 26.1.2 / Vanilla 1.21.1 不再需要 `ExistingFileHelper` |
| **替代** | 使用 `GatherDataEvent.getResourceManager(PackType)` 获取 `ResourceManager` 来验证资源 |
| **影响** | BlockStateProvider/ItemModelProvider 构造器本需 `ExistingFileHelper`，但由于这俩类已不存在，影响范围局限于自定义 DataProvider |

---

## 5. 关键 API 变更总结（相比原计划假设）

| 计划中的假设 | 实际 26.1.2 API | 影响 |
|-------------|----------------|------|
| `BlockStateProvider` 存在且可用 | ❌ 不存在 → 使用 Vanilla `BlockModelGenerators` | **TASK-003 需重设计** — 不能用旧 Neo 方式生成 blockstate |
| `ItemModelProvider` 存在且可用 | ❌ 不存在 → 使用 Vanilla `ItemModelGenerators` | **TASK-004 需重设计** — 不能用旧 Neo 方式生成 item model |
| 需要 `ExistingFileHelper` | ❌ 已移除 | 构造器签名简化 |
| `generator.addProvider(true, ...)` | ✅ 兼容，但推荐 `event.addProvider()` 或 `event.createProvider()` | 可迁移可不迁移；向后兼容 |
| `LootTableProvider` 构造器兼容 | ✅ 兼容 | 构造器: `(PackOutput, Set<ResourceKey>, List<SubProviderEntry>)` |
| `BlockTagsProvider` 构造器兼容 | ✅ 兼容，但构造器签名不同 | `(PackOutput, CompletableFuture<HolderLookup.Provider>, String modId)` — **无 `ExistingFileHelper` 参数** |
| `ItemTagsProvider` 构造器兼容 | ✅ 兼容 | `(PackOutput, CompletableFuture<HolderLookup.Provider>, CompletableFuture<TagLookup<Block>>, String modId)` 或 modId 省略版 |
| `FluidTagsProvider` 有 modId 构造器 | ✅ NeoForge patch 添加了 | `(PackOutput, CompletableFuture<HolderLookup.Provider>, String modId)` — 推荐使用 |
| `BlockTagCopyingItemTagProvider` | ✅ NeoForge 额外提供 | 可自动复制 block tag → item tag |

---

## 6. Risk / Unknown

| # | 风险 / 未知项 | 概率 | 影响 | 缓解措施 |
|---|--------------|:----:|:----:|---------|
| 1 | Vanilla `BlockModelGenerators` API 是否足以生成 TASK-001 baseline 中所有 43 个方块的 blockstate + block model（含电缆/管道的多部件模型、机器的 active/idle 变体）？ | 高 | 🔴 高 | TASK-003 前需先原型验证一个简单方块和一个复杂方块；如不够则需写自定义 `DataProvider` 直接输出 JSON。 |
| 2 | Vanilla `ItemModelGenerators` API 是否支持简单 `item/generated` 和 block item 的模型？ | 中 | 🟡 中 | 原型验证一个简单物品（tin_ingot）和一个 block item（tin_ore）；如不够则自定义 DataProvider。 |
| 3 | `GatherDataEvent.Client` 和 `GatherDataEvent.Server` 的事件总线注册方式是否有 `@OnlyIn` / `Dist` 限制？当前 `DataGenerators.java` 使用 `value = Dist.CLIENT`。Server 侧事件是否也需要 `Dist` 标注？ | 低 | 🟡 中 | 验证：Server 侧事件应使用 `@EventBusSubscriber(modid = TEN.MOD_ID, bus = EventBusSubscriber.Bus.MOD)` 而不指定 Dist。 |
| 4 | `LootTableProvider` 的 `SubProviderEntry` 和 `BlockLootSubProvider` 在 26.1.2 中的导入路径和 API 是否有变动？ | 低 | 🟢 低 | 确认 Vanilla 路径：`net.minecraft.data.loot.BlockLootSubProvider`。构造器可能需要 `HolderLookup.Provider`。 |
| 5 | `TENModels.java` 存根是否需要保留？ | 低 | 🟢 低 | 可删除或清空；在 TASK-003 或 TASK-004 中处理。 |
| 6 | 现有 `TENRecipeGen` 使用 `DataProvider` 接口直接输出 JSON — 与 `event.addProvider()` 兼容性良好。但 `TENVanillaPackGen` 是否也兼容？ | 低 | 🟢 低 | 简单验证：两个 provider 都可在 Server 侧事件中注册。 |
| 7 | 现有 LanguageProvider 是在 `GatherDataEvent.Client` 中注册的，但语言文件实际应算 Client 还是 Server 侧？ | 中 | 🟢 低 | 当前在 Client 侧注册，似乎没问题。可保留现状或迁移到 Server 侧。无功能影响。 |

---

## 7. 后续 TASK 参考映射

| 原计划 TASK | 实际需要 | 调整建议 |
|-----------|---------|---------|
| TASK-003 — BlockStateProvider | 改为使用 Vanilla `BlockModelGenerators` 或自定义 DataProvider 输出 blockstate JSON | 彻底重写实现方案 |
| TASK-004 — ItemModelProvider | 改为使用 Vanilla `ItemModelGenerators` 或自定义 DataProvider 输出 item model JSON | 彻底重写实现方案 |
| TASK-005 — LootTableProvider | 协议兼容，正常执行 | 无需调整 |
| TASK-006 — TagsProvider | `BlockTagsProvider` / `ItemTagsProvider` 构造器签名无 `ExistingFileHelper`，`FluidTagsProvider` 用 modId 构造器 | 构造器参数调整 |
| TASK-007 — DataGenerators 整合 | 需改为分离 Client/Server 事件注册；使用 `event.addProvider()` 替代 `generator.addProvider()`（推荐但非强制） | 重写注册方式 |

---

*证据采集完成。本文件可作为 TASK-003 ~ TASK-007 的 API 参考源。由于 BlockStateProvider 和 ItemModelProvider 在 26.1.2 中不存在，相关 TASK 需在设计阶段调整实现策略。*
