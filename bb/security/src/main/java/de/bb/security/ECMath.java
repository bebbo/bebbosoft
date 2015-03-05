package de.bb.security;

import de.bb.util.Misc;

public class ECMath {

    int p[];
    int len;
    
    class P {
        int x[];
        int y[];
        
        P add(P p) {
            return null;
        }
        P dub() {
            return null;
        }
    }
    class Fp extends P{
        P add(P p) {
            if (p == INFINITY)
                return this;
            if (this == INFINITY)
                return p;
            
            // add negative or double
            if (Misc.equals(x, p.x)) {
                if (Misc.equals(y, p.y))
                    return dub();
                return INFINITY;
            }
            
            int lambda[] = new int[len];
            
            return null;
        }

        P dub() {
            if (this == INFINITY)
                return INFINITY;
            
            // check y == 0
            int i = 0;
            for (; i < len; ++i) {
                if (y[i] != 0)
                    break;
            }
            if (i == len)
                return INFINITY;
            
            return null;
        }
    }

    final P INFINITY = new P();

    protected P montgomeryLadder(P p1, int e[], int eLen) {
        P p0 = INFINITY;

        while (--eLen > 0) {
            if ((e[eLen >> 6] & (1 << (eLen & 31))) == 0) {
                p1 = p1.add(p0);
                p0 = p0.add(p0);
            } else {
                p0 = p0.add(p1);
                p1 = p1.add(p1);
            }
        }
        return p0;
    }
}
