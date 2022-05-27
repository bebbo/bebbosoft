/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/
package de.bb.bejy.http;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.http.Cookie;

import de.bb.bejy.Protocol;
import de.bb.util.ByteRef;
import de.bb.util.Mime;

/**
 * This class is designed to ...
 * 
 * @author bebbo
 */
public class HttpRequestBase {
    private final static boolean DEBUG = HttpProtocol.DEBUG;

    //  final static ByteRef ACCEPTLANGUAGE = new ByteRef("ACCEPT-LANGUAGE");

    //  final static ByteRef AUTHORIZATION = new ByteRef("AUTHORIZATION");

    //  final static ByteRef BREMOTE = new ByteRef("B-REMOTEHOST");

    //  final static ByteRef BSPORT = new ByteRef("B-SERVERPORT");

    final static ByteRef BASIC = new ByteRef("BASIC");

    //  final static ByteRef CONTENTTYPE = new ByteRef("CONTENT-TYPE");

    //  final static ByteRef DEFAULTHOST = new ByteRef("127.0.0.1");

    //  final static ByteRef HOST = new ByteRef("HOST");

    final static ByteRef JSESSIONID = new ByteRef("JSESSIONID");

    final static ByteRef DOTSLASHSLASH = new ByteRef("://");

    final static ByteRef POST = new ByteRef("POST");

    final static ByteRef ROOT = new ByteRef("/");

    final static ByteRef JSID = new ByteRef(";jsessionid=");

    final static ByteRef PDIR = new ByteRef("/../");

    final static ByteRef CDIR = new ByteRef("/./");

    HttpContext context;

    ByteRef authType;

    String charEncoding;

    ByteRef contentType;

    String fullhost; // host name with port

    String host; // host name

    String method; // GET, POST, PUT, OPTIONS, ...

    HashMap<String, String[]> parameters = new HashMap<String, String[]>();

    HashMap<String, Object> attributes = new HashMap<String, Object>();

    // the URLparameters inhere
    boolean paramParsed;

    ByteRef path; // path without server

    ByteRef protocol; // HTTP/1.0 or HTTP/1.1

    String queryString;

    String remotePass;

    String remoteUser;

    RequestDispatcher reqDisp;

    javax.servlet.ServletInputStream sis;

    //Vector stringHeaders = null;

    long inContentLength;

    ArrayList<Cookie> inCookies = new ArrayList<Cookie>(); // inCookies used here

    HashMap<String, String> inHeaders = new HashMap<String, String>();

    ArrayList<String> inHeaderNames = new ArrayList<String>();

    ArrayList<Locale> locales = new ArrayList<Locale>();

    // InputStream is;

    // boolean isSsl;

    int port;

    BufferedReader ir;

    boolean fromC;

    boolean fromU;

    String sid;

    Protocol proto;

    protected boolean isSecure;

    HttpRequestBase(Protocol proto, int port) {
        this.proto = proto;
        this.port = port;
    }

