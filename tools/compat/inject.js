// 把 convert.js 产出的表行写回 TENCompatGen.java 的 ALLOYS / GRIND / PRESS 数组。
// 只替换数组体，工厂方法与 emit 逻辑保持原样（手改过的断言、门控不会被覆盖）。
// 自动生成只打基底：先看 out/review.md 的决定（排除/冲突/取代），确认后再运行本脚本。
// 用法：node tools/compat/convert.js → 看 out/review.md → node tools/compat/inject.js → ./gradlew runData
const fs = require("fs");
const path = require("path");
const { display } = require("./common");

const TARGET = path.join(__dirname, "../../src/main/java/com/modularmc/ten/data/TENCompatGen.java");
const ROWS = path.join(__dirname, "out/rows.json");
const REVIEW = path.join(__dirname, "out/review.md");

// 评审清单过期就拒绝写回：避免「跑完转换器直接注入」把没人看过的决定带进代码
if (!process.argv.includes("--force")) {
  const fresh = fs.existsSync(REVIEW) && fs.existsSync(ROWS)
    && fs.statSync(REVIEW).mtimeMs >= fs.statSync(ROWS).mtimeMs;
  if (!fresh) {
    console.error("out/review.md 缺失或早于 out/rows.json：先核对评审清单再写回（确需跳过用 --force）");
    process.exit(1);
  }
}

const rows = JSON.parse(fs.readFileSync(ROWS, "utf8"));

/** 用 rows.json 里的一组表行替换 `Recipe[] NAME = { ... };` 的数组体。 */
function replaceTable(src, fieldName, list) {
  const head = `private static final Recipe[] ${fieldName} = {`;
  const from = src.indexOf(head);
  if (from < 0) throw new Error(`找不到 ${head}`);
  const end = src.indexOf("\n    };", from);
  if (end < 0) throw new Error(`${fieldName} 数组体结尾没找到`);
  if (!list.length) throw new Error(`${fieldName} 转换结果为空，拒绝写入（先检查 dumps 路径与 config.json）`);
  const indent = "        ";
  return src.slice(0, from + head.length) + "\n" + list.map(r => indent + r).join(",\n") + src.slice(end);
}

let src = fs.readFileSync(TARGET, "utf8");
src = replaceTable(src, "ALLOYS", rows.alloy);
src = replaceTable(src, "GRIND", rows.grind);
// 压缩机表可以空（config 里没有对应来源时），空数组不值得为它改写文件
if (rows.press && rows.press.length) src = replaceTable(src, "PRESS", rows.press);
else console.log("PRESS 无导入行，保持原样");
fs.writeFileSync(TARGET, src);
console.log(`已写回 ${display(TARGET)}：ALLOYS=${rows.alloy.length} GRIND=${rows.grind.length} PRESS=${(rows.press || []).length}`);
console.log("下一步：./gradlew runData && node tools/compat/check.js");
