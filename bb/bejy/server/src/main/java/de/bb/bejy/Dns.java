/******************************************************************************
 * $Source: /export/CVS/java/de/bb/bejy/server/src/main/java/de/bb/bejy/Dns.java,v $
 * $Revision: 1.28 $
 * $Date: 2013/11/28 10:46:34 $
 * $Author: bebbo $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2000.
 * All rights reserved
 *
 *
 ******************************************************************************
    NON COMMERCIAL PUBLIC LICENSE
 ******************************************************************************
  
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:
  
      1. Every product and solution using this software, must be free
        of any charge. If the software is used by a client part, the
        server part must also be free and vice versa.
  
      2. Each redistribution must retain the copyright notice, and
        this list of conditions and the following disclaimer.
  
      3. Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer in
        the documentation and/or other materials provided with the
        distribution.
  
      4. All advertising materials mentioning features or use of this
        software must display the following acknowledgment:
          "This product includes software developed by BebboSoft,
            written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"
  
      5. Redistributions of any form whatsoever must retain the following
        acknowledgment:
          "This product includes software developed by BebboSoft,
          written by Stefan Bebbo Franke. (http://www.bebbosoft.de)"
  
 ******************************************************************************
  DISCLAIMER OF WARRANTY
  
  Software is provided "AS IS," without a warranty of any kind.
  You may use it on your own risk.
  
 ******************************************************************************
  LIMITATION OF LIABILITY
  
  I SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY YOU OR ANY THIRD PARTY
  AS A RESULT OF USING OR DISTRIBUTING SOFTWARE. IN NO EVENT WILL I BE LIABLE
  FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
  CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
  OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
  SOFTWARE, EVEN IF I HAVE ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
  
 *****************************************************************************
  COPYRIGHT
  
  (c) 2003 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.bejy;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import de.bb.log.Logger;
import de.bb.util.LogFile;
import de.bb.util.Misc;
import de.bb.util.MultiMap;
import de.bb.util.SessionManager;
import de.bb.util.XmlFile;

/**
 * Provides some DNS functionality.
 * 
 * @author bebbo
 */
public class Dns extends Configurable {
	private final static Logger LOG = Logger.getLogger(Dns.class);

	private final static String PROPERTIES[][] = { { "server", "domain name or ip address of the used DNS server" },
			{ "defaultSpf", "a spf string which is used for domains without an spf entry", "v=spf1 a mx ~all" } };

	public final static int TYPE_A = 1;

	public final static int TYPE_NS = 2;

	// private final static int TYPE_CNAME = 5;
	public final static int TYPE_SOA = 6;

	public final static int TYPE_PTR = 12;

	public final static int TYPE_MX = 15;

	public final static int TYPE_TXT = 16;

	public final static int TYPE_SPF = 99;

	private final static LinkedList<RD> EMPTY = new LinkedList<RD>();

	private static final HashMap<String, LinkedList<RD>> STATIC = new HashMap<String, LinkedList<RD>>();

	private static int uniqueid = (int) System.currentTimeMillis();

	private ArrayList<String> nameServers = new ArrayList<String>();

	private SessionManager<String, Object> sMan;

	private long xspfLastModified;

	private XmlFile extendSpf;

	private long ospfLastModified;

	private XmlFile overrideSpf;

	Dns() {
		sMan = new SessionManager<String, Object>(5 * 1000, 1000);
		init("DNS", PROPERTIES);
	}

