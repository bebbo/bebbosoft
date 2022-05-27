package de.bb.wiki;

import java.util.LinkedList;
import java.util.Stack;

public class SimpleWiki {

    private WikiManager manager;

    private String cssName;

    private String title;

    private boolean hasMenu;

    private Stack<Integer> menuStack;

    private boolean hasLink;

    private String url;

    private String alt;

    private String target;

    private boolean wantsTarget;

    private Stack<String> stack = new Stack<String>();

    private int tocLevel;

    private StringBuilder toc = new StringBuilder();

    private Object context;

    public SimpleWiki(WikiManager wikiManager, String cssName, String context) {
        this.manager = wikiManager;
        this.cssName = cssName;
        this.context = context;
    }

    public StringBuilder convert(String wikiText) {
        if (wikiText.startsWith("<<<")) {
            int end = wikiText.indexOf(">>>");
            if (end < 0)
                end = wikiText.length() - 3;
            cssName = wikiText.substring(3, end);
            wikiText = wikiText.substring(end + 3).trim();
        }

        char[] data = new char[wikiText.length()];
        wikiText.getChars(0, data.length, data, 0);
        StringBuilder sb = new StringBuilder();

        // the various flags to track the content
        int header = 0;
        boolean isBold = false;
        boolean isCourier = false;
        boolean isItalics = false;
        // track the order of format expressions
        LinkedList<String> formatStack = new LinkedList<String>();

        final int length = data.length;
        StringBuilder line = new StringBuilder();
        StringBuilder first = null;
        for (int index = 0; index < length; ++index) {
            char ch = data[index];

            if (line.length() == 0) {
                switch (ch) {
                case '\r':
                    continue;
                case '\n':
                    while (formatStack.size() > 0) {
                        closeFormatTag(formatStack, sb, formatStack.getFirst());
                    }
                    isCourier = false;
                    isBold = false;
                    isItalics = false;
                    if (stack.size() > 0) {
                        if (":*#/|".indexOf(peek(data, index + 1)) < 0) {
                            String s = stack.peek();
                            char tt = s.charAt(0);
                            // was a table?
                            if (tt == 'd' || tt == 'h') {
                                sb.append("</t");
                                sb.append(tt);
                                sb.append("></tr></table>");
                                stack.pop();
                            } else {
                                makeList(sb, "");
                            }
                        }
                    } else {
                        if (hasMenu) {
                            sb.append("\r\n");
                            closeMenu(sb, 0);
                            sb.append("</div>\r\n");
                            hasMenu = false;
                        }
                        sb.append("<br>");
                    }
                    continue;
                case '=':
                    header = 1;
                    while (header < 5 && peek(data, index + 1) == '=') {
                        ++index;
                        ++header;
                    }
                    continue;
                case '!':
                    if (peek(data, index + 1) == 'T' && peek(data, index + 2) == 'O' && peek(data, index + 3) == 'C') {
                        first = sb;
                        sb = new StringBuilder();
                        index += 3;
                        if (peek(data, index + 1) == '\r')
                            ++index;
                        if (peek(data, index + 1) == '\n')
                            ++index;
                        continue;
                    }
                case ':':
                case '*':
                case '#':
                    if (hasLink)
                        break;

                    StringBuilder list = new StringBuilder();
                    list.append(ch);
                    while (":#*".indexOf(peek(data, index + 1)) >= 0) {
                        list.append(peek(data, index + 1));
                        ++index;
                    }
                    makeList(line, list.toString());
                    continue;
                case '/': // th
                case '|': // td
                    if (peek(data, index + 1) == ch) {
                        ++index;
                        int count = count(data, index + 1, ch) / 2;
                        int colspan = 1 + count;
                        if (count > 0)
                            index += count + count;
                        
                        boolean lspace = index + 1 < data.length && data[index + 1] <= 32;
                        
                        boolean rspace = index + 1 < data.length && data[index + 1] <= 32;
                        
                        makeTable(sb, colspan, ch == '/' ? 'h' : 'd', true);
                        continue;
                    }
                    break;
                }
            }
            char peek;
            switch (ch) {
            case '\r':
                continue;
            case '\n':
                if (hasLink)
                    line = endLink(line);

                String sl = line.toString();
                line = new StringBuilder();
                if (header > 0) {
                    closeHeader(sb, header, sl);
                    header = 0;
                } else {
                    sb.append(sl);
                }
                sb.append(ch);
                continue;
            case '~':
                peek = peek(data, index + 1);
                if (peek > ' ') {
                    ++index;
                    line.append(peek);
                } else {
                    line.append(ch);
                }
                continue;
            case '_':
                if (peek(data, index + 1) == '_') {
                    ++index;
                    if (isBold) {
                        closeFormatTag(formatStack, line, "b");
                        isBold = false;
                    } else {
                        isBold = true;
                        formatStack.addFirst("b");
                        line.append("<b>");
                    }
                    continue;
                }
                break;
            case '\'':
                if (peek(data, index + 1) == '\'') {
                    ++index;
                    if (isBold) {
                        closeFormatTag(formatStack, line, "i");
                        isBold = false;
                    } else {
                        isBold = true;
                        formatStack.addFirst("i");
                        line.append("<i>");
                    }
                    continue;
                }
                break;
            case '{':
                if (isCourier || peek(data, index + 1) != '{')
                    break;

                if (peek(data, index + 2) == '{') {
                    int preStart = index + 3;
                    int preEnd = wikiText.indexOf("}}}", preStart);
                    if (preEnd < 0)
                        preEnd = length;
                    line.append("\n<pre>").append(EnDeCode.escape(wikiText.substring(preStart, preEnd)))
                            .append("</pre>\n");
                    index = preEnd + 2;
                    if (index > length)
                        index = length;
                    continue;
                }

                ++index;
                isCourier = true;
                formatStack.addFirst("tt");
                line.append("<tt>");
                continue;
            case '}':
                if (!isCourier || peek(data, index + 1) != '}')
                    break;

                ++index;
                closeFormatTag(formatStack, line, "tt");
                isCourier = false;
                continue;
            case '[':
                if (!hasLink) {
                    sb.append(line);
                    line = new StringBuilder();
                    startLink();
                    continue;
                }
                break;
            case '=':
                if (hasLink && peek(data, index + 1) == '=' && peek(data, index + 2) == '>') {
                    wantsTarget = true;
                    index += 2;
                    alt = line.toString().trim();
                    line = new StringBuilder();
                    continue;
                }
                break;
            case '|':
            case '/':
                if (hasLink) {
                    // ignore '/' inside of links in table headers
                	if (ch == '/')
                		break;
                    if (alt != null) {
                        target = line.toString().trim();
                    } else {
                        alt = line.toString().trim();
                    }
                    line = new StringBuilder();
                    continue;
                }
                if (peek(data, index + 1) == ch) {
                    ++index;

                    sb.append(line);
                    line = new StringBuilder();

                    int count = count(data, index + 1, ch) / 2;
                    int colspan = 1 + count;
                    if (count > 0)
                        index += count + count;

                    makeTable(sb, colspan, ch == '/' ? 'h' : 'd', false);
                    continue;
                }
                break;
            case ']':
                if (hasLink) {
                    line = endLink(line);
                    continue;
                }
                break;
            case '\\':
                if (peek(data, index + 1) == '\\') {
                    ++index;
                    line.append("<br>");
                    continue;
                }
            case '§':
                if (peek(data, index + 1) == '§') {
                    index += 2;
                    int level = data[index] - '0';
                    if (level < 0)
                        level = 0;
                    if (level > 9)
                        level = 9;
                    addMenu(line, level);
                    continue;
                }
                break;
            default:
                // nada
            }

            switch (ch) {
            case '<':
                line.append("&lt;");
                break;
            case '>':
                line.append("&gt;");
                break;
            case 'ä':
                line.append("&auml;");
                break;
            case 'ö':
                line.append("&ouml;");
                break;
            case 'ü':
                line.append("&uuml;");
                break;
            case 'Ä':
                line.append("&Auml;");
                break;
            case 'Ö':
                line.append("&Ouml;");
                break;
            case 'Ü':
                line.append("&Uuml;");
                break;
            case 'ß':
                line.append("&szlig;");
                break;
            case '&':
                line.append("&amp;");
                break;
            case '©':
                line.append("&copy;");
                break;
            default:
                line.append(ch);
            }
        }

        if (header > 0) {
            String sl = line.toString();
            line = new StringBuilder();
            closeHeader(sb, header, sl);
            header = 0;
        }

        sb.append(line);

        while (stack.size() > 0) {
            String s = stack.peek();
            char ch = s.charAt(0);
            // was a table?
            if (ch == 'd' || ch == 'h') {
                sb.append("</t");
                sb.append(ch);
                sb.append("></tr></table>");
                stack.pop();
            } else {
                makeList(sb, "");
            }
        }

        if (hasMenu) {
            sb.append("\r\n");
            closeMenu(sb, 0);
            sb.append("</div>\r\n");
        }

        while (tocLevel > 0) {
            toc.append("</li></ul>");
            --tocLevel;
        }

        if (first != null) {
            first.append(toc).append(sb);
            return first;
        }
        return sb;
    }

