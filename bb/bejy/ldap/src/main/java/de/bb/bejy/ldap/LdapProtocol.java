package de.bb.bejy.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.naming.SizeLimitExceededException;

import de.bb.bejy.Protocol;
import de.bb.bejy.Version;
import de.bb.security.Asn1;
import de.bb.security.SHA;
import de.bb.security.SecureRandom;
import de.bb.util.ByteRef;
import de.bb.util.LRUCache;
import de.bb.util.LogFile;
import de.bb.util.Mime;
import de.bb.util.Misc;
import de.bb.util.XmlFile;

public class LdapProtocol extends Protocol {

	private static final int BASEOBJECT = 0;
	private static final int SINGLELEVEL = 1;
	private static final int WHOLESUBTREE = 2;

	private final static byte RESPONSE_BIND[] = { (byte) 0x61, (byte) 0x0 };

	private final static byte RESPONSE_SEARCH[] = { (byte) 0x64, (byte) 0x0 };

	private final static byte RESPONSE_SEARCH_DONE[] = { (byte) 0x65, (byte) 0x0 };

	private final static byte RESPONSE_MODIFY[] = { (byte) 0x67, (byte) 0x0 };

	private final static byte RESPONSE_INSERT[] = { (byte) 0x69, (byte) 0x0 };

	private final static byte RESPONSE_DELETE[] = { (byte) 0x6b, (byte) 0x0 };

	private final static byte RESPONSE_MODIFYDN[] = { (byte) 0x6d, (byte) 0x0 };

	private final static byte RESPONSE_EXTENDED_OPERATION[] = { 0x78, 0x00 };

	private final static byte NADA[] = {};

	private final static byte RESULT_SUCCESS[] = { 0x0A, 0x01, 0x00 };

	private final static byte ERROR_UNSUPORTED_OPERATION[] = { 0x0A, 0x01, 0x01 };

	private final static byte ERROR_NO_SUCH_OBJECT[] = { 0x0A, 0x01, 0x20 };

	private final static byte ERROR_INVALID_CREDENTIALS[] = { 0x0A, 0x01, 49 };

	private final static byte ERROR_INSUFFICIENT_ACCESS_RIGHTS[] = { 0x0A, 0x01, 50 };

	private final static byte ERROR_ENTRY_ALREADY_EXISTS[] = { 0x0A, 0x01, 68 };

	private final static byte NEWSET[] = { 0x31, 0x00 };

	private static final LRUCache<ByteRef, ArrayList<byte[]>> CACHE = new LRUCache<ByteRef, ArrayList<byte[]>>();

	// version stuff

	private final static String version;

	static {
		version = Version.getShort() + " LDAP " + V.V
				+ " (c) 2000-" + V.Y + " by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
	}

	private LogFile logFile;

	private ByteRef br;

	private int msgId;

	private ByteRef currentUser;
	private ArrayList<String> currentReadPermissions = new ArrayList<String>();
	private ArrayList<String> currentWritePermissions = new ArrayList<String>();
	private ArrayList<byte[]> currentResponses;
	private int sizeLimit;
	private byte[] data;

	public static String getVersion() {
		return V.V;
	}

	public String getFullVersion() {
		return version;
	}

	LdapProtocol(LdapFactory hf, LogFile _logFile) {
		super(hf);
		this.logFile = _logFile;
	}

	private XmlFile getXmlFile() {
		synchronized (factory) {
			return ((LdapFactory) this.factory).getXmlFile();
		}
	}

	private void saveXmlFile() throws IOException {
		synchronized (factory) {
			CACHE.clear();
			((LdapFactory) this.factory).saveXmlFile();
		}
	}

	protected boolean doit() throws Exception {
		if (br == null || br.length() == 0)
			br = readFirst();

		if (br == null)
			return false;

		data = new byte[0x400];

		// try to read a sequence
		// first byte is the code for sequence 0x30
		// then we need at least one positive byte
		int sLen;
		for (;;) {
			int avail = br.length();
			if (avail > 1) {
				if (avail > data.length)
					data = new byte[avail];
				br.copy(data);

				sLen = Asn1.getLen(data);
				if (sLen > 0 && sLen <= avail)
					break;
			}
			ByteRef r = br.update(is);
			if (r == null)
				return false;
		}

		// remove read sequence from buffer
		byte inc[] = br.substring(0, sLen).toByteArray();
		br = br.substring(sLen);

		Asn1 in = new Asn1(inc);
		// System.out.println("< " + in);
		// Misc.dump(System.out, inc);
		// get the sequence data
		Iterator<Asn1> seq = in.children();

		msgId = seq.next().asInt();
		Asn1 request = seq.next();

		Asn1 control = null;
		if (seq.hasNext())
			control = seq.next();

		Iterator<Asn1> content = request.children();
		int requestType = request.getType() & 0x1f;
		switch (requestType) {
		case 0x0: // bind
			bind(content);
			break;
		case 0x2: // unbind
			logFile.writeDate("bye bye " + currentUser);
			currentUser = null;
			currentReadPermissions.clear();
			currentWritePermissions.clear();
			// no response
			break;
		case 0x3: // search
			search(request);
			break;
		case 0x6: // modifyRequest
			modify(content);
			break;
		case 0x8: // addRequest
			insert(content);
			break;
		case 0xa: // delRequest
			remove(request); // request directly contains the dn
			break;
		case 0xc: // modifyDN
			modifyDn(content);
			break;
		case 0x10: // abandon
			// TODO: once search is threaded, abort the search - for now:
			// ignore
			break;
		case 0x17: // extended request
			extendedOperation(content);
			break;
		default:
			byte result[] = new byte[] { (byte) (requestType + 0x60), 0 };
			result = Asn1.addTo(result, ERROR_UNSUPORTED_OPERATION);
			ByteRef errorMsg = new ByteRef();
			result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));
			result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

