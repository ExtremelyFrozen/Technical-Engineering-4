// 兼容层转换器：邻居解包数据（config.json 的 dumps/neighbourRecipes）→ TENCompatGen 的表行。
// 用法：node tools/compat/convert.js        输出 out/rows.json 与 ORE_CHAIN 候选建议
// 邻居更新后重跑这一步，再跑 node tools/compat/inject.js，最后 ./gradlew runData。
const fs = require("fs");
const path = require("path");
const { CFG, GEN, MAIN, walk, load, rel, tagIndex, ownerOf, gateOf, alignTicks } = require("./common");

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

// c:mats/X 只服务感应炉：只有它允许「锭或粉皆可」，粉碎机与压缩机一律只认锭
const MAT_MACHINES = new Set(["induction_furnace"]);

/** 锭输入：mats=true（感应炉）且该材料有 c:mats/X 聚合标签时升级成「锭或粉皆可」。 */
function inExpr(inp, mats = false) {
  const mat = inp.isTag && inp.key.startsWith("c:ingots/") ? inp.key.slice("c:ingots/".length) : null;
  if (mats && mat && TAGS.has("c:mats/" + mat)) return `matIngot("${mat}")`;
  const n = inp.count > 1 ? inp.count : null;
  if (inp.isTag) return n ? `tagN("${inp.key}", ${n})` : `tag("${inp.key}")`;
  return n ? `itemN("${inp.key}", ${n})` : `item("${inp.key}")`;
}

/**
 * 输入的落地标签（与 inExpr 同一判据）：签名比较要用它，否则本模组通用配方与导入行
 * 会对同一输入写出两种拼写，去重形同无效。
 */
const effectiveKey = (inp, mats = false) => {
  const mat = inp.isTag && inp.key.startsWith("c:ingots/") ? inp.key.slice("c:ingots/".length) : null;
  return mats && mat && TAGS.has("c:mats/" + mat) ? "c:mats/" + mat : inp.key;
};

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
      // 手写真身的行以 config.manualRows 为准：它们参与输入签名基准，机械导入撞到就跳过
      const PROTECTED = new Set(CFG.manualRows || []);
      const short = rel(file);
      if (short.includes("/recipe/compat/") && !PROTECTED.has(path.basename(file, ".json"))) continue;
      const j = load(file);
      if (!j || j.type !== "kenergyengineering:" + machineType) continue;
      // key 要过 effectiveKey（按机器决定是否升级成 c:mats/X）：两侧拼写不一致会让去重形同无效
      set.set((j.inputs || []).map(i => {
        const n = i.count ?? i.amount ?? 1;
        return effectiveKey({ isTag: i.type === "tag", key: i.key, count: n }, MAT_MACHINES.has(machineType)) + "x" + n;
      }).sort().join("+"), path.basename(file));
    }
  }
  return set;
}

/** 对方条件里的第三方 mod_loaded（EVO 只在装了 Industrial Foregoing 时才有的合金行一类）要带过来。 */
function extraGates(j, sourceNs) {
  const out = [];
  for (const c of j["neoforge:conditions"] || []) {
    if (c && c.type === "neoforge:mod_loaded" && c.modid !== sourceNs) out.push(c.modid);
  }
  return out;
}

/**
 * 行名 = 相对该机器配方目录的路径（`aluminum/ingot.json` → `aluminum_ingot`）。
 * 只取文件名会让 ATO 那种按材料分层的目录里几十个 `ingot.json` 写进同一个输出文件、互相覆盖。
 */
function nameOf(file, dir) {
  return path.relative(dir, file).replace(/\.json$/, "").split(path.sep).join("_");
}

/** IE 的输入写法是 {basePredicate:{tag|item}, count}，先摊平回通用形状。 */
const unwrap = i => (i && i.basePredicate ? { ...i.basePredicate, count: i.count ?? i.basePredicate.count ?? 1 } : i);

const rows = { alloy: [], grind: [], press: [], chain: [] };
const notes = [];

