// generate_placeholders.mjs
// 材料×变体 纹理占位符生成脚本
// 用 iron_dust.png 作为模板，为缺失的纹理创建占位符

import fs from 'fs';
import path from 'path';

const template = 'E:/GitHub/Technical-Engineering-4/src/main/resources/assets/kenergyengineering/textures/item/material/dust/iron_dust.png';
const baseDir = 'E:/GitHub/Technical-Engineering-4/src/main/resources/assets/kenergyengineering/textures/item';

// 验证模板存在
if (!fs.existsSync(template)) {
    console.error(`模板文件不存在: ${template}`);
    process.exit(1);
}

let created = 0;
let skipped = 0;

// ===== 材料×变体矩阵 =====
const matrix = [
    ['copper', '铜', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['iron', '铁', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['gold', '金', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['netherite', '下界合金', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['nickel', '镍', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['tin', '锡', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['powered_tin', '充能锡', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['chlorium', '叶绿', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['mushrium', 'mushrium', ['nugget', 'dust', 'gear', 'plate', 'wire', 'rod']],
    ['starlight', 'starlight', ['dust']],
    ['diamond', '钻石', ['nugget', 'dust', 'gear', 'plate']],
    ['emerald', '绿宝石', ['nugget', 'dust', 'gear', 'plate']],
    ['lapis', '青金石', ['nugget', 'dust', 'gear', 'plate']],
    ['quartz', '石英', ['nugget', 'dust', 'gear', 'plate']],
    ['amethyst', '紫水晶', ['dust', 'gear', 'plate']],
    ['redstone', '红石', ['gear', 'plate']],
];

// ===== 模具矩阵 =====
const moulds = [
    ['compressed_small', '压缩-小型'],
    ['compressed_large', '压缩-大型'],
    ['split', '拆分'],
    ['coin', '币'],
    ['dense_plate', '致密板'],
];

console.log('===== 材料类占位符 =====');
for (const [mat, matCn, variants] of matrix) {
    for (const variant of variants) {
        const fileName = `${mat}_${variant}.png`;
        const targetDir = path.join(baseDir, 'material', variant);
        const targetPath = path.join(targetDir, fileName);

        if (!fs.existsSync(targetPath)) {
            if (!fs.existsSync(targetDir)) {
                fs.mkdirSync(targetDir, { recursive: true });
                console.log(`  [DIR] 创建目录: ${targetDir}`);
            }
            fs.copyFileSync(template, targetPath);
            console.log(`  [CREATE] ${matCn} (${mat}) → ${variant}/${fileName}`);
            created++;
        } else {
            console.log(`  [SKIP] ${matCn} (${mat}) → ${variant}/${fileName} (已存在)`);
            skipped++;
        }
    }
}

console.log('');
console.log('===== 模具类占位符 =====');
const mouldDir = path.join(baseDir, 'mold');
if (!fs.existsSync(mouldDir)) {
    fs.mkdirSync(mouldDir, { recursive: true });
    console.log(`  [DIR] 创建目录: ${mouldDir}`);
}
for (const [mouldId, mouldCn] of moulds) {
    const fileName = `${mouldId}.png`;
    const targetPath = path.join(mouldDir, fileName);
    if (!fs.existsSync(targetPath)) {
        fs.copyFileSync(template, targetPath);
        console.log(`  [CREATE] 模具 → ${fileName}`);
        created++;
    } else {
        console.log(`  [SKIP] 模具 → ${fileName} (已存在)`);
        skipped++;
    }
}

console.log('');
console.log('===== 汇总 =====');
console.log(`创建: ${created} 个占位符文件`);
console.log(`跳过: ${skipped} 个已有文件`);
console.log('完成！');
