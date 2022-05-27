/*
 * This file is initially generated by MEJB.
 * PLACE YOUR MODIFICATIONS HERE!
*/

package de.bb.web.board.mejb;

import java.rmi.RemoteException;
import java.util.Collection;

import de.bb.web.board.mejb.BoardHome;
import de.bb.web.board.mejb.BoardHomeBaseBean;

public class BoardHomeBean extends BoardHomeBaseBean implements de.bb.web.board.mejb.BoardHome {
    public BoardHomeBean() throws java.rmi.RemoteException {
        super();
    }

    public Collection<Board> findAllOrdered() throws RemoteException {
        return queryCollection("SELECT * FROM Board ORDER BY orderNr", NOPARAM);
    }
}
