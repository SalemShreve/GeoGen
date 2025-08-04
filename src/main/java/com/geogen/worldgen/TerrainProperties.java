package com.geogen.worldgen;

import com.geogen.worldgen.plates.PlateData;
import com.geogen.worldgen.plates.CrustType;

/**
 * Container for all terrain properties at a given coordinate
 */
public class TerrainProperties {
    private final PlateData plateData;
    private final CrustType crustType;
    private final double terrainHeight;
    private final double boundaryDistance;

    public TerrainProperties(PlateData plateData, CrustType crustType, double terrainHeight, double boundaryDistance) {
        this.plateData = plateData;
        this.crustType = crustType;
        this.terrainHeight = terrainHeight;
        this.boundaryDistance = boundaryDistance;
    }

    public PlateData getPlateData() { return plateData; }
    public CrustType getCrustType() { return crustType; }
    public double getTerrainHeight() { return terrainHeight; }
    public double getBoundaryDistance() { return boundaryDistance; }

    public boolean isNearBoundary(double threshold) {
        return boundaryDistance < threshold;
    }
}