	public void activate(LogFile logFile) throws Exception {
		final String nameServer = getProperty("server", "");
		nameServers.clear();
		for (final StringTokenizer st = new StringTokenizer(nameServer, " \t,;\r\n"); st.hasMoreElements();) {
			nameServers.add(st.nextToken());
		}
		LOG.setLevel(Logger.INFO);
		LOG.info("using name servers: " + nameServers);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.bb.bejy.Configurable#update(de.bb.util.LogFile)
	 */
	public void update(LogFile logFile) throws Exception {
		activate(logFile);
	}

	/**
	 * Remove the cached mail exchangers for the domain.
	 * 
	 * @param domain
	 *            the domain
	 */
	public void removeMx(String domain) {
		String key = domain + '\15';
		sMan.remove(key);
	}

	/**
	 * Get a map of mail exchangers (MX) by domain name. The map is sorted by
	 * priority.
	 * 
	 * @param domain
	 *            the domain name
	 * @return a Map with the mail exchangers.
	 */
	public List<String> getMXfromDomain(String domain) {
		String key = domain + '\15';
		List<String> list = (List<String>) sMan.get(key);
		if (list != null)
			return list;
		LinkedList<RD> ll = queryDns(domain, TYPE_MX);
		if (ll == null)
			ll = new LinkedList<RD>();

		// detect minimal TTL
		int ttl = Integer.MAX_VALUE;
		Map<Integer, RD> map = new MultiMap<Integer, RD>();
		// sort entries by Priority
		for (Iterator<RD> i = ll.iterator(); i.hasNext();) {
			RD rdData = i.next();
			if (ttl > rdData.getTtl())
				ttl = rdData.getTtl();
			map.put(new Integer(rdData.getPriority()), rdData);
		}

		if (map.isEmpty()) {
			// also add domain to prevent empty lists
			RD rdData = new RD(domain);
			map.put(new Integer(Integer.MAX_VALUE), rdData);
		}
		// reduce TTL to 1 mins if none was set
		if (ttl == Integer.MAX_VALUE)
			ttl = 1 * 60 * 1000;

		list = new ArrayList<String>(map.size());
		for (Iterator<RD> i = map.values().iterator(); i.hasNext();) {
			RD rd = i.next();
			list.add(rd.getData());
		}

		if (!list.isEmpty()) {
			sMan.put(key, list);
			sMan.touch(key, ttl);
		}
		return list;
	}

	/**
	 * Get a map of mail exchangers (MX) by domain name. The map is sorted by
	 * priority.
	 * 
	 * @param domain
	 *            the domain name
	 * @return a Map with the mail exchangers.
	 */
	public List<String> getRealMXfromDomain(String domain) {
		String key = domain + "\15\1";
		List<String> list = (List<String>) sMan.get(key);
		if (list != null)
			return list;
		LinkedList<RD> ll = queryDns(domain, TYPE_MX);
		if (ll == null)
			ll = new LinkedList<RD>();

		if (ll.size() == 0)
			return Collections.emptyList();

		// detect minimal TTL
		int ttl = Integer.MAX_VALUE;
		Map<Integer, RD> map = new MultiMap<Integer, RD>();
		// sort entries by Priority
		for (Iterator<RD> i = ll.iterator(); i.hasNext();) {
			RD rdData = i.next();
			if (ttl > rdData.getTtl())
				ttl = rdData.getTtl();
			map.put(new Integer(rdData.getPriority()), rdData);
		}

		// create result
		list = new ArrayList<String>(map.size());

		for (Iterator<RD> i = map.values().iterator(); i.hasNext();) {
			RD rd = i.next();
			list.add(rd.getData());
		}

		sMan.put(key, list);
		sMan.touch(key, ttl);
		return list;
	}

	/**
	 * @param the
	 *            key to cache
	 * @param ll
	 * @return
	 */
	private String getFirst(String key, LinkedList<RD> ll) {
		if (ll == null || ll.size() == 0)
			return null;
		RD rdData = ll.getFirst();
		String ret = rdData.getData();
		sMan.put(key, ret);
		sMan.touch(key, rdData.getTtl());
		return ret;
	}

	/**
	 * @param the
	 *            key to cache
	 * @param ll
	 * @return
	 */
	private String getFirstWith(String key, LinkedList<RD> ll, String start) {
		if (ll == null || ll.size() == 0)
			return null;
		RD rdData = null;
		for (Iterator<RD> i = ll.iterator(); i.hasNext();) {
			RD rd = i.next();
			if (rd.getData().startsWith(start)) {
				rdData = rd;
				break;
			}
		}
		if (rdData == null)
			return null;
		String ret = rdData.getData();
		sMan.put(key, ret);
		sMan.touch(key, rdData.getTtl());
		return ret;
	}

	/**
	 * Retrieves the domain name for a given ip address.
	 * 
	 * @param ip
	 *            the ip address.
	 * @return the domain name - if found or null.
	 */
	public String getDomainFromIp(String ip) {
		String key = ip + '\12';
		String ret = (String) sMan.get(key);
		if (ret != null)
			return ret;
		String domain = ip2Domain(ip);
		LinkedList<RD> ll = queryDns(domain, TYPE_PTR);
		return getFirst(key, ll);
	}

	/**
	 * Retrieves the domain names for a given ip address.
	 * 
	 * @param ip
	 *            the ip address.
	 * @return the domain name - if found or null.
	 */
	public Iterator<String> getDomainsFromIp(String ip) {
		String key = ip + "\12\1";
		List<String> ret = (List<String>) sMan.get(key);
		if (ret != null)
			return ret.iterator();
		String domain = ip2Domain(ip);
		List<RD> lll = queryDns(domain, TYPE_PTR);
		return toStringListIterator(lll, key);
	}

	/**
	 * Retrieves the ip address for a given domain name.
	 * 
	 * @param domain
	 *            the domain name
	 * @return the ip address if found or null.
	 */
	public String getIpFromDomain(String domain) {
		String key = domain + '\1';
		String ret = (String) sMan.get(key);
		if (ret != null)
			return ret;
		LinkedList<RD> ll = queryDns(domain, TYPE_A);
		return getFirst(key, ll);
	}

	/**
	 * Retrieves the TXT for a given domain name.
	 * 
	 * @param domain
	 *            the domain name
	 * @return the ip address if found or null.
	 */
	public String getTextFromDomain(String domain) {
		String key = domain + '\16';
		String ret = (String) sMan.get(key);
		if (ret != null)
			return ret;
		LinkedList<RD> ll = queryDnsAll(domain, TYPE_TXT);
		return getFirst(key, ll);
	}

	/**
	 * Retrieves the SPF for a given domain name which starts with the specified
	 * start.
	 * 
	 * @param domain
	 *            the domain name
	 * @param start
	 *            the match
	 * @return the ip address if found or null.
	 */
	public String getSpfFromDomain(String domain, String start) {
		String key = domain + "\123" + start + "\123";
		String ret = (String) sMan.get(key);
		if (ret != null)
			return ret;
		LinkedList<RD> ll = queryDnsAll(domain, TYPE_SPF);
		return getFirstWith(key, ll, start);
	}

	/**
	 * Retrieves the TXT for a given domain name which starts with the specified
	 * start.
	 * 
	 * @param domain
	 *            the domain name
	 * @param start
	 *            the match
	 * @return the ip address if found or null.
	 */
	public String getTextFromDomain(String domain, String start) {
		String key = domain + "\16" + start + "\16";
		String ret = (String) sMan.get(key);
		if (ret != null)
			return ret;
		LinkedList<RD> ll = queryDnsAll(domain, TYPE_TXT);
		return getFirstWith(key, ll, start);
	}

	/**
	 * Retrieves the SOA information for a given domain name.
	 * 
	 * @param domain
	 *            the domain name
	 * @return the ip address if found or null.
	 */
	public String getSoaFromDomain(String domain) {
		String key = domain + '\6';
		String ret = (String) sMan.get(key);
		if (ret != null)
			return ret;
		LinkedList<RD> ll = queryDns(domain, TYPE_SOA);
		return getFirst(key, ll);
	}

	/**
	 * Retrieves the ip addresses for a given domain name.
	 * 
	 * @param domain
	 *            the domain name
	 * @return the ip address if found or null.
	 */
	public Iterator<String> getIpsFromDomain(String domain) {
		String key = domain + "\1\1";
		List<String> ret = (List<String>) sMan.get(key);
		if (ret != null)
			return ret.iterator();
		List<RD> lll = queryDns(domain, TYPE_A);
		return toStringListIterator(lll, key);
	}

	/**
	 * Retrieve the NS for the domain
	 * 
	 * @param resolvedDomain
	 */
	public Iterator<String> getNsFromDomain(String domain) {
		String key = domain + "\2";
		List<String> ret = (List<String>) sMan.get(key);
		if (ret != null)
			return ret.iterator();
		List<RD> lll = queryDns(domain, TYPE_NS);
		return toStringListIterator(lll, key);
	}

	private Iterator<String> toStringListIterator(List<RD> lll, String key) {
		ArrayList<String> ll = new ArrayList<String>(lll.size());
		for (Iterator<RD> i = lll.iterator(); i.hasNext();) {
			RD rd = i.next();
			ll.add(rd.getData());
		}
		if (!ll.isEmpty())
			sMan.put(key, ll);
		return ll.iterator();
	}

	private static String ip2Domain(String ipIn) {
		String ip = "";
		for (; ipIn.length() > 0;) {
			int dot = ipIn.lastIndexOf('.') + 1;
			ip += ipIn.substring(dot) + ".";

			if (dot > 0)
				--dot;
			ipIn = ipIn.substring(0, dot);
		}
		ip += "in-addr.arpa";
		return ip;
	}

	/**
	 * Query the DNS multiple times.
	 * 
	 * @param name
	 *            the name to query
	 * @param inType
	 *            the type to Query.
	 * @return a LinkedList containing all entries with different responses.
	 */
	private LinkedList<RD> queryDnsAll(String name, int inType) {
		HashSet<RD> unique = new HashSet<RD>();
		int n = 0;
		Loop: for (;;) {
			LinkedList<RD> ll = queryDns(name, inType);
			if (ll.isEmpty() && ++n >= 2)
				break;
			for (Iterator<RD> i = ll.iterator(); i.hasNext();) {
				RD rd = i.next();
				if (!unique.add(rd))
					break Loop;
			}
		}
		LinkedList<RD> ll = new LinkedList<RD>();
		ll.addAll(unique);
		return ll;
	}

	/**
	 * Query the DNS.
	 * 
	 * @param domain
	 *            the name to query
	 * @param inType
	 *            the type to Query.
	 * @return a LinkedList containing all entries for 1 response.
	 */
	private LinkedList<RD> queryDns(String domain, int inType) {
		if (nameServers.isEmpty()) {
			final String key = domain + "#" + inType;
			final LinkedList<RD> r = STATIC.get(key);
			if (r == null)
				return EMPTY;
			return r;
		}
		DatagramSocket local = null;
		try {
			byte sendData[] = new byte[512];
			byte responseData[] = new byte[1400];

			/*
			 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | ID |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ |QR| Opcode
			 * |AA|TC|RD|RA| Z | RCODE |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | QDCOUNT |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | ANCOUNT |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | NSCOUNT |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | ARCOUNT |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
			 */
			int sendLength = 0;
			sendData[sendLength++] = 0;
			sendData[sendLength++] = 1; // ID
			sendData[sendLength++] = 1;
			sendData[sendLength++] = 0; // QR...
			sendData[sendLength++] = 0;
			sendData[sendLength++] = 1; // QDCOUNT
			sendData[sendLength++] = 0;
			sendData[sendLength++] = 0; // ANCOUNT
			sendData[sendLength++] = 0;
			sendData[sendLength++] = 0; // NSCOUNT
			sendData[sendLength++] = 0;
			sendData[sendLength++] = 0; // ARCOUNT

			/*
			 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | | / QNAME / /
			 * / +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | QTYPE |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+ | QCLASS |
			 * +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
			 */

			sendLength = insert(sendData, sendLength, domain); // QNAME
			sendData[sendLength++] = 0;
			sendData[sendLength++] = (byte) inType;
			sendData[sendLength++] = 0;
			sendData[sendLength++] = 1; // QCLASS: arpa = 1

			// =====
			local = new DatagramSocket(); //
			DatagramPacket p = new DatagramPacket(sendData, sendLength);
			DatagramPacket a = new DatagramPacket(responseData, 1400);

//			Misc.dump("->", System.out, sendData, sendLength);

			for (final String nameServer : nameServers) {

				InetAddress ia = InetAddress.getByName(nameServer);
				p.setAddress(ia);
				p.setPort(53);

				int aCount = -1;
				// send 5 times
				int k = 0;
				for (; k < 3; ++k) {
					int id = getNextId();
					sendData[0] = (byte) (id >> 8);
					sendData[1] = (byte) id;
					local.send(p);
					local.setSoTimeout(500);
					local.receive(a);

					if (responseData[0] == sendData[0] && responseData[1] == sendData[1]) {
//						Misc.dump(System.out, responseData);
						aCount = ((responseData[6] & 0xff) << 8) | (responseData[7] & 0xff);
						break;
					}
					Thread.sleep(250);
				}
				
				
				if (aCount == 0) {
					if ((responseData[2] & 2) == 0)
						break;
					
					// truncated message -> use TCP
					
					final Socket socket = new Socket(nameServer, 53);
					final OutputStream os = socket.getOutputStream();
					
					System.arraycopy(sendData, 0, sendData, 2, sendLength);
					
					sendData[0] = (byte) (sendLength >> 8);
					sendData[1] = (byte) sendLength;
					
					os.write(sendData, 0, sendLength + 2);
					InputStream is = socket.getInputStream();
					int responseLength = (is.read() & 0xff) << 8;
					responseLength += (is.read() & 0xff);
					if (responseLength > responseData.length)
						responseData = new byte[responseLength];
					for(int pos = 0; pos < responseLength;) {
						int read = is.read(responseData, pos, responseLength - pos);
						if (read == 0)
							break;
						pos += read;
					}
					socket.close();
					
					if (responseData[0] != sendData[2] || responseData[1] != sendData[3])
						break;
					
					//	Misc.dump(System.out, responseData);
					aCount = ((responseData[6] & 0xff) << 8) | (responseData[7] & 0xff);
				}

				LOG.debug("got {0} entries", aCount);
				LinkedList<RD> ll = new LinkedList<RD>();
				
				int offset = sendLength;
				while (aCount-- > 0) {
					RD rdData = new RD(responseData, offset);
					if (rdData.getType() == inType) {
						ll.add(rdData);
					}
					offset += rdData.getLength();
					if (offset >= responseData.length) {
						offset = responseData.length;
						break;
					}
				}
//				Misc.dump("<-", System.out, responseData, offset);
				return ll;
			}
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);
		} finally {
			if (local != null)
				local.close();
		}
		return EMPTY;

	}

