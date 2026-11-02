// 粉碎机：1 个输入槽，最多 4 个输出
// 复制到 kubejs/server_scripts/ 即可生效
ServerEvents.recipes(event => {
    // 1. 基础：单个物品进，单个物品出
    event.recipes.kenergyengineering.pulverizer(
        ['minecraft:iron_ore'],
        ['2x minecraft:iron_ingot'],
        100
    )

    // 2. 标签输入 + 概率副产
    event.recipes.kenergyengineering.pulverizer(
        ['#c:ores/gold'],
        [
            'minecraft:gold_ingot',
            {form: 'item', type: 'static', key: 'minecraft:gold_nugget', count: 2, chance: 0.5}
        ],
        120
    )

    // 3. 完整对象：显式写出 chance 与 rolls
    event.recipes.kenergyengineering.pulverizer(
        [{form: 'item', type: 'tag', key: 'c:raw_materials/copper'}],
        [{form: 'item', type: 'static', key: 'minecraft:copper_ingot', count: 3, chance: 1.0, rolls: 1}],
        80
    )

    // 4. 四路输出：主产物 + 三种副产，副产各自独立掷骰
    event.recipes.kenergyengineering.pulverizer(
        ['minecraft:deepslate_gold_ore'],
        [
            {form: 'item', type: 'static', key: 'minecraft:gold_ingot', count: 2},
            {form: 'item', type: 'static', key: 'minecraft:gold_nugget', count: 4, chance: 0.75},
            {form: 'item', type: 'static', key: 'minecraft:cobbled_deepslate', count: 1, chance: 0.25},
            {form: 'item', type: 'static', key: 'minecraft:experience_bottle', count: 1, chance: 0.05}
        ],
        150
    )
})
