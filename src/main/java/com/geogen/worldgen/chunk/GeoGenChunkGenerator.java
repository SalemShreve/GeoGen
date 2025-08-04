// src/main/java/com/geogen/worldgen/chunk/GeoGenChunkGenerator.java
package com.geogen.worldgen.chunk;

import com.geogen.worldgen.density.TectonicDensityFunction;
import com.geogen.worldgen.plates.PlateSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Custom chunk generator that replaces vanilla terrain generation with geological simulation
 */
public abstract class GeoGenChunkGenerator extends ChunkGenerator {
    public static final Codec<GeoGenChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource),
                    Codec.LONG.fieldOf("seed").forGetter(generator -> generator.seed)
            ).apply(instance, GeoGenChunkGenerator::new)
    );

    private final long seed;
    private final PlateSystem plateSystem;
    private final TectonicDensityFunction densityFunction;

    public GeoGenChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.seed = seed;
        this.plateSystem = new PlateSystem(seed);
        this.densityFunction = new TectonicDensityFunction(seed, 64.0, 1.0);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomSource random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // TODO: Implement geological carving (rivers, caves) based on rock hardness and permeability
        // For now, skip carving to focus on basic terrain generation
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomSource random, ChunkAccess chunk) {
        // Apply surface rules based on geological properties
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkX * 16 + localX;
                int worldZ = chunkZ * 16 + localZ;

                // Get terrain properties
                var plateData = plateSystem.getPlateAt(worldX, worldZ);

                // Apply surface based on crust type
                applySurfaceForColumn(chunk, localX, localZ, plateData);
            }
        }
    }

    private void applySurfaceForColumn(ChunkAccess chunk, int x, int z, com.geogen.worldgen.plates.PlateData plateData) {
        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

        // Surface rules based on crust type
        BlockState surfaceBlock;
        BlockState subsurfaceBlock;

        switch (plateData.getCrustType()) {
            case OCEANIC:
                surfaceBlock = Blocks.GRAVEL.defaultBlockState(); // Oceanic sediments
                subsurfaceBlock = Blocks.BASALT.defaultBlockState(); // Oceanic basalt
                break;
            case CONTINENTAL:
                surfaceBlock = Blocks.GRASS_BLOCK.defaultBlockState(); // Continental surface
                subsurfaceBlock = Blocks.STONE.defaultBlockState(); // Continental granite/metamorphic
                break;
            default:
                surfaceBlock = Blocks.STONE.defaultBlockState();
                subsurfaceBlock = Blocks.STONE.defaultBlockState();
        }

        // Apply surface layers
        if (topY >= chunk.getMinBuildHeight()) {
            chunk.setBlockState(new BlockPos(x, topY, z), surfaceBlock, false);

            // Add a few layers of subsurface material
            for (int i = 1; i <= 3 && (topY - i) >= chunk.getMinBuildHeight(); i++) {
                chunk.setBlockState(new BlockPos(x, topY - i, z), subsurfaceBlock, false);
            }
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Standard mob spawning - no changes needed for basic implementation
    }

    @Override
    public int getGenDepth() {
        return 384; // Standard world height
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomSource random,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            // Fill chunk using our tectonic density function
            fillChunkWithTectonicTerrain(chunk);
            return chunk;
        }, executor);
    }

    private void fillChunkWithTectonicTerrain(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        // Process each column in the chunk
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.x * 16 + localX;
                int worldZ = chunkPos.z * 16 + localZ;

                // Fill column based on density function
                for (int y = minY; y < maxY; y++) {
                    var context = new DensityFunction.SinglePointContext(worldX, y, worldZ);
                    double density = densityFunction.compute(context);

                    if (density > 0) {
                        // Positive density = solid block
                        BlockState blockState = getBlockStateForLocation(worldX, y, worldZ);
                        chunk.setBlockState(new BlockPos(localX, y, localZ), blockState, false);
                    }
                }
            }
        }
    }

    private BlockState getBlockStateForLocation(int x, int y, int z) {
        // Get plate data to determine rock type
        var plateData = plateSystem.getPlateAt(x, z);

        // Simple rock type assignment based on depth and crust type
        if (y < 0) {
            // Deep crustal rocks
            return plateData.getCrustType() == com.geogen.worldgen.plates.CrustType.OCEANIC
                    ? Blocks.BASALT.defaultBlockState()
                    : Blocks.STONE.defaultBlockState(); // Changed from GRANITE to STONE
        } else if (y < 32) {
            // Mid-level rocks
            return plateData.getCrustType() == com.geogen.worldgen.plates.CrustType.OCEANIC
                    ? Blocks.TUFF.defaultBlockState()
                    : Blocks.ANDESITE.defaultBlockState();
        } else {
            // Surface level
            return Blocks.STONE.defaultBlockState();
        }
    }

    @Override
    public int getSeaLevel() {
        return 63; // Standard sea level
    }

    @Override
    public int getMinY() {
        return -64; // Standard minimum Y
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level,
                             RandomSource random) {
        // Calculate terrain height using our geological system
        var plateData = plateSystem.getPlateAt(x, z);

        // Base height calculation (simplified version of density function logic)
        double baseHeight = 64 + (plateData.getBaseElevation() * 32);
        baseHeight += plateData.getCrustType().getElevationModifier() * 16;

        // Add distance-from-center variation
        double distanceToCenter = plateData.getDistanceToCenter(x, z);
        double centerEffect = Math.exp(-distanceToCenter / 2048.0) * plateData.getAge() * 8;
        baseHeight += centerEffect;

        return (int) Math.max(level.getMinBuildHeight(), Math.min(level.getMaxBuildHeight() - 1, baseHeight));
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomSource random) {
        // Create a noise column representing the terrain at this location
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        BlockState[] states = new BlockState[maxY - minY];

        for (int y = minY; y < maxY; y++) {
            var context = new DensityFunction.SinglePointContext(x, y, z);
            double density = densityFunction.compute(context);

            states[y - minY] = density > 0 ? getBlockStateForLocation(x, y, z) : Blocks.AIR.defaultBlockState();
        }

        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomSource random, BlockPos pos) {
        // Add geological debug information
        var plateData = plateSystem.getPlateAt(pos.getX(), pos.getZ());
        double boundaryDistance = plateSystem.getDistanceToBoundary(pos.getX(), pos.getZ());

        info.add("GeoGen Debug:");
        info.add("Plate ID: " + plateData.getPlateId());
        info.add("Crust Type: " + plateData.getCrustType());
        info.add("Plate Age: " + String.format("%.2f", plateData.getAge()));
        info.add("Base Elevation: " + String.format("%.2f", plateData.getBaseElevation()));
        info.add("Boundary Distance: " + String.format("%.1f", boundaryDistance));
        info.add("Center Distance: " + String.format("%.1f", plateData.getDistanceToCenter(pos.getX(), pos.getZ())));
    }

    @Override
    public Climate.Sampler climateSampler() {
        // Return a basic climate sampler - we'll enhance this later for realistic climate simulation
        return (x, y, z) -> {
            // Basic climate based on coordinates (temperature decreases with Z, humidity varies)
            long temperature = (long) (-z * 100); // Colder toward negative Z
            long humidity = (long) (Math.sin(x * 0.001) * 1000); // Vary humidity with X
            long continentalness = 0; // Neutral continentalness for now
            long erosion = 0; // No erosion factor yet
            long depth = 0; // No depth factor
            long weirdness = 0; // No weirdness

            return Climate.target(temperature, humidity, continentalness, erosion, depth, weirdness);
        };
    }
}