    private int count(char[] data, int index, char c) {
        int n = 0;
        while (index + n < data.length && data[index + n] == 'c') {
            ++n;
        }
        return n;
    }

    private void makeTable(StringBuilder sb, int colspan, char ch, boolean addTr) {
        int lastTx = 0;
        if (stack.size() > 0) {
            String s = stack.peek();
            int c = s.charAt(0);
            if (c == 'd' || c == 'h') {
                lastTx = c;
                stack.pop();
            }
        }
        if (lastTx == 0) {
            sb.append("<table>");
            if (!addTr)
                sb.append("<tr>");
        } else {
            sb.append("</t");
            sb.append((char) lastTx);
            sb.append(">");
            if (addTr)
                sb.append("</tr>");
        }
        if (addTr)
            sb.append("\r\n<tr>");

        sb.append("<t");
        sb.append(ch);
        if (colspan > 1) {
            sb.append(" colspan='");
            sb.append(colspan);
            sb.append('\'');
        }
        sb.append('>');
        stack.push("" + ch);
    }

    private void makeList(StringBuilder sb, String list) {
        String currentList = "";
        if (stack.size() > 0) {
            String s = stack.peek();
            int ch = s.charAt(0);
            if (ch != 'd' && ch != 'h') {
                currentList = s;
                stack.pop();
            }
        }

        if (list.equals(currentList)) {
            if (list.length() > 0) {

                int ch = list.charAt(list.length() - 1);
                switch (ch) {
                case ':':
                case '#':
                case '*':
                    sb.append("</li><li>");
                    break;
                }
                stack.push(list);
            }
            return;
        }

        int len = list.length();
        if (currentList.length() < len)
            len = currentList.length();
        int i = 0;
        for (; i < len; ++i) {
            int cneu = list.charAt(i);
            int calt = currentList.charAt(i);
            if (cneu != calt)
                break;
        }
        // close current list entries
        int j = currentList.length() - 1;
        for (; j >= i; --j) {
            int ch = currentList.charAt(j);
            switch (ch) {
            case '#':
                sb.append("</li></ol>");
                break;
            case ':':
            case '*':
                sb.append("</li></ul>");
                break;
            }
        }

        for (; i < list.length(); ++i) {
            int ch = list.charAt(i);
            switch (ch) {
            case ':':
                sb.append("<ul style='list-style-type:none;'><li>");
                break;
            case '#':
                sb.append("<ol><li>");
                break;
            case '*':
                sb.append("<ul><li>");
                break;
            }
        }
        if (list.length() > 0)
            stack.push(list);
    }

