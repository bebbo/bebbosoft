package de.bb.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Dialog extends JDialog implements ActionListener, WindowListener, KeyListener {

    private static final long serialVersionUID = 1L;

    int retVal;
    private HashMap<String, String> dialogData = new HashMap<String, String>();

    public Dialog(Frame owner) {
        super(owner);
        addWindowListener(this);
        setResizable(false);
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            retVal = Integer.parseInt(cmd);
            if (retVal > 0)
                updateData();
            if (retVal < 100)
                endModal();
        } catch (Exception ex) {
        }
    }

    public void setDialogData(HashMap<String, String> dd) {
        this.dialogData = new HashMap<String, String>(dd);
        Component children[] = this.getComponents();
        for (int i = 0; i < children.length; ++i) {
            setDialogData(children[i]);
        }
    }

    public HashMap<String, String> getDialogData() {
        return dialogData;
    }

    private void setDialogData(Component component) {
        if (component instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) component;
            component = sp.getViewport().getView();
        }
        if (component instanceof JTextField) {
            JTextField e = (JTextField) component;
            String id = e.getName();
            String val = dialogData.get(id);
            if (val != null)
                e.setText(val);
            return;
        }
        if (component instanceof JTextArea) {
            JTextArea e = (JTextArea) component;
            String id = e.getName();
            String val = dialogData.get(id);
            if (val != null)
                e.setText(val);
            return;
        }
        if (component instanceof Container) {
            Component children[] = ((Container) component).getComponents();
            for (int i = 0; i < children.length; ++i) {
                setDialogData(children[i]);
            }
            return;
        }
    }

    private void updateData() {
        Component children[] = this.getComponents();
        for (int i = 0; i < children.length; ++i) {
            updateData(children[i]);
        }
    }

    private void updateData(Component component) {
        if (component instanceof JTextField) {
            JTextField e = (JTextField) component;
            String id = e.getName();
            String val = e.getText();
            dialogData.put(id, val);
            return;
        }
        if (component instanceof Container) {
            Component children[] = ((Container) component).getComponents();
            for (int i = 0; i < children.length; ++i) {
                updateData(children[i]);
            }
            return;
        }
    }

    private void endModal() {
        setVisible(false);
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        retVal = -1;
        endModal();
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
    public void keyTyped(KeyEvent e) {
        // ESCAPE == cancel dialog
        if (e.getKeyChar() == KeyEvent.VK_ESCAPE) {
            retVal = -1;
            endModal();
            return;
        }
        // buttons with id < 100 can end a modal dialog
        if (e.getSource() instanceof JButton) {
            JButton b = (JButton) e.getSource();
            try {
                int cmd = Integer.parseInt(b.getActionCommand());
                if (cmd < 100) {
                    retVal = cmd;
                    endModal();
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub

    }

	public void init() {
		// TODO Auto-generated method stub
		
	}

}
