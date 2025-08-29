package org.minefortress.entity.ai.goal.warrior;

import net.minecraft.util.math.Vec3d;
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

    @Override
    public void tick() {
        getTarget().ifPresent(target -> {
            var away = pawn.getPos().subtract(target.getPos());
            // What do you mean archers shouldn't escape from targets into the sky?
            away = new Vec3d(away.x, 0.0, away.z);
            away = away.normalize().multiply(pawn.getMovementSpeed());
            pawn.addVelocity(away);
        });
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && getSquaredDistance() < keptSquaredDistance;
    }
}
