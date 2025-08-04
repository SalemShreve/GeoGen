package com.geogen.worldgen.plates;

import net.minecraft.util.Mth;

public class PlateData {
    private final int plateId;
    private final double centerX, centerZ;
    private final double age; // 0.0 to 1.0, where 1.0 is oldest
    private final double movementDirection; // radians
    private final double movementSpeed; // arbitrary units
    private final double baseElevation; // -1.0 to 1.0
    private final CrustType crustType;

    public PlateData(int plateId, double centerX, double centerZ, double age,
                     double movementDirection, double movementSpeed, double baseElevation) {
        this.plateId = plateId;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.age = age;
        this.movementDirection = movementDirection;
        this.movementSpeed = movementSpeed;
        this.baseElevation = baseElevation;
        this.crustType = determineCrustType();
    }

    private CrustType determineCrustType() {
        // Implement 70/30 oceanic/continental split based on age and elevation
        double ageComponent = Math.pow(age, 2.0);
        double elevationComponent = Math.pow((baseElevation + 1.0) / 2.0, 1.5);

        double continentalProbability = ageComponent * elevationComponent;
        return continentalProbability > 0.3 ? CrustType.CONTINENTAL : CrustType.OCEANIC;
    }

    public double getDistanceToCenter(double x, double z) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    // Getters
    public int getPlateId() { return plateId; }
    public double getCenterX() { return centerX; }
    public double getCenterZ() { return centerZ; }
    public double getAge() { return age; }
    public double getMovementDirection() { return movementDirection; }
    public double getMovementSpeed() { return movementSpeed; }
    public double getBaseElevation() { return baseElevation; }
    public CrustType getCrustType() { return crustType; }
}