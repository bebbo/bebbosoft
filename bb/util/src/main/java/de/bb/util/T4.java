package de.bb.util;

public class T4 <A,B,C,D> extends T3<A,B,C> {
    public D d;
    public String toString() {
        return super.toString() + ", d:" + d;
    }}
