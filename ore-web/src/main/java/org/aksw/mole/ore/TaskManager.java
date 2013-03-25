package org.aksw.mole.ore;

import java.util.ArrayList;
import java.util.List;

import org.aksw.mole.ore.task.BackgroundTask;

public class TaskManager {
	
	public interface TaskManagerListener{
		void taskStarted(String taskName);
		void taskFinished(String taskName);
	}
	
	private List<TaskManagerListener> listeners;
	private List<BackgroundTask> tasks;
	private BackgroundTask currentExecutingTask;
	
	public TaskManager() {
		tasks = new ArrayList<BackgroundTask>();
	}
	
	public void execute(BackgroundTask task){
		currentExecutingTask = task;
//		currentExecutingTask.start();
	}
	
	public void addListener(TaskManagerListener l){
		listeners.add(l);
	}
	
	public void removeListener(TaskManagerListener l){
		listeners.remove(l);
	}
	
	private void fireTaskStarted(BackgroundTask task){
		for(TaskManagerListener l : listeners){
			l.taskStarted(task.getTaskName());
		}
	}
	
	private void fireTaskFinished(BackgroundTask task){
		for(TaskManagerListener l : listeners){
			l.taskFinished(task.getTaskName());
	}
	
	}

}
