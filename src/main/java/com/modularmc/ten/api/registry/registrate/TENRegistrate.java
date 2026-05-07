package com.modularmc.ten.api.registry.registrate;

import com.modularmc.ten.TEN;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import com.tterrag.registrate.AbstractRegistrate;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Optional;

public class TENRegistrate extends AbstractRegistrate<TENRegistrate> {

    private static final Map<String, TENRegistrate> EXISTING_REGISTRATES = new Object2ObjectOpenHashMap<>();

    /**
     * Construct a new Registrate for the given mod ID.
     *
     * @param modid The mod ID for which objects will be registered
     */
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
                    String message = "# [GTRegistrate] Failed to register eventListeners for mod " + modId +
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
}
