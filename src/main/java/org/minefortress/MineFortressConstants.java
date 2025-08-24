package org.minefortress;

public class MineFortressConstants {

    public static final double PICK_DISTANCE = 120;
    public static final float PICK_DISTANCE_FLOAT = (float) PICK_DISTANCE;

    public static final double ESTIMATED_BREAKING_TIME = (0.4 * 5.0 + 0.6) / 6.0; // In seconds per block. Calculation: (0.4 (Dirt mining speed) * 5 + 0.6 (Stone mining speed)) / 6
    public static final double ESTIMATED_PLACING_TIME = 4.0 / 20.0; // In seconds per block. "#rightClickSpeed" taken from baritone, 4 ticks = 0.2.
    public static final double ESTIMATED_RUNNING_SPEED = 5.612; // In blocks per second.
}
