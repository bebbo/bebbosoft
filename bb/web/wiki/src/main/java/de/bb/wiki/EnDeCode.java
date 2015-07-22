package de.bb.wiki;

public class EnDeCode {

    private final static String ILLEGAL = "<>'\"+&";

    /**
     * Escape some characters... "<b>über</b>" -->
     * "&lt;b&gt;&uuml;ber&lt;/b&gt;"
     * 
     * @param s
     *            a String
     * @return the escaped String.
     */
    public static String escape(String s) {
        return escape(s, false);
    }

    /**
     * Escape some characters... "<b>über</b>" -->
     * "&lt;b&gt;&uuml;ber&lt;/b&gt;"
     * 
     * @param s
     *            a String
     * @param isEdit
     *            true, if encoding is done for edit page
     * @return the escaped String.
     */
    public static String escape(String s, boolean isEdit) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            switch (ch) {
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case 'ä':
                sb.append("&auml;");
                break;
            case 'ö':
                sb.append("&ouml;");
                break;
            case 'ü':
                sb.append("&uuml;");
                break;
            case 'Ä':
                sb.append("&Auml;");
                break;
            case 'Ö':
                sb.append("&Ouml;");
                break;
            case 'Ü':
                sb.append("&Uuml;");
                break;
            case '&':
                if (isEdit) {
                    sb.append("&amp;");
                    break;
                }
            default:
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Convert illegal characters into % quoted representation. "foo bar" ->
     * "foo%20bar" No support for UTF yet.
     * 
     * @param url
     *            the url
     * @return the escaped url
     */
    public static String urlEscape(String url) {
        url = url.trim();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < url.length(); ++i) {
            char ch = url.charAt(i);
            if (ch <= 32 || ch > 127 || ILLEGAL.indexOf(ch) >= 0) {
                sb.append("%").append(Integer.toHexString(ch >> 4)).append(Integer.toHexString(ch & 0xf));
            } else
                sb.append(ch);
        }
        return sb.toString();
    }

}