			writeResponse(result);
		}
		return true;
	}

	private void modifyDn(Iterator<Asn1> content) throws IOException, SizeLimitExceededException {
		final ByteRef modifyDn = content.next().asByteRef().toLowerCase();
		final ByteRef newType = content.next().asByteRef();
		byte[] resultVal = null;

		if (checkWritePermission(modifyDn)) {
			final String modifyKey = "/ldap/" + dn2Key(modifyDn);
			final String last = base(XmlFile.getLastSegment(modifyKey));

			final ByteRef attr = newType.nextWord('=').toLowerCase();
			final String val = newType.toString();

			if (attr.equals(last)) {
				synchronized (factory) {
					final XmlFile xml = getXmlFile();
					setValue(xml, modifyKey, val);
					saveXmlFile();
					resultVal = RESULT_SUCCESS;
				}
			}
		}

		if (resultVal == null)
			resultVal = ERROR_INSUFFICIENT_ACCESS_RIGHTS;
		// create response
		byte result[] = RESPONSE_MODIFYDN;
		result = Asn1.addTo(result, resultVal);
		result = Asn1.addTo(result, Asn1.makeASN1(modifyDn.toByteArray(), Asn1.OCTET_STRING));
		ByteRef errorMsg = new ByteRef();
		result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

		writeResponse(result);
	}

	private void setValue(XmlFile xml, String key, String val) {
		String l = XmlFile.getLastSegment(key);
		if (l.endsWith("userpassword") || l.endsWith("attributetypes") || l.endsWith("objectclasses")) {
			xml.setString(key, "name", val);
			xml.setString(key, "value", null);
			return;
		}
		String lower = val.toLowerCase();
		xml.setString(key, "name", lower);
		if (lower.equals(val)) {
			xml.setString(key, "value", null);
		} else {
			xml.setString(key, "value", val);
		}
	}

	private void remove(Asn1 request) throws IOException, SizeLimitExceededException {
		final ByteRef removeDn = request.asByteRef();
		// check access for the key
		byte[] resultVal = null;

		if (checkWritePermission(removeDn)) {

			final String removeKey = "/ldap/" + dn2Key(removeDn);

			synchronized (factory) {
				final XmlFile xml = getXmlFile();

				xml.setString(removeKey, null, "");
				saveXmlFile();
				resultVal = RESULT_SUCCESS;
			}
		}

		if (resultVal == null)
			resultVal = ERROR_INSUFFICIENT_ACCESS_RIGHTS;
		// create response
		byte result[] = RESPONSE_DELETE;
		result = Asn1.addTo(result, resultVal);
		result = Asn1.addTo(result, Asn1.makeASN1(removeDn.toByteArray(), Asn1.OCTET_STRING));
		ByteRef errorMsg = new ByteRef();
		result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

		writeResponse(result);
	}

	/**
	 * Insert a new entry.
	 * 
	 * @param content
	 * @throws IOException
	 * @throws SizeLimitExceededException
	 */
	private void insert(Iterator<Asn1> content) throws IOException, SizeLimitExceededException {
		final ByteRef insertDn = content.next().asByteRef().toLowerCase();

		// check access for the key
		byte[] resultVal = null;
		if (checkWritePermission(insertDn)) {

			final Asn1 data = content.next();

			final String insertKey = "/ldap/" + dn2Key(insertDn);
			final String lastSegment = getLastSegment(insertKey);
			final String last = base(lastSegment).toLowerCase();
			final String parentKey = insertKey.substring(0, insertKey.length() - lastSegment.length() - 1);

			synchronized (factory) {
				// find inserted name
				String newName = null;
				for (final Iterator<Asn1> i = data.children(); i.hasNext();) {
					final Iterator<Asn1> j = i.next().children();
					final ByteRef attr = j.next().asByteRef().toLowerCase();
					final Iterator<Asn1> values = j.next().children();
					if (attr.equals(last)) {
						newName = lastSegment.length() > last.length() + 2 ? lastSegment.substring(last.length() + 2)
								: values.hasNext() ? values.next().asByteRef().toString() : "";
						break;
					}
				}

				if (newName == null) {
					resultVal = ERROR_NO_SUCH_OBJECT;
				} else {
					final XmlFile xml = getXmlFile();

					// check if object already exists
					Map<String, String> existingAttrs = xml
							.getAttributes(parentKey + "\\" + last + "\\" + newName.toLowerCase());
					if (existingAttrs != null) {
						resultVal = ERROR_ENTRY_ALREADY_EXISTS;
					} else {
						final String createdKey = xml.createSection(parentKey + last);
						// apply the attributes
						for (final Iterator<Asn1> i = data.children(); i.hasNext();) {
							final Iterator<Asn1> j = i.next().children();
							final ByteRef attr = j.next().asByteRef().toLowerCase();
							final Iterator<Asn1> values = j.next().children();

							// update own entry
							if (attr.equals(last)) {
								final String val = lastSegment.length() > last.length() + 2
										? lastSegment.substring(last.length() + 2)
										: values.hasNext() ? values.next().asByteRef().toString("utf-8") : "";
								setValue(xml, createdKey, val);
							} else {

								// create child entries per attribute value
								while (values.hasNext()) {
									final ByteRef value = values.next().asByteRef();
									if (attr.equals("userpassword")) {
										setPassword(createdKey, value);
									} else {
										final String val = value.toString("utf-8");
										final String vkey = xml.createSection(createdKey + attr);
										setValue(xml, vkey, val);
									}
								}
							}
							resultVal = RESULT_SUCCESS;
						}
						saveXmlFile();
					}
				}
			}
		}

		if (resultVal == null)
			resultVal = ERROR_INSUFFICIENT_ACCESS_RIGHTS;
		// create response
		byte result[] = RESPONSE_INSERT;
		result = Asn1.addTo(result, resultVal);
		result = Asn1.addTo(result, Asn1.makeASN1(insertDn.toByteArray(), Asn1.OCTET_STRING));
		ByteRef errorMsg = new ByteRef();
		result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

		writeResponse(result);

	}

	private boolean checkWritePermission(ByteRef dn) {
		for (final String wp : currentWritePermissions) {
			if (dn.endsWith(wp)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Modify an existing entry
	 * 
	 * @param content
	 * @throws IOException
	 * @throws SizeLimitExceededException
	 */
	private void modify(Iterator<Asn1> content) throws IOException, SizeLimitExceededException {
		final ByteRef modifyBaseDn = content.next().asByteRef();
		// check access for the key
		byte[] resultVal = null;

		boolean access = checkWritePermission(modifyBaseDn);
		boolean selfAccess = !access && modifyBaseDn.endsWith(currentUser);

		if (!access && !selfAccess) {
			logFile.writeDate("search: " + currentUser + " has NO WRITE ACCESS to " + modifyBaseDn);
			sendModifyError();
			return;
		}

		final Asn1 modifications = content.next();

		final String modifyKey = "/ldap/" + dn2Key(modifyBaseDn);

		synchronized (factory) {
			final XmlFile xml = getXmlFile();

			// apply the incoming changes
			for (Iterator<Asn1> i = modifications.children(); i.hasNext();) {
				final Iterator<Asn1> mod = i.next().children();
				final int operation = mod.next().asInt();
				final Iterator<Asn1> attrTypeAndValue = mod.next().children();
				final ByteRef attr = attrTypeAndValue.next().asByteRef().toLowerCase();

				if (selfAccess) {
					if (attr.equals("readpermission") || attr.equals("writepermission")) {
						logFile.writeDate("search: " + currentUser
								+ " has NO WRITE ACCESS to readpermission/writepermission at " + modifyBaseDn);
						sendModifyError();
						return;
					}
				}

				final Iterator<Asn1> vals = attrTypeAndValue.next().children();
				switch (operation) {
				case 0: // add
				{
					final ByteRef value = vals.next().asByteRef();
					if (attr.equals("userpassword")) {
						setPassword(modifyKey, value);
					} else {
						final String key = xml.createSection(modifyKey + attr);
						final String val = value.toString("utf-8");
						setValue(xml, key, val);
					}
					if (resultVal == null)
						resultVal = RESULT_SUCCESS;
					break;
				}
				case 1: // del
				{
					final String key = vals.hasNext()
							? modifyKey + "\\" + attr + "\\" + vals.next().asByteRef().toString("utf-8")
							: modifyKey + attr;
					xml.setString(key, null, "");
					if (resultVal == null)
						resultVal = RESULT_SUCCESS;
					break;
				}
				case 2: // change
				{
					final ByteRef value = vals.next().asByteRef();
					if (attr.equals("userpassword")) {
						setPassword(modifyKey, value);
					} else {
						final String key = modifyKey + attr;
						String val = value.toString("utf-8");
						setValue(xml, key, val);
					}
					if (resultVal == null)
						resultVal = RESULT_SUCCESS;
					break;
				}
				default:
					resultVal = ERROR_INSUFFICIENT_ACCESS_RIGHTS;
				}
			}

			if (resultVal == RESULT_SUCCESS)
				saveXmlFile();
		}
		if (resultVal == null)
			resultVal = ERROR_INSUFFICIENT_ACCESS_RIGHTS;
		// create response
		byte result[] = RESPONSE_MODIFY;
		result = Asn1.addTo(result, resultVal);
		result = Asn1.addTo(result, Asn1.makeASN1(modifyBaseDn.toByteArray(), Asn1.OCTET_STRING));
		ByteRef errorMsg = new ByteRef();
		result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

		writeResponse(result);

	}

	/**
	 * Handle an extended operation
	 * 
	 * @param content
	 * @throws IOException
	 * @throws SizeLimitExceededException
	 */
	private void extendedOperation(Iterator<Asn1> content) throws IOException, SizeLimitExceededException {
		ByteRef oid = content.next().asByteRef();

		if (oid.equals("1.3.6.1.4.1.4203.1.11.1")) {
			extendedChangePassword(content);
			return;
		}

		sendExOpError("unsupported extended Operation", ERROR_UNSUPORTED_OPERATION);
	}

	private void sendExOpError(String msg, byte[] error) throws IOException, SizeLimitExceededException {
		byte result[] = RESPONSE_EXTENDED_OPERATION;
		result = Asn1.addTo(result, error);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(msg, Asn1.OCTET_STRING));
		writeResponse(result);
	}

	private void extendedChangePassword(Iterator<Asn1> content) throws IOException, SizeLimitExceededException {
		Iterator<Asn1> params = content.next().children().next().children();
		ByteRef uid = params.next().asByteRef();
		ByteRef opw = params.next().asByteRef();
		ByteRef npw = params.next().asByteRef();

		String key = "/ldap/" + dn2Key(uid);
		if (!checkPassword(key, opw)) {
			sendExOpError("invalid credentials", ERROR_INVALID_CREDENTIALS);
			return;
		}

		setPassword(key, npw);

		// Asn1 rest = content.next();
		// System.out.println(rest.toString());

		byte result[] = RESPONSE_EXTENDED_OPERATION;
		result = Asn1.addTo(result, RESULT_SUCCESS);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		writeResponse(result);

	}

	private void setPassword(final String key, final ByteRef newPassword) throws IOException {
		// update the password
		synchronized (factory) {
			final XmlFile xml = getXmlFile();
			// clear the manual password
			xml.setString(key, "auth", null);

			String value = newPassword.toString("utf-8");
			if (!value.toLowerCase().startsWith("{ssha}")) {
				byte salt[] = new byte[16];
				SecureRandom.getInstance().nextBytes(salt);

				SHA sha = new SHA();
				sha.update(newPassword.toByteArray());
				sha.update(salt);
				final byte[] digest = sha.digest();

				ByteRef all = new ByteRef(digest).append(new ByteRef(salt));

				value = "{ssha}" + new String(Mime.encode(all.toByteArray()), 0);
			}
			xml.setString(key + "userpassword", "name", value);

			saveXmlFile();
		}
	}

	private boolean checkPassword(String key, ByteRef password) throws IOException {
		String val = getXmlFile().getString(key, "auth", null);
		if (val != null) {
			if (!password.equals(val))
				return false;

			// update the credentials
			setPassword(key, new ByteRef(val));
			return true;
		}

		final XmlFile xml = getXmlFile();
		val = xml.getString(key + "userpassword", "name", "invalid");

		if (val.toLowerCase().startsWith("{ssha}")) {
			byte[] data = Mime.decode(val.getBytes(), 6, val.length());
			SHA sha = new SHA();
			sha.update(password.toByteArray());
			sha.update(data, 20, data.length - 20);
			byte[] digest = sha.digest();
			return Misc.equals(data, 0, digest, 0, 20);
		}

		return false;
	}

	private void search(Asn1 content) throws IOException {

		final Iterator<Asn1> seq = content.children();
		ByteRef searchBaseDn = seq.next().asByteRef().toLowerCase();

		boolean access = false;
		for (final String rp : currentReadPermissions) {
			if (searchBaseDn.endsWith(rp)) {
				access = true;
				break;
			}
		}

		int scope = seq.next().asInt();
		int deref = seq.next().asInt();
		sizeLimit = seq.next().asInt();
		int timeLimit = seq.next().asInt();
		int typesOnly = seq.next().asInt();

		Asn1 searchAsn = seq.next();
		Search search = createSearch(searchAsn);

		boolean selfAccess = access ? false : searchBaseDn.endsWith(currentUser);
		if (!selfAccess && search instanceof EqualityFilter) {
			final EqualityFilter ef = (EqualityFilter) search;
			final String efsearch = ef.what + "=" + ef.value + "," + searchBaseDn;
			selfAccess = currentUser.equals(efsearch);
		}
		if (!access && !selfAccess) {
			logFile.writeDate("search: " + currentUser + " has NO ACCESS to " + searchBaseDn);
			try {
				sendSearchError();
			} catch (SizeLimitExceededException e) {
				// ignore
			}
			return;
		}

		Asn1 attributeDescriptionList = seq.next();

		logFile.writeDate("search: ldap:///" + searchBaseDn + "?" + printableAttrs(attributeDescriptionList) + "?"
				+ (scope == 0 ? "base" : scope == 1 ? "one" : "sub") + "?" + search + "?" + sizeLimit);

		final ByteRef cacheKey = currentUser.append(content.asByteRef());

		// null --> cached response
		currentResponses = new ArrayList<byte[]>();
		try {
			ArrayList<byte[]> cachedResults = CACHE.get(cacheKey);
			if (cachedResults != null) {
				for (final byte[] b : cachedResults) {
					writeResponse(b);
				}
			} else {

				// apply the baseDn
				String searchRoot = dn2Key(searchBaseDn);
				searchRoot = "/ldap/" + searchRoot;

				// System.out.println(search);
				final XmlFile xmlFile = getXmlFile();

				String searchRoot2 = xmlFile.normalizeSection(searchRoot);
				if (searchRoot2 == null) {
					// TODO remove after upgrade
					searchRoot2 = "";
					for (String rest = searchRoot; rest.length() > 0;) {
						int bs = rest.indexOf('\\');
						if (bs < 0) {
							searchRoot2 += rest;
							break;
						}

						searchRoot2 += rest.substring(0, bs);
						rest = rest.substring(bs + 1);
						bs = rest.indexOf('\\');
						String part = rest.substring(0, bs);
						int sl = rest.indexOf('/', bs);
						String name = rest.substring(bs + 1, sl);
						rest = rest.substring(sl + 1);

						boolean f = false;
						for (String key : xmlFile.getSections(searchRoot2 + part)) {
							if (name.equalsIgnoreCase(xmlFile.getString(key, "name", null))) {
								f = true;
								searchRoot2 = key;
								break;
							}
						}
						if (!f) {
							searchRoot2 = null;
							break;
						}
					}
				}

				if (searchRoot2 != null) {
					if (xmlFile.sections(searchRoot2).hasNext()) {
						findRecursive(xmlFile, searchRoot2, search, attributeDescriptionList, scope);
					}
				} else {
					currentResponses = null;
				}
			}
		} catch (SizeLimitExceededException slee) {
		}

		if (currentResponses != null)
		CACHE.put(cacheKey, currentResponses);

		// create response
		byte result[] = currentResponses == null ? ERROR_NO_SUCH_OBJECT : RESPONSE_SEARCH_DONE;
		result = Asn1.addTo(result, RESULT_SUCCESS);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));

		try {
			writeResponse(result);
		} catch (SizeLimitExceededException e) {
		}
	}

	private String printableAttrs(Asn1 attributeDescriptionList) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<Asn1> i = attributeDescriptionList.children(); i.hasNext();) {
			Asn1 attr = i.next();
			ByteRef sattr = attr.asByteRef().toLowerCase();
			if (sb.length() > 0)
				sb.append(",");
			sb.append(sattr.toString());
		}
		return sb.toString();
	}

	private void sendModifyError() throws IOException, SizeLimitExceededException {
		// create response
		byte result[] = RESPONSE_MODIFY;
		result = Asn1.addTo(result, ERROR_INSUFFICIENT_ACCESS_RIGHTS);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		writeResponse(result);
	}

	private void sendSearchError() throws IOException, SizeLimitExceededException {
		// create response
		byte result[] = RESPONSE_SEARCH_DONE;
		result = Asn1.addTo(result, ERROR_INSUFFICIENT_ACCESS_RIGHTS);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		writeResponse(result);
	}

	/**
	 * Search using the given search and regard the scope
	 * 
	 * @param xml
	 * @param key
	 * @param search
	 * @param attributeDescriptionList
	 * @param scope
	 *            level / recursion
	 * @throws IOException
	 * @throws SizeLimitExceededException
	 */
	private void findRecursive(XmlFile xml, String key, Search search, Asn1 attributeDescriptionList, int scope)
			throws IOException, SizeLimitExceededException {
		// search only if BASEOBJECT or WHOLESUBTREE
		if (scope != SINGLELEVEL) {
			int hit = search.match(xml, key);
			if (hit > 0) {
				if (hit == 1) {
					writeSearchResult(xml, key, attributeDescriptionList, scope == BASEOBJECT);
				} else if (hit == 2) {
					// discard the last part of the key
					int slash = key.lastIndexOf('/', key.length() - 2);
					String key2 = key.substring(0, slash + 1);
					writeSearchResult(xml, key2, attributeDescriptionList, scope == BASEOBJECT);
				}
			}
		}
		if (scope == SINGLELEVEL || scope == WHOLESUBTREE) {
			if (scope == SINGLELEVEL)
				scope = BASEOBJECT;
			if (search instanceof EqualityFilter) {
				EqualityFilter ef = (EqualityFilter) search;
				String found = xml.normalizeSection(key + "\\" + ef.what + "\\" + ef.value);
				if (found != null) {
					writeSearchResult(xml, found, attributeDescriptionList, scope == BASEOBJECT);
					return;
				}
			}
			for (Iterator<String> i = xml.sections(key); i.hasNext();) {
				String nextkey = i.next();
				findRecursive(xml, nextkey, search, attributeDescriptionList, scope);
			}
		}
	}

	private void writeSearchResult(XmlFile xml, String key, Asn1 attributeDescriptionList, boolean appendAllAttributes)
			throws IOException, SizeLimitExceededException {
		final String dn = key2Dn(xml, key);

		byte attrResult[] = Asn1.newSeq;
		String last = XmlFile.getLastSegment(key);

		// build the list of attributes for the response
		HashSet<ByteRef> attrsToMatch = new HashSet<ByteRef>();
		if (!appendAllAttributes) {
			// read the supplies attributes
			for (Iterator<Asn1> i = attributeDescriptionList.children(); i.hasNext();) {
				Asn1 attr = i.next();
				ByteRef sattr = attr.asByteRef().toLowerCase();
				attrsToMatch.add(sattr);
				if (sattr.equals("dn")) {
					byte attName[] = Asn1.newSeq;
					attName = Asn1.addTo(attName, Asn1.makeASN1(sattr.toByteArray(), Asn1.OCTET_STRING));
					byte attVal[] = NEWSET;
					attVal = Asn1.addTo(attVal, Asn1.makeASN1(dn, Asn1.OCTET_STRING));
					attName = Asn1.addTo(attName, attVal);
					attrResult = Asn1.addTo(attrResult, attName);
				} else if (sattr.equals("*"))
					appendAllAttributes = true;
			}
		}

		if (appendAllAttributes || attrsToMatch.isEmpty()) {
			// read all attributes
			attrsToMatch.add(new ByteRef(last));
			for (String s : xml.getSections(key)) {
				// only empty tags are own attributes
				if (!xml.sections(s).hasNext())
					attrsToMatch.add(new ByteRef(XmlFile.getLastSegment(s)));
			}
		}

		for (ByteRef sattr : attrsToMatch) {
			if (sattr.equals("1.1")) {
				byte attName[] = Asn1.newSeq;
				attName = Asn1.addTo(attName, Asn1.makeASN1("dn".getBytes(), Asn1.OCTET_STRING));
				byte attVal[] = NEWSET;
				attVal = Asn1.addTo(attVal, Asn1.makeASN1(key2Dn(xml, key), Asn1.OCTET_STRING));
				attName = Asn1.addTo(attName, attVal);
				attrResult = Asn1.addTo(attrResult, attName);
				continue;
			}

			if (sattr.equals("ismemberof")) {
				// support "isMemberOf"
				byte attVal[] = memberOfSearch(xml, NEWSET, "/ldap/", dn.toLowerCase());

				if (attVal.length > NEWSET.length) {
					byte attName[] = Asn1.newSeq;
					attName = Asn1.addTo(attName, Asn1.makeASN1("ismemberof".getBytes(), Asn1.OCTET_STRING));
					attName = Asn1.addTo(attName, attVal);
					attrResult = Asn1.addTo(attrResult, attName);
				}
				continue;
			}

			// main attribute => self key
			if (sattr.equals(last)) {
				String val = getValue(xml, key);
				if (val != null) {
					byte attName[] = Asn1.newSeq;
					attName = Asn1.addTo(attName, Asn1.makeASN1(sattr.toByteArray(), Asn1.OCTET_STRING));
					byte attVal[] = NEWSET;
					attVal = Asn1.addTo(attVal, Asn1.makeASN1(val, Asn1.OCTET_STRING));
					attName = Asn1.addTo(attName, attVal);
					attrResult = Asn1.addTo(attrResult, attName);
				}
				continue;
			}

			boolean hasAttr = false;
			byte attName[] = Asn1.newSeq;
			attName = Asn1.addTo(attName, Asn1.makeASN1(sattr.toByteArray(), Asn1.OCTET_STRING));
			byte attVal[] = NEWSET;
			for (Iterator<String> j = xml.sections(key + sattr); j.hasNext();) {
				final String k = j.next();
				String val = getValue(xml, k);
				if (val != null) {
					hasAttr = true;
					attVal = Asn1.addTo(attVal, patchOctetString(Asn1.makeASN1(val, Asn1.UTF8String)));
				}
			}
			if (hasAttr) {
				attName = Asn1.addTo(attName, attVal);
				attrResult = Asn1.addTo(attrResult, attName);
			}
		}

		if (attrResult.length == 2)
			return;

		byte res[] = RESPONSE_SEARCH;
		res = Asn1.addTo(res, patchOctetString(Asn1.makeASN1(dn.getBytes(), Asn1.UTF8String)));
		res = Asn1.addTo(res, attrResult);
		writeResponse(res);
	}

	private String getValue(XmlFile xml, final String k) {
		String val = xml.getString(k, "value", null);
		if (val == null) {
			val = xml.getString(k, "name", null);
		}
		// TODO remove this - it's only for upgrading existing data
		if (val != null)
			setValue(xml, k, val);
		return val;
	}

	private byte[] patchOctetString(byte[] b) {
		b[0] = Asn1.OCTET_STRING;
		return b;
	}

	/**
	 * global ismemberof search
	 * 
	 * @param attVal
	 * @param key
	 * @param dn
	 * @return the collected attrVal
	 */
	private byte[] memberOfSearch(XmlFile xml, byte[] attVal, String key, String dn) {
		final String uniquemember = xml.getString(key + "\\uniquemember\\" + dn, "name", null);
		if (uniquemember != null)
			attVal = Asn1.addTo(attVal, Asn1.makeASN1(key2Dn(xml, key), Asn1.OCTET_STRING));

		for (final String subkey : xml.getSections(key)) {
			attVal = memberOfSearch(xml, attVal, subkey, dn);
		}
		return attVal;
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	private static String key2Dn(XmlFile xml, String key) {
		// create a dn
		String dnPath = key.substring(0, key.length() - 1); // skip last /slash
		StringBuilder dn = new StringBuilder();
		for (int slash = dnPath.lastIndexOf('/'); slash > 0; slash = dnPath.lastIndexOf('/')) {
			String s = dnPath.substring(slash + 1);

			String val;
			int bs = s.indexOf('\\');
			if (bs < 0) {
				val = xml.getString(dnPath, "name", null);
				s = stripNr(s);
				if (val == null) {
					val = s;
				}
			} else {
				s = s.substring(bs + 1);
				bs = s.indexOf('\\');
				val = s.substring(bs + 1);
				s = s.substring(0, bs);
			}
			if (dn.length() > 0)
				dn.append(",");
			dn.append(s).append("=").append(val);
			dnPath = dnPath.substring(0, slash);
		}

		// System.out.println(dn);
		return dn.toString();
	}

	private static String stripNr(String s) {
		int nr = s.lastIndexOf('#');
		if (nr > 0) {
			s = s.substring(0, nr);
		} else if (s.endsWith("/")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String dn2Key(ByteRef dn) {
		ByteRef searchBaseDn = (ByteRef) dn.clone();
		String dnKey = "";
		for (ByteRef part = searchBaseDn.nextWord(','); part != null; part = searchBaseDn.nextWord(',')) {
			ByteRef key = part.nextWord('=');
			dnKey = "\\" + key + "\\" + part.trim('"') + "/" + dnKey;
		}
		return dnKey.toLowerCase();
	}

	/**
	 * Handle the bind command.
	 * 
	 * @param seq
	 * @param content
	 * @return
	 * @throws IOException
	 * @throws SizeLimitExceededException
	 */
	private void bind(Iterator<Asn1> seq) throws IOException, SizeLimitExceededException {
		int version = seq.next().asInt();
		ByteRef name = seq.next().asByteRef();
		ByteRef auth = seq.next().asByteRef();

		ByteRef dn = new ByteRef();
		ByteRef errorMsg = new ByteRef();

		byte[] resultVal = RESULT_SUCCESS;

		String key = "/ldap/" + dn2Key(name);

		if (name.length() > 0) {
			if (!checkPassword(key, auth)) {
				resultVal = ERROR_INVALID_CREDENTIALS;
			}
		}

		logFile.writeDate("bind: v" + version + " for '" + name + "' "
				+ ((resultVal == RESULT_SUCCESS) ? "login successful" : "login failed"));

		currentUser = name;

		// fetch the read and write permissions.
		XmlFile xml = getXmlFile();
		currentReadPermissions.clear();
		for (final String rp : xml.getSections(key + "readpermission")) {
			currentReadPermissions.add(xml.getString(rp, "name", "$invalid$"));
		}

		currentWritePermissions.clear();
		for (final String wp : xml.getSections(key + "writepermission")) {
			currentWritePermissions.add(xml.getString(wp, "name", "$invalid$"));
		}

		// create response
		byte result[] = RESPONSE_BIND;
		result = Asn1.addTo(result, resultVal);
		result = Asn1.addTo(result, Asn1.makeASN1(dn.toByteArray(), Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

		writeResponse(result);
	}

	private void writeResponse(byte[] responseContent) throws IOException, SizeLimitExceededException {
		// support response caching
		if (currentResponses != null)
			currentResponses.add(responseContent);

		byte response[] = Asn1.newSeq;
		response = Asn1.addTo(response, Asn1.makeASN1(msgId, Asn1.INTEGER));
		response = Asn1.addTo(response, responseContent);

		Iterator<Asn1> msg = new Asn1(responseContent).children();
		Asn1 cn = msg.next();
		byte[] data = Asn1.getData(cn.toByteArray());
		if (data.length > 1)
			logFile.writeDate("> " + new ByteRef(data));
		Asn1 r = msg.next();
		if ((0x1f & r.getType()) == Asn1.SEQUENCE) {
			for (Iterator<Asn1> j = r.children(); j.hasNext();) {
				Asn1 r2 = j.next();
				if ((0x1f & r2.getType()) == Asn1.SEQUENCE) {
					Iterator<Asn1> i = r2.children();
					Asn1 nameAsn1 = i.next();
					String name = nameAsn1.asByteRef().toString();
					Asn1 setAsn1 = i.next();
					StringBuilder vals = new StringBuilder();
					if ((0x1f & setAsn1.getType()) == Asn1.SET) {
						for (i = setAsn1.children(); i.hasNext();) {
							if (vals.length() > 0)
								vals.append(", ");
							vals.append(i.next().asByteRef());
						}
					}
					logFile.writeDate("  > " + name + " = " + vals);
				}
			}
		}

		// System.out.println("> " + new Asn1(response));
		// Misc.dump(System.out, response);
		os.write(response);

		if (this.sizeLimit > 0 && --this.sizeLimit == 0)
			throw new SizeLimitExceededException();
	}

	Search createSearch(Asn1 searchAsn) {
		Iterator<Asn1> i = searchAsn.children();
		switch (searchAsn.getType() & 0xf) {
		case 0x0:
			return new AndFilter(i);
		case 0x1:
			return new OrFilter(i);
		case 0x2:
			return new NotFilter(i);
		case 0x3:
			return new EqualityFilter(i);
		case 0x4:
			return new SubStringFilter(i);
		case 0x7:
			return new PresentFilter(searchAsn);
		default:
			throw new UnsupportedOperationException();
		}
	}

	interface Search {
		public int match(XmlFile xml, String key);
	}

	class SubStringFilter implements Search {

		private String attribute;

		private int where;

		private ArrayList<String> substrings = new ArrayList<String>();

		SubStringFilter(Iterator<Asn1> i) {
			this.attribute = i.next().asByteRef().toString();
			Asn1 list = i.next();
			this.where = list.getType() & 3;
			for (Iterator<Asn1> j = list.children(); j.hasNext();) {
				ByteRef val = j.next().asByteRef();
				substrings.add(val.toString().toLowerCase());
			}
		}

		public int match(XmlFile xml, String searchRoot) {

			String id = XmlFile.getLastSegment(searchRoot);
			if (attribute.equalsIgnoreCase(id)) {
				String value = xml.getString(searchRoot, "name", null);
				for (String v : substrings) {
					if (value.toLowerCase().indexOf(v) >= 0)
						return 1;
				}
			}

			for (Iterator<String> i = xml.sections(searchRoot); i.hasNext();) {
				String key = i.next();

				// skip LDAP objects - identified by nested object class
				if (xml.sections(key + "objectclass").hasNext())
					continue;

				id = getLastSegment(key);
				if (!attribute.equalsIgnoreCase(id))
					continue;

				String value = xml.getString(key, "name", null);
				if (value == null)
					continue;

				for (String v : substrings) {
					if (value.toLowerCase().indexOf(v) >= 0)
						return 1;
				}
			}
			return -1;
		}

		public String toString() {
			String s = substrings.toString();
			s = s.substring(1, s.length() - 1);
			return "(" + attribute + "=*" + s + "*)";
		}
	}

	class PresentFilter implements Search {

		private ByteRef attribute;

		private int where;

		private ArrayList<ByteRef> substrings = new ArrayList<ByteRef>();

		PresentFilter(Iterator<Asn1> i) {
			this.attribute = i.next().asByteRef().toLowerCase();
			Asn1 list = i.next();
			this.where = list.getType() & 3;
			for (Iterator<Asn1> j = list.children(); j.hasNext();) {
				ByteRef val = j.next().asByteRef();
				substrings.add(val);
			}
		}

		public int match(XmlFile xml, String searchRoot) {
			final String found = xml.normalizeSection(searchRoot + attribute);
			if (found == null)
				return 0;
			if (xml.hasChildSections(found))
				return 0;

			return 1;
		}

		public String toString() {
			return "(" + attribute + "=*)";
		}
	}

	class OrFilter implements LdapProtocol.Search {
		ArrayList<LdapProtocol.Search> searches = new ArrayList<Search>();

		public OrFilter(Iterator<Asn1> i) {
			while (i.hasNext()) {
				searches.add(createSearch(i.next()));
			}
		}

		public int match(XmlFile xml, String key) {
			for (LdapProtocol.Search s : this.searches) {
				int r = s.match(xml, key);
				if (r > 0) {
					return r;
				}
			}
			return -1;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(|");
			for (Search s : searches) {
				sb.append(s.toString());
			}
			sb.append(")");
			return sb.toString();
		}
	}

	class NotFilter implements LdapProtocol.Search {
		ArrayList<LdapProtocol.Search> searches = new ArrayList<Search>();

		public NotFilter(Iterator<Asn1> i) {
			while (i.hasNext()) {
				searches.add(createSearch(i.next()));
			}
		}

		public int match(XmlFile xml, String key) {
			for (LdapProtocol.Search s : this.searches) {
				int r = s.match(xml, key);
				return r > 0 ? 0 : 1;
			}
			return -1;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(!");
			for (Search s : searches) {
				sb.append(s.toString());
			}
			sb.append(")");
			return sb.toString();
		}
	}

	class AndFilter implements Search {
		ArrayList<Search> searches = new ArrayList<Search>();

		public AndFilter(Iterator<Asn1> i) {
			while (i.hasNext()) {
				searches.add(createSearch(i.next()));
			}
		}

		public int match(XmlFile xml, String key) {
			for (Search s : searches) {
				int r = s.match(xml, key);
				if (r <= 0)
					return r;
			}
			return 1;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(&");
			for (Search s : searches) {
				sb.append(s.toString());
			}
			sb.append(")");
			return sb.toString();

		}
	}

	class EqualityFilter implements Search {
		private ByteRef what;

		private String value;

		public EqualityFilter(Iterator<Asn1> i) {
			what = i.next().asByteRef().toLowerCase();
			ByteRef v = i.next().asByteRef();
			if (v.startsWith(what) && v.charAt(what.length()) == '=')
				v.nextWord('=');
			value = v.toString().toLowerCase();
		}

		/**
		 * A XML node can reflect an LDAP object or an attribute.
		 * 
		 * If the last segment matches, it's a LDAP object. Otherwise it's an
		 * attribute.
		 * 
		 * Ensure that LDAP objects are not found as attribute.
		 */
		public int match(XmlFile xml, String searchRoot) {
			String id = XmlFile.getLastSegment(searchRoot);
			if (what.equals(id)) {
				if (value.equalsIgnoreCase(xml.getString(searchRoot, "name", null)))
					return 1;
			}

			// support "isMemberOf"
			if (what.equals("ismemberof")) {
				final String group = "/ldap/" + dn2Key(new ByteRef(value));
				final String dn = key2Dn(xml, searchRoot);
				final String uniquemember = xml.getString(group + "\\uniquemember\\" + dn, "name", null);
				return dn.equals(uniquemember) ? 1 : -1;
			}

			String found = xml.normalizeSection(searchRoot + "\\" + what + "\\" + value);
			if (found != null && xml.hasChildSections(found))
				found = null;
			if (found != null)
				return 1;
			// for (Iterator<String> i = xml.sections(searchRoot); i.hasNext();)
			// {
			// String key = i.next();
			//
			// // skip LDAP objects - identified by nested object class
			// if (xml.sections(key).hasNext())
			// continue;
			// id = getLastSegment(key);
			// if (what.equalsIgnoreCase(id)) {
			// if (value.equalsIgnoreCase(xml.getString(key, "name", null)))
			// return 1;
			// }
			// /*
			// * String v = xml.getString(key, "ref", null); if (v != null &&
			// * v.indexOf(value) >= 0) return 2;
			// */
			// }

			return -1;
		}

		public String toString() {
			return "(" + what + "=" + value + ")";
		}
	}

	/**
	 * Get the last segment of a XML key.
	 * 
	 * @param key
	 * @return
	 */
	static String getLastSegment(final String key) {
		int slash = key.lastIndexOf('/', key.length() - 2) + 1;
		int num = key.indexOf('#', slash);
		if (num < slash)
			num = key.length() - 1;
		return key.substring(slash, num);
	}

	static String base(final String segment) {
		if (!segment.startsWith("\\"))
			return segment;
		int b2 = segment.indexOf('\\', 1);
		return segment.substring(1, b2);
	}

}
