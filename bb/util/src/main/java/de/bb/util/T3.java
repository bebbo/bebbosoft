package de.bb.util;

public class T3 <A,B,C> extends T2<A,B> {
    public C c;
    public String toString() {
        return super.toString() + ", c:" + c;
    }
}
