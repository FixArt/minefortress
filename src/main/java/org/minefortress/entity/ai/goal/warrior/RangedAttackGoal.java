package org.minefortress.entity.ai.goal.warrior;

import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.Items;
import org.minefortress.entity.BasePawnEntity;

public class RangedAttackGoal extends AttackGoal {

    private static final int INTERVAL = 20;

    private int targetSeeingTicker = 0;
    private int cooldown = 0;
    private int plannedTimeToShoot;

    public RangedAttackGoal(BasePawnEntity pawn) {
        super(pawn);
    }

    @Override
    public void start() {
        super.start();
        pawn.putItemInHand(Items.BOW);
        plannedTimeToShoot = pawn.getRandom().nextBetween(20, 35);
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

            if (pawn.isUsingItem()) {
                if (!visible && this.targetSeeingTicker < -60) {
                    pawn.clearActiveItem();
                } else if (visible) {
                    int i = pawn.getItemUseTime();
                    if (i >= plannedTimeToShoot) {
                        pawn.clearActiveItem();
                        float progress = BowItem.getPullProgress(i);
                        // Use special value for critical hits.
                        // Sadly, this seems like an abuse of `RangedAttackMob` interface.
                        if(i >= 22)
                            progress = 1.1F;
                        ((RangedAttackMob)pawn).shootAt(target, progress);
                        this.cooldown = INTERVAL;
                        plannedTimeToShoot = pawn.getRandom().nextBetween(20, 35);
                    }
                }
            } else if (--this.cooldown <= 0 && this.targetSeeingTicker >= -60) {
                pawn.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(pawn, Items.BOW));
            }
        });
    }

    @Override
    public void stop() {
        super.stop();
        this.targetSeeingTicker = 0;
        this.cooldown = 0;
        if(pawn.isItemInHand(Items.BOW)) {
            pawn.clearActiveItem();
        }
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue() && pawn.isItemInHand(Items.BOW);
    }
}
