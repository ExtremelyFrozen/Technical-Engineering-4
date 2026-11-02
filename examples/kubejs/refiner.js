// 精炼机：2 个输入槽，2 个输出
// 双输出的机器，适合「主产物 + 副产物」或「两种产物同时产出」
ServerEvents.recipes(event => {
    // 1. 基础：主产物 + 副产物
    event.recipes.kenergyengineering.refiner(
        ['minecraft:raw_iron', 'minecraft:coal'],
        ['2x minecraft:iron_ingot', 'minecraft:gravel'],
        200
    )

    // 2. 两个输出各自带概率
    event.recipes.kenergyengineering.refiner(
        ['#c:raw_materials/gold', '#c:dusts/redstone'],
        [
            {form: 'item', type: 'static', key: 'minecraft:gold_ingot', count: 2, chance: 0.9},
            {form: 'item', type: 'static', key: 'minecraft:gold_nugget', count: 3, chance: 0.5}
        ],
        250
    )

    // 3. 标签输入 + 流体输入混合
    event.recipes.kenergyengineering.refiner(
        [
            {form: 'item', type: 'tag', key: 'c:raw_materials/copper'},
            {form: 'fluid', type: 'static', key: 'minecraft:water', amount: 250}
        ],
        ['3x minecraft:copper_ingot', 'minecraft:clay_ball'],
        180
    )
})
