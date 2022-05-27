// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

package de.bb.web;


public class Util
{

    public Util()
    {
    }

    public static int toInteger(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch(Exception exception)
        {
            return 0;
        }
    }
}
