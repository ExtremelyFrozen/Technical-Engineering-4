// 兼容层工具的公共部分：路径配置、JSON 遍历、以及按优先级建的标签索引。
// 邻居的 jar 需要先解包成 dumps（见 README.md），路径都写在 config.json 里。
const fs = require("fs");
const path = require("path");

const ROOT = path.resolve(__dirname, "../..");
const CFG = JSON.parse(fs.readFileSync(path.join(__dirname, "config.json"), "utf8"));
const GEN = path.join(ROOT, "src/generated/resources/data");
const MAIN = path.join(ROOT, "src/main/resources/data");

function walk(dir, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith(".json")) out.push(p);
  }
  return out;
}

// 解包/编辑过的 JSON 可能有 BOM 与尾逗号，解析失败一律当「没有这条数据」
function load(file) {
  try {
    return JSON.parse(fs.readFileSync(file, "utf8"));
  } catch {
    return null;
  }
}

const rel = p => p.split(path.sep).join("/");

/** 仓库相对路径，报错与日志里用短路径。 */
const display = p => rel(p).slice(rel(ROOT).length + 1);

/**
 * 标签索引：`c:dusts/tin` → 成员物品 id 列表。
 * 按 config.providerPriority 的顺序累加各 dump 的 c/tags/item，先出现的排前面，
 * 于是「按优先级取第一个成员」就是对整个候选集合排序后取头一个。
 */
function tagIndex() {
  const members = new Map();
  for (const ns of CFG.providerPriority) {
    for (const root of CFG.dumps[ns] || []) {
      const dir = path.join(root, "c/tags/item");
      for (const file of walk(dir)) {
        const tag = "c:" + rel(file).slice(rel(dir).length + 1, -".json".length);
        const j = load(file);
        if (!j || !Array.isArray(j.values)) continue;
        if (!members.has(tag)) members.set(tag, []);
        const list = members.get(tag);
        for (const v of j.values) {
          const id = typeof v === "string" ? v : v.id;
          if (!id) continue;
          if (id.startsWith("#")) {
            for (const inner of members.get("c:" + id.slice(1)) || []) {
              if (!list.includes(inner)) list.push(inner);
            }
          } else if (!list.includes(id)) list.push(id);
        }
      }
    }
  }
  return members;
}

/** 物品 id 归属的模组（无命名空间按原版算）。 */
const ownerOf = id => (id.includes(":") ? id.split(":")[0] : "minecraft");

/** 归属 → 门控模组；原版与本模组不需要条件。 */
const gateOf = ns => (ns === "minecraft" || ns === "kenergyengineering" ? null : ns);

/** 对方能量（每次操作总 FE）→ 本模组 tick 数。 */
const ticksFromEnergy = fe =>
  Math.max(CFG.timeFloor, Math.min(CFG.timeCeil, Math.round((fe || 2400) / CFG.fePerTick)));

module.exports = { ROOT, GEN, MAIN, CFG, walk, load, rel, display, tagIndex, ownerOf, gateOf, ticksFromEnergy };
