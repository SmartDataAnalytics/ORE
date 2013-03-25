package org.aksw.mole.ore.task;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.vaadin.Application;

public abstract class BackgroundTask implements Runnable {

	public static interface ProgressListener {

		/**
		 * This method gets called when the progress-attribute gets updated. The
		 * call happens with the lock of the Application-instance held, so it is
		 * absolutely legal to update the UI from here. Note that any
		 * ProgressListener must return as quick as possible in order to prevent
		 * the UI from freezing.
		 * 
		 * @param progress
		 *            a value between 0 and 100, representing the made progress
		 *            in percent.
		 * @param state
		 *            a description for what is currently getting done.
		 * @param task
		 *            the progressed Backgroundtask-instance.
		 */
		public void workProgressed(int progress, String state, BackgroundTask task);

	}

	public final static int INDETERMINATE = -1;

	public final static int MAX = 100;

	private final Set<ProgressListener> progressListeners;

	private volatile boolean canceled = false;

	private volatile boolean finished = false;

	private boolean indeterminate;

	private boolean cancelable;

	private String state;

	private String taskName;

	private final Application app;

	public BackgroundTask(Application app, String taskName, ProgressListener... listeners) {
		this(app, taskName, Arrays.asList(listeners));
	}

	public BackgroundTask(Application app, String taskName, Collection<ProgressListener> listeners) {
		this.app = app;
		this.taskName = taskName;
		this.cancelable = true;
		this.indeterminate = false;
		this.state = "";

		if (listeners != null && !listeners.isEmpty()) {
			this.progressListeners = new CopyOnWriteArraySet<ProgressListener>(listeners);
		} else {
			this.progressListeners = new CopyOnWriteArraySet<ProgressListener>();
		}

	}

	/**
	 * Adds a ProgressListener. Nothing happens if the listener is
	 * <code>null</code> or is already registered.
	 */
	public void addListener(ProgressListener l) {
		if (l != null) {
			this.progressListeners.add(l);
		}
	}

	/**
	 * Removes a ProgressListener. Nothing happens if the listener is
	 * <code>null</code> or is not known.
	 */
	public void removeListener(ProgressListener l) {
		if (l != null) {
			this.progressListeners.remove(l);
		}
	}

	/**
	 * Controls whether the task can be canceled by the user or not.
	 */
	protected final void setCancelable(boolean cancelable) {
		this.cancelable = cancelable;
	}

	/**
	 * Returns true if the user can cancel this task manually.
	 */
	public final boolean isCancelable() {
		return this.cancelable;
	}

	/**
	 * Controls whether you can tell how long the task will run or not.
	 */
	protected final void setIndeterminate(boolean indeterminate) {
		this.indeterminate = indeterminate;
	}

	/**
	 * Returns true if you cannot tell in advance how long the task will
	 * probably take to execute.
	 */
	public final boolean isIndeterminate() {
		return this.indeterminate;
	}

	/**
	 * Requests Cancellation of this VaadinWorker. All subclasses need to check
	 * for cancel-requests on a regular basis for this to have any effect.
	 * 
	 * @throws IllegalStateException
	 *             if the task is not cancelable.
	 */
	protected void cancel() throws IllegalStateException {
		if (!isCancelable()) {
			throw new IllegalStateException("Cannot cancel this task!");
		}
		canceled = true;
	}

	/**
	 * Returns true if cancellation of this VaadinWorker has been requested.
	 * This must not mean that the work has effectively stopped already. Use
	 * <code>isFinished()</code> for checking that.
	 */
	public final boolean isCanceled() {
		return canceled;
	}

	/**
	 * Returns true if the work has effectively stopped, either due to a cancel-
	 * request by the user or because there is no more work to do.
	 */
	public final boolean isFinished() {
		return finished;
	}

	public final void run() {
		try {
			synchronized (app) {
				updateUIBefore();
			}

			runInBackground();

			synchronized (app) {
				updateUIAfter();
			}
			finished = true;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * If you need to do any UI-initialization-work prior to starting the
	 * long-running task, you can do it here. The method will be called inside a
	 * synchronized block.
	 */
	public abstract void updateUIBefore();

	/**
	 * Do your long-running tasks that should run in a background-thread here.
	 */
	public abstract void runInBackground();

	/**
	 * Publish your result to the UI using this method. It will be called after
	 * <code>doInBackground()</code> finishes.
	 */
	public abstract void updateUIAfter();

	/**
	 * Call this method to communicate your progress. It informs all registered
	 * <code>ProgressListener<//code>s and lets them do their work guarded by
	 * the <code>Application</code>
	 * -lock. Note that any task done by a <code>ProgressListener</code> must
	 * finish quickly in order to not freeze the UI.
	 * 
	 * @param progress
	 *            a value between 0 and 100, mapping to 0% to 100%. If you do
	 *            not know how long the task will take, an negative value should
	 *            be overgiven. The param will be ignored when
	 *            <code>isProgressIndeterminate()</code> returns true. In this
	 *            case always <code>INDETERMINATE</code> will be communicated to
	 *            the listeners.
	 */
	protected final void updateProgress(int progress) {
		updateProgress(progress, state);
	}

	protected final void updateProgress(int progress, String state) {
		if (isIndeterminate() || progress < 0) {
			progress = INDETERMINATE;
		} else if (progress > 100) {
			progress = MAX;
		}

		if (state == null) {
			state = "";
		}
		this.state = state;

		for (ProgressListener listener : progressListeners) {
			synchronized (app) {
				listener.workProgressed(progress, state, this);
			}
		}
	}

	public String getTaskName() {
		return taskName;
	}
}
