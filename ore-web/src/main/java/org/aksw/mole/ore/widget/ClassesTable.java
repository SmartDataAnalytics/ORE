package org.aksw.mole.ore.widget;

import java.util.Collection;

import org.aksw.mole.ore.ExplanationManager2;
import org.aksw.mole.ore.RepairManagerListener;
import org.aksw.mole.ore.UserSession;
import org.aksw.mole.ore.util.Renderer;
import org.aksw.mole.ore.util.Renderer.Syntax;
import org.semanticweb.owlapi.model.OWLClass;

import com.github.wolfie.refresher.Refresher;
import com.github.wolfie.refresher.Refresher.RefreshListener;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

public class ClassesTable extends Table implements RepairManagerListener{
	
	private static final String IS_ROOT_CLASS = "root";
	private static final String CLASS = "class";
	
	private Collection<OWLClass> classes;
	private Collection<OWLClass> rootClasses;
	private IndexedContainer container;
	
	private Refresher refresher = new Refresher();
	
	public ClassesTable() {
		setCaption("Unsatisfiable Classes");
		setSizeFull();
		setPageLength(0);
//		setHeight(null);
		setColumnExpandRatio("Class", 1.0f);
		setSelectable(true);
        setMultiSelect(true);
        setImmediate(true);
        
        container = new IndexedContainer();
        container.addContainerProperty(IS_ROOT_CLASS, String.class, null);
		container.addContainerProperty(CLASS, String.class, null);
		setContainerDataSource(container);
		
		setCellStyleGenerator(new Table.CellStyleGenerator() {

			@Override
			public String getStyle(Object itemId, Object propertyId) {
				if(propertyId != null && propertyId.equals(IS_ROOT_CLASS)){
					return "is-root-column";
				}
				return "empty";
			}
			
		});
		
		addGeneratedColumn(CLASS, new ColumnGenerator() {
			
			private Renderer renderer = new Renderer();
			
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				if (CLASS.equals(columnId)) {
					if(itemId instanceof OWLClass){
						OWLClass cls = ((OWLClass) itemId);
						return new Label(renderer.render(cls, Syntax.MANCHESTER), Label.CONTENT_XHTML);
		        	}
				}
				return null;
			}
		});
		
		setColumnHeader(IS_ROOT_CLASS, "");
		setColumnWidth(IS_ROOT_CLASS, 30);
		setColumnExpandRatio(CLASS, 1.0f);
		
		refresher.addListener(new RefreshListener() {
			
			@Override
			public void refresh(Refresher source) {
				if(rootClasses != null){
					source.setEnabled(false);
					showRootUnsatisfiableClasses();
				}
				
			}
		});
		refresher.setRefreshInterval(1000);
		refresher.setEnabled(false);
	}
	
	@Override
	public void attach() {
		super.attach();
		getWindow().addComponent(refresher);
		refresh();
	}
	
	@Override
	public void detach() {
		super.detach();
		getWindow().removeComponent(refresher);
	}
	
	public void showRootUnsatisfiableClasses(){
		synchronized (getApplication()) {
			Item item;
			for(OWLClass cls : rootClasses){
				item = container.getItem(cls);
				item.getItemProperty(IS_ROOT_CLASS).setValue("Root");
			}
			container.sort(new Object[]{IS_ROOT_CLASS,  CLASS}, new boolean[]{false, true});
		}
	}
	
	public void setClasses(Collection<OWLClass> classes){
		this.classes = classes;
		
		container.removeAllItems();
		setValue(null);
		
		Item item;
		for(OWLClass cls : classes){
			item = container.addItem(cls);
			item.getItemProperty(CLASS).setValue(cls);
		}
	}
	
	public void refresh(){
		classes = UserSession.getReasoner().getUnsatisfiableClasses().getEntitiesMinusBottom();
		setClasses(classes);
		//compute root classes in separate thread and poll
		if(!classes.isEmpty()){
			rootClasses = null;
			refresher.setEnabled(true);
			new ComputeRootClassesProcess(UserSession.getExplanationManager()).start();
		}
	}
	
	@Override
	public void repairPlanChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void repairPlanExecuted() {
		UserSession.getExplanationManager().refreshRootClassFinder();
		refresh();
	}
	
	class ComputeRootClassesProcess extends Thread{
		
		private ExplanationManager2 expMan;
		
		public ComputeRootClassesProcess(ExplanationManager2 expMan) {
			this.expMan = expMan;
		}
		
		@Override
		public void run() {
			rootClasses = expMan.getRootUnsatisfiableClasses();
		}
	}

}
