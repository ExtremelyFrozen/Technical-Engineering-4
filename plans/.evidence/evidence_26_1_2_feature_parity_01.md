# Evidence: origin/1.21 ConfigHolder.java Annotations Reference

> 用途：FP-001 ModConfigSpec 迁移的权威字段/范围/默认值证据
> 证据源：`git show origin/1.21:src/main/java/com/modularmc/ten/config/ConfigHolder.java`
> origin/1.21 ref: `406d105`
> 检索日期：2026-07-12
> 注意：origin/1.21 使用 `dev.toma.configuration` 注解系统，**不等于** NeoForge `ModConfigSpec` 的 `defineInRange`。以下逐行引用原注解，迁移到 `ModConfigSpec` 时用 `defineInRange` 等价表达。

---

## 文件结构概览

origin/1.21 `ConfigHolder.java` 使用 `@Config(id = TEN.MOD_ID)` 标注类，通过 `Configuration.registerConfig(ConfigHolder.class, ConfigFormats.YAML)` 初始化。格式为 YAML。

**迁移到 26.1.2**：改用 NeoForge `ModConfigSpec`，格式为 TOML，分 COMMON + CLIENT 两侧。

---

## 逐字段注解引用

### COMMON 侧（→ `te4-config.toml`）

#### `machine` 区块

| 字段 | 原注解 | 原默认值 | 行号/上下文 | ModConfigSpec 迁移 |
|------|--------|----------|-------------|-------------------|
| `energyMultiplier` | `@Configurable.DecimalRange(min = 0.01, max = 100.0)` | `1.0` | 字段声明处，`public double energyMultiplier = 1.0;` | `defineInRange("energyMultiplier", 1.0, 0.01, 100.0)` |
| `baseEnergyCapacity` | `@Configurable.Range(min = 1000, max = 1000000000)` | `10000` | 字段声明处，`public int baseEnergyCapacity = 10000;` | `defineInRange("baseEnergyCapacity", 10000, 1000, 1000000000)` |
| `enableSmelter` | `@Configurable`（无 range） | `true` | `public boolean enableSmelter = true;` | `define("enableSmelter", true)` |
| `enablePulverizer` | `@Configurable` | `true` | `public boolean enablePulverizer = true;` | `define("enablePulverizer", true)` |
| `enableCompressor` | `@Configurable` | `true` | `public boolean enableCompressor = true;` | `define("enableCompressor", true)` |
| `enableRefiner` | `@Configurable` | `true` | `public boolean enableRefiner = true;` | `define("enableRefiner", true)` |
| `enableInductionFurnace` | `@Configurable` | `true` | `public boolean enableInductionFurnace = true;` | `define("enableInductionFurnace", true)` |
| `enablePsionicant` | `@Configurable` | `true` | `public boolean enablePsionicant = true;` | `define("enablePsionicant", true)` |
| `enableBeacon` | `@Configurable` | `true` | `public boolean enableBeacon = true;` | `define("enableBeacon", true)` |
| `enableMobRipper` | `@Configurable` | `true` | `public boolean enableMobRipper = true;` | `define("enableMobRipper", true)` |
| `enableQuarry` | `@Configurable` | `true` | `public boolean enableQuarry = true;` | `define("enableQuarry", true)` |
| `enableEnchantmentFlusher` | `@Configurable` | `true` | `public boolean enableEnchantmentFlusher = true;` | `define("enableEnchantmentFlusher", true)` |
| `enableCondenser` | `@Configurable` | `true` | `public boolean enableCondenser = true;` | `define("enableCondenser", true)` |
| `enableFarmManager` | `@Configurable` | `true` | `public boolean enableFarmManager = true;` | `define("enableFarmManager", true)` |

#### `energyUnit` 区块

| 字段 | 原注解 | 原默认值 | 行号/上下文 | ModConfigSpec 迁移 |
|------|--------|----------|-------------|-------------------|
| `maxEnergy` | `@Configurable.Range(min = 1000, max = 100000000)` | `400_000` | `public int maxEnergy = 400_000;` | `defineInRange("maxEnergy", 400000, 1000, 100000000)` |
| `chargeRate` | `@Configurable.Range(min = 1, max = 100000)` | `2_000` | `public int chargeRate = 2_000;` | `defineInRange("chargeRate", 2000, 1, 100000)` |
| `inputRate` | `@Configurable.Range(min = 1, max = 100000)` | `2_000` | `public int inputRate = 2_000;` | `defineInRange("inputRate", 2000, 1, 100000)` |
| `outputRate` | `@Configurable.Range(min = 1, max = 100000)` | `2_000` | `public int outputRate = 2_000;` | `defineInRange("outputRate", 2000, 1, 100000)` |
| `chargingDefault` | `@Configurable`（无 range） | `false` | `public boolean chargingDefault = false;` | `define("chargingDefault", false)` |

#### `farm` 区块

| 字段 | 原注解 | 原默认值 | 行号/上下文 | ModConfigSpec 迁移 |
|------|--------|----------|-------------|-------------------|
| `bushCrops` | `@Configurable`（字符串数组） | `{"minecraft:sweet_berry_bush"}` | `public String[] bushCrops = { "minecraft:sweet_berry_bush" };` | `defineList("bushCrops", List.of("minecraft:sweet_berry_bush"), o -> o instanceof String)` |

### CLIENT 侧（→ `te4-client.toml`）

| 字段 | 原注解 | 原默认值 | 行号/上下文 | ModConfigSpec 迁移 |
|------|--------|----------|-------------|-------------------|
| `showMachineHUD` | `@Configurable` | `true` | `public boolean showMachineHUD = true;` | `define("showMachineHUD", true)` |
| `showCableHUD` | `@Configurable` | `true` | `public boolean showCableHUD = true;` | `define("showCableHUD", true)` |

---

## 区块名与键名策略

origin/1.21 中这些字段是嵌套类的 Java 字段。`dev.toma.configuration` 自动按嵌套类名分组。
ModConfigSpec 使用 builder 手动建区块：

```
// COMMON
builder.push("machine")
  .comment("Machine-related configuration options")
  .defineInRange(...)
  ...
  .pop()
builder.push("energyUnit")
  ...
builder.push("farm")
  ...

// CLIENT
builder.push("client")
  ...
```

---

## 键名兼容性注意

- Java 字段名作为键名（如 `energyMultiplier`、`enableSmelter`）
- `dev.toma.configuration` YAML 使用 `snake_case` 字段名直接序列化
- ModConfigSpec TOML 也使用声明的键名（如 `"energyMultiplier"`）
- **结论**：键名天然兼容，不需要额外转换层
