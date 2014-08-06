/**
 * 
 */
package org.aksw.ore.component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

/**
 * @author Lorenz Buehmann
 *
 */
public class Dialog extends Window {
	public static final String MODAL = "modal";
	
	public static final int OK = -1;
	public static final int CANCEL = -2;
	public static final int CLOSE = 0;
	
	protected final CssLayout innerComponent;
	protected final HorizontalLayout bpComponent;
	protected final List<Action> items;
	protected DialogClickListener clickListener = null;
	protected boolean closed = false;

	public Dialog(String caption, Component content, Action[] items) {
		super(caption);

		super.setStyleName("dialog-window");
		setResizable(false);

		this.innerComponent = new CssLayout();
		this.innerComponent.addStyleName("inner");

		this.items = Arrays.asList(items);

		this.bpComponent = new HorizontalLayout() {
		};
		for (Action item : items) {
			addAction(item);
		}

		VerticalLayout l = new VerticalLayout();
		setContent(l);
		l.addComponent(innerComponent);
		addCloseListener(new Window.CloseListener() {
			public void windowClose(Window.CloseEvent e) {
				if (Dialog.this.closed == true) {
					return;
				}
				Dialog.this.doAction(e, 0);
			}
		});
		if (content != null)
			getInnerContent().addComponent(content);
		
		l.addComponent(bpComponent);
	}

	public Dialog(String caption, Action[] items) {
		this(caption, null, items);
	}

	public Dialog(String caption) {
		this(caption, null, new Action[0]);
	}

	public Dialog(String caption, Component content) {
		this(caption, content, new Action[0]);
	}

	public void setStyleName(String style) {
		super.addStyleName(style);
	}

	public void show() {
		this.closed = false;
		if (isAttached()) {
			bringToFront();
			return;
		}
		UI.getCurrent().addWindow(this);
	}

	public Dialog addStyle(String name) {
		super.addStyleName(name);
		return this;
	}

	public DialogClickListener getDialogClickListener() {
		return this.clickListener;
	}

	public void setDialogClickListener(DialogClickListener listener) {
		this.clickListener = listener;
	}

	public final AbstractLayout getInnerContent() {
		return this.innerComponent;
	}

	public Dialog noIcon() {
		removeStyleName("modal");
		return this;
	}

	public Button getButton(int action) {
		try {
			return getAction(action).button;
		} catch (Exception x) {
		}
		return null;
	}

	public Action getAction(int action) {
		for (Action a : this.items) {
			if (a.action == action) {
				return a;
			}
		}

		return null;
	}

	public Button addAction(Action item) {
		return addAction(item, -1);
	}

	public Button addAction(Action item, int index) {
		item.getButton().addClickListener(new Button.ClickListener() {
			public void buttonClick(Button.ClickEvent event) {
				for (Dialog.Action item : Dialog.this.items)
					if (item.button.equals(event.getButton())) {
						Dialog.this.doAction(event, item.action);
						return;
					}
			}
		});
		if (index >= 0)
			this.bpComponent.addComponent(item.button, index);
		else {
			this.bpComponent.addComponent(item.button);
		}
		return item.button;
	}

	public void removeAction(int action) {
		for (Action a : this.items) {
			if (a.action == action) {
				this.bpComponent.removeComponent(a.button);
				return;
			}
		}
	}

	protected void doAction(Component.Event event, int action) {
		if (this.clickListener == null) {
			this.closed = true;
			UI.getCurrent().removeWindow(this);
			return;
		}

		if (this.clickListener.buttonClick(event, action)) {
			this.closed = true;
			UI.getCurrent().removeWindow(this);
		}
	}
	
	public static Dialog createOkCancel(String title, Component content, DialogClickListener listener) {
		Dialog w = new Dialog(title, content, new Action[] { new Action("ok", -1), new Action("cancel", -2) }) {
		};
		w.setDialogClickListener(listener);

		return w;
	}

	public static Dialog createOkCancel(String title, DialogClickListener listener) {
		Dialog w = new Dialog(title, new Action[] { new Action("ok", -1), new Action("cancel", -2) }) {
		};
		w.setDialogClickListener(listener);

		return w;
	}

	public static Dialog createYesNo(String title, DialogClickListener listener) {
		Dialog w = new Dialog(title, new Action[] { new Action("yes", -1), new Action("no", -2) }) {
		};
		w.setDialogClickListener(listener);

		return w;
	}

	public static Dialog createSaveCancel(String title, DialogClickListener listener) {
		Dialog w = new Dialog(title, new Action[] { new Action("save", -1), new Action("cancel", -2) }) {
		};
		w.setDialogClickListener(listener);

		return w;
	}

	public static Dialog createMessage(String title, final String message) {
		Dialog w = new Dialog(title, new Action[] { new Action("ok", -1) }) {
		};
		return w;
	}

	public static Dialog createWarning(String title, final String message) {
		Dialog w = new Dialog(title, new Action[] { new Action("ok", -1) }) {
		};
		return w;
	}

	public static Dialog createBan(String title, final String message) {
		Dialog w = new Dialog(title, new Action[] { new Action("ok", -1) }) {
		};
		return w;
	}

	public static class Action implements Serializable {
		int action;
		final Button button;

		public Action(final String style, String name, int action) {
			this.action = action;

			this.button = new Button(name) {
			};
		}

		public Action(String i18nkey, int action) {
			this.action = action;
			this.button = new Button(i18nkey);
		}

		public Action(Button button, int action) {
			this.action = action;
			this.button = button;
		}

		public String getLabel() {
			return this.button.getCaption();
		}

		public void setLabel(String name) {
			this.button.setCaption(name);
		}

		public int getAction() {
			return this.action;
		}

		public void setAction(int action) {
			this.action = action;
		}

		public Button getButton() {
			return this.button;
		}

		public int hashCode() {
			int hash = 5;
			hash = 83 * hash + this.action;
			return hash;
		}

		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Action other = (Action) obj;
			return this.action == other.action;
		}

		public String toString() {
			return String.valueOf(this.action);
		}
	}

	public static abstract interface DialogClickListener {
		public abstract boolean buttonClick(Component.Event paramEvent, int paramInt);
	}
}
