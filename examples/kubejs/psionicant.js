// 灵能处理器：2 个输入槽，1 个输出
// 适合做「两种材料合成一件产物」的转化，不涉及模具
ServerEvents.recipes(event => {
    // 1. 基础：两种原版材料
    event.recipes.kenergyengineering.psionicant(
        ['minecraft:amethyst_shard', 'minecraft:ender_pearl'],
        ['minecraft:ender_eye'],
        200
    )

    // 2. 标签 + 物品混合
    event.recipes.kenergyengineering.psionicant(
        ['#c:dusts/redstone', 'minecraft:glowstone_dust'],
        ['4x minecraft:redstone'],
        80
    )

    // 3. 完整对象：输出带概率（本机器只有 1 个输出槽）
    event.recipes.kenergyengineering.psionicant(
        [
            {form: 'item', type: 'tag', key: 'c:gems/diamond'},
            {form: 'item', type: 'static', key: 'minecraft:nether_star'}
        ],
        [
            {form: 'item', type: 'static', key: 'minecraft:beacon', count: 1, chance: 0.25}
        ],
        600
    )
})
