package de.bb.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import de.bb.swing.anno.Id;
import de.bb.util.XmlFile;

public class Frame extends JFrame implements WindowListener, ActionListener {

    private static final long serialVersionUID = 1L;

    private static final String SMAX = Integer.toString(Short.MAX_VALUE);

    private static HashMap<Class<?>, Collection<Field>> class2Annotations = new HashMap<Class<?>, Collection<Field>>();

    protected final Manager manager;
    private final XmlFile xml;
    private final HashMap<Field, Component> created = new HashMap<Field, Component>();

    public Frame(Manager manager) {
        super(manager.getAppParam("name"));
        this.manager = manager;
        manager.setFrame(this);
        xml = manager.getXml();

        final String menuName = manager.getAppParam("menu");
        if (menuName != null) {
            JMenuBar mb = manager.loadMenu(menuName);
            setJMenuBar(mb);
        }

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(this);

        setSize(800, 600);

        loadControls("/bwt/application/");

    }

    private void loadControls(final String path) {
        try {
            final HashMap<String, Field> id2Field = new HashMap<String, Field>();
            fillId2FieldMap(getClass(), id2Field);

            createRecurse(getContentPane(), path, id2Field);

            for (final Entry<Field, Component> e : created.entrySet()) {
                final Field f = e.getKey();
                final Component child = e.getValue();
                f.setAccessible(true);
                if (f.getDeclaringClass() == getClass()) {
                    f.set(this, child);
                } else {
                    for (Component p : created.values()) {
                        if (p.getClass() == f.getDeclaringClass()) {
                            f.set(p, child);
                            break;
                        }
                    }
                }
            }

            ArrayList<Component> al = new ArrayList<Component>(created.values());
            al.add(this);
            for (final Component child : al) {
                try {
                    Method m = child.getClass().getDeclaredMethod("init");
                    if (m != null) {
                        m.setAccessible(true);
                        m.invoke(child);
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createRecurse(final Container c, final String path, HashMap<String, Field> id2Field) throws Exception {
        for (final String key : xml.getSections(path)) {
            Component child = createChild(c, id2Field, key);
            if (c != null) {
                c.add(child);
            }
        }
    }

    private Component createChild(final Container c, HashMap<String, Field> id2Field, final String key)
            throws Exception {
        final String id = xml.getString(key, "id", null);
        Component child = null;
        if (id != null) {
            final Field f = id2Field.get(id);
            child = (Component) f.getType().newInstance();
            created.put(f, child);
        } else {
            final String name = XmlFile.getLastSegment(key);
            if ("splitpane".equals(name)) {
                final JSplitPane sp = new JSplitPane();
                sp.setOrientation("horizontal".equals(xml.getString(key, "orientation", "horizontal")) ? JSplitPane.HORIZONTAL_SPLIT
                        : JSplitPane.VERTICAL_SPLIT);
                child = sp;

                final Iterator<String> i = xml.sections(key);
                final Component left = createChild(null, id2Field, i.next());
                sp.setLeftComponent(left);
                final Component right = createChild(null, id2Field, i.next());
                sp.setRightComponent(right);
            } else if ("scrollpane".equals(name)) {
                final JScrollPane js = new JScrollPane();
                final Iterator<String> i = xml.sections(key);
                final Component view = createChild(null, id2Field, i.next());
                js.setViewportView(view);
                child = js;
            } else if ("vbox".equals(name)) {
                final JPanel jp = new JPanel();
                jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));
                child = jp;
            }
        }
        final String maxWidth = xml.getString(key, "maxWidth", null);
        final String maxHeight = xml.getString(key, "maxHeight", null);
        if (maxWidth != null || maxHeight != null) {
            int width = maxWidth != null ? Integer.parseInt(maxWidth) : Short.MAX_VALUE;
            int height = maxHeight != null ? Integer.parseInt(maxHeight) : Short.MAX_VALUE;
            Dimension d = new Dimension(width, height);
            child.setMaximumSize(d);
        }
        final String sWidth = xml.getString(key, "width", maxWidth);
        final String sHeight = xml.getString(key, "height", maxHeight);
        if (sWidth != null && sHeight != null) {
            int width = Integer.parseInt(sWidth);
            int height = Integer.parseInt(sHeight);
            Dimension d = new Dimension(width, height);
            child.setPreferredSize(d);
        }
        
        for (Field f : child.getClass().getDeclaredFields()) {
        	de.bb.swing.anno.Manager m = f.getAnnotation(de.bb.swing.anno.Manager.class);
        	if (m != null) {
        		f.setAccessible(true);
        		f.set(child, manager);
        	}
        }

        if (child instanceof Container && !(child instanceof JSplitPane) && !(child instanceof JScrollPane)) {
            createRecurse((Container) child, key, id2Field);
        }
        return child;
    }

    private void fillId2FieldMap(Class<?> clazz, HashMap<String, Field> id2Field) {
        final Collection<Field> fields = getAnnotatedFields(clazz);
        for (final Field f : fields) {
            final Id id = f.getAnnotation(Id.class);
            if (id != null) {
                id2Field.put(id.value(), f);
                fillId2FieldMap(f.getType(), id2Field);
            }
        }
    }

    private static Collection<Field> getAnnotatedFields(Class<?> clazz) {
        Collection<Field> r = class2Annotations.get(clazz);
        if (r != null)
            return r;

        r = new ArrayList<Field>();
        collectAnnotatedFields(clazz, r);
        return r;
    }

    private static void collectAnnotatedFields(Class<?> clazz, Collection<Field> collectedFields) {
        for (final Field f : clazz.getDeclaredFields()) {
            if (f.getAnnotations() != null && f.getAnnotations().length > 0) {
                collectedFields.add(f);
            }
        }
        if (clazz.getSuperclass() != null)
            collectAnnotatedFields(clazz.getSuperclass(), collectedFields);
    }

    public void doClose() {
        System.exit(0);
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        doClose();
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }
}
