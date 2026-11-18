// 压缩机：2 个输入槽，1 个输出
// 本模组的板/杆/齿轮都靠「材料 + 模具」压缩，模具本身不消耗
ServerEvents.recipes(event => {
    // 1. 板：4 份材料 + 板模具
    event.recipes.kenergyengineering.compressor(
        ['4x #c:ingots/iron', 'kenergyengineering:mould_plate'],
        ['minecraft:iron_block'],
        100
    )

    // 2. 齿轮：4 份材料 + 齿轮模具
    event.recipes.kenergyengineering.compressor(
        ['4x #c:ingots/copper', 'kenergyengineering:mould_gear'],
        ['minecraft:copper_block'],
        120
    )

    // 3. 2x2 压缩：四份材料 + 小压缩模具
    event.recipes.kenergyengineering.compressor(
        ['4x minecraft:coal', 'kenergyengineering:mould_compressed_small'],
        ['minecraft:coal_block'],
        150
    )

    // 4. 3x3 压缩：九份材料 + 大压缩模具
    event.recipes.kenergyengineering.compressor(
        ['9x minecraft:redstone', 'kenergyengineering:mould_compressed_large'],
        ['minecraft:redstone_block'],
        200
    )

    // 5. 解压：方块 + 解压缩模具
    event.recipes.kenergyengineering.compressor(
        ['minecraft:lapis_block', 'kenergyengineering:mould_split'],
        ['9x minecraft:lapis_lazuli'],
        80
    )
})
