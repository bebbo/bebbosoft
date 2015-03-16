import java.util.HashMap;

import de.bb.swing.Dialog;
import de.bb.swing.Frame;
import de.bb.swing.Manager;

public class Tcp2TcpApp extends Frame {
    private static final long serialVersionUID = 1L;

    public Tcp2TcpApp(Manager manager) {
        super(manager);
    }

    public HashMap<String, String> showConfig(HashMap<String, String> dd) {
        final Dialog d = manager.loadDialog("configuration");
        d.setDialogData(dd);
        final int r = manager.showModal(d);
        if (r == 1) {
            return d.getDialogData();
        }
        return null;
    }

    public void showError(final String message) {
        final Dialog d = manager.loadDialog("error");
        HashMap<String, String> dd = new HashMap<String, String>();
        dd.put("message", message);
        d.setDialogData(dd);
        manager.showModal(d);
    }

    public boolean acceptCertificate(HashMap<String, String> dd) {
        final Dialog d = manager.loadDialog("certificate");
        d.setDialogData(dd);
        final int r = manager.showModal(d);
        return r == 1;
    }

}
