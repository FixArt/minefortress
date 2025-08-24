package net.remmintan.mods.minefortress.core.interfaces.entities.pawns.controls;

import net.minecraft.util.math.BlockPos;
import net.remmintan.mods.minefortress.core.interfaces.tasks.IBaseTask;

public interface ITaskQueueControl {
    void addTask(IBaseTask task);
    boolean hasTasks();
    boolean isOnTask(IBaseTask task);

    void cancelCurrent();
    boolean currentTaskIsOfType(Class<? extends IBaseTask> taskClass);

    void setDoingEverydayTasks(boolean doingEverydayTasks);
    boolean isDoingEverydayTasks();

    double estimateTimeRequired();
    BlockPos getLastPos();

    void tick();
}