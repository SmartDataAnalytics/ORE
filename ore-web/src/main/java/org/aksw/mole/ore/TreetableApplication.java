package org.aksw.mole.ore;

import org.semanticweb.owlapi.model.AxiomType;

import com.vaadin.Application;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.GeneratedRow;
import com.vaadin.ui.Table.RowGenerator;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class TreetableApplication extends Application {
    @Override
    public void init() {
        Window mainWindow = new Window("Treetableunsync Application");
        mainWindow.setSizeFull();
        setMainWindow(mainWindow);
        
        VerticalLayout mainLayout = new VerticalLayout();
        mainWindow.setContent(mainLayout);

        TreeTable tt = new TreeTable();
        tt.addContainerProperty("i", Object.class, null);
        tt.addGeneratedColumn("text", new Table.ColumnGenerator() {
            public Object generateCell(Table source, Object itemId,
                    Object columnId) {
                if ("text".equals(columnId)) {
                	if(source.getContainerDataSource().getItem(itemId)
                            .getItemProperty("i").getValue() instanceof AxiomType){
                		return ((AxiomType) source.getContainerDataSource().getItem(itemId)
                        .getItemProperty("i").getValue()).getName();
                	} else {
                		Button button = new Button("text " + (Integer) source
                                .getContainerDataSource().getItem(itemId)
                                .getItemProperty("i").getValue());
                        button.addListener(new Button.ClickListener() {
                            public void buttonClick(ClickEvent event) {
                                // TODO Auto-generated method stub
                            }
                        });
                        return button;
                	}
                    
                }
                return null;
            }
        });
        Object item1 = tt.addItem(new Object[] { AxiomType.CLASS_ASSERTION }, null);
        Object item2 = tt.addItem(new Object[] { 2 }, null);
        tt.addItem(new Object[] { 3 }, null);
        tt.setParent(item2, item1);
        
        Table table = new Table();table.setSizeFull();
        IndexedContainer c = new IndexedContainer();
        c.addContainerProperty("Property 1", String.class, "");
        c.addContainerProperty("Property 2", String.class, "");
        c.addContainerProperty("Property 3", String.class, "");
        c.addContainerProperty("Property 4", String.class, "");
        for (int ix = 0; ix < 500; ix++) {
            Item i = c.addItem(ix);
            i.getItemProperty("Property 1").setValue("Item " + ix + ",1");
            i.getItemProperty("Property 2").setValue("Item " + ix + ",2");
            i.getItemProperty("Property 3").setValue("Item " + ix + ",3");
            i.getItemProperty("Property 4").setValue("Item " + ix + ",4");
        }
        table.setContainerDataSource(c);
        table.setRowGenerator(new RowGenerator() {
			
			@Override
			public GeneratedRow generateRow(Table table, Object itemId) {
				if ((Integer) itemId % 5 == 0) {
		            if ((Integer) itemId % 10 == 0) {
		                return new GeneratedRow(
		                        "foobarbazoof very extremely long, most definitely will span.");
		            } else {
		                return new GeneratedRow("foo", "bar", "baz", "oof");
		            }
		        }
		        return null;
			}
		});

        mainLayout.addComponent(table);
    }

}
