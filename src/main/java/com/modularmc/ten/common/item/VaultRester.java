package com.modularmc.ten.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;

public class VaultRester extends Item {
    public VaultRester(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        //激活和未激活状态下可用，防止开启过程中使用出现问题
        if (level.getBlockEntity(blockpos) instanceof VaultBlockEntity entity && level.getBlockState(blockpos).getValue(VaultBlock.STATE).ordinal() < 2){
            if (level instanceof ServerLevel sLevel) {
                VaultServerData data = entity.getServerData();
                if (data != null) {
                    data.getRewardedPlayers().clear();
                    data.markChanged();
                    VaultConfig config = entity.getConfig();
                    entity.getSharedData().updateConnectedPlayersWithinRange(sLevel, blockpos, data, config, config.deactivationRange());
                }
            }else ParticleUtils.spawnParticlesOnBlockFaces(level, blockpos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
            return InteractionResult.sidedSuccess(level.isClientSide);

        }

        return InteractionResult.PASS;
    }

}
