/**
 * 
 */
package org.aksw.ore.component;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.EntityRenderingStyle;
import org.aksw.ore.rendering.Syntax;

import com.vaadin.data.Item;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * @author Lorenz Buehmann
 *
 */
public class SettingsDialog extends Window{
	
	private boolean dirty = false;
	private OptionGroup entityRenderingOptions;
	private OptionGroup axiomRenderingOptions;
	
	public SettingsDialog() {
		super("Settings");
		setModal(true);
		setCloseShortcut(KeyCode.ESCAPE);
		
		VerticalLayout main = new VerticalLayout();
		main.setSizeUndefined();
		main.setMargin(true);
		main.setSpacing(true);
		
		setContent(main);
		
		TabSheet tabs = new TabSheet();
		tabs.setSizeUndefined();
		main.addComponent(tabs);
		main.setExpandRatio(tabs, 1f);
		
		tabs.addTab(createRenderingTab(), "Rendering");
		
		main.addComponent(new Label("<hr/>", ContentMode.HTML));
		
		//apply changes button
		Button applyButton = new Button("Apply", new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				ORESession.getRenderer().setRenderingStyle((Syntax) axiomRenderingOptions.getValue(), (EntityRenderingStyle) entityRenderingOptions.getValue());
				close();
			}
		});
		applyButton.setDescription("Apply changes.");
		applyButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
		main.addComponent(applyButton);
		main.setComponentAlignment(applyButton, Alignment.BOTTOM_RIGHT);
	}
	
	private Component createRenderingTab(){
		FormLayout form = new FormLayout();
		form.setSizeUndefined();
		
		entityRenderingOptions = new OptionGroup();
		entityRenderingOptions.setItemCaptionMode(ItemCaptionMode.ID);
		for (EntityRenderingStyle style : EntityRenderingStyle.values()) {
			Item item = entityRenderingOptions.addItem(style);
			
		}
		entityRenderingOptions.setValue(EntityRenderingStyle.SHORT_FORM);
		CssLayout panel = new CssLayout(entityRenderingOptions);
		panel.setCaption("Entity Rendering");
//		Panel panel = new Panel("Entity rendering", entityRenderingOptions);
		panel.setSizeUndefined();
		form.addComponent(panel);
		
		axiomRenderingOptions = new OptionGroup();
		for (Syntax syntax : Syntax.values()) {
			Item item = axiomRenderingOptions.addItem(syntax);
		}
		axiomRenderingOptions.setValue(Syntax.MANCHESTER);

		panel = new CssLayout(axiomRenderingOptions);
		panel.setCaption("Axiom Rendering");
//		panel = new Panel("Axiom rendering", axiomRenderingOptions);
		panel.setSizeUndefined();
		form.addComponent(panel);
		
		return form;
	}

}
