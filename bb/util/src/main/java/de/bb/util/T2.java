package de.bb.util;

import java.io.Serializable;

public class T2 <A,B> implements Serializable {
    public A a;
    public B b;
    public String toString() {
        return "a:" + a + ", b:" + b;
    }
}
