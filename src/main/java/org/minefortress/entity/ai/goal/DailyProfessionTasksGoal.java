package org.minefortress.entity.ai.goal;

import net.remmintan.mods.minefortress.core.interfaces.entities.pawns.controls.ITaskQueueControl;
import org.minefortress.entity.Colonist;
import org.minefortress.entity.ai.professions.*;

import java.util.Map;

import static java.util.Map.entry;

public class DailyProfessionTasksGoal extends AbstractFortressGoal {

    private final Map<String, ProfessionDailyTask> dailyTasks = Map.ofEntries(
            entry("crafter", new CrafterDailyTask()),
            entry("blacksmith", new BlacksmithDailyTask()),
            entry("forester", new ForesterDailyTask()),
            entry("fisher", new FisherDailyTask()),
            entry("farmer", new FarmerDailyTask()),
            entry("miner", new MinerDailyTask()),
            entry("lumberjack", new LumberjackDailyTask())
    );

    private ProfessionDailyTask currentTask;

    public DailyProfessionTasksGoal(Colonist colonist) {
        super(colonist);
    }

    @Override
    public boolean canStart() {
        if (this.wantAndCanEatSomeFood()) return false;
        final ITaskQueueControl taskQueueControl = getTaskQueueControl();
        if(taskQueueControl.hasTasks()) return false;
        final String professionId = colonist.getProfessionId();

        for(String professionIdPart : dailyTasks.keySet()) {
            if(professionId.startsWith(professionIdPart)) {
                final ProfessionDailyTask task = dailyTasks.get(professionIdPart);
                if(task.canStart(colonist)) {
                    this.currentTask = task;
                    return true;
                }
            }
        }

        this.currentTask = dailyTasks.get(professionId);
        return currentTask != null && this.currentTask.canStart(colonist);
    }

    @Override
    public void start() {
        getTaskQueueControl().setDoingEverydayTasks(true);
        this.currentTask.start(colonist);
    }

    @Override
    public void tick() {
        this.currentTask.tick(colonist);
    }

    @Override
    public boolean shouldContinue() {
        return !wantAndCanEatSomeFood()
                && this.currentTask != null
                && this.currentTask.shouldContinue(colonist)
                && !getTaskQueueControl().hasTasks();
    }

    @Override
    public void stop() {
        this.currentTask.stop(colonist);
        getTaskQueueControl().setDoingEverydayTasks(false);
    }

    @Override
    public boolean canStop() {
        return this.wantAndCanEatSomeFood();
    }

    private ITaskQueueControl getTaskQueueControl() {
        return colonist.getTaskQueueControl();
    }
}