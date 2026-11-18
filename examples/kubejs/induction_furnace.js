// 感应炉：3 个输入槽，1 个输出
// 适合做合金与需要多材料配比的转化，输入可以混物品与流体
ServerEvents.recipes(event => {
    // 1. 二元合金
    event.recipes.kenergyengineering.induction_furnace(
        ['#c:ingots/copper', '#c:ingots/tin'],
        ['2x minecraft:redstone'],
        150
    )

    // 2. 三元合金 + 低概率副产
    event.recipes.kenergyengineering.induction_furnace(
        ['#c:ingots/iron', '#c:dusts/coal', '#c:obsidians'],
        [{form: 'item', type: 'static', key: 'minecraft:netherite_scrap', count: 1, chance: 0.1}],
        400
    )

    // 3. 物品 + 流体混合输入（流体必须写完整对象）
    event.recipes.kenergyengineering.induction_furnace(
        [
            {form: 'item', type: 'tag', key: 'c:ingots/iron'},
            {form: 'fluid', type: 'static', key: 'minecraft:water', amount: 1000}
        ],
        ['2x minecraft:iron_block'],
        200
    )
})
