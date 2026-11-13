// 兼容层校验：跑在 src/{main,generated}/resources 的配方成品上，与生成期断言、运行期扫描同一套规则。
// 用法：node tools/compat/check.js      （有问题时退出码非 0，可直接接进脚本/CI）
const { CFG, GEN, MAIN, walk, load, display, ownerOf } = require("./common");
const path = require("path");

const MACHINE_TYPES = new Set([
  "kenergyengineering:pulverizer",
  "kenergyengineering:compressor",
  "kenergyengineering:induction_furnace",
  "kenergyengineering:psionicant",
  "kenergyengineering:refiner",
]);

// ── 1. 输入签名歧义（同 RecipeSignature#claim）─────────────────────────
// 变体组豁免：provider_priority 的同一候选组内每个模组各一条，加载期只留赢家，不算冲突；
// 但「同一组里同模组出现两次」「同一输入挂两条不同的组」「变体组与无条件配方撞车」仍然报。
// 条件分流豁免：mod_loaded(X) 与 not(mod_loaded(X)) 成对出现时两条互斥，属于设计内重复；
// 两条能同时成立（含任一方无条件）仍是冲突。
const claims = new Map();
const chainGroups = new Map();
const sigConds = new Map();
const duplicates = [];
const recipes = [];

// 负侧可能是 mod_loaded，也可能是 and(mod_loaded, compat_enabled)——后者表达「该来源可用」，照样记进负集合
const collectMod = (c, set) => {
  if (!c || !c.type) return;
  if (c.type === "neoforge:mod_loaded" && c.modid) set.add(c.modid);
  else if (c.type === "neoforge:and" && Array.isArray(c.values)) c.values.forEach(v => collectMod(v, set));
};
const condOf = j => {
  const on = new Set(), off = new Set();
  for (const c of j["neoforge:conditions"] || []) {
    if (!c) continue;
    if (c.type === "neoforge:mod_loaded") collectMod(c, on);
    if (c.type === "neoforge:not" && c.value) collectMod(c.value, off);
  }
  return { on, off };
};
const disjoint = (a, b) => { for (const x of a) if (b.has(x)) return false; return true; };
// 能同时成立即冲突：只有「一条要 X、另一条要非 X」才算互斥
const canCoexist = (a, b) => disjoint(a.on, b.off) && disjoint(b.on, a.off);
const describe = c => [...[...c.on].map(m => "+" + m), ...[...c.off].map(m => "-" + m)].join("") || "无条件";

for (const root of [MAIN, GEN]) {
  for (const file of walk(path.join(root, "kenergyengineering/recipe"))) {
    const j = load(file);
    if (!j) {
      duplicates.push("解析失败：" + display(file));
      continue;
    }
    if (!MACHINE_TYPES.has(j.type)) continue;
    recipes.push({ file, json: j });
    const sig = j.type + "|" + (j.inputs || [])
      .map(i => [i.form || "item", i.key, i.count ?? i.amount ?? 1, i.chance ?? 1].join(":"))
      .sort()
      .join(",");
    const prio = (j["neoforge:conditions"] || []).find(c => c && c.type === "kenergyengineering:provider_priority");
    const who = display(file);
    if (!prio) {
      const owner = chainGroups.get(sig);
      if (owner) duplicates.push(`无条件配方与互斥变体组抢同一输入 ${sig}\n      ${owner}\n      ${who}`);
      const key = condOf(j);
      const list = sigConds.get(sig) || [];
      for (const prev of list) {
        if (canCoexist(prev.key, key)) {
          duplicates.push(`输入签名重复且两条能同时生效 ${sig}（${describe(prev.key)} / ${describe(key)}）\n      ${prev.who}\n      ${who}`);
          break;
        }
      }
      list.push({ key, who });
      sigConds.set(sig, list);
      continue;
    }
    if (sigConds.has(sig)) duplicates.push(`互斥变体组与条件分流行抢同一输入 ${sig}\n      ${sigConds.get(sig)[0].who}\n      ${who}`);
    const group = prio.candidates.join("+");
    const prevGroup = chainGroups.get(sig);
    if (prevGroup !== undefined && prevGroup !== group) {
      duplicates.push(`同一输入挂了两个不同的候选模组组 ${sig}\n      ${prevGroup}\n      ${group}`);
    }
    chainGroups.set(sig, group);
    const key = sig + "#" + group + "#" + prio.mod;
    if (claims.has(key) && claims.get(key) !== who) {
      duplicates.push(`同一候选组里模组 ${prio.mod} 写了两次 ${sig}\n      ${claims.get(key)}\n      ${who}`);
    }
    claims.set(key, who);
  }
}

