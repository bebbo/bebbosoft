package de.bb.mejb;

import de.bb.rmi.Principal;
import de.bb.util.LRUCache;
import de.bb.util.Pool;
import java.sql.*;
import java.util.*;

public abstract class CMPDbi {

    protected CMPDbi() {
        lruCache = null;
    }

    public abstract void remove(Connection connection, String s) throws SQLException;

    public abstract ResultSet select(Connection connection, Object obj) throws SQLException;

    public abstract PreparedStatement insert(Connection connection) throws SQLException;

    public abstract String getId(Statement statement) throws SQLException;

    public abstract PreparedStatement update(Connection connection) throws SQLException;

    public Connection getConnection(Principal principal) throws Exception {
        return (Connection) a.obtain(principal);
    }

    public void releaseConnection(Principal principal, Connection connection) {
        a.release(principal, connection);
    }

    void Code(Pool pool) {
        a = pool;
        synchronized (Code) {
            Object aobj[] = (Object[]) Code.get(pool);
            if (aobj != null) {
                lruCache = (LRUCache) aobj[0];
                map = (HashMap) aobj[1];
                return;
            }
        }
        lruCache = new LRUCache();
        map = new HashMap();
        Code.put(pool, ((Object) (new Object[] { lruCache, map })));
    }

    String a() {
        return d;
    }

    void b(String s) {
        d = s;
    }

    void c(String s) {
        int i = s.indexOf('\001');
        if (i >= 0)
            s = s.substring(0, i);
        Logger.debug("clear cache: " + s);
        s = s.toLowerCase();
        int j = s.indexOf("from");
        int k = s.indexOf("where");
        if (j >= 0)
            if (k >= 0)
                s = s.substring(j + 4, k);
            else
                s = s.substring(j + 4);
        s = s.trim();
        synchronized (lruCache) {
            LinkedList linkedlist;
            for (StringTokenizer stringtokenizer = new StringTokenizer(s, ", \t\n\r\f"); stringtokenizer
                    .hasMoreElements(); linkedlist.add(new Long(System.currentTimeMillis() + 1000L))) {
                String s1 = stringtokenizer.nextToken();
                if (s1.charAt(0) == '"')
                    s1 = s1.substring(1, s1.length() - 1);
                Logger.debug("clear cache, table: " + s1);
                linkedlist = (LinkedList) map.get(s1);
                if (linkedlist == null) {
                    linkedlist = new LinkedList();
                    linkedlist.add(new Long(0x7fffffffffffffffL));
                    map.put(s1, linkedlist);
                }
                String s2;
                for (; linkedlist.size() > 1; lruCache.remove(s2)) {
                    s2 = (String) linkedlist.removeLast();
                    Logger.debug("clear cache, key: " + s2);
                }

                linkedlist.removeFirst();
            }

        }
    }

    void d(long l, String s, Object obj) {
        String s1 = s.toLowerCase();
        int i = s1.indexOf('\001');
        if (i >= 0)
            s1 = s1.substring(0, i);
        Logger.debug("adding query to cache: " + s1);
        int j = s1.indexOf("from");
        int k = s1.indexOf("where");
        if (j >= 0)
            if (k >= 0)
                s1 = s1.substring(j + 4, k);
            else
                s1 = s1.substring(j + 4);
        s1 = s1.trim();
        Logger.debug("adding tables to cache: " + s1);
        synchronized (lruCache) {
            LinkedList linkedlist;
            for (StringTokenizer stringtokenizer = new StringTokenizer(s1, ", \t\n\r\f"); stringtokenizer
                    .hasMoreElements(); linkedlist.add(s)) {
                String s2 = stringtokenizer.nextToken();
                if (s2.charAt(0) == '"')
                    s2 = s2.substring(1, s2.length() - 1);
                Logger.debug("adding table to cache: " + s2);
                linkedlist = (LinkedList) map.get(s2);
                if (linkedlist != null) {
                    Iterator iterator = linkedlist.iterator();
                    long l1 = ((Long) iterator.next()).longValue();
                    if (l1 <= l)
                        continue;
                    Logger.debug("not cached: " + s);
                    return;
                }
                linkedlist = new LinkedList();
                linkedlist.add(new Long(0x8000000000000000L));
                map.put(s2, linkedlist);
            }

            Logger.debug("adding to cache: " + s);
            lruCache.put(s, obj);
        }
    }

    private static HashMap Code = new HashMap();
    Pool a;
    LRUCache lruCache;
    HashMap map;
    private String d;

}
