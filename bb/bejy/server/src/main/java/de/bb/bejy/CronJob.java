/**
 * 
 */
package de.bb.bejy;

import de.bb.util.SessionManager;

public class CronJob implements SessionManager.Callback {
    private String name;

    private Runnable run;

    private SessionManager<Runnable, CronJob> sMan;

    private int intervall;

    private int pass;

    private CronThread thread;

    private long startTime;

    CronJob(String name, Runnable r, SessionManager<Runnable, CronJob> sm, int intervall) {
        this.name = name;
        run = r;
        sMan = sm;
        this.intervall = intervall;
    }

    public boolean dontRemove(Object _o) {
        Runnable o = (Runnable) _o;
        // prevent duplicate instances
        if (thread == null) {
            thread = new CronThread(name + " #" + (++pass), run);
            thread.start();
            sMan.touch(o, 30000);
            return true;
        }
        if (thread.isAlive()) {
            sMan.touch(o, 10000);
            return true;
        }
        thread = null;
        if (intervall <= 0)
            return false;

        sMan.touch(o, intervall);
        startTime = System.currentTimeMillis() + intervall;
        return true; // keep me
    }

    public String getName() {
        return name;
    }

    public long getIntervall() {
        return intervall;
    }

    public long nextLaunch() {
        return startTime;
    }

    public boolean isActive() {
        return thread != null && thread.isAlive();
    }

    void setStartTime(long l) {
        startTime = l;
    }
}