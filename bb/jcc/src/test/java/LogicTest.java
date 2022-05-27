import org.junit.Test;


public class LogicTest {

    public static int int1() {
        int x = 1;
        return x;
    }
    
    public void and4(boolean a, boolean b, boolean c, boolean d) {
        if (a && b && c && d) {
            foo();
        }
    }

    public boolean or4(boolean a, boolean b, boolean c, boolean d) {
        boolean r = a;
        if (a || b || c || (r = d)) {
            foo();
        } else {
            r = c;
        }
        return r;
    }

    public boolean andor4(boolean a, boolean b, boolean c, boolean d) {
        boolean r = a;
        if ((a || b) && (c || (r = d))) {
            foo();
        } else {
            r = c;
        }
        return r;
    }

    @Test
    public void foo() {
        
    }
}
