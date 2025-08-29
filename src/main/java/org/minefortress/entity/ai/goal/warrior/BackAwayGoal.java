package org.minefortress.entity.ai.goal.warrior;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.minefortress.entity.BasePawnEntity;
import java.util.EnumSet;

public class BackAwayGoal extends AttackGoal {
    private final double keptSquaredDistance;

    public BackAwayGoal(BasePawnEntity pawn, double keptDistance) {
        super(pawn);
        this.keptSquaredDistance = keptDistance * keptDistance;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    private double getSquaredDistance() {
        return getTarget().map(target -> {
            Vec3d targetPosition = target.getPos();
            // If you have the high ground...
            if(pawn.getPos().y > targetPosition.y)
                // ...you better not to lose it.
                // This will make archers be less inclined to back away,
                // if the mob comes from downward direction.
                // So archers will not jump off from towers and walls.
                targetPosition = new Vec3d(targetPosition.x, targetPosition.y * 4, targetPosition.z);
            return pawn.getPos().squaredDistanceTo(targetPosition);
        }).orElse(Double.POSITIVE_INFINITY);
    }

    @Override
    public boolean canStart() {
        return super.canStart() && getSquaredDistance() < keptSquaredDistance;
    }

    // Check if the pawn would fall off at the given position
    private boolean isLocationFallSafe(Vec3d position) {
        World world = pawn.getWorld();
        BlockPos blockPos = new BlockPos((int)position.x, (int)position.y, (int)position.z);

        // Check if there's a solid block beneath the pawn
        BlockPos beneathPos = blockPos.down();
        BlockState beneathState = world.getBlockState(beneathPos);

        // If there's no solid block beneath, the pawn would fall
        return beneathState.isSolidBlock(world, beneathPos);
    }

    // Find a safe direction to move
    private Vec3d findSafeDirection(Vec3d away) {
        // Try the original direction first
        Vec3d newPos = pawn.getPos().add(away.normalize().multiply(pawn.getMovementSpeed()));
        if (isLocationFallSafe(newPos)) {
            return away;
        }

        // Try moving to the right
        Vec3d right = new Vec3d(-away.z, 0, away.x).normalize();
        newPos = pawn.getPos().add(right.multiply(pawn.getMovementSpeed()));
        if (isLocationFallSafe(newPos)) {
            return right;
        }

        // Try moving to the left
        Vec3d left = new Vec3d(away.z, 0, -away.x).normalize();
        newPos = pawn.getPos().add(left.multiply(pawn.getMovementSpeed()));
        if (isLocationFallSafe(newPos)) {
            return left;
        }

        // If all directions would lead to falling, don't move
        return Vec3d.ZERO;
    }

    @Override
    public void tick() {
        getTarget().ifPresent(target -> {
            var away = pawn.getPos().subtract(target.getPos());
            // What do you mean archers shouldn't escape from targets into the sky?
            away = new Vec3d(away.x, 0.0, away.z);
            away = away.normalize();

            // Find a safe direction to move
            Vec3d safeDirection = findSafeDirection(away);
            safeDirection = safeDirection.multiply(pawn.getMovementSpeed());

            pawn.setVelocity(safeDirection);
        });
    }
}