// ── 感应炉 ← 对方的合金炉 ──
{
  const seen = new Set();
  // 感应炉也要按「本模组优先」去重：ALLOY_CHAIN 的通用合金配方与对方写给合金炉的行是同输入
  // 竞争关系，导入进来会让 DataGen 的签名断言失败（本模组的配比是默认值）。
  const ownAlloy = ownSignatures("induction_furnace");
  for (const [ns, sub] of CFG.neighbourRecipes.induction_furnace) {
    const dir = path.join((CFG.dumps[ns] || [])[0] || "", sub);
    for (const file of walk(dir)) {
      const j = load(file);
      if (!j) continue;
      const base = nameOf(file, dir);
      const raw = (Array.isArray(j.inputs) ? j.inputs : [j.input0, j.input1]).filter(Boolean).map(unwrap).map(inputOf)
        .filter(Boolean);
      if (!raw.length) continue;
      const o = unwrap(j.result || j.output || (j.outputs || [])[0]);
      const outKey = outKeyOf(o);
      if (!outKey) continue;
      const resolved = resolveOutput(outKey);
      if (!resolved) {
        notes.push(`合金 ${base}：输出标签 ${outKey} 无人提供，跳过`);
        continue;
      }
      const count = outCountOf(o);
      const altIdx = raw.findIndex(r => r.anyOf);
      const variants = altIdx < 0
        ? [{ name: base, ins: raw }]
        : raw[altIdx].anyOf.map(alt => ({
            name: base + "_" + alt.key.split(/[:/]/).pop(),
            ins: raw.map((r, i) => (i === altIdx ? { key: alt.key, isTag: alt.isTag, count: 1 } : r)),
          }));
      for (const v of variants) {
        const sig = ns + "|" + resolved.item + "|" + v.ins.map(i => i.key + i.count).join(",");
        if (seen.has(sig)) continue;
        seen.add(sig);
        const ownSig = v.ins.map(i => effectiveKey(i, true) + "x" + (i.count || 1)).sort().join("+");
        if (ownAlloy.has(ownSig)) {
          notes.push(`合金 ${base}：输入 ${ownSig} 本模组已有通用配方，跳过`);
          continue;
        }
        const gates = [ns, ...extraGates(j, ns), resolved.gate].filter((g, i, a) => g && a.indexOf(g) === i);
        const time = alignTicks("induction_furnace", v.ins.reduce((a, i) => a + (i.count || 1), 0));
        rows.alloy.push(`alloy("${gates.join(",")}", "${v.name}", "${resolved.item}", ${count}, ${time}, ` +
          v.ins.map(i => inExpr(i, MAT_MACHINES.has("induction_furnace"))).join(", ") + ")");
      }
    }
  }
}

