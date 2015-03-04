package de.bb.bejy;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * @author franke
 */
class UIFrame
  extends JFrame
  implements ActionListener, WindowListener, MouseListener, TreeSelectionListener
{
  /** the treeview.*/
  private JTree treeView;
  private DefaultMutableTreeNode root, globals, groups, servers;

  /** the content for associated information. */
  private JComponent content;

  /** the info label */
  private Label info;

  /** the main menu. */
  private JMenuBar mainMenu;
  /** the file menu part. */
  private JMenu fileMenu;
  /** the help menu part. */
  private JMenu helpMenu;

  /** file open dialog.*/
  private FileDialog openFileDialog;
  /** file save dialog. */
  private FileDialog saveFileDialog;
  /** about dialog. */
  private Dialog about;

  private JPopupMenu serverPopup, globalPopup, groupPopup;

  private JTable table;
  private static Object [] tableNames = { "name", "value" };

  UIFrame()
  {
    super("BEJY Manager");

    //
    addWindowListener(this);

    setSize(640, 480);

    // init members
    openFileDialog =
      new FileDialog(this, "open configuration", FileDialog.LOAD);
    saveFileDialog =
      new FileDialog(this, "save configuration as", FileDialog.SAVE);

    //  init Menu Bar
    mainMenu = new JMenuBar();

    fileMenu =
      makeMenu("&File", new String[][] { { "&open ...", "10010", "true" }, {
        "&save", "10020", "true" }, {
        "save &as ...", "10030", "true" }, {
        null }, {
        "e&xit", "10099", "true" }
    });

    helpMenu =
      makeMenu("&Help", new String[][] { { "&about ...", "90099", "true" }
    });

    mainMenu.add(fileMenu);
    mainMenu.add(helpMenu);

    setJMenuBar(mainMenu);

    // init popupMenus
    serverPopup =
      makePopupMenu("Server", new String[][] { { "&new server", "100001" }
    });
    globalPopup =
      makePopupMenu("Global", new String[][] { { "&new JDBC connection", "100002" }
    });
    groupPopup = 
      makePopupMenu("Group", new String[][] { { "&new group", "100003" }
    });

    // init child windows
    content = new JPanel();
    content.setLayout(new BorderLayout());
    info = new Label(Version.getFull());
    content.add(info);

    table = new JTable(1, 2);

    treeView = new JTree();
    treeView.addMouseListener(this);
    treeView.addTreeSelectionListener(this);

    JSplitPane sp =
      new JSplitPane(
        javax.swing.JSplitPane.HORIZONTAL_SPLIT,
        new JScrollPane(treeView),
        content);
    sp.setDividerSize(3);
    getContentPane().add(sp);

    fillTreeView();

    // init about dialog
    about = new Dialog(this, "about");
    about.add(new Label(Version.getFull()));
    about.setModal(true);
    about.setResizable(false);
    about.setSize(500, 50);
    about.addWindowListener(this);
  }

  /**
   * Method fillTreeView.
   */
  private void fillTreeView()
  {
    root = new DefaultMutableTreeNode("BEJY");
    globals = new DefaultMutableTreeNode("globals");
    groups = new DefaultMutableTreeNode("groups");
    servers = new DefaultMutableTreeNode("servers");

    root.add(globals);
    root.add(groups);

    for (Iterator i = Config.servers.values().iterator(); i.hasNext();)
    {
      Server s = (Server) i.next();
      servers.add(new DefaultMutableTreeNode(s));
    }
    root.add(servers);

    treeView.setModel(new DefaultTreeModel(root));
  }

  JMenu makeMenu(String title, String[][] md)
  {
    int ke = getKey(title);
    title = xVK(title);
    JMenu m = new JMenu(title);
    if (ke > 0)
    {
      m.setMnemonic(ke);
    }

    for (int i = 0, n = md.length; i < n; ++i)
    {
      String[] d = md[i];
      String t = d[0];
      if (t == null)
      {
        m.addSeparator();
        continue;
      }
      ke = getKey(t);
      t = xVK(t);
      JMenuItem mi = new JMenuItem(t);
      if (ke > 0)
      {
        mi.setMnemonic(ke);
      }
      if (d.length > 1)
      {
        mi.setActionCommand(d[1]);
        if (d.length > 2)
        {
          mi.setEnabled("true".equals(d[2]));
        }
      }

      mi.addActionListener(this);

      m.add(mi);
    }
    return m;
  }

  JPopupMenu makePopupMenu(String title, String[][] md)
  {
    JPopupMenu m = new JPopupMenu(title);

    for (int i = 0, n = md.length; i < n; ++i)
    {
      String[] d = md[i];
      String t = d[0];
      if (t == null)
      {
        m.addSeparator();
        continue;
      }
      int ke = getKey(t);
      t = xVK(t);
      JMenuItem mi = new JMenuItem(t);
      if (ke > 0)
      {
        mi.setMnemonic(ke);
      }
      if (d.length > 1)
      {
        mi.setActionCommand(d[1]);
        if (d.length > 2)
        {
          mi.setEnabled("true".equals(d[2]));
        }
      }

      mi.addActionListener(this);

      m.add(mi);
    }
    return m;
  }

  /*
    int getVK(String s)
    {
      int idx = s.indexOf('&');
      if (idx < 0)
        return idx;
      String vk = "VK_" + s.substring(idx + 1, idx + 2).toUpperCase();
  
      try
      {
        Field field = KeyEvent.class.getDeclaredField(vk);
        return field.getInt(field);
      } catch (Exception ex)
      {
      }
      return -1;
    }
  */
  int getKey(String s)
  {
    int idx = s.indexOf('&');
    if (idx < 0)
      return idx;
    return s.charAt(idx + 1);
  }

  String xVK(String s)
  {
    int idx = s.indexOf('&');
    if (idx < 0)
      return s;
    return s.substring(0, idx) + s.substring(idx + 1);
  }

  // ActionListener functions
  public void actionPerformed(ActionEvent ae)
  {
    System.out.println(ae);

    String scmd = ae.getActionCommand();
    try
    {
      int cmd = Integer.parseInt(scmd);
      switch (cmd)
      {
        case 10010 :
          openFileDialog.setVisible(true);
          String fileName = openFileDialog.getFile();
          if (fileName != null)
          {
            Config.loadConfig(openFileDialog.getDirectory() + fileName);
            fillTreeView();
          }
          break;
        case 10020 :
          Config.save();
          break;
        case 10030 :
          saveFileDialog.setVisible(true);
          fileName = saveFileDialog.getFile();
          if (fileName != null)
          {
            Config.setFileName(saveFileDialog.getDirectory() + fileName);
            Config.save();
          }
          break;
        case 10099 :
          setVisible(false);
          break;
        case 90099 :
          about.setVisible(true);
          break;
      }
    } catch (Exception ex)
    {
    }
  }

  // WindowListener functions
  public void windowActivated(WindowEvent e)
  {
  }
  public void windowClosed(WindowEvent e)
  {
  }
  public void windowDeactivated(WindowEvent e)
  {
  }
  public void windowDeiconified(WindowEvent e)
  {
  }
  public void windowIconified(WindowEvent e)
  {
  }
  public void windowOpened(WindowEvent e)
  {
  }
  public void windowClosing(WindowEvent e)
  {
    if (e.getWindow() == about)
    {
      about.setVisible(false);
      return;
    }

    if (Config.servers.size() == 0)
    {
      System.exit(0);
    }
  }
  // MouseListener - used from treeView
  public void mouseClicked(MouseEvent me)
  {
  }
  public void mouseEntered(MouseEvent me)
  {
  }
  public void mouseExited(MouseEvent me)
  {
  }
  public void mousePressed(MouseEvent me)
  {
    if (me.getSource() == treeView)
    {
      if (me.getButton() == MouseEvent.BUTTON3)
      {
        TreePath tp = treeView.getPathForLocation(me.getX(), me.getY());
        if (tp == null)
          return;

        Object o = tp.getLastPathComponent();
        System.out.println(o);
        if (o == servers)
        {
          serverPopup.show(treeView, me.getX(), me.getY());
        } else
        if (o == groups) {
          groupPopup.show(treeView, me.getX(), me.getY());
        } else
        if (o == globals) {
          globalPopup.show(treeView, me.getX(), me.getY());
        }
      }
    }
  }
  public void mouseReleased(MouseEvent me)
  {
  }
  // TreeSelectionListener
  public void valueChanged(TreeSelectionEvent tse)
  {
    TreePath tp = tse.getNewLeadSelectionPath();
    System.out.println(tp);
    if (tp == null)
      return;

    content.removeAll();

    Object o = tp.getLastPathComponent();
    if (o == globals)
    {
      TableModel tm =
        new TM(new Object[][] { { "mainDomain", "xxx.de" }, {
          "nameServer", "127.0.0.1" }
      });
      table.setModel(tm);
      content.add(table);      
    } else {
      content.add(info);
    }
    
    content.revalidate();
  }
  
  private static class TM extends AbstractTableModel {
    Object [][] data;
    TM(Object[][]o)
    {
      data = o;
    }
    TM(String path, String [] names)
    {}
     
    public int getColumnCount() {
      return 2;
    }
    public int getRowCount() {
      return data == null ? 0 : data.length;
    }
    public Object getValueAt(int row, int col) {
      if (data == null)
        return null;
        
      if (row >= data.length)
        return null;
      
      Object r[] = data[row];
      if (r == null)
        return null;
        
      if (col >= r.length)
        return null;
      return r[col];
    }
    public boolean isCellEditable(int row, int coloumn) {
      return coloumn == 1;
    }
  }
}
