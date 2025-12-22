package org.minefortress.entity.ai.goal.warrior;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.minefortress.entity.BasePawnEntity;

import java.util.EnumSet;
import java.util.Optional;

public class BackAwayGoal extends AttackGoal {

    private final double keptSquaredDistance;

    private Optional<Vec3d> pendingJumpVector = Optional.empty();

    private int climbIntentTicks = 0;
    private static final int CLIMB_INTENT_DURATION = 5;

    private static final double MAX_SAFE_FALL = 2.5;
    private static final double DESPERATE_MAX_FALL = 12.0;
    private static final double LOW_HEALTH_THRESHOLD = 0.3;

    public BackAwayGoal(BasePawnEntity pawn, double keptDistance) {
        super(pawn);
        this.keptSquaredDistance = keptDistance * keptDistance;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    private Vec3d getFeetPos() {
        return pawn.getPos().subtract(0.0, pawn.getHeight() * 0.5, 0.0);
    }

    private BlockPos getFeetBlockPos(Vec3d pos) {
        Vec3d feet = pos.subtract(0.0, pawn.getHeight() * 0.5, 0.0);
        return new BlockPos(
                MathHelper.floor(feet.x),
                MathHelper.floor(feet.y),
                MathHelper.floor(feet.z)
        );
    }

    private double getSquaredDistance() {
        return getTarget().map(target -> {
            Vec3d delta = pawn.getPos().subtract(target.getPos());

            // If pawn is above target, vertical difference feels less threatening
            double verticalWeight = delta.y > 0 ? 0.5 : 1.0;
            double dy = delta.y * verticalWeight;

            return delta.x * delta.x + dy * dy + delta.z * delta.z;
        }).orElse(Double.POSITIVE_INFINITY);
    }

    private boolean isHealthLow() {
        return pawn.getHealth() < pawn.getMaxHealth() * LOW_HEALTH_THRESHOLD;
    }

    private boolean isLocationFallSafe(Vec3d pos) {
        return getFallDistance(pos) <= MAX_SAFE_FALL;
    }

    private boolean canDesperatelyFall(Vec3d pos) {
        if (!isHealthLow()) return false;
        return getFallDistance(pos) <= DESPERATE_MAX_FALL;
    }

    private double getFallDistance(Vec3d pos) {
        World world = pawn.getWorld();
        BlockPos.Mutable checkPos = getFeetBlockPos(pos).mutableCopy();

        for (int i = 0; i <= Math.ceil(DESPERATE_MAX_FALL) + 1; i++) {
            BlockState state = world.getBlockState(checkPos);
            if (state.isSolidBlock(world, checkPos)) {
                return i - 1;
            }
            checkPos.move(Direction.DOWN);
        }

        return Double.POSITIVE_INFINITY;
    }

    private Optional<Vec3d> checkForClimb(Vec3d awayDir) {
        World world = pawn.getWorld();
        Vec3d dir = awayDir.normalize();
        Vec3d feet = getFeetPos();

        Vec3d frontFeet = feet.add(dir.multiply(0.5));
        BlockPos frontBlock = new BlockPos(
                MathHelper.floor(frontFeet.x),
                MathHelper.floor(frontFeet.y),
                MathHelper.floor(frontFeet.z)
        );

        BlockPos stepBlock = frontBlock.up();

        if (!world.getBlockState(stepBlock).isSolidBlock(world, stepBlock)) {
            return Optional.empty();
        }

        int clearance = MathHelper.ceil(pawn.getHeight());
        for (int y = 1; y <= clearance; y++) {
            if (!world.getBlockState(stepBlock.up(y)).isAir()) {
                return Optional.empty();
            }
        }

        Vec3d targetFeet = feet.add(dir.multiply(0.5)).add(0, 1.0, 0);
        if (!world.isSpaceEmpty(
                pawn.getBoundingBox().offset(targetFeet.subtract(feet))
        )) {
            return Optional.empty();
        }

        return Optional.of(dir.add(0.0, 0.42, 0.0));
    }

    private Optional<Vec3d> tryClimbNow(Vec3d away) {
        if (!pawn.isOnGround()) return Optional.empty();

        Vec3d nextPos = pawn.getPos()
                .add(away.normalize().multiply(pawn.getMovementSpeed()));

        boolean blocked = !pawn.getWorld().isSpaceEmpty(
                pawn.getBoundingBox().offset(nextPos.subtract(pawn.getPos()))
        );

        if (blocked) {
            return checkForClimb(away);
        }

        return Optional.empty();
    }

    private Vec3d findSafeDirection(Vec3d away) {
        Vec3d base = away.normalize();

        Vec3d[] options = new Vec3d[]{
                base,
                new Vec3d(-base.z, 0, base.x).normalize(),
                new Vec3d(base.z, 0, -base.x).normalize()
        };

        for (Vec3d dir : options) {
            Vec3d test = pawn.getPos()
                    .add(dir.multiply(pawn.getMovementSpeed()));

            if (isLocationFallSafe(test) || canDesperatelyFall(test)) {
                return dir;
            }
        }

        return Vec3d.ZERO;
    }

    @Override
    public boolean canStart() {
        return super.canStart() && getSquaredDistance() < keptSquaredDistance;
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && getSquaredDistance() < keptSquaredDistance * 1.5;
    }

    @Override
    public void tick() {
        getTarget().ifPresent(target -> {

            if (climbIntentTicks > 0) {
                climbIntentTicks--;
            }

            Vec3d away = pawn.getPos().subtract(target.getPos());
            away = new Vec3d(away.x, 0.0, away.z);

            if (away.lengthSquared() < 0.01) return;

            away = away.normalize();

            Optional<Vec3d> climb = tryClimbNow(away);
            if (climb.isPresent()) {
                pendingJumpVector = climb;
                climbIntentTicks = CLIMB_INTENT_DURATION;
            }

            Vec3d moveDir;
            boolean shouldJump = false;

            if (pendingJumpVector.isPresent()) {
                Vec3d jump = pendingJumpVector.get();
                shouldJump = jump.y > 0;

                moveDir = new Vec3d(jump.x, 0, jump.z).normalize();
                if (climbIntentTicks > 0) {
                    moveDir = moveDir.multiply(1.1);
                }

                if (climbIntentTicks <= 0) {
                    pendingJumpVector = Optional.empty();
                }
            } else {
                moveDir = findSafeDirection(away);
            }

            if (moveDir.lengthSquared() < 0.01) return;

            Vec3d velocity = pawn.getVelocity();
            Vec3d horizontal = moveDir.multiply(pawn.getMovementSpeed());

            if (isHealthLow()) {
                horizontal = horizontal.multiply(1.15);
            }

            pawn.setVelocity(
                    horizontal.x,
                    velocity.y,
                    horizontal.z
            );

            if (shouldJump && pawn.isOnGround()) {
                pawn.performJump();
            }
        });
    }
}
