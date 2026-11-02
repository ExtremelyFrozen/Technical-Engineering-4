// 把 convert.js 产出的表行写回 TENCompatGen.java 的 ALLOYS / GRIND 数组。
// 只替换数组体，工厂方法与 emit 逻辑保持原样（手改过的断言、门控不会被覆盖）。
// 用法：node tools/compat/convert.js && node tools/compat/inject.js && ./gradlew runData
const fs = require("fs");
const path = require("path");
const { display } = require("./common");

const TARGET = path.join(__dirname, "../../src/main/java/com/modularmc/ten/data/TENCompatGen.java");
const rows = JSON.parse(fs.readFileSync(path.join(__dirname, "out/rows.json"), "utf8"));

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
