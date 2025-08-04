package com.geogen;

import com.geogen.registration.ModRegistries;
import com.geogen.worldgen.chunk.GeoGenChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(GeoGen.MODID)
public class GeoGen {
    public static final String MODID = "geogen";
    public static final Logger LOGGER = LoggerFactory.getLogger(GeoGen.class);

    public GeoGen() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register our mod components
        ModRegistries.CHUNK_GENERATORS.register(modEventBus);
        ModRegistries.DENSITY_FUNCTIONS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("GeoGen mod initialized - Geological terrain generation loading...");
    }

    public static ResourceLocation location(String path) {
        return new ResourceLocation(MODID, path);
    }
}