	private static synchronized int getNextId() {
		return ++uniqueid;
	}

	private static int insert(byte b[], int i, String ip) {
		for (; ip.length() > 0;) {
			int idx = ip.indexOf('.');
			if (idx == -1)
				idx = ip.length();
			String part = ip.substring(0, idx);
			if (idx < ip.length())
				++idx;
			ip = ip.substring(idx);

			b[i++] = (byte) part.length();
			for (int j = 0; j < part.length(); ++j)
				b[i++] = (byte) part.charAt(j);
		}
		b[i++] = 0;
		return i;
	}

	/**
	 * reads a name from buffer and updates the specified offset.
	 * 
	 * @param b
	 *            the DNS message data
	 * @param i
	 *            current index
	 * @param res
	 *            holds the new position
	 * @return the name
	 */
	static String resolveName(byte[] b, int i, int[] res) {
		String s = "";
		for (;;) {
			int c = 0xff & b[i++];
			if (c == 0)
				break;
			if (s.length() > 0)
				s += ".";
			if (c == 0xc0) {
				s += resolveName(b, 0xff & b[i++], null);
				break;
			}
			if (c > 0xc0) {
				s += resolveName(b, i - 1 - (c - 0xc0), null);
				break;
			}
			while (c-- > 0) {
				s += (char) (0xff & b[i++]);
			}
		}
		if (res != null)
			res[0] = i;
		return s;
	}

