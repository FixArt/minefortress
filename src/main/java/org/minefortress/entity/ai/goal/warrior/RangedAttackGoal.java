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
    private int longShootingCooldown;
    private int shortShootingCooldown;

    public RangedAttackGoal(BasePawnEntity pawn) {
        super(pawn);
    }

    @Override
    public void start() {
        super.start();
        pawn.putItemInHand(Items.BOW);
        longShootingCooldown = pawn.getRandom().nextBetween(20, 35);
        shortShootingCooldown = pawn.getRandom().nextBetween(10, 15);
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

            double distance = pawn.getPos().squaredDistanceTo(target.getPos());

            if (pawn.isUsingItem()) {
                if (!visible && this.targetSeeingTicker < -60) {
                    pawn.clearActiveItem();
                } else if (visible) {
                    int i = pawn.getItemUseTime();

                    boolean heDroppedHisShield = target.getOffHandStack().getItem() instanceof ShieldItem && !target.isBlocking();

                    boolean shortAttack = (distance < 5.0 * 5.0 || heDroppedHisShield) && i >= shortShootingCooldown;
                    boolean longAttack = i >= longShootingCooldown;
                    if (shortAttack || longAttack) {
                        pawn.clearActiveItem();
                        float progress = BowItem.getPullProgress(i);
                        // Use special value for critical hits.
                        // Sadly, this seems like an abuse of `RangedAttackMob` interface.
                        if(i >= 22)
                            progress = 1.1F;
                        ((RangedAttackMob)pawn).shootAt(target, progress);
                        longShootingCooldown = pawn.getRandom().nextBetween(20, 35);
                        shortShootingCooldown = pawn.getRandom().nextBetween(10, 15);
                    }
                }
            } else if (this.targetSeeingTicker >= -60) {
                pawn.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(pawn, Items.BOW));
            }

            if (distance < 5.0 * 5.0) {
                var away = pawn.getPos().subtract(target.getPos());
                // What do you mean archers shouldn't escape from targets into the sky?
                away = new Vec3d(away.x, 0.0, away.z);
                away = away.normalize().multiply(pawn.getMovementSpeed());
                pawn.setVelocity(away);
            }
        });
    }

    @Override
    public void stop() {
        super.stop();
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
