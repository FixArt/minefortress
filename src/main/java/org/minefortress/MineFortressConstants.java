package org.minefortress;

import org.minefortress.entity.Colonist;

public class MineFortressConstants {

    public static final double PICK_DISTANCE = 120;
    public static final float PICK_DISTANCE_FLOAT = (float) PICK_DISTANCE;

    public static final float PLACE_COOLDOWN = 6f;
    public static final int TASK_COOLDOWN = 20; // In ticks.
    public static final double TASK_COOLDOWN_IN_SEC = TASK_COOLDOWN / 20.0; // In seconds.

    public static final double ESTIMATED_BREAKING_TIME = (0.4 * 5.0 + 0.6) / 6.0; // In seconds per block. Calculation: (0.4 (Dirt mining speed) * 5 + 0.6 (Stone mining speed)) / 6
    public static final double ESTIMATED_PLACING_TIME = PLACE_COOLDOWN / 20.0; // In seconds per block. "#rightClickSpeed" taken from baritone, 4 ticks = 0.2.
    public static final double ESTIMATED_RUNNING_SPEED = Colonist.FAST_MOVEMENT_SPEED * 20.0; // In blocks per second.
}
