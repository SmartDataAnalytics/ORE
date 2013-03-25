package org.aksw.mole.ore;

public class BackgroundTask extends Thread{

	private String taskName;
	
	public BackgroundTask(Runnable runnable, String taskName) {
		super(runnable);
		this.taskName = taskName;
	}
	
	public String getTaskName() {
		return taskName;
	}
}
