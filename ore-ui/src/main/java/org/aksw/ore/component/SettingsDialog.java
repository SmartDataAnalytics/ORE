/**
 * 
 */
package org.aksw.ore.component;

import org.aksw.ore.ORESession;
import org.aksw.ore.rendering.EntityRenderingStyle;
import org.aksw.ore.rendering.Syntax;

import com.vaadin.data.Item;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;

/**
 * @author Lorenz Buehmann
 *
 */
public class SettingsDialog extends Window{
	
	public SettingsDialog() {
		super("Settings");
		setModal(true);
		setCloseShortcut(KeyCode.ESCAPE, null);
		setClosable(false);
		
		FormLayout form = new FormLayout();
		setContent(form);
		
		//entity rendering options
		final OptionGroup entityRenderingOptions = new OptionGroup();
		entityRenderingOptions.setItemCaptionMode(ItemCaptionMode.ID);
		for (EntityRenderingStyle style : EntityRenderingStyle.values()) {
			Item item = entityRenderingOptions.addItem(style);
			
		}
		entityRenderingOptions.setValue(EntityRenderingStyle.SHORT_FORM);
		Panel panel = new Panel("Entity rendering");
		panel.setSizeUndefined(); 
		form.addComponent(panel);
		panel.setContent(entityRenderingOptions);
		
		//axiom rendering options
		final OptionGroup axiomRenderingOptions = new OptionGroup();
		for (Syntax syntax : Syntax.values()) {
			Item item = axiomRenderingOptions.addItem(syntax);
		}
		axiomRenderingOptions.setValue(Syntax.MANCHESTER);

		panel = new Panel("Axiom rendering");
		panel.setSizeUndefined();
		form.addComponent(panel);
		panel.setContent(axiomRenderingOptions);
		
		//apply changes button
		Button applyButton = new Button("Apply", new ClickListener() {
			@Override
			public void buttonClick(ClickEvent event) {
				ORESession.getRenderer().setRenderingStyle((Syntax) axiomRenderingOptions.getValue(), (EntityRenderingStyle) entityRenderingOptions.getValue());
			}
		});
		applyButton.setDescription("Apply changes.");
		form.addComponent(applyButton);
	}

}
