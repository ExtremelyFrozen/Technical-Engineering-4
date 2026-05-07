package com.modularmc.ten.api.registry.registrate;

import com.modularmc.ten.TEN;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.NoConfigBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

public class TENRegistrate extends AbstractRegistrate<TENRegistrate> {

    private static final Map<String, TENRegistrate> EXISTING_REGISTRATES = new Object2ObjectOpenHashMap<>();

    protected TENRegistrate(String modid) {
        super(modid);
    }

    public static TENRegistrate create(String modId) {
        return create(modId, true);
    }

    public static TENRegistrate create(String modId, boolean registerEvents) {
        return innerCreate(modId, false, registerEvents);
    }

    private static TENRegistrate innerCreate(String modId, boolean registerEvents, boolean requireValidEventBus) {
        if (EXISTING_REGISTRATES.containsKey(modId)) {
            return EXISTING_REGISTRATES.get(modId);
        }
        var registrate = new TENRegistrate(modId);
        if (registerEvents) {
            Optional<IEventBus> modEventBus = ModList.get().getModContainerById(modId).map(ModContainer::getEventBus);
            if (requireValidEventBus) {
                modEventBus.ifPresentOrElse(registrate::registerEventListeners, () -> {
                    String message = "# [TENRegistrate] Failed to register eventListeners for mod " + modId +
                            ", This should be reported to this mod's dev #";
                    String hashtags = "#".repeat(message.length());
                    TEN.LOGGER.fatal(hashtags);
                    TEN.LOGGER.fatal(message);
                    TEN.LOGGER.fatal(hashtags);
                });
            } else {
                registrate.registerEventListeners(modEventBus.orElse(TEN.tenModBus));
            }
        }
        EXISTING_REGISTRATES.put(modId, registrate);
        return registrate;
    }

    protected <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> createCreativeModeTab(P parent, String name,
                                                                                             Consumer<CreativeModeTab.Builder> config) {
        return this.generic(parent, name, Registries.CREATIVE_MODE_TAB, () -> {
            var builder = CreativeModeTab.builder()
                    .icon(() -> ItemStack.EMPTY);
            config.accept(builder);
            return builder.build();
        });
    }

    public <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> defaultCreativeTab(P parent, String name,
                                                                                        Consumer<CreativeModeTab.Builder> config) {
        return createCreativeModeTab(parent, name, config);
    }

    private @Nullable RegistryEntry<CreativeModeTab, ? extends CreativeModeTab> currentTab;
    private static final Map<RegistryEntry<?, ?>, RegistryEntry<CreativeModeTab, ? extends CreativeModeTab>> TAB_LOOKUP = new IdentityHashMap<>();

    public @Nullable RegistryEntry<CreativeModeTab, ? extends CreativeModeTab> creativeModeTab() {
        return this.currentTab;
    }

    public void creativeModeTab(Supplier<RegistryEntry<CreativeModeTab, ? extends CreativeModeTab>> currentTab) {
        this.currentTab = currentTab.get();
    }

    public void resetCreativeModeTab() {
        this.currentTab = null;
    }

    public void creativeModeTab(RegistryEntry<CreativeModeTab, ? extends CreativeModeTab> currentTab) {
        this.currentTab = currentTab;
    }

    public boolean isInCreativeTab(RegistryEntry<?, ?> entry,
                                   RegistryEntry<CreativeModeTab, ? extends CreativeModeTab> tab) {
        return TAB_LOOKUP.get(entry) == tab;
    }

    public void setCreativeTab(RegistryEntry<?, ?> entry,
                                @Nullable RegistryEntry<CreativeModeTab, ? extends CreativeModeTab> tab) {
        TAB_LOOKUP.put(entry, tab);
    }
}