    private void startLink() {
        hasLink = true;
        url = null;
        alt = null;
        wantsTarget = false;
        target = null;
    }

    private StringBuilder endLink(StringBuilder line) {
        hasLink = false;
        
        String sline = line.toString().trim();
        line = new StringBuilder();
        
        if (wantsTarget && target == null) {
            int space = sline.indexOf(' ');
            if (space > 0) {
                target = sline.substring(0, space);
                sline = sline.substring(space + 1).trim();
            }
        }

        if (alt != null) {
            url = sline;
        } else {
            alt = url = sline;
        }

        // prepend context root, to relative links
        if (url.startsWith("/") && !context.equals("/"))
            url = context + url;

        StringBuilder sb = new StringBuilder();

        int dot = url.lastIndexOf('.');
        boolean isImg = dot > 0 && manager.getImages().contains(url.substring(dot));
        if (isImg && url == alt) {
            sb.append("<img alt='");
            sb.append(EnDeCode.escape(alt));
            sb.append("'");
            sb.append(" src='");
            sb.append(EnDeCode.urlEscape(url));
            sb.append("'/>");
            return sb;
        }

        if (target == null && (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ftp://"))) {
            target = "_blank";
        }

        sb.append("<a href='");
        sb.append(EnDeCode.urlEscape(url));
        // if (linkExt != null)
        // sb.append(linkExt);
        if (target != null) {
            sb.append("' target='");
            sb.append(target);
        }
        sb.append("'");
        // addClass();

        dot = alt.lastIndexOf('.');
        boolean altImg = dot > 0 && manager.getImages().contains(alt.substring(dot));
        if (altImg) {
            sb.append("><img alt='").append(EnDeCode.escape(url)).append("'");
            sb.append(" title='").append(EnDeCode.escape(url)).append("'");
            sb.append(" width='").append(manager.getThumbWidth()).append("'");
            sb.append(" height='").append(manager.getThumbHeight()).append("'");
            sb.append(" src='");
            sb.append(EnDeCode.urlEscape(alt));
            sb.append("'/>");
        } else {
            sb.append(">");
            sb.append(alt);
        }

        sb.append("</a>");
        return sb;
    }

    private char peek(char[] data, int index) {
        if (index < data.length)
            return data[index];
        return 0;
    }

    int mn = 0;
    private void addMenu(StringBuilder line, int newLevel) {
        if (!hasMenu) {
            hasMenu = true;
            menuStack = new Stack<Integer>();
//            line.append("<script type='text/javascript' src='script/menu.js'></script>\r\n<div>\r\n");
        }

        closeMenu(line, newLevel);
        if (newLevel > 0) {
            menuStack.push(newLevel);
            line.append("<div class='m'><table><tr><td>");
            line.append("<img src='/gfx/opened.gif' alt='+' onclick='javascript:$t(this)'/></td>");
            line.append("\r\n  <td><div class='m'><div class='m' id='__menu_" + mn + "'><img src='/gfx/drawer.gif' onclick='javascript:$s(this)' alt='D' id='__menu_" + mn + "_ico'/>");
            ++mn;
        }
    }

    private void closeMenu(StringBuilder line, int newLevel) {
        if (menuStack.size() > 0) {
            Integer level = menuStack.peek();

            line.append("</div>");
            while (menuStack.size() > 0 && level.intValue() >= newLevel) {
                menuStack.pop();
                if (menuStack.size() > 0)
                    level = menuStack.peek();
                line.append("</div>");
                line.append("</td></tr></table></div>\r\n");
            }
        }
    }

    private void closeHeader(StringBuilder sb, int header, String sl) {
        int i = sl.length();
        // remove the ending ===
        for (; i > 0; --i) {
            if (sl.charAt(i - 1) != '=')
                break;
        }
        sl = sl.substring(0, i);
        sb.append("<h").append(header).append(">");

        // remove tags from title
        String withoutTags = removeTags(sl);
        String escaped = EnDeCode.urlEscape(EnDeCode.escape(withoutTags));
         String ided = escaped.replace(' ', '_').replace('&',
         '_').replace(';', '_');
        
        sb.append("<a id='").append(ided).append("'></a>");
        sb.append(sl).append("</h").append(header).append(">");
        if (header == 1 || title == null) {
            title = withoutTags;
        }

        while (tocLevel > header) {
            toc.append("</li></ul>");
            --tocLevel;
        }
        if (tocLevel == header) {
            toc.append("</li><li>");
        } else {
            while (tocLevel < header) {
                toc.append("<ul><li>");
                ++tocLevel;
            }
        }

        toc.append("<a href='#" + escaped + "'>" + sl + "</a>");
    }

    private String removeTags(String withoutTags) {
        for (int bra = withoutTags.indexOf('<'); bra >= 0; bra = withoutTags.indexOf('<')) {
            int ket = withoutTags.indexOf('>', bra);
            if (ket < 0) {
                withoutTags = withoutTags.substring(0, bra);
                break;
            }
            withoutTags = withoutTags.substring(0, bra) + withoutTags.substring(ket + 1);
        }
        return withoutTags;
    }

    private void closeFormatTag(LinkedList<String> formatStack, StringBuilder line, String close) {
        for (String tag : formatStack) {
            if (tag.equals(close))
                break;
            line.append("</").append(tag).append(">");
        }
        line.append("</").append(close).append(">");
        for (String tag : formatStack) {
            if (tag.equals(close))
                break;
            line.append("<").append(tag).append(">");
        }
        formatStack.remove(close);
    }

    public String getTitle() {
        return title;
    }

    public String getCssName() {
        return cssName;
    }
}
