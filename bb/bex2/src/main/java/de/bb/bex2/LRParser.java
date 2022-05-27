package de.bb.bex2;

import java.util.ArrayList;
import java.util.Map;

import de.bb.util.Pair;
import de.bb.util.SingleMap;

public class LRParser {
    private ParseEntry root;

    // the used tables to parse
    int action[][];
    int actionStart[];
    Map<Integer, Map<String, Integer>> jump;
    String names[];
    int lengths[];
    Map<String, Integer> type2Id;

    public void parse(Scanner scanner) {
        root = new ParseEntry(scanner, 0, 0);

        final ArrayList<Pair<String, Integer>> stack = new ArrayList<Pair< String, Integer>>();
        
        ParseEntry current = root;
        int pos = 0;
        String name = "";
        for(;;) {
            stack.add(Pair.makePair(name, pos));
            
            if (jump.containsKey(pos)) {
                current = current.append(new ParseEntry(current, scanner, type2Id.get(names[pos]), scanner.position));
            }
            int ch = scanner.peek();
            if (ch < 0) ch = 256; // sleot 256 is EOF
            else
            if (ch > 255) ch = 257; // slot 257 is used for all combined terminals - assume that there is no clash
            int sr = action[ch][pos - actionStart[ch]];
            if (sr == 0)
                break;
            if (sr > 0) {
                // shift
                name = "'" + (char)ch + "'"; // an terminal
                pos = sr;
                scanner.move(1);
                continue;
            }
            
            // reduce
            sr = -sr;
            int length = lengths[sr];
            while (length-- > 0) {
                stack.remove(stack.size() - 1);
            }
            name = names[sr];
            int topPos = stack.get(stack.size() -1).getSecond();
            pos = jump.get(topPos).get(name);
            
            // close current element
            current.setEnd(scanner.position);
            current = current.getParent();
        }
    }
    
    public static void main(String [] args) {
        new LRParser().test();
    }

    private void test() {
        action = new int[257][];
        actionStart = new int[257];
        action['a'] = new int[]{6, 5, 0, -5, 0, -4, -6};
        actionStart['a'] = 3;
        action['b'] = new int[]{3, 2, -2, 0, 0, -3, 3, 9, 0, -2};
        actionStart['b'] = 0;
        action['c'] = new int[]{-3, 0, 8, 0, -2};
        actionStart['c'] = 5;

        type2Id = new SingleMap<String, Integer>();
        type2Id.put("", 0);
        type2Id.put("A", 1);
        type2Id.put("S", 2);

        jump = new SingleMap<Integer, Map<String,Integer>>();
        SingleMap<String, Integer> m = new SingleMap<String,Integer>();
        m.put("S", 1);
        jump.put(0, m);
        m = new SingleMap<String,Integer>();
        m.put("A", 4);
        jump.put(3, m);
        m = new SingleMap<String,Integer>();
        m.put("S", 7);
        jump.put(6, m);
        
        names = new String[] {"", "Z", " S", "S", "A", "A", "A"};
        
        lengths = new int[] {0, 1, 2, 3, 3, 1, 3};
        
        Scanner scanner = new Scanner("baab");
        parse(scanner);
    }
}
