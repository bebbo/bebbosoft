package de.bb.mejb;

import java.util.Hashtable;

class h {
    private String Code;
    private String a;
    Hashtable b;

    h(String s, String s1) {
        b = new Hashtable();
        Code = s;
        a = s1;
    }

    void Code(String s, EnterpriseModule enterprisemodule) {
        b.put(s, enterprisemodule);
    }

}