	static class RD {
		private String name;

		private int type;

		// private int clazz;
		private int ttl;

		private String data;

		private int length;

		private int prio;

		RD(String name) {
			this.name = name;
			this.data = name;
			ttl = 5 * 60 * 1000; // 5 mins
		}

		RD(byte b[], int xoffset) {
			int ii[] = new int[1];
			name = resolveName(b, xoffset, ii);
			if (name == null)
				return;
			int tOffset = ii[0];
			type = ((b[tOffset] & 0xff) << 8) | (b[tOffset + 1] & 0xff);
			tOffset += 2;
			// clazz = ((b[tOffset] & 0xff) << 8) | (b[tOffset + 1] & 0xff);
			tOffset += 2;
			ttl = ((b[tOffset] & 0xff) << 24) | ((b[tOffset + 1] & 0xff) << 16) | ((b[tOffset + 2] & 0xff) << 8)
					| (b[tOffset + 3] & 0xff);
			tOffset += 4;

			int rLen = ((b[tOffset] & 0xff) << 8) | (b[tOffset + 1] & 0xff);
			tOffset += 2;
			switch (type) {
			case TYPE_A: {
				int a1 = 0xff & b[tOffset];
				int a2 = 0xff & b[tOffset + 1];
				int a3 = 0xff & b[tOffset + 2];
				int a4 = 0xff & b[tOffset + 3];
				data = a1 + "." + a2 + "." + a3 + "." + a4;
			}
				break;
			case TYPE_SPF:
			case TYPE_TXT:
				data = "";
				do {
					length = b[tOffset++] & 0xff;
					data += new String(b, 0, tOffset, length);
					tOffset += length;
					rLen -= length + 1;
				} while (rLen > 0);
				break;
			case TYPE_MX:
				prio = ((b[tOffset] & 0xff) << 8) | (b[tOffset + 1] & 0xff);
				tOffset += 2;
				// fallthrough
				// case TYPE_PTR:
				// case TYPE_CNAME:
			default:
				data = resolveName(b, tOffset, null);
				break;
			}
			length = tOffset - xoffset + rLen;
		}

