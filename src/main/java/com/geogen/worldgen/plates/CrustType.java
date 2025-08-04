package com.geogen.worldgen.plates;

public enum CrustType {
    OCEANIC(0.8, 2.9, -0.3), // Dense, low elevation
    CONTINENTAL(0.4, 2.7, 0.2); // Less dense, higher elevation

    private final double density;
    private final double specificGravity;
    private final double elevationModifier;

    CrustType(double density, double specificGravity, double elevationModifier) {
        this.density = density;
        this.specificGravity = specificGravity;
        this.elevationModifier = elevationModifier;
    }

    public double getDensity() { return density; }
    public double getSpecificGravity() { return specificGravity; }
    public double getElevationModifier() { return elevationModifier; }
}