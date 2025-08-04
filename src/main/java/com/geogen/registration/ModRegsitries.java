package com.geogen.registration;

import com.geogen.GeoGen;
import com.geogen.worldgen.chunk.GeoGenChunkGenerator;
import com.geogen.worldgen.density.TectonicDensityFunction;
import com.geogen.worldgen.density.PlateBoundaryDensityFunction;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModRegistries {

    // Chunk Generator Registration
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, GeoGen.MODID);

    public static final RegistryObject<Codec<GeoGenChunkGenerator>> GEOGEN_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("geogen", () -> GeoGenChunkGenerator.CODEC);

    // Density Function Registration
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTIONS =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, GeoGen.MODID);

    public static final RegistryObject<Codec<TectonicDensityFunction>> TECTONIC_DENSITY =
            DENSITY_FUNCTIONS.register("tectonic", () -> TectonicDensityFunction.CODEC);

    public static final RegistryObject<Codec<PlateBoundaryDensityFunction>> PLATE_BOUNDARY_DENSITY =
            DENSITY_FUNCTIONS.register("plate_boundary", () -> PlateBoundaryDensityFunction.CODEC);
}