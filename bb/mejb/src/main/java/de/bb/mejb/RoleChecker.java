package de.bb.mejb;

import de.bb.rmi.Principal;

public interface RoleChecker {

    public abstract boolean isCallerInRole(Principal principal, String s);
}