// ── 2. 引用能否落地 ────────────────────────────────────────────────────
// 邻居的 dump 里查得到才算数：c: 标签要有人定义，外部物品 id 要有物品模型或对应语言键。
const fs = require("fs");
const knownTags = new Map();
/** 命名空间 → 该 dump 里查得到的物品 id；空集合表示这个邻居没有可用资源目录（如原版资产不在手），跳过判定。 */
const knownItems = new Map();
for (const [ns, roots] of Object.entries(CFG.dumps)) {
  if (!knownItems.has(ns)) knownItems.set(ns, new Set());
  const ids = knownItems.get(ns);
  for (const root of roots) {
    // 标签：data/<任意模组>/tags/item/**，键写成 <模组>:<路径>（c: 前缀原样保留）
    for (const owner of fs.existsSync(root) ? fs.readdirSync(root) : []) {
      const dir = path.join(root, owner, "tags/item");
      for (const file of walk(dir)) {
        knownTags.set(owner + ":" + path.relative(dir, file).slice(0, -5).split(path.sep).join("/"), true);
      }
    }
    const assets = path.join(root, "..", "assets", ns);
    if (!fs.existsSync(assets)) continue;
    for (const model of walk(path.join(assets, "models/item"))) ids.add(ns + ":" + path.basename(model, ".json"));
    for (const langFile of walk(path.join(assets, "lang"))) {
      for (const key of Object.keys(load(langFile) || {})) {
        const m = key.match(/^(?:item|block)\./ + ns.replace(/[.*+?^${}()|[\]\\]/g, "\\$&") + "\\.(.+)$/");
        if (m) ids.add(ns + ":" + m[1]);
      }
    }
  }
}

const unresolved = [];
const tagOutputs = [];
for (const { file, json } of recipes) {
  for (const [slot, list] of [["输入", json.inputs], ["输出", json.outputs]]) {
    for (const ing of list || []) {
      if (slot === "输出" && ing.type === "tag") {
        tagOutputs.push(`${display(file)} 输出写成标签 ${ing.key}（标准语义里输出必须是具体物品，会得到空产物）`);
      }
      if (ing.form === "fluid") continue; // 流体不在 dump 的资源目录里，判不了，交给实机
      const key = ing.key || "";
      if (ing.type === "tag") {
        if (!knownTags.has(key)) unresolved.push(`${display(file)} ${slot}标签无人定义 ${key}`);
        continue;
      }
      const ids = knownItems.get(ownerOf(key));
      if (ids && ids.size && !ids.has(key)) {
        unresolved.push(`${display(file)} ${slot}物品在 ${ownerOf(key)} 的 dump 里查不到 ${key}`);
      }
    }
  }
}

// ── 3. 汇总 ────────────────────────────────────────────────────────────
const bySource = new Map();
for (const { file, json } of recipes) {
  if (!display(file).includes("/recipe/compat/")) continue;
  const loaded = (json["neoforge:conditions"] || []).filter(c => c && c.type === "neoforge:mod_loaded").map(c => c.modid);
  for (const m of loaded) bySource.set(m, (bySource.get(m) || 0) + 1);
}
console.log(`机器配方 ${recipes.length} 条，其中 compat 按来源：` +
  [...bySource].map(([m, n]) => `${m}=${n}`).join(" "));
console.log(`输入签名冲突 ${duplicates.length}，引用问题 ${unresolved.length + tagOutputs.length}`);
for (const line of [...tagOutputs, ...duplicates, ...unresolved]) console.log("  " + line);
process.exit(duplicates.length + unresolved.length + tagOutputs.length ? 1 : 0);
