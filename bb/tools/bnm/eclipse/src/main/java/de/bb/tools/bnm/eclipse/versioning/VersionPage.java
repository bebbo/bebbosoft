package de.bb.tools.bnm.eclipse.versioning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

public abstract class VersionPage extends UserInputWizardPage implements Listener {

	@SuppressWarnings("rawtypes")
    private static final Comparator[] SORTER = {
		new Comparator<VI>() {
			
			public int compare(VI o1, VI o2) {
				return o1.id.compareTo(o2.id);
			}			
		},
		new Comparator<VI>() {
			
			public int compare(VI o1, VI o2) {
				return o1.version.compareTo(o2.version);
			}			
		},
		new Comparator<VI>() {
			
			public int compare(VI o1, VI o2) {
				String s1 = o1.bundleId;
				if (s1 == null) s1 = "";
				String s2 = o2.bundleId;
				if (s2 == null) s2 = "";
				return s1.compareTo(s2);
			}			
		},
		new Comparator<VI>() {
			
			public int compare(VI o1, VI o2) {
				String s1 = o1.bundleVersion;
				if (s1 == null) s1 = "";
				String s2 = o2.bundleVersion;
				if (s2 == null) s2 = "";
				return s1.compareTo(s2);
			}			
		},
	};
	protected VI[] data;
	private Table table;
	private ArrayList<TableColumn> cols;
	private TableColumn lastSort;
	private TV tv;
	
	private String noteText = "Note that modified version SHOULD end with -SNAPSHOT\r\n"
            + "All references are updated accordingly\r\n"
            + "If a referenced module is not yet a SNAPSHOT version, the version gets promoted to the next SNAPSHOT.";

	public VersionPage(VI[] data, String name, String description) {
		super(name);
		this.data = data;
		this.setDescription(description);
	}

	public void createControl(Composite parent) {
	    Composite client = new Composite(parent, SWT.NONE);
	
	    GridLayout layout = new GridLayout();
	    layout.marginWidth = 1;
	    layout.numColumns = 1;
	    client.setLayout(layout);    
	    client.setLayoutData(new GridData(GridData.FILL_BOTH));
	
	    Label label = new Label(client, SWT.NONE);
	    label
	        .setText(noteText);
	    label.setLayoutData(new GridData());
	
	    table = new Table(client, SWT.BORDER | SWT.FULL_SELECTION);
	    table.setLinesVisible(true);
	    table.setHeaderVisible(true);
	    table.setLayoutData(new GridData(GridData.FILL_BOTH));
	
	    this.cols = new ArrayList<TableColumn>();

	    TableColumn c0 = new TableColumn(table, SWT.LEFT);
	    c0.setText("module name");
	    c0.setWidth(300);
	    c0.addListener(SWT.Selection, this);
	    cols.add(c0);
    
	    TableColumn c1 = new TableColumn(table, SWT.LEFT);
	    c1.setText("version");
	    c1.setWidth(160);
	    c1.addListener(SWT.Selection, this);
	    cols.add(c1);
	
	    TableColumn c2 = new TableColumn(table, SWT.LEFT);
	    c2.setText("bundle name");
	    c2.setWidth(300);
	    c2.addListener(SWT.Selection, this);
	    cols.add(c2);
	
	    TableColumn c3 = new TableColumn(table, SWT.LEFT);
	    c3.setText("bundle-version");
	    c3.setWidth(160);
	    c3.addListener(SWT.Selection, this);
	    cols.add(c3);
	
	    tv = new TV(table);
	    CellModifier cm = new CellModifier(tv);
		    
	    /*
	     * TableItem ti1 = new TableItem(table, 0); ti1.setText(1, "foo.bar");
	     * ti1.setText(2, "1.0.0");
	     */
	    CellEditor editors[] = new CellEditor[4];
	    editors[0] = null; // new CheckboxCellEditor(table);
	    ComboBoxViewerCellEditor cedit = new ComboBoxViewerCellEditor(table); // TextCellEditor(table);
	
	    cedit.getViewer().addSelectionChangedListener(cm);
	    cedit.getControl().addFocusListener(cm);
	    
	    
	    editors[1] = cedit;
	    editors[2] = null; 
	    editors[3] = null; // new TextCellEditor(table);
	
	    String cp[] = { "0", "1", "2", "3" };
	
	    tv.setColumnProperties(cp);
	    tv.setCellEditors(editors);
	    tv.setContentProvider(new ArrayContentProvider());
	    IBaseLabelProvider lp = new LabelProvider();
	    tv.setLabelProvider(lp);
	       
	    tv.setCellModifier(cm);
	
	    sortData(c0, 0);
    
	
	    setPageComplete(false);
	    setControl(client);
	  }

	
	public boolean canFlipToNextPage() {
	    if (data == null)
	      return false;
	    boolean ok = true;
	    for (int i = 0; i < data.length; ++i) {
	      String s = Util.toOsgiVersion(data[i].version);
	      TableItem ti = table.getItem(i);
	      if (data[i].version != null && !data[i].version.equals(data[i].origVersion)) {
	        ti.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
	      } else {
	        ti.setBackground(null);
	      }
	      
	      if (data[i].bundleVersion != null){
	        if (!s.equals(data[i].bundleVersion)) {
	          data[i].bundleVersion = s;
	        }      
	        if (data[i].origBundleVersion != null && !data[i].bundleVersion.equals(data[i].origBundleVersion)) {
	          ti.setBackground(2, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
	          ti.setBackground(3, Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
	        } else {
	        ti.setBackground(2, null);        
	        ti.setBackground(3, null);        
	        }
	      }
	    }
	    return ok;
	  }

	public void handleEvent (Event event) {
		TableColumn column = (TableColumn) event.widget;
		int index = cols.indexOf(column);
		
		sortData(column, index);
		
	}

	private void sortData(TableColumn column, int index) {
		int dir = lastSort == column ? SWT.DOWN : SWT.UP;
		lastSort = dir == SWT.UP ? column : null;
		
		@SuppressWarnings("unchecked")
        Comparator<VI> sorter = SORTER[index];		
		Arrays.sort(data, sorter);
		
		if (dir == SWT.DOWN) {
			for (int i = 0, j = data.length - 1; i < j; ++i, --j) {
				VI temp = data[i];
				data[i] = data[j];
				data[j] = temp;
			}
		}
		
		tv.setInput(data);
        table.setSortColumn(column);
        table.setSortDirection(dir);
	}

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }
	
	
}