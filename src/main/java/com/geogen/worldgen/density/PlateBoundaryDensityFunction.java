package com.geogen.worldgen.density;

import com.geogen.worldgen.plates.PlateSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density function that provides plate boundary information for debugging and surface rules
 */
public class PlateBoundaryDensityFunction implements DensityFunction {
    public static final Codec<PlateBoundaryDensityFunction> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("seed").forGetter(f -> f.seed),
                    Codec.DOUBLE.fieldOf("boundary_threshold").orElse(256.0).forGetter(f -> f.boundaryThreshold)
            ).apply(instance, PlateBoundaryDensityFunction::new)
    );

    private final long seed;
    private final double boundaryThreshold;
    private final PlateSystem plateSystem;

    public PlateBoundaryDensityFunction(long seed, double boundaryThreshold) {
        this.seed = seed;
        this.boundaryThreshold = boundaryThreshold;
        this.plateSystem = new PlateSystem(seed);
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX();
        int z = context.blockZ();

        double boundaryDistance = plateSystem.getDistanceToBoundary(x, z);

        // Return 1.0 near boundaries, 0.0 far from boundaries
        return Math.max(0.0, 1.0 - (boundaryDistance / boundaryThreshold));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PlateBoundaryDensityFunction(seed, boundaryThreshold));
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public Codec<? extends DensityFunction> codec() {
        return CODEC;
    }
}