    /**
     * Method parseHeader.
     * 
     * @return ByteRef
     */
    ByteRef parseHeader(ByteRef br) {
        ByteRef line;
        /*
        for (;;)
        {
          line = proto._readLine(br);
          if (line == null)
            return null;

          // end of header
          if (line.length() == 0)
            break;
        }
        if (true) {
          inHeaders.put("CONNECTION", "Keep-Alive");
          inHeaders.put("CONTENT-LENGTH", "66");
          return br;
        }
        */
        for (;;) {
            line = proto.readLine(br);
            if (line == null) {
                return null;
            }

            // end of header
            if (line.length() == 0) {
                break;
            }

            if (DEBUG) {
                System.out.println(line);
            }

            // get the data for the current line
            byte[] l = line.toByteArray();
            if (l.length < 2) {
                continue;
            }
            // System.out.println(l);
            switch (l[0]) {
            case 'A':
            case 'a': // AUTHORIZATION, ACCEPT-LANGUAGE
                byte b1 = l[1];
                // AUTHORIZATION
                if (l.length > 14 && (b1 == 'U' || b1 == 'u')) {
                    b1 = l[2];
                    if (b1 == 'T' || b1 == 't') {
                        b1 = l[3];
                        if (b1 == 'H' || b1 == 'h') {
                            b1 = l[4];
                            if (b1 == 'O' || b1 == 'o') {
                                b1 = l[5];
                                if (b1 == 'R' || b1 == 'r') {
                                    b1 = l[6];
                                    if (b1 == 'I' || b1 == 'i') {
                                        b1 = l[7];
                                        if (b1 == 'Z' || b1 == 'z') {
                                            b1 = l[8];
                                            if (b1 == 'A' || b1 == 'a') {
                                                b1 = l[9];
                                                if (b1 == 'T' || b1 == 't') {
                                                    b1 = l[10];
                                                    if (b1 == 'I' || b1 == 'i') {
                                                        b1 = l[11];
                                                        if (b1 == 'O' || b1 == 'o') {
                                                            b1 = l[12];
                                                            if (b1 == 'N' || b1 == 'n') {
                                                                if (l[13] == ':') {
                                                                    int i = 14;
                                                                    while (i < l.length && l[i] == ' ') {
                                                                        ++i;
                                                                    } // skip SPACES
                                                                    if (i + 5 < l.length) {
                                                                        b1 = l[i];
                                                                        if (b1 == 'B' || b1 == 'b') {
                                                                            b1 = l[i + 1];
                                                                            if (b1 == 'A' || b1 == 'a') {
                                                                                b1 = l[i + 2];
                                                                                if (b1 == 'S' || b1 == 's') {
                                                                                    b1 = l[i + 3];
                                                                                    if (b1 == 'I' || b1 == 'i') {
                                                                                        b1 = l[i + 4];
                                                                                        if (b1 == 'C' || b1 == 'c') {
                                                                                            if (l[i + 5] == ' ') {
                                                                                                byte b[] = Mime.decode(
                                                                                                        l, i + 6,
                                                                                                        l.length);
                                                                                                line = new ByteRef(b);
                                                                                                i = 0;
                                                                                                while (i < b.length
                                                                                                        && b[i] != ':') {
                                                                                                    ++i;
                                                                                                } // search :
                                                                                                if (i < b.length) {
                                                                                                    remoteUser = new String(
                                                                                                            b, 0, 0, i);
                                                                                                    remotePass = new String(
                                                                                                            b, 0,
                                                                                                            i + 1,
                                                                                                            b.length
                                                                                                                    - i
                                                                                                                    - 1);
                                                                                                    continue;
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // ACCEPT-LANGUAGE
                if (l.length > 16 && (b1 == 'C' || b1 == 'c')) {
                    b1 = l[2];
                    if (b1 == 'C' || b1 == 'c') {
                        b1 = l[3];
                        if (b1 == 'E' || b1 == 'e') {
                            b1 = l[4];
                            if (b1 == 'P' || b1 == 'p') {
                                b1 = l[5];
                                if (b1 == 'T' || b1 == 't') {
                                    if (l[6] == '-') {
                                        b1 = l[7];
                                        if (b1 == 'L' || b1 == 'l') {
                                            b1 = l[8];
                                            if (b1 == 'A' || b1 == 'a') {
                                                b1 = l[9];
                                                if (b1 == 'N' || b1 == 'n') {
                                                    b1 = l[10];
                                                    if (b1 == 'G' || b1 == 'g') {
                                                        b1 = l[11];
                                                        if (b1 == 'U' || b1 == 'u') {
                                                            b1 = l[12];
                                                            if (b1 == 'A' || b1 == 'a') {
                                                                b1 = l[13];
                                                                if (b1 == 'G' || b1 == 'g') {
                                                                    b1 = l[14];
                                                                    if (b1 == 'E' || b1 == 'e') {
                                                                        if (l[15] == ':') {
                                                                            int i = 16;
                                                                            // add all languages - like de-de, en-us ...
                                                                            while (i < l.length) {
                                                                                while (i < l.length && l[i] == ' ') {
                                                                                    ++i;
                                                                                } // skip SPACES
                                                                                int s = i;
                                                                                while (i < l.length && l[i] != '-') {
                                                                                    ++i;
                                                                                } // search '-'
                                                                                int m = i++;
                                                                                String la = new String(l, 0, s, m - s);
                                                                                ++m;
                                                                                if (i < l.length) {
                                                                                    while (i < l.length && l[i] != ',') {
                                                                                        ++i;
                                                                                    } // search ','
                                                                                    String vr = new String(l, 0, m, i
                                                                                            - m);
                                                                                    locales.add(new Locale(la, vr));
                                                                                } else {
                                                                                    locales.add(new Locale(la));
                                                                                }
                                                                            }
                                                                            continue;
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case 'B': // B-REMOTEHOST or B-SERVERPORT
                if (l.length > 13 && l[1] == '-') {
                    if (l[2] == 'R') {
                        if (l[3] == 'E') {
                            if (l[4] == 'M') {
                                if (l[5] == 'O') {
                                    if (l[6] == 'T') {
                                        if (l[7] == 'E') {
                                            if (l[8] == 'H') {
                                                if (l[9] == 'O') {
                                                    if (l[10] == 'S') {
                                                        if (l[11] == 'T') {
                                                            if (l[12] == ':') {
                                                                int i = 13;
                                                                while (i < l.length && l[i] == ' ') {
                                                                    ++i;
                                                                } // skip SPACES
                                                                if (i < l.length) {
                                                                    proto.setRemoteAddress(new String(l, 0, i, l.length
                                                                            - i));
                                                                }
                                                                continue;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (l[2] == 'S') {
                        if (l[3] == 'E') {
                            if (l[4] == 'R') {
                                if (l[5] == 'V') {
                                    if (l[6] == 'E') {
                                        if (l[7] == 'R') {
                                            if (l[8] == 'P') {
                                                if (l[9] == 'O') {
                                                    if (l[10] == 'R') {
                                                        if (l[11] == 'T') {
                                                            if (l[12] == ':') {
                                                                int i = 13;
                                                                while (i < l.length && l[i] == ' ') {
                                                                    ++i;
                                                                } // skip SPACES
                                                                if (i < l.length) {
                                                                    port = line.substring(i).toInteger();
                                                                    if (port <= 0) {
                                                                        port = 80;
                                                                    }
                                                                }
                                                                continue;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (l[4] == 'C') {
                                if (l[5] == 'U') {
                                    if (l[6] == 'R') {
                                        if (l[7] == 'E') {
                                            isSecure = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case 'C':
            case 'c': // COOKIE, CONTENT-TYPE, CONTENT-LENGTH
                b1 = l[1];
                if (l.length > 7 && (b1 == 'O' || b1 == 'o')) {
                    b1 = l[2];
                    if (b1 == 'O' || b1 == 'o') {
                        b1 = l[3];
                        if (b1 == 'K' || b1 == 'k') {
                            b1 = l[4];
                            if (b1 == 'I' || b1 == 'i') {
                                b1 = l[5];
                                if (b1 == 'E' || b1 == 'e') {
                                    if (l[6] == ':') {
                                        int i = 7;
                                        // add all cookies
                                        while (i < l.length) {
                                            while (i < l.length && l[i] == ' ') {
                                                ++i;
                                            } // skip SPACES
                                            int s = i;
                                            while (i < l.length && l[i] != '=') {
                                                ++i;
                                            } // search '='
                                            int m = i++;
                                            if (i < l.length) {
                                                while (i < l.length && l[i] != ';') {
                                                    ++i;
                                                } // search ';'
                                                String name = new String(l, 0, s, m - s);
                                                ++m;
                                                int e = i;
                                                if (l[m] == '"' || l[m] == '\'') {
                                                    ++m;
                                                    --e;
                                                }
                                                String val = new String(l, 0, m, e - m);
                                                inCookies.add(new Cookie(name, val));
                                                ++i;
                                            }
                                        }
                                        continue;
                                    }
                                }
                            }
                        }
                    } else if (l.length > 13 && (b1 == 'N' || b1 == 'n')) {
                        b1 = l[3];
                        if (b1 == 'T' || b1 == 't') {
                            b1 = l[4];
                            if (b1 == 'E' || b1 == 'e') {
                                b1 = l[5];
                                if (b1 == 'N' || b1 == 'n') {
                                    b1 = l[6];
                                    if (b1 == 'T' || b1 == 't') {
                                        if (l[7] == '-') {
                                            b1 = l[8];

                                            if (b1 == 'T' || b1 == 't') {
                                                b1 = l[9];
                                                if (b1 == 'Y' || b1 == 'y') {
                                                    b1 = l[10];
                                                    if (b1 == 'P' || b1 == 'p') {
                                                        b1 = l[11];
                                                        if (b1 == 'E' || b1 == 'e') {
                                                            if (l[12] == ':') {
                                                                int i = 13;
                                                                // search for CHARSET=
                                                                while (i + 9 < l.length) {
                                                                    b1 = l[i++];
                                                                    if (b1 == 'C' || b1 == 'c') {
                                                                        b1 = l[i++];
                                                                        if (b1 == 'H' || b1 == 'h') {
                                                                            b1 = l[i++];
                                                                            if (b1 == 'A' || b1 == 'a') {
                                                                                b1 = l[i++];
                                                                                if (b1 == 'R' || b1 == 'r') {
                                                                                    b1 = l[i++];
                                                                                    if (b1 == 'S' || b1 == 's') {
                                                                                        b1 = l[i++];
                                                                                        if (b1 == 'E' || b1 == 'e') {
                                                                                            b1 = l[i++];
                                                                                            if (b1 == 'T' || b1 == 't') {
                                                                                                if (l[i++] == '=') {
                                                                                                    int s = i;
                                                                                                    while (i < l.length
                                                                                                            && l[i] != ';') {
                                                                                                        ++i;
                                                                                                    } // search ';'
                                                                                                    if (i > s) {
                                                                                                        String ce = new String(
                                                                                                                l, 0,
                                                                                                                s,
                                                                                                                i - s);
                                                                                                        charEncoding = ce;
                                                                                                        break;
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (l.length > 15 && (b1 == 'L' || b1 == 'l')) {
                                                b1 = l[9];
                                                if (b1 == 'E' || b1 == 'e') {
                                                    b1 = l[10];
                                                    if (b1 == 'N' || b1 == 'n') {
                                                        b1 = l[11];
                                                        if (b1 == 'G' || b1 == 'g') {
                                                            b1 = l[12];
                                                            if (b1 == 'T' || b1 == 't') {
                                                                b1 = l[13];
                                                                if (b1 == 'H' || b1 == 'h') {
                                                                    if (l[14] == ':') {
                                                                        int i = 15;
                                                                        while (i < l.length && l[i] == ' ') {
                                                                            ++i;
                                                                        } // skip SPACES
                                                                        inContentLength = line.substring(i).toInteger();
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }

            // just some HEADER
            int i = 1;
            for (; i < l.length && l[i] != ':'; ++i) {
            }
            if (i == l.length) {
                continue; // ignore junk
            }
            String key = new String(l, 0, 0, i);

            ++i;
            while (i < l.length && l[i] == ' ') {
                ++i;
            }
            String value = new String(l, 0, i, l.length - i);

            inHeaderNames.add(key);
            inHeaders.put(key.toUpperCase(), value);
            /*      
                  // line contains the rest
                  ByteRef k = line.nextWord(':');
                  if (k == null)
                    continue;
                  String key = k.toString();
                  String ku = k.toUpperCase().toString();
                  line.removeLeft();

                  if (DEBUG)
                    System.out.println(key + ": " + line);

                  // handle those headers which do NOT end in the inHeaders.
                  if (ku.equals("AUTHORIZATION"))
                  {
                    authType = line.nextWord(' ').toUpperCase();
                    if (authType.equals(BASIC))
                    {
                      byte b[] = line.toByteArray();
                      b = Mime.decode(b, 0, b.length);
                      line = new ByteRef(b);
                      remoteUser = line.nextWord(':');
                      remotePass = line;
                    }
                    continue;
                  }

                  if (key.equals("B-REMOTEHOST"))
                  {
                    remoteAddress = line.toString();
                    continue;
                  }

                  if (key.equals("B-SERVERPORT"))
                  {
                    port = line.toInteger();
                    if (port <= 0)
                      port = 80;
                    continue;
                  }

                  // add to inHeaders
                  if (inHeaders.get(ku) == null)
                  {
                    inHeaderNames.add(key);
                    inHeaders.put(ku, line.toString());
                  } else
                  {
                    String v2 = (String) inHeaders.get(ku);
                    v2 += "," + line.toString();
                    inHeaders.put(ku, v2);
                  }

                  // handler headers which also remain in the inHeaders
                  if (ku.equals("COOKIE"))
                  {
                    int sp = 0;
                    for (;;)
                    {
                      ByteRef n = line.nextWord('=');
                      if (n == null)
                        break;
                      ByteRef v = line.nextWord(';');
                      if (v == null)
                        break;
                      if (v.charAt(0) == '"')
                        v = v.trim('"');

                      if (n.equals(JSESSIONID))
                      {
                        sid = v.toString();
                        if (DEBUG)
                          System.out.println("got jsessionid: " + sid);
                        fromC = true;
                        continue;
                      }

                      javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(n.toString(), v.toString());
                      inCookies.add(c);
                    }
                    continue;
                  }

                  if (ku.equals("ACCEPT-LANGUAGE"))
                  {
                    for (ByteRef vr = line.nextWord(','); vr != null; vr = line.nextWord(','))
                    {
                      vr = vr.trim();
                      ByteRef la = vr.nextWord('-');
                      locales.addElement(new Locale(la.toString(), vr.toString()));
                    }
                    continue;
                  }

                  if (ku.equals("CONTENT-TYPE"))
                  {
                    ByteRef ct = line.nextWord(';');
                    while (line.length() > 0)
                    {
                      ByteRef part = line.nextWord(';').trim();

                      ByteRef r = part.nextWord('=');
                      if (r.equalsIgnoreCase("CHARSET"))
                      {
                        charEncoding = part;
                      } else
                      {
                        ct = ct.append(";").append(r.append("=").append(part));
                      }
                    }
                    inHeaders.put(ku, ct);
                  }
            */
        }

        return br;
    }

    /**
     * Parse the parseRequestLine. GET /dsf/dsf/dsf;JSESSION=SADASDASDASD HTTP/1.0
     * 
     * Not the nicest implementation, but performant.
     */
    void parseRequestLine(ByteRef requestLine) {
        /**/
        byte[] b = requestLine.toByteArray();
        int bLen = b.length;
        int i = 0, j = 0;
        // search end of method
        for (; i < bLen; ++i) {
            if (b[i] == ' ') {
                break;
            }
        }
        method = new String(b, 0, j, i);
        for (; i < bLen; ++i) {
            if (b[i] != ' ') {
                break;
            }
        }
        // start of url
        j = i;
        // end of url
        int query = -1;
        int lastSemi = -1;
        int k = i;
        int count = 0;
        for (; i < bLen; ++i, ++k) {
            byte a = b[i];
            if (a == ';' && query < 0) {
                lastSemi = k;
            } else if (a == '?') {
                query = k;
            }
            if (a == ' ') {
                break;
            }
            if (query < 0) {
                if (a == '+') {
                    a = ' ';
                } else if (a == '%') {
                    int n;
                    a = b[++i];
                    if (a >= '0' && a <= '9') {
                        n = a - '0';
                    } else if (a >= 'A' && a <= 'F') {
                        n = a - 'A' + 10;
                    } else if (a >= 'a' && a <= 'f') {
                        n = a - 'a' + 10;
                    } else {
                        break;
                    }
                    n <<= 4;
                    a = b[++i];
                    if (a >= '0' && a <= '9') {
                        n |= a - '0';
                    } else if (a >= 'A' && a <= 'F') {
                        n |= a - 'A' + 10;
                    } else if (a >= 'a' && a <= 'f') {
                        n |= a - 'a' + 10;
                    } else {
                        break;
                    }
                    a = (byte) n;
                }
                if (count == 0 && a == '/') {
                    count = 1;
                } else if (count == 1 && a == '.') {
                    count = 2;
                } else if (count == 2 && a == '.') {
                    count = 3;
                } else if (count == 3 && a == '/') {
                    count = 1;
                    k -= 3;
                    while (k > j && b[k - 1] != '/') {
                        --k;
                    }
                } else {
                    count = 0;
                }
            }
            b[k] = a;
        }

        if (query > 0) {
            queryString = new String(b, 0, query + 1, k - query - 1);
            k = query;
        }

        if (lastSemi > 0) {
            if (lastSemi + 12 < i) {
                if (b[lastSemi + 1] == 'j' && b[lastSemi + 2] == 's' && b[lastSemi + 3] == 'e'
                        && b[lastSemi + 4] == 's' && b[lastSemi + 5] == 's' && b[lastSemi + 6] == 'i'
                        && b[lastSemi + 7] == 'o' && b[lastSemi + 8] == 'n' && b[lastSemi + 9] == 'i'
                        && b[lastSemi + 10] == 'd' && b[lastSemi + 11] == '=') {
                    fromU = true;
                    sid = new String(b, 0, lastSemi + 12, k - lastSemi - 12);
                }
                k = lastSemi;
            }
        }

        path = new ByteRef(b, j, k);

        for (; i < bLen; ++i) {
            if (b[i] != ' ') {
                break;
            }
        }
        protocol = new ByteRef(b, i, bLen);

        // get host name
        String tfh = inHeaders.get("HOST");
        if (tfh != null) {
            fullhost = tfh;
        }
        if (fullhost != null) {
            //      fullhost.toLowerCase();
            int spx = fullhost.indexOf(':');
            if (spx > 0) {
                host = fullhost.substring(0, spx);
            } else {
                host = fullhost;
            }
        } else {
            host = "127.0.0.1";
        }
        if (DEBUG) {
            System.out.println("host = " + host);
        }

    }

    /**
     * Returns the value of the specified request header
     * 
     * @param name
     * @return
     */
    public String getHeader(String name) {
        Object o = inHeaders.get(name.toUpperCase());
        if (o == null) {
            return null;
        }
        if (!(o instanceof String)) {
            return o.toString();
        }
        return (String) o;
    }

}