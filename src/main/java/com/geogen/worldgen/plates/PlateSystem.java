package com.geogen.worldgen.plates;

import com.geogen.worldgen.noise.SimplexNoise;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

public class PlateSystem {
    private final long seed;
    private final SimplexNoise plateNoise;
    private final SimplexNoise ageNoise;
    private final SimplexNoise elevationNoise;
    private final Map<Integer, PlateData> plateCache;

    // Configuration
    private static final double PLATE_SCALE = 0.0001; // Large plates
    private static final int PLATE_COUNT_ESTIMATE = 20; // Approximate number of plates

    public PlateSystem(long seed) {
        this.seed = seed;
        this.plateNoise = new SimplexNoise(RandomSource.create(seed));
        this.ageNoise = new SimplexNoise(RandomSource.create(seed + 1));
        this.elevationNoise = new SimplexNoise(RandomSource.create(seed + 2));
        this.plateCache = new HashMap<>();
    }

    /**
     * Get the plate data for coordinates using Voronoi (distance-based) method
     */
    public PlateData getPlateAt(double x, double z) {
        // Use noise to generate pseudo-random plate centers
        int plateId = findNearestPlateId(x, z);

        return plateCache.computeIfAbsent(plateId, id -> generatePlateData(id, x, z));
    }

    private int findNearestPlateId(double x, double z) {
        // Sample noise in a grid pattern to create consistent plate centers
        double minDistance = Double.MAX_VALUE;
        int nearestPlateId = 0;

        // Check a 3x3 grid of potential plate centers around the point
        int gridSize = 2048; // Distance between potential plate centers
        int gridX = (int) Math.floor(x / gridSize);
        int gridZ = (int) Math.floor(z / gridSize);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int checkGridX = gridX + dx;
                int checkGridZ = gridZ + dz;

                // Generate pseudo-random offset for this grid cell
                RandomSource random = RandomSource.create(
                        seed ^ (checkGridX * 374761393L) ^ (checkGridZ * 668265263L)
                );

                double offsetX = random.nextDouble() * gridSize - gridSize * 0.5;
                double offsetZ = random.nextDouble() * gridSize - gridSize * 0.5;

                double centerX = checkGridX * gridSize + offsetX;
                double centerZ = checkGridZ * gridSize + offsetZ;

                double distance = Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));

                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPlateId = checkGridX * 31 + checkGridZ; // Simple but unique ID
                }
            }
        }

        return nearestPlateId;
    }

    private PlateData generatePlateData(int plateId, double sampleX, double sampleZ) {
        // Regenerate the plate center for this ID
        RandomSource random = RandomSource.create(seed ^ (plateId * 1234567L));

        // Calculate grid position from plate ID (reverse of ID generation)
        int gridX = plateId / 31;
        int gridZ = plateId % 31;
        int gridSize = 2048;

        double offsetX = random.nextDouble() * gridSize - gridSize * 0.5;
        double offsetZ = random.nextDouble() * gridSize - gridSize * 0.5;

        double centerX = gridX * gridSize + offsetX;
        double centerZ = gridZ * gridSize + offsetZ;

        // Generate plate properties using noise
        double age = (ageNoise.getValue(centerX * PLATE_SCALE * 2, centerZ * PLATE_SCALE * 2) + 1.0) * 0.5;
        age = Mth.clamp(age, 0.0, 1.0);

        double elevation = elevationNoise.getValue(centerX * PLATE_SCALE * 3, centerZ * PLATE_SCALE * 3);
        elevation = Mth.clamp(elevation, -1.0, 1.0);

        double movementDirection = random.nextDouble() * Math.PI * 2;
        double movementSpeed = 0.1 + random.nextDouble() * 0.9; // 0.1 to 1.0

        return new PlateData(plateId, centerX, centerZ, age, movementDirection, movementSpeed, elevation);
    }

    /**
     * Calculate distance to nearest plate boundary
     */
    public double getDistanceToBoundary(double x, double z) {
        PlateData currentPlate = getPlateAt(x, z);
        double minBoundaryDistance = Double.MAX_VALUE;

        // Check surrounding area for different plates
        int searchRadius = 512;
        int step = 64;

        for (int dx = -searchRadius; dx <= searchRadius; dx += step) {
            for (int dz = -searchRadius; dz <= searchRadius; dz += step) {
                PlateData checkPlate = getPlateAt(x + dx, z + dz);
                if (checkPlate.getPlateId() != currentPlate.getPlateId()) {
                    double distance = Math.sqrt(dx * dx + dz * dz);
                    minBoundaryDistance = Math.min(minBoundaryDistance, distance);
                }
            }
        }

        return minBoundaryDistance == Double.MAX_VALUE ? searchRadius : minBoundaryDistance;
    }
}