// ── 粉碎机 ← 对方的研磨类配方（EIO sag_milling 与 IE crusher 两种形状）──
{
  const own = ownSignatures("pulverizer");
  const byInput = new Map();
  for (const [ns, sub] of CFG.neighbourRecipes.pulverizer) {
    const dir = path.join((CFG.dumps[ns] || [])[0] || "", sub);
    for (const file of walk(dir)) {
      const j = load(file);
      if (!j) continue;
      const mekCrushing = j.type === "mekanism:crushing";
      const ieCrusher = j.type === "immersiveengineering:crusher";
      if (!ieCrusher && !mekCrushing && j.type !== "enderio:sag_milling" && j.type !== "enderio_evolution:sag_milling") continue;
      const inp = inputOf(unwrap(j.input));
      if (!inp || inp.anyOf) continue;
      const name = nameOf(file, dir);
      const raw = ieCrusher
        ? [j.result, ...(j.secondaries || []).map(s => ({ ...s.output, chance: s.chance }))].filter(Boolean)
        : mekCrushing ? [j.output] : (j.outputs || []);
      const outs = raw.map(raw0 => {
        const o = unwrap(raw0);
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
      // 与 ownSignatures 同一拼写（感应炉以外不做 mats 升级）
      if (own.has(effectiveKey(inp) + "x" + (inp.count || 1))) {
        notes.push(`研磨 ${name}：输入 ${inp.key} 本模组已有配方，跳过`);
        continue;
      }
      // 门控 = 来源模组 + 对方自带第三方门控 + 输出物品归属
      const inputOwner = inp.isTag ? ownerOf((TAGS.get(inp.key) || [])[0] || "") : ownerOf(inp.key);
      const gates = [ns, ...extraGates(j, ns), ...outs.map(o => o.gate), gateOf(inputOwner)]
        .filter((g, i, a) => g && a.indexOf(g) === i);
      const yieldSum = outs.reduce((a, o) => a + o.count * (o.chance == null ? 1 : o.chance), 0);
      // 同一输入全池只留一条：机器按第一条命中，留两条等于一条死配方。
      // 优先取门控更少的（更可及），再取期望产量高的。
      const inputSig = "pulverizer|" + inp.key + "x" + (inp.count || 1);
      const row = () => `grind("${gates.join(",")}", "${name}", ${inExpr(inp)}, ` +
        `${alignTicks("pulverizer", inp.count || 1)}, ` +
        outs.map(o => `out("${o.key}", ${o.count}${o.chance != null ? ", " + o.chance : ""})`).join(", ") + ")";
      const prev = byInput.get(inputSig);
      if (prev) {
        const keep = gates.length < prev.gates.length || (gates.length === prev.gates.length && yieldSum > prev.yield);
        if (!keep) {
          notes.push(`研磨 ${name}：与 ${prev.name} 撞同一输入 ${inp.key}，保留门控更少/产量更高的那条`);
          continue;
        }
        const at = rows.grind.indexOf(prev.row);
        if (at >= 0) rows.grind.splice(at, 1);
        notes.push(`研磨 ${prev.name}：被 ${name} 取代（同输入 ${inp.key}）`);
      }
      const built = row();
      rows.grind.push(built);
      byInput.set(inputSig, { gates: gates.length, yield: yieldSum, name, row: built });
    }
  }
}

// ── 压缩机 ← IE 冲压机（模具换成本模组的对应模具）──
{
  const own = ownSignatures("compressor");
  // 同一「材料 + 模具」只留一个赢家：按 config 里 compressor 源的先后顺序，前面的模组优先
  // 值记「来源/行名」，用来区分跨来源让位与同来源真重复（后者不能静默丢）
  const seen = new Map();
  const MOULDS = { mold_plate: "mould_plate", mold_gear: "mould_gear", mold_rod: "mould_rod", mold_wire: "mould_string" };
  for (const [ns, sub] of CFG.neighbourRecipes.compressor || []) {
    const dir = path.join((CFG.dumps[ns] || [])[0] || "", sub);
    for (const file of walk(dir)) {
      const j = load(file);
      if (!j || j.type !== "immersiveengineering:metal_press") continue;
      const name = nameOf(file, dir);
      const mould = MOULDS[String(j.mold).split(":").pop()];
      if (!mould) {
        notes.push(`冲压 ${name}：模具 ${j.mold} 无对应本模组模具，跳过`);
        continue;
      }
      const inp = inputOf(unwrap(j.input));
      if (!inp || inp.anyOf) continue;
      // IE 的 rod/wire 输出写成 {basePredicate:{tag},count}，先摊平才认得出标签
      const res = unwrap(j.result);
      const outKey = outKeyOf(res);
      if (!outKey) {
        notes.push(`冲压 ${name}：输出形状未识别，跳过（补 unwrap 或 inputOf 一类归一化）`);
        continue;
      }
      const resolved = resolveOutput(outKey);
      if (!resolved) {
        notes.push(`冲压 ${name}：输出 ${outKey} 无人提供，跳过`);
        continue;
      }
      const mouldId = "kenergyengineering:" + mould;
      // 与 ownSignatures 同一算法：压缩机配方是「材料 + 模具」两项输入。
      // 压缩机不做 c:mats 升级，两侧都用原始 c:ingots/X 拼写比较。
      const sig = [effectiveKey(inp) + "x" + (inp.count || 1), mouldId + "x1"].sort().join("+");
      if (own.has(sig)) {
        notes.push(`冲压 ${name}：本模组已有同输入压缩配方，跳过`);
        continue;
      }
      const winner = seen.get(sig);
      if (winner) {
        if (winner.startsWith(ns + "/")) {
          notes.push(`冲压 ${name}：与同来源的 ${winner} 输入签名相同，邻居数据需人工核对（不再静默丢弃，交由生成期断言）`);
        } else {
          notes.push(`冲压 ${name}：同输入已由前一个来源 ${winner} 提供，跳过`);
          continue;
        }
      } else {
        seen.set(sig, ns + "/" + name);
      }
      const gates = [ns, ...extraGates(j, ns), resolved.gate].filter((g, i, a) => g && a.indexOf(g) === i);
      rows.press.push(`pressed("${gates.join(",")}", "${name}", ${inExpr(inp)}, "${mouldId}", ` +
        `"${resolved.item}", ${outCountOf(res)}, ${alignTicks("compressor", inp.count || 1)})`);
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
console.log(`表行：感应炉 ${rows.alloy.length}，粉碎机 ${rows.grind.length}，压缩机 ${rows.press.length}；` +
  `ORE_CHAIN 候选 ${rows.chain.length} 个材料`);
console.log("跳过的对方条目：\n" + notes.map(n => "  " + n).join("\n"));
console.log("\n// 粘到 TENRecipeGen.ORE_CHAIN（已有材料请只补候选，不要重复加行）：");
console.log(rows.chain.join(",\n            "));
