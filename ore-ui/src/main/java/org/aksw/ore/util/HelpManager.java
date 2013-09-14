package org.aksw.ore.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.aksw.ore.component.HelpOverlay;
import org.aksw.ore.view.NamingView;

import com.vaadin.navigator.View;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

public class HelpManager {

    private UI ui;
    private List<HelpOverlay> overlays = new ArrayList<HelpOverlay>();
    private String helpText;
    
    private Set<View> disabledHelp = new HashSet<View>();

    public HelpManager(UI ui) {
        this.ui = ui;
        loadHelp();
    }
    
    private void loadHelp(){
    	StringBuilder text = new StringBuilder();
        Scanner scanner = new Scanner(this.getClass().getClassLoader().getResourceAsStream("help-naming-view.txt"), "UTF-8");
        try {
          while (scanner.hasNextLine()){
            text.append(scanner.nextLine() + " ");
          }
        }
        finally{
          scanner.close();
        }
        helpText = text.toString();
    }

    public void closeAll() {
        for (HelpOverlay overlay : overlays) {
            overlay.close();
        }
        overlays.clear();
    }

    public void showHelpFor(View view) {
         showHelpFor(view.getClass());
    }

    public void showHelpFor(Class<? extends View> view) {
         if (view == NamingView.class) {
        	 addOverlay("Naming Issue Detection", helpText, "table-rows");
         }
    }
    
    public void setHelpEnabledFor(View view){
    	disabledHelp.remove(view);
    }
    
    public void setHelpDisabledFor(View view){
    	disabledHelp.add(view);
    }
    
    public boolean isHelpEnabledFor(View view){
    	return !disabledHelp.contains(view);
    }

    protected HelpOverlay addOverlay(String caption, String text, String style) {
        HelpOverlay o = new HelpOverlay();
        o.setCaption(caption);
        o.addComponent(new Label(text, ContentMode.HTML));
        o.setStyleName(style);
        ui.addWindow(o);
        overlays.add(o);
        o.center();
        return o;
    }

}