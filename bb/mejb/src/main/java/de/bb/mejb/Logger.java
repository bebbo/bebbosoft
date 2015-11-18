package de.bb.mejb;

import de.bb.util.LogFile;

public class Logger {

    public Logger() {
    }

    public static void info(String s) {
        if (a < 900)
            return;
        Code("INFO ", s, null);
    }

    public static void debug(String s) {
        if (a < 600)
            return;
        Code("DEBUG", s, null);
    }

    public static void warn(String s) {
        if (a < 300)
            return;
        Code("*WARN", s, null);
    }

    public static void error(String s) {
        Code("**ERR", s, null);
    }

    public static void error(String s, Throwable throwable) {
        Code("**ERR", s, throwable);
    }

    private static void Code(String s, String s1, Throwable throwable) {
        Code.writeDate("[MEJB][" + s + "] " + s1);
        if (throwable != null)
            throwable.printStackTrace();
    }

    public static void setLevel(int i) {
        a = i;
        if (a > 1000)
            a = 1000;
        else if (a < 0)
            a = 0;
    }

    public static void setLogFile(String s) {
        Code = new LogFile(s);
    }

    private static LogFile Code = new LogFile("*");
    private static int a = 100;

}
