package com.geogen.worldgen.density;

import com.geogen.worldgen.plates.PlateSystem;
import com.geogen.worldgen.plates.PlateData;
import com.geogen.worldgen.plates.CrustType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Main density function that calculates terrain density based on plate tectonics
 */
public class TectonicDensityFunction implements DensityFunction {
    public static final Codec<TectonicDensityFunction> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("seed").forGetter(f -> f.seed),
                    Codec.DOUBLE.fieldOf("surface_level").orElse(64.0).forGetter(f -> f.surfaceLevel),
                    Codec.DOUBLE.fieldOf("height_scale").orElse(1.0).forGetter(f -> f.heightScale)
            ).apply(instance, TectonicDensityFunction::new)
    );

    private final long seed;
    private final double surfaceLevel;
    private final double heightScale;
    private final PlateSystem plateSystem;

    public TectonicDensityFunction(long seed, double surfaceLevel, double heightScale) {
        this.seed = seed;
        this.surfaceLevel = surfaceLevel;
        this.heightScale = heightScale;
        this.plateSystem = new PlateSystem(seed);
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int y = context.blockY();
        int z = context.blockZ();

        // Get plate data for this location
        PlateData plate = plateSystem.getPlateAt(x, z);

        // Calculate base terrain height based on plate properties
        double baseHeight = calculateTerrainHeight(plate, x, z);

        // Calculate density: positive = solid, negative = air
        double density = (baseHeight - y) * 0.1;

        // Apply crust type modifications
        if (plate.getCrustType() == CrustType.OCEANIC) {
            density += 0.05; // Oceanic crust is denser
        } else {
            density -= 0.02; // Continental crust is less dense, more elevated
        }

        return density * heightScale;
    }

    private double calculateTerrainHeight(PlateData plate, double x, double z) {
        // Base height from plate elevation
        double height = surfaceLevel + (plate.getBaseElevation() * 32);

        // Add crust type elevation modifier
        height += plate.getCrustType().getElevationModifier() * 16;

        // Add distance-from-center variation (older crust is more elevated toward center)
        double distanceToCenter = plate.getDistanceToCenter(x, z);
        double centerEffect = Math.exp(-distanceToCenter / 2048.0) * plate.getAge() * 8;
        height += centerEffect;

        // Add boundary effects
        double boundaryDistance = plateSystem.getDistanceToBoundary(x, z);
        if (boundaryDistance < 256) {
            // Near plate boundaries - could be mountains or valleys depending on interaction type
            double boundaryEffect = (1.0 - boundaryDistance / 256.0) * 12;

            // For now, simple uplift near boundaries (will be refined with boundary type logic)
            height += boundaryEffect;
        }

        return height;
    }

    @Override
    public void fillArray(double[] densities, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new TectonicDensityFunction(seed, surfaceLevel, heightScale));
    }

    @Override
    public double minValue() {
        return -64.0;
    }

    @Override
    public double maxValue() {
        return 64.0;
    }

    @Override
    public Codec<? extends DensityFunction> codec() {
        return CODEC;
    }
}