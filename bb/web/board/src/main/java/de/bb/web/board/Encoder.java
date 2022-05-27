package de.bb.web.board;

import de.bb.util.LRUCache;
import java.util.Enumeration;
import java.util.ResourceBundle;

public class Encoder {

    public Encoder() {
    }

    public static Enumeration getSmileyNames() {
        return bundle.getKeys();
    }

    public static String getSmileyURL(String key) {
        return bundle.getString(key);
    }

    public static String encode(Object o) {
        return encode(o.toString());
    }
    
    public static String encode(String text) {
        String val = (String) cache.get(text);
        if (val != null)
            return val;
        StringBuffer sb = new StringBuffer();
        encode(sb, text, null);
        val = sb.toString();
        try {
            cache.put(text, val);
        } catch (Exception _ex) {
        }
        return val;
    }

    private static int encode(StringBuffer sb, String content, String terminator) {
        int i;
        for (i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            switch (ch) {
            case 13: // '\r'
                break;

            case 10: // '\n'
                sb.append("<br>\n");
                break;

            case 60: // '<'
                sb.append("&lt;");
                break;

            case 58: // ':'
            case 59: // ';'
                int sl = maxLen;
                if (i + sl > content.length())
                    sl = content.length() - i;
                String check = content.substring(i, i + sl);
                for (; sl < maxLen; sl++)
                    check = check + ' ';

                boolean hit = false;
                if (bundle != null)
                    while (check.length() > 0)
                        try {
                            String img = bundle.getString(check);
                            if (img == null)
                                continue;
                            sb.append("<img border='0' src='" + img + "'>");
                            i += check.length() - 1;
                            hit = true;
                            break;
                        } catch (Exception _ex) {
                            check = check.substring(0, check.length() - 1);
                        }
                if (!hit)
                    sb.append(ch);
                break;

            case 91: // '['
                if (terminator != null && content.substring(i).startsWith(terminator))
                    return i + terminator.length();
                int ket = content.indexOf(']', i + 1);
                if (ket >= 0) {
                    int bra = content.indexOf('[', i + 1);
                    if (bra >= ket) {
                        String term = "[/" + content.substring(i + 1, ket) + "]";
                        if ("[/quote]".equals(term)) {
                            StringBuffer sb2 = new StringBuffer();
                            i += term.length() - 1;
                            i += encode(sb2, content.substring(i), term) - 1;
                            sb.append("\r\n<table class='quote'><tr><td>&nbsp;</td><td>");
                            String txt = sb2.toString();
                            if (txt.startsWith("<br>\n"))
                                txt = txt.substring(5);
                            for (; txt.endsWith(" "); txt = txt.substring(0, txt.length() - 1))
                                ;
                            if (txt.endsWith("<br>\n"))
                                txt = txt.substring(0, txt.length() - 5);
                            sb.append(txt);
                            sb.append("</td></tr></table>\r\n");
                            continue;
                        }
                        if ("[/link]".equals(term)) {
                            StringBuffer sb2 = new StringBuffer();
                            i += term.length() - 1;
                            i += encode(sb2, content.substring(i), term) - 1;
                            sb.append("<a href='");
                            sb.append(sb2.toString());
                            sb.append("'>");
                            sb.append(sb2.toString());
                            sb.append("</a>");
                            continue;
                        }
                        if ("[/img]".equals(term)) {
                            sb.append("<img src='");
                            i += term.length() - 1;
                            i += encode(sb, content.substring(i), term) - 1;
                            sb.append("'/>");
                            continue;
                        }
                        if ("[/b]".equals(term)) {
                            sb.append("<b>");
                            i += term.length() - 1;
                            i += encode(sb, content.substring(i), term) - 1;
                            sb.append("</b>");
                            continue;
                        }
                        if ("[/u]".equals(term)) {
                            sb.append("<u>");
                            i += term.length() - 1;
                            i += encode(sb, content.substring(i), term) - 1;
                            sb.append("</u>");
                            continue;
                        }
                        int eq = term.indexOf('=');
                        String term2 = term;
                        if (eq >= 0) {
                            term2 = term.substring(0, eq) + "]";
                            eq++;
                            if ("[/size]".equals(term2)) {
                                sb.append("<font size='" + term.substring(eq, term.length() - 1) + "'>");
                                i += term.length() - 1;
                                i += encode(sb, content.substring(i), term2) - 1;
                                sb.append("</font>");
                                continue;
                            }
                            if ("[/url]".equals(term2)) {
                                String url = term.substring(eq, term.length() - 1);
                                if (!url.startsWith("http://"))
                                    url = "http://" + url;
                                sb.append("<a target='_blank' href='" + url + "'>");
                                i += term.length() - 1;
                                i += encode(sb, content.substring(i), term2) - 1;
                                sb.append("</a>");
                                continue;
                            }
                        }
                    }
                }
                // fall through

            default:
                sb.append(ch);
                break;
            }
        }

        return i;
    }

    public static String stats() {
        return "de.bb.web.board.Encoder: " + cache.toString();
    }

    private static LRUCache cache = new LRUCache();
    private static ResourceBundle bundle;
    private static int maxLen;

    static {
        bundle = ResourceBundle.getBundle("smiley");
        for (Enumeration e = bundle.getKeys(); e.hasMoreElements();) {
            String s = (String) e.nextElement();
            if (s.length() > maxLen)
                maxLen = s.length();
        }

    }
}
