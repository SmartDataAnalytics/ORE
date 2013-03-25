package org.aksw.mole.ore;

import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.aksw.mole.ore.model.Knowledgebase;
import org.aksw.mole.ore.model.UserAction;
import org.aksw.mole.ore.model.UserAction.Task;

public class HistoryManager {
	
	private SortedSet<UserAction> history;
	
	private Stack<UserAction> undoStack;
	private Stack<UserAction> redoStack;
	
	public HistoryManager(Knowledgebase kb) {
		history = new TreeSet<UserAction>();
		undoStack = new Stack<UserAction>();
		redoStack = new Stack<UserAction>();
	}
	
	public void clearHistory(){
		history.clear();
		undoStack.clear();
		redoStack.clear();
	}
	
	public UserAction getLastUserAction(){
		return history.first();
	}
	
	public SortedSet<UserAction> getUserActionsForTask(Task task){
		SortedSet<UserAction> actions = new TreeSet<UserAction>();
		
		for(UserAction action : history){
			if(action.getTask() == task){
				actions.add(action);
			}
		}
		
		return actions;
	}
	
	public void undo(){
		if(canUndo()){
			UserAction a = undoStack.pop();
			UserAction revertedAction = a.revert();
			history.add(revertedAction);
			redoStack.push(a);
		}
	}
	
	public void redo(){
		if(canRedo()){
			UserAction a = redoStack.pop();
			
			undoStack.push(a);
		}
	}
	
	public boolean canUndo(){
		return undoStack.size() > 0;
	}
	
	public boolean canRedo(){
		return redoStack.size() > 0;
	}
	
	public void addUserAction(UserAction action){
		history.add(action);
		undoStack.push(action);
	}
	
	public void removeUserAction(UserAction action){
		history.remove(action);
	}
	
}
