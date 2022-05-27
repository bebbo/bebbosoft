package de.bb.awt;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class Dialog extends java.awt.Dialog implements ActionListener, WindowListener {

  int retVal;

  public Dialog(Frame owner)
  {
    super(owner);
    addWindowListener(this);
  }

  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    try {
      retVal = Integer.parseInt(cmd);
      if (retVal < 100) endModal();
    } catch (Exception ex) {}
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

}
