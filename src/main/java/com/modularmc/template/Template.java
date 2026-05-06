package com.modularmc.template;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(Template.MOD_ID)
public class Template {

    public static final String MOD_ID = "template";
    public static final String MOD_NAME = "Template";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public Template(IEventBus bus) {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