		/**
		 * @return
		 */
		String getData() {
			return data;
		}

		/**
		 * @return
		 */
		int getLength() {
			return length;
		}

		/**
		 * @return
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return
		 */
		public int getTtl() {
			if (ttl == 0)
				ttl = 60 * 60 * 1000;
			return ttl;
		}

		/**
		 * @return
		 */
		int getType() {
			return type;
		}

		/**
		 * @return
		 */
		int getPriority() {
			return prio;
		}

		public String toString() {
			return name + ":" + ttl + " = " + data;
		}

		public boolean equals(Object o) {
			if (!(o instanceof RD))
				return false;
			RD rd = (RD) o;
			return this.type == rd.type && this.length == rd.length && this.prio == rd.prio && this.name.equals(rd.name)
					&& this.data.equals(rd.data);
		}

		public int hashCode() {
			return name.hashCode() * 17 + data.hashCode() * 31 + prio * 3 + length * 41 + type * 91;
		}
	}

	/**
	 * Returns an iterator over all ips from all mx of the specified domain.
	 * 
	 * @param domain
	 *            the domain to query the mx IP adresses
	 * @return an iterator containing Strings with the IP address
	 *         aaa.bbb.ccc.ddd
	 */
	public Iterator<String> getMxIps(String domain) {
		HashSet<String> ips = new HashSet<String>();
		for (Iterator<String> i = getMXfromDomain(domain).iterator(); i.hasNext();) {
			for (Iterator<String> j = getIpsFromDomain(i.next()); j.hasNext();) {
				ips.add(j.next());
			}
		}
		return ips.iterator();
	}

