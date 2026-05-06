package com.modularmc.template.mixin;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.util.Set;

public class TemplateMixinConfig implements IMixinConfig {
    @Override
    public MixinEnvironment getEnvironment() {
        return null;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public IMixinConfigSource getSource() {
        return null;
    }

    @Override
    public String getCleanSourceId() {
        return "";
    }

    @Override
    public String getMixinPackage() {
        return "";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public IMixinConfigPlugin getPlugin() {
        return null;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public Set<String> getTargets() {
        return Set.of();
    }

    @Override
    public <V> void decorate(String key, V value) {

    }

    @Override
    public boolean hasDecoration(String key) {
        return false;
    }

    @Override
    public <V> V getDecoration(String key) {
        return null;
    }
}
