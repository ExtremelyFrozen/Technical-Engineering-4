// 兼容层转换器：邻居解包数据（config.json 的 dumps/neighbourRecipes）→ TENCompatGen 的表行。
// 用法：node tools/compat/convert.js        输出 out/rows.json 与 ORE_CHAIN 候选建议
// 邻居更新后重跑这一步，再跑 node tools/compat/inject.js，最后 ./gradlew runData。
const fs = require("fs");
const path = require("path");
const { CFG, GEN, MAIN, walk, load, rel, tagIndex, ownerOf, gateOf, ticksFromEnergy } = require("./common");

const TAGS = tagIndex();

/** 标签→按优先级排好的候选物品（每个归属模组只留它自己的第一个成员）。 */
function candidates(tag) {
  const byOwner = new Map();
  for (const id of TAGS.get(tag) || []) {
    const owner = ownerOf(id);
    if (!CFG.dumps[owner]) continue; // 没解包的模组不算候选
    if (!byOwner.has(owner)) byOwner.set(owner, id);
  }
  return [...byOwner.values()];
}

/** 对方输入项 → {key, isTag, count} / {anyOf:[...]}；标签与物品按其数据结构区分，不靠命名空间前缀猜。 */
function inputOf(i) {
  if (!i) return null;
  if (typeof i === "string") return { key: i, isTag: false, count: 1 };
  if (i.type === "neoforge:compound" || i.type === "neoforge:any_of") {
    return { anyOf: (i.children || []).map(c => inputOf(c)).filter(Boolean) };
  }
  if (i.tag) return { key: tagKey(i.tag), isTag: true, count: i.count || 1 };
  if (i.item) return { key: typeof i.item === "string" ? i.item : (i.item.item || i.item.id), isTag: false, count: i.count || 1 };
  if (i.id) return { key: i.id, isTag: false, count: i.count || 1 };
  return null;
}
/** 标签键：对方已带命名空间就原样用，否则补 c: 前缀。 */
const tagKey = t => (t.includes(":") ? t : "c:" + (t.startsWith("c:") ? t.slice(2) : t));

/** 锭输入：该材料有 c:mats/X 聚合标签时升级成「锭或粉皆可」。 */
function inExpr(inp) {
  const mat = inp.isTag && inp.key.startsWith("c:ingots/") ? inp.key.slice("c:ingots/".length) : null;
  if (mat && TAGS.has("c:mats/" + mat)) return `ingot("${mat}")`;
  const n = inp.count > 1 ? inp.count : null;
  if (inp.isTag) return n ? `tagN("${inp.key}", ${n})` : `tag("${inp.key}")`;
  return n ? `itemN("${inp.key}", ${n})` : `item("${inp.key}")`;
}

/** 对方输出三态：{item:"ns:path"} / {item:{id|"tag",count}} / {tag|id, count}。 */
function outKeyOf(o) {
  if (!o) return null;
  if (typeof o === "string") return o;
  if (typeof o.item === "string") return o.item;
  const inner = o.item && typeof o.item === "object" ? o.item : o;
  return inner.id ?? (inner.tag ? tagKey(inner.tag) : null);
}
const outCountOf = o => {
  const inner = o && o.item && typeof o.item === "object" ? o.item : o;
  return inner?.count ?? 1;
};

/** 输出必须落成具体物品：标签输出按标准处理会得到空产物，所以编写期按优先级解析一次。 */
function resolveOutput(key) {
  if (!key.startsWith("c:")) return { item: key, gate: gateOf(ownerOf(key)) };
  const list = TAGS.get(key) || [];
  for (const id of list) {
    if (CFG.dumps[ownerOf(id)]) return { item: id, gate: gateOf(ownerOf(id)) };
  }
  return null; // 没人提供 → 不导入这条
}

/** 本模组已有的 (类型, 输入签名)：撞车的对方条目不导入，本模组语义优先。 */
function ownSignatures(machineType) {
  const set = new Map();
  for (const root of [MAIN, GEN]) {
    for (const file of walk(path.join(root, "kenergyengineering/recipe"))) {
      // 兼容层里机械生成的条目不参与基准；手写的压缩行要参与，否则导入行会与它撞同一输入
      const PROTECTED = new Set(["grains_to_dust_block", "dust_block_to_compressed", "compressed_to_double"]);
      const short = rel(file);
      if (short.includes("/recipe/compat/") && !PROTECTED.has(path.basename(file, ".json"))) continue;
      const j = load(file);
      if (!j || j.type !== "kenergyengineering:" + machineType) continue;
      set.set((j.inputs || []).map(i => i.key + "x" + (i.count ?? i.amount ?? 1)).sort().join("+"), path.basename(file));
    }
  }
  return set;
}

const rows = { alloy: [], grind: [], chain: [] };
const notes = [];