	public String getSpfFromDomain(String domain) {
		String spf = getOverriddenSpf(domain);
		if (spf != null)
			return spf;

		spf = getSpfFromDomain(domain, "v=spf1");

		LOG.info("SPF: " + domain + " --> " + spf);
		if (spf != null)
			return spf;

		spf = getTextFromDomain(domain, "v=spf1");

		LOG.info("TXT: " + domain + " --> " + spf);
		if (spf != null)
			return spf;

		spf = getExtendedSpf(domain);
		if (spf != null)
			return spf;

		return getProperty("defaultSpf", "v=spf1 a/23 mx/23 ~all");
	}

	public String getOverriddenSpf(String domain) {
		File f = new File("overrideSpf.xml");
		if (!f.exists())
			return null;

		long dt = f.lastModified();
		if (dt != ospfLastModified) {
			overrideSpf = new XmlFile();
			overrideSpf.readFile(f.getAbsolutePath());
			ospfLastModified = dt;
		}
		return overrideSpf.getString("/spf/\\domain\\" + domain, "value", null);
	}

	public String getExtendedSpf(String domain) {
		File f = new File("extendSpf.xml");
		if (!f.exists())
			return null;

		long dt = f.lastModified();
		if (dt != xspfLastModified) {
			extendSpf = new XmlFile();
			extendSpf.readFile(f.getAbsolutePath());
			xspfLastModified = dt;
		}
		return extendSpf.getString("/spf/\\domain\\" + domain, "value", null);
	}

