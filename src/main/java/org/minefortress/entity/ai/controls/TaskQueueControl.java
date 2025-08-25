package org.minefortress.entity.ai.controls;

import net.minecraft.util.math.BlockPos;
import net.remmintan.mods.minefortress.core.interfaces.entities.pawns.controls.IAreaBasedTaskControl;
import net.remmintan.mods.minefortress.core.interfaces.entities.pawns.controls.ITaskControl;
import net.remmintan.mods.minefortress.core.interfaces.entities.pawns.controls.ITaskQueueControl;
import net.remmintan.mods.minefortress.core.interfaces.tasks.IAreaBasedTask;
import net.remmintan.mods.minefortress.core.interfaces.tasks.IBaseTask;
import net.remmintan.mods.minefortress.core.interfaces.tasks.ITask;
import org.minefortress.MineFortressConstants;
import org.minefortress.entity.Colonist;

import java.util.*;

public class TaskQueueControl implements ITaskQueueControl {
    private final Deque<IBaseTask> tasks = new ArrayDeque<>();
    private final Set<IBaseTask> uniqueTasks = new HashSet<>();
    private IBaseTask currentTask = null;

    private final ITaskControl taskControl;
    private final IAreaBasedTaskControl areaBasedTaskControl;

    private final Colonist worker;
    private double requiredTime = 0;

    private int cooldown = 0;
    private boolean doingEverydayTasks = false;

    public TaskQueueControl(Colonist worker, ITaskControl boundTaskControl, IAreaBasedTaskControl boundAreaBasedTaskControl) {
        this.worker = worker;
        taskControl = boundTaskControl;
        areaBasedTaskControl = boundAreaBasedTaskControl;
    }

    @Override
    public void addTask(IBaseTask task) {
        if(isOnTask(task)) return;
        task.addWorker();
        var fromPos = tasks.isEmpty() ? worker.getPos() : tasks.getLast().getPos().toCenterPos();
        var distanceTravelled = task.getPos().getSquaredDistance(fromPos);
        distanceTravelled = Math.sqrt(distanceTravelled);
        tasks.add(task);
        uniqueTasks.add(task);
        requiredTime += distanceTravelled / MineFortressConstants.ESTIMATED_RUNNING_SPEED + MineFortressConstants.TASK_COOLDOWN_IN_SEC;
    }

    @Override
    public boolean hasTasks() {
        return currentTask != null || !tasks.isEmpty() || isDoingEverydayTasks();
    }

    @Override
    public boolean isOnTask(IBaseTask task) {
        return uniqueTasks.contains(task);
    }

    @Override
    public void cancelCurrent() {
        if(currentTask != null) {
            if (currentTask instanceof ITask) {
                taskControl.fail();
            } else if (currentTask instanceof IAreaBasedTask) {
                areaBasedTaskControl.reset();
            } else {
                throw new IllegalStateException("Wrong task class");
            }
            currentTask = null;
        }
    }
    private boolean isCurrentTaskFinalized() {
        return currentTask != null && !taskControl.hasTask() && !areaBasedTaskControl.hasTask();
    }
    private void removeCurrentTask() {
        uniqueTasks.remove(currentTask);
        currentTask = null;
        cooldown = MineFortressConstants.TASK_COOLDOWN;
        if(tasks.isEmpty())
            requiredTime = 0;
    }
    @Override
    public void tick() {
        if (cooldown > 0) cooldown--;
        if(requiredTime != 0)
            requiredTime = Math.max(0, requiredTime - 0.05);

        if(isCurrentTaskFinalized()) {
            // Due to the way tasks choose amount of workers, even if this pawn finished
            // there still may be place for another pawn. So before finishing task
            // we check if there still actually no work to do.
            if (currentTask instanceof ITask ct) {
                if (ct.hasAvailableParts()) {
                    ct.addWorker();
                    taskControl.setTask(ct);
                }
                else removeCurrentTask();
            } else if (currentTask instanceof IAreaBasedTask abt) {
                if (abt.hasMoreBlocks()) {
                    abt.addWorker();
                    areaBasedTaskControl.setTask(abt);
                }
                else removeCurrentTask();
            } else {
                throw new IllegalStateException("Wrong task class");
            }
        }

        if(currentTask == null && cooldown == 0) {
            this.currentTask = tasks.poll();
            if(currentTask == null) {
                return;
            }
            if (currentTask instanceof ITask ct) {
                if (ct.hasAvailableParts())
                    taskControl.setTask(ct);
                else
                    currentTask = null;
            } else if (currentTask instanceof IAreaBasedTask abt) {
                if (abt.hasMoreBlocks())
                    areaBasedTaskControl.setTask(abt);
                else
                    currentTask = null;
            } else {
                throw new IllegalStateException("Wrong task class");
            }
        }
    }

    @Override
    public boolean currentTaskIsOfType(Class<? extends IBaseTask> taskClass) { return taskClass.isInstance(currentTask); }

    @Override
    public void setDoingEverydayTasks(boolean doingEverydayTasks) {
        this.doingEverydayTasks = doingEverydayTasks;
    }

    @Override
    public boolean isDoingEverydayTasks() {
        return doingEverydayTasks;
    }

    @Override
    public double estimateTimeRequired() {
        // It would be much more accurate to recompute that each time
        // since a lot of things can change: workers die, distance travelled faster.
        // However, doing so will likely incur high compute costs.
        // Maybe do that as a separate method used only for display purposes?
        return requiredTime;
    }

    @Override
    public BlockPos getLastPos() {
        if(!tasks.isEmpty()) {
            return tasks.getLast().getPos();
        } else if(currentTask != null) {
            return currentTask.getPos();
        } else {
            return worker.getBlockPos();
        }
    }
}