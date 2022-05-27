package de.bb.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;

import de.bb.swing.anno.Id;
import de.bb.util.SingleMap;
import de.bb.util.XmlFile;

/**
 * Helper stuff for easy dialog creation/usage
 * 
 * @author sfranke
 * 
 */
public class Manager implements IManager {
    private Frame frame;
    private XmlFile xml;

    private File file;
    private long lastRead;

    public Manager(String fileName) {
        this.file = new File(fileName);
        this.xml = new XmlFile();
    }

    public JMenuBar loadMenu(String name) {
        uptodateCheck();

        JMenuBar mb = new JMenuBar();
        String dKey = "/bwt/\\menubar\\" + name + "/";
        addMenus(mb, dKey, frame);

        return mb;
    }

    private void addMenus(MenuElement mc, String key, ActionListener al) {
        for (Iterator i = xml.sections(key + "menu"); i.hasNext();) {
            String cKey = (String) i.next();
            if (xml.sections(cKey + "menu").hasNext()) {
                String label = readText(cKey);
                if (label.length() == 0) {
                    ((JMenu) mc).addSeparator();
                    continue;
                }
                JMenu m = new JMenu();
                if (mc instanceof JMenuBar) {
                    ((JMenuBar) mc).add(m);
                } else {
                    ((JMenu) mc).add(m);
                }
                int amp = label.indexOf('&');
                if (amp >= 0) {
                    int ch = label.toUpperCase().charAt(amp + 1);
                    m.setMnemonic(ch);
                    label = label.substring(0, amp) + label.substring(amp + 1);
                }
                m.setText(label);
                String cmd = readCommand(cKey);
                m.setActionCommand(cmd);
                addMenus(m, cKey, al);
            } else {
                String label = readText(cKey);
                if (label.length() == 0) {
                    ((JMenu) mc).addSeparator();
                    continue;
                }
                JMenuItem m = new JMenuItem();
                ((JMenu) mc).add(m);
                int amp = label.indexOf('&');
                if (amp >= 0) {
                    int ch = label.toUpperCase().charAt(amp + 1);
                    m.setMnemonic(ch);
                    label = label.substring(0, amp) + label.substring(amp + 1);
                }
                m.setText(label);
                String cmd = readCommand(cKey);
                m.setActionCommand(cmd);
                m.addActionListener(al);
                
                
                String shortCut = readShortcut(cKey);
                if (shortCut.length() > 0) {
					KeyStroke keyStroke = KeyStroke.getKeyStroke(shortCut);
					m.setAccelerator(keyStroke);
                }

            }
        }
    }

	public Dialog loadDialog(String name) {
        uptodateCheck();
        String dKey = "/bwt/\\dialog\\" + name + "/";
        String className = xml.getString(dKey, "class", "de.bb.swing.Dialog");
        try {
			Class<? extends Dialog> clazz = (Class<? extends Dialog>) Class.forName(className);
	        Dialog dialog = clazz.getConstructor(Frame.class).newInstance(frame);
	        dialog.setLayout(null);

	        Dimension dim = readSize(dKey);
	        dim.height += 10;
	        dialog.setSize(dim);
	        String title = readTitle(dKey);
	        dialog.setTitle(title);
	        
	        SingleMap<String, Field> id2field = new SingleMap<String, Field>();
	        for (final Field f : clazz.getDeclaredFields()) {
	        	final Id id = f.getAnnotation(Id.class);
	        	if (id != null) {
	        		f.setAccessible(true);
	        		id2field.put( id.value(), f);
	        	}
	        		
	        }
	        
	        addChildren(dialog, dKey, dialog, id2field);

	        dialog.init();
	        
	        return dialog;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }

    private void uptodateCheck() {
        if (!file.exists()) {
            if (lastRead == 0) {
                lastRead = -1;
                InputStream is = getClass().getResourceAsStream("/" + file.getName());
                xml = new XmlFile();
                xml.read(is);
            }
            return;
        }
        long lm = file.lastModified();
        if (lm != lastRead) {
            lastRead = lm;
            xml = new XmlFile();
            xml.readFile(file.getAbsolutePath());
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

    private void addChildren(Container parent, String key, Dialog dialog, SingleMap<String, Field> id2field) throws IllegalArgumentException, IllegalAccessException {
        for (Iterator<String> i = xml.sections(key + "child"); i.hasNext();) {
            String cKey = i.next();
            String type = readType(cKey);

            JComponent c = null;
            JComponent proxy = null;

            if ("Label".equals(type)) {
                JLabel l = new JLabel();
                c = l;
                String text = readText(cKey);
                l.setText(text);
            } else if ("Button".equals(type)) {
                JButton b = new JButton();
                c = b;
                String t = readText(cKey);
                b.setText(t);
                t = readCommand(cKey);
                b.setActionCommand(t);
                b.addActionListener(dialog);
            } else if ("Edit".equals(type)) {
                String t = readText(cKey);
                JTextField e = new JTextField(t);
                c = e;
                t = readCommand(cKey);
                e.setName(t);
            } else if ("EditArea".equals(type)) {
                String t = readText(cKey);
                JTextArea e = new JTextArea(t);
                e.setLineWrap(true);
                c = e;
                t = readCommand(cKey);
                e.setName(t);
                proxy = new JScrollPane(c);
            }

            if (c == null)
                continue;

            String id = xml.getString(cKey, "id", null);
            if (id != null) {
            	Field f = id2field.get(id);
            	if (f != null)
            		f.set(dialog, c);
            }
            
            c.addKeyListener(dialog);
            
            if (proxy != null)
                c = proxy;
            
            parent.add(c);
            Rectangle r = readBounds(cKey);
            c.setBounds(r);
        }
    }
    private String readShortcut(String key) {
        return xml.getString(key, "key", "");
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
        } catch (Exception ex) {
        }
        int height = 100;
        try {
            height = Integer.parseInt(sHeight);
        } catch (Exception ex) {
        }

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
        } catch (Exception ex) {
        }
        int y = 0;
        try {
            y = Integer.parseInt(sY);
        } catch (Exception ex) {
        }
        int width = 200;
        try {
            width = Integer.parseInt(sWidth);
        } catch (Exception ex) {
        }
        int height = 100;
        try {
            height = Integer.parseInt(sHeight);
        } catch (Exception ex) {
        }

        return new Rectangle(x, y, width, height);
    }

    public String getAppParam(String name) {
        return xml.getString("/bwt/application", name, null);
    }

    void setFrame(Frame app) {
        this.frame = app;
    }

    public XmlFile getXml() {
        uptodateCheck();
        return xml;
    }

    public static <T extends Frame> T loadApplication(final Class<T> appClazz) {
        try {
            final Manager manager = new Manager("src/dialog.xml");
            final Constructor<? extends Frame> ct = appClazz.getConstructor(Manager.class);
            final T app = (T) ct.newInstance(manager);
            manager.setFrame(app);
            return app;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