	/**
	 * Used e.g. for tests if no name server is configured.
	 * 
	 * @param string
	 */
	public void addStaticEntry(int type, String domain, String value) {
		RD rd = new RD(value);
		String key = domain + "#" + type;
		LinkedList<RD> l = STATIC.get(key);
		if (l == null) {
			l = new LinkedList<Dns.RD>();
			STATIC.put(key, l);
		}
		l.add(rd);
	}

	/**
	 * / public static void main(String args[]) { try { String dnsIp =
	 * "192.168.0.1"; // String dns = "81.169.163.106"; dnsIp = "8.8.8.8";
	 * 
	 * if (args.length > 0) dnsIp = args[0]; System.out.println("using: " +
	 * dnsIp);
	 * 
	 * // new T(args[0]).start();
	 * 
	 * if (args.length < 2) { args = new String[] { dnsIp, "amazon.com" }; args
	 * = new String[] { dnsIp, "b.gillastore.amazplus.com" }; }
	 * 
	 * for (int i = 1; i < args.length; ++i) { for (int j = 0; j < 5; ++j) { Dns
	 * dns = new Dns(); dns.setProperty("server", dnsIp); LogFile log = new
	 * LogFile("*"); dns.activate(log); System.out.println(args[i]); String name
	 * = args[i]; System.out.println(args[i] + " -> " + name); if (name == null)
	 * name = args[i]; String text = dns.getTextFromDomain(name, "v=spf1");
	 * System.out.println("TXT: " + name + " -> " + text); // text =
	 * dns.getSoaFromDomain(name); // System.out.println("SOA: " + name + " -> "
	 * + text); // String ip = dns.getIpFromDomain(name); // System.out.println(
	 * "IP: " + name + " -> " + ip); } } } catch (Exception ex) {
	 * ex.printStackTrace(); } } /
	 **/
}
/******************************************************************************
 * $Log: Dns.java,v $ Revision 1.28 2013/11/28 10:46:34 bebbo
 * 
 * @N DNS supports now multiple DNS servers
 * @B enhanced DNS query handling in case of missing responses Revision 1.27
 *    2013/09/01 08:58:33 bebbo
 * 
 * @B splitted TXT records are read correctly now Revision 1.26 2013/06/18
 *    13:23:37 bebbo
 * 
 * @I preparations to use nio sockets
 * @V 1.5.1.68 Revision 1.25 2012/08/11 17:03:52 bebbo
 * 
 * @I typed collections Revision 1.24 2011/09/16 16:14:34 bebbo
 * 
 * @D removed DEBUG output Revision 1.23 2011/05/20 09:43:02 bebbo
 * 
 * @R change xml file names to extend or override SPF: extendSpf.xml and
 *    overrideSpf.xml Revision 1.22 2010/07/08 18:14:35 bebbo
 * 
 * @N added a function to get the real MX without adding the server name itself
 * 
 *    Revision 1.21 2010/07/01 11:56:13 bebbo
 * @R removed some permission checks
 * @R changed default SPF to be more strict
 * @R an 5xx error now stops resending e-mails
 * 
 *    Revision 1.20 2010/07/01 10:10:09 bebbo
 * @N added overriddenSpf.xml to override SPF definitions
 * 
 *    Revision 1.19 2010/04/11 17:57:22 bebbo
 * @B removed PTR from default SPF
 * 
 *    Revision 1.18 2010/04/11 12:41:28 bebbo
 * @B fixed SPF handling. now all TXT entries are fetched and the valid one is
 *    taken
 * 
 *    Revision 1.17 2010/04/10 12:11:38 bebbo
 * @R default SPF now yields ~ instead of -
 * 
 *    Revision 1.16 2009/11/18 08:05:11 bebbo
 * @L log the used name server
 * 
 *    Revision 1.15 2008/06/04 06:41:45 bebbo
 * @N added support for a local spf file to maintain custom spf entries:
 *    extendedSpf.xml
 * 
 *    Revision 1.14 2008/03/13 17:21:22 bebbo
 * @D removed SPF output to log file
 * 
 *    Revision 1.13 2008/01/17 17:21:21 bebbo
 * @N added a new method to retrieve the SOA information
 * 
 *    Revision 1.12 2007/08/09 16:06:55 bebbo
 * @I integrated new SSL implementation
 * 
 *    Revision 1.11 2007/04/13 14:21:28 bebbo
 * @R added logging into global log file
 * 
 *    Revision 1.10 2007/01/18 22:10:14 bebbo
 * @N added default SPF setting
 * 
 *    Revision 1.9 2007/01/18 21:42:13 bebbo
 * @N more methods to support SPF
 * 
 *    Revision 1.8 2006/03/17 11:28:43 bebbo
 * @B closing UDPSockets in finally block
 * 
 *    Revision 1.7 2006/02/06 09:12:43 bebbo
 * @B getMXforDomain now always returns a list with at least the domain inside.
 * 
 *    Revision 1.6 2004/04/16 13:39:05 bebbo
 * @O removed unused variables
 * 
 *    Revision 1.5 2003/06/24 19:47:34 bebbo
 * @R updated build.xml and tools
 * @C better comments - less docheck mournings
 * 
 *    Revision 1.4 2003/06/24 09:37:08 bebbo
 * @R getMxFromDomain now returns a sorted List
 * 
 *    Revision 1.3 2003/06/18 13:54:46 bebbo
 * @R modified some descriptions
 * @R removed mainDomain from global
 * @B fixed activate of Dns
 * 
 *    Revision 1.2 2003/06/18 13:44:18 bebbo
 * @R modified some descriptions
 * 
 *    Revision 1.1 2003/06/17 10:18:10 bebbo
 * @N added Configurator and Configurable
 * @R redesign to utilize the new configuration scheme
 * 
 *    Revision 1.4 2003/05/13 15:42:07 bebbo
 * @N added config classes for future runtime configuration support
 * 
 *    Revision 1.3 2003/04/30 13:54:00 bebbo
 * @R completed this class
 * @N new functionality
 * 
 */
