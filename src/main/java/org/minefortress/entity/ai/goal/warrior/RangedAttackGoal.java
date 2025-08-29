package org.minefortress.entity.ai.goal.warrior;

import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.math.Vec3d;
import org.minefortress.entity.BasePawnEntity;

public class RangedAttackGoal extends AttackGoal {
    private int targetSeeingTicker = 0;
    private int relaxCooldown;
    private int longShootingCooldown;
    private int shortShootingCooldown;
    private boolean isCurrentShotCritical;

    public RangedAttackGoal(BasePawnEntity pawn) {
        super(pawn);
    }

    private void updateCooldowns() {
        var random = pawn.getRandom();
        relaxCooldown = random.nextBetween(5, 11);
        longShootingCooldown = random.nextBetween(22, 35);
        shortShootingCooldown = random.nextBetween(10, 15);
        isCurrentShotCritical = random.nextFloat() > 0.6F;
    }

    @Override
    public void start() {
        super.start();
        pawn.putItemInHand(Items.BOW);
        updateCooldowns();
    }

    @Override
    public void tick() {
        this.getTarget().ifPresent(target -> {
            boolean visible = pawn.getVisibilityCache().canSee(target);
            boolean alreadySeeing = this.targetSeeingTicker > 0;
            if (visible != alreadySeeing) {
                this.targetSeeingTicker = 0;
            }

            pawn.getLookControl().lookAt(target);

            if (visible) {
                ++this.targetSeeingTicker;
            } else {
                --this.targetSeeingTicker;
            }

            Vec3d targetPosition = target.getPos();
            // If you have the high ground...
            if(pawn.getPos().y > targetPosition.y)
                // ...you better not to lose it.
                // This will make archers be less inclined to back away,
                // if the mob comes from downward direction.
                // So archers will not jump off from towers and walls.
                targetPosition = new Vec3d(targetPosition.x, targetPosition.y * 4, targetPosition.z);

            double distance = pawn.getPos().squaredDistanceTo(targetPosition);

            if (pawn.isUsingItem()) {
                if (!visible && this.targetSeeingTicker < -60) {
                    pawn.clearActiveItem();
                } else if (visible) {
                    int i = pawn.getItemUseTime();

                    boolean heDroppedHisShield = target.getOffHandStack().getItem() instanceof ShieldItem && !target.isBlocking();

                    boolean shortAttack = (distance < 5.0 * 5.0 || heDroppedHisShield) && i >= shortShootingCooldown;
                    boolean nonCriticalAttack = (!isCurrentShotCritical && i >= 20);
                    boolean criticalAttack = i >= longShootingCooldown;
                    boolean longAttack = (nonCriticalAttack || criticalAttack) && relaxCooldown < 1;
                    if (shortAttack || longAttack) {
                        pawn.clearActiveItem();
                        float progress = BowItem.getPullProgress(i);
                        // Use special value for critical hits.
                        // Sadly, this seems like an abuse of `RangedAttackMob` interface.
                        if(i >= 22)
                            progress = 1.1F;
                        ((RangedAttackMob)pawn).shootAt(target, progress);
                        updateCooldowns();
                    }
                }
            } else if (--this.relaxCooldown <= 0 && this.targetSeeingTicker >= -60) {
                pawn.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(pawn, Items.BOW));
            }

            if (distance < 8.0 * 8.0) {
                var away = pawn.getPos().subtract(target.getPos());
                // What do you mean archers shouldn't escape from targets into the sky?
                away = new Vec3d(away.x, 0.0, away.z);
                away = away.normalize().multiply(pawn.getMovementSpeed());
                pawn.addVelocity(away);
            }
        });
    }

    @Override
    public void stop() {
        super.stop();
        this.relaxCooldown = 0;
        this.targetSeeingTicker = 0;
        if(pawn.isItemInHand(Items.BOW)) {
            pawn.clearActiveItem();
        }
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && pawn.isItemInHand(Items.BOW);
    }
}
