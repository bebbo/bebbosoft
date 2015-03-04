/**
 * 
 */
package de.bb.bejy;

public class CronThread extends Thread {
  private Runnable run;
  private long startTime;

  CronThread(String name, Runnable r) {
    super(name);
    run = r;
  }

  public void run() {
    Cron cron = Config.getCron();
    cron.addThread(this);
    startTime = System.currentTimeMillis();
    try {

      run.run();
    } catch (Throwable t) {

    }
    cron.removeThread(this);
  }

  public long getStartTime() {
    return startTime;
  }
}