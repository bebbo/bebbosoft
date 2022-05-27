package de.bb.awt;

import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuComponent;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;

import de.bb.util.XmlFile;

/**
 * Helper stuff for easy dialog creation/usage
 * @author sfranke
 *
 */
public class Manager {
  private Frame frame;
  private XmlFile xml;
  private File file;
  private long lastRead;
  
  public Manager(Frame frame, String fileName) {
    this.frame = frame;
    this.file = new File(fileName);
    this.xml = new XmlFile();
  }
  
  public MenuBar loadMenu(String name) {
    uptodateCheck();

    MenuBar mb = new MenuBar();
    String dKey = "/awt/\\menubar\\" + name + "/";
    addMenus(mb, dKey, frame);
    
    return mb;
  }
  
  private void addMenus(MenuComponent mc, String key, ActionListener al) {
    for (Iterator i = xml.sections(key + "menu"); i.hasNext();) {
      String cKey = (String)i.next();
      if (xml.sections(cKey + "menu").hasNext()) {
        Menu m = new Menu();
        if (mc instanceof MenuBar) {
          ((MenuBar)mc).add(m);
        } else {
          ((Menu)mc).add(m);
        }
        String label = readText(cKey);
        int amp = label.indexOf('&');
        if (amp >= 0) {
          MenuShortcut ms = new MenuShortcut(label.toUpperCase().charAt(amp + 1));
          // m.setShortcut(ms);
          label = label.substring(0, amp) + label.substring(amp + 1);
        }
        m.setLabel(label);        
        addMenus(m, cKey, al);
      } else {
        MenuItem m = new java.awt.MenuItem();
        ((Menu)mc).add(m);
        String label = readText(cKey);
        int amp = label.indexOf('&');
        if (amp >= 0) {
          MenuShortcut ms = new MenuShortcut(label.toUpperCase().charAt(amp + 1));
          //m.setShortcut(ms);
          label = label.substring(0, amp) + label.substring(amp + 1);
        }
        m.setLabel(label);
        String cmd = readCommand(cKey);
        m.setActionCommand(cmd);
        m.addActionListener(al);
      }
    }
  }

  public Dialog loadDialog(String name) {    
    uptodateCheck();
    Dialog dialog = new Dialog(frame);
    dialog.setLayout(null);
    
    String dKey = "/awt/\\dialog\\" + name + "/";
    Dimension dim = readSize(dKey);
    dialog.setSize(dim);
    String title = readTitle(dKey);
    dialog.setTitle(title);
    addChildren(dialog, dKey, dialog);
    
    return dialog;
  }
  
  private void uptodateCheck() {
    long lm = file.lastModified();
    if (lm != lastRead) {
      lastRead = lm;
      this.xml.readFile(file.getAbsolutePath());
    }
  }

  public int showModal(Dialog d) {
    synchronized (d) {
      d.setVisible(false);
      d.setModal(true);
      
      Rectangle f = frame.getBounds();
      Dimension dim = d.getSize();
      
      int x = f.x + (f.width - dim.width >> 1);
      int y = f.y + (f.height - dim.height >> 1);
      
      Rectangle r = new Rectangle(x, y, dim.width, dim.height);
      d.setBounds(r);
      d.setVisible(true);      
    }
    return d.retVal;
  }
  
  
  private void addChildren(Container parent, String key, ActionListener al) {
    for (Iterator i = xml.sections(key + "child"); i.hasNext();) {
      String cKey = (String)i.next();
      String type = readType(cKey);

      Component c = null;
      
      if ("Label".equals(type)) {
        Label l = new Label();
        c = l;
        String text = readText(cKey);
        l.setText(text);
      } else
      if ("Button".equals(type)) {
        Button b = new Button();
        c = b;
        String t = readText(cKey);
        b.setLabel(t);
        t = readCommand(cKey);
        b.setActionCommand(t);    
        b.addActionListener(al);
      }

      if (c == null) 
        continue;
      
      parent.add(c);
      Rectangle r = readBounds(cKey);
      c.setBounds(r);      
    }
  }

  private String readCommand(String key) {
    return xml.getString(key, "command", "");
  }
    
  private String readText(String key) {
    return xml.getString(key, "text", "");
  }
 
  private String readType(String key) {
    return xml.getString(key, "type", "");
  }

  private String readTitle(String key) {
    return xml.getString(key, "title", "");
  }
  
  private Dimension readSize(String key) {
    key += "size";
    String sWidth = xml.getString(key, "width", null);
    String sHeight = xml.getString(key, "height", null);
    int width = 200;
    try {
      width = Integer.parseInt(sWidth);
    } catch (Exception ex) {}
    int height = 100;
    try {
      height = Integer.parseInt(sHeight);
    } catch (Exception ex) {}
    
    return new Dimension(width, height);
  }
  
  private Rectangle readBounds(String key) {
    key += "bounds";
    String sX = xml.getString(key, "x", null);
    String sY = xml.getString(key, "y", null);
    String sWidth = xml.getString(key, "width", null);
    String sHeight = xml.getString(key, "height", null);
    int x = 0;
    try {
      x = Integer.parseInt(sX);
    } catch (Exception ex) {}
    int y = 0;
    try {
      y = Integer.parseInt(sY);
    } catch (Exception ex) {}
    int width = 200;
    try {
      width = Integer.parseInt(sWidth);
    } catch (Exception ex) {}
    int height = 100;
    try {
      height = Integer.parseInt(sHeight);
    } catch (Exception ex) {}
    
    return new Rectangle(x, y, width, height);
  }

}
