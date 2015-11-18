package de.bb.mejb;

import de.bb.rmi.Principal;
import de.bb.util.LRUCache;

public class Client {

    private static LRUCache<Thread, Client> CLIENT_CACHE = new LRUCache<Thread, Client>();
    private Principal principal;
    UserTA userTransaction;

    private Client(Principal principal) {
        this.principal = principal;
        userTransaction = new UserTA(principal);
    }

    public static Client getClient() {
        Thread t = Thread.currentThread();
        Client client = CLIENT_CACHE.get(t);
        if (client == null) {
            client = new Client(new Principal(t.getName(), ""));
            CLIENT_CACHE.put(t, client);
        }
        return client;
    }

    protected void finalize() throws Throwable {
        CLIENT_CACHE.remove(principal);
        super.finalize();
    }

    public Principal getPrincipal() {
        return principal;
    }

    public boolean isCallerInRole(String s) {
        return Config.isCallerInRole(principal, s);
    }

    void a(CMPDbi cmpdbi, String s) throws Exception {
        userTransaction.addDbi(cmpdbi, s);
    }

}