// ── 感应炉 ← 对方的合金炉 ──
{
  const seen = new Set();
  for (const [ns, sub] of CFG.neighbourRecipes.induction_furnace) {
    for (const file of walk(path.join((CFG.dumps[ns] || [])[0] || "", sub))) {
      const j = load(file);
      if (!j) continue;
      const raw = (Array.isArray(j.inputs) ? j.inputs : [j.inputs]).map(inputOf).filter(Boolean);
      if (!raw.length) continue;
      const o = j.output || (j.outputs || [])[0];
      const outKey = outKeyOf(o);
      if (!outKey) continue;
      const resolved = resolveOutput(outKey);
      if (!resolved) {
        notes.push(`合金 ${path.basename(file, ".json")}：输出标签 ${outKey} 无人提供，跳过`);
        continue;
      }
      const count = outCountOf(o);
      const altIdx = raw.findIndex(r => r.anyOf);
      const variants = altIdx < 0
        ? [{ name: path.basename(file, ".json"), ins: raw }]
        : raw[altIdx].anyOf.map(alt => ({
            name: path.basename(file, ".json") + "_" + alt.key.split(/[:/]/).pop(),
            ins: raw.map((r, i) => (i === altIdx ? { key: alt.key, isTag: alt.isTag, count: 1 } : r)),
          }));
      for (const v of variants) {
        const sig = ns + "|" + resolved.item + "|" + v.ins.map(i => i.key + i.count).join(",");
        if (seen.has(sig)) continue;
        seen.add(sig);
        const time = ticksFromEnergy(j.energy || 3200);
        rows.alloy.push(`alloy("${ns}", "${v.name}", "${resolved.item}", ${count}, ${time}, ` +
          v.ins.map(inExpr).join(", ") + ")");
      }
    }
  }
}

// ── 粉碎机 ← 对方的 SAG 研磨 ──
{
  const own = ownSignatures("pulverizer");
  const seen = new Set();
  const byInput = new Map();
  for (const [ns, sub] of CFG.neighbourRecipes.pulverizer) {
    for (const file of walk(path.join((CFG.dumps[ns] || [])[0] || "", sub))) {
      const j = load(file);
      if (!j) continue;
      const inp = inputOf(j.input);
      if (!inp || inp.anyOf) continue;
      const name = path.basename(file, ".json");
      const outs = (j.outputs || []).map(o => {
        const key = outKeyOf(o);
        if (!key) return null;
        const resolved = resolveOutput(key);
        if (!resolved) return { missing: key };
        return { key: resolved.item, count: outCountOf(o), chance: o.chance, gate: resolved.gate };
      });
      if (!outs.length || outs.some(o => !o || o.missing)) {
        notes.push(`研磨 ${name}：输出 ${outs.find(o => !o || o.missing)?.missing || "?"} 无人提供，跳过`);
        continue;
      }
      const sig = ns + "|" + inp.key + "|" + outs.map(o => o.key + o.count + (o.chance || "")).join(",");
      if (seen.has(sig)) continue;
      seen.add(sig);
      if (own.has(inp.key + "x" + (inp.count || 1))) {
        notes.push(`研磨 ${name}：输入 ${inp.key} 本模组已有配方，跳过`);
        continue;
      }
      // 同一输入对方可能给多条：机器不允许歧义，保留期望产量更高的一条
      const yieldSum = outs.reduce((a, o) => a + o.count * (o.chance == null ? 1 : o.chance), 0);
      const inputSig = ns + "|" + inp.key + "x" + (inp.count || 1);
      if (byInput.has(inputSig)) {
        const prev = byInput.get(inputSig);
        if (prev.yield >= yieldSum) {
          notes.push(`研磨 ${name}：与 ${prev.name} 同输入且产量更低，跳过`);
          continue;
        }
        rows.grind.splice(rows.grind.indexOf(prev.row), 1);
        notes.push(`研磨 ${prev.name}：被 ${name} 取代（同输入、产量更高）`);
      }
      // 门控 = 来源模组 + 输出物品归属（+ 输入标签首个成员的归属）
      const inputOwner = inp.isTag ? ownerOf((TAGS.get(inp.key) || [])[0] || "") : ownerOf(inp.key);
      const gates = [ns, ...outs.map(o => o.gate), gateOf(inputOwner)]
        .filter((g, i, a) => g && a.indexOf(g) === i);
      const row = `grind("${gates.join(",")}", "${name}", ${inExpr(inp)}, ${ticksFromEnergy(j.energy || 2400)}, ` +
        outs.map(o => `out("${o.key}", ${o.count}${o.chance != null ? ", " + o.chance : ""})`).join(", ") + ")";
      rows.grind.push(row);
      byInput.set(inputSig, { yield: yieldSum, name, row });
    }
  }
}

// ── ORE_CHAIN 候选建议（人工核对后粘进 TENRecipeGen）──
const chainMat = new Set();
for (const tag of TAGS.keys()) {
  if (!tag.startsWith("c:ores/")) continue;
  const mat = tag.slice("c:ores/".length);
  if (mat.includes("/") || chainMat.has(mat)) continue;
  const raws = candidates("c:raw_materials/" + mat);
  const dusts = candidates("c:dusts/" + mat);
  if (!raws.length || !dusts.length) continue;
  chainMat.add(mat);
  rows.chain.push(`new ChainRow("${mat}", List.of(${raws.map(r => `"${r}"`).join(", ")}), ` +
    `List.of(${dusts.map(d => `"${d}"`).join(", ")}))`);
}

fs.mkdirSync(path.join(__dirname, "out"), { recursive: true });
fs.writeFileSync(path.join(__dirname, "out/rows.json"), JSON.stringify(rows, null, 2));
console.log(`表行：感应炉 ${rows.alloy.length}，粉碎机 ${rows.grind.length}；ORE_CHAIN 候选 ${rows.chain.length} 个材料`);
console.log("跳过的对方条目：\n" + notes.map(n => "  " + n).join("\n"));
console.log("\n// 粘到 TENRecipeGen.ORE_CHAIN（已有材料请只补候选，不要重复加行）：");
console.log(rows.chain.join(",\n            "));
