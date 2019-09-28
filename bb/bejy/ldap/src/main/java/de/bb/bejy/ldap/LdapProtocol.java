package de.bb.bejy.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.naming.SizeLimitExceededException;

import de.bb.bejy.Protocol;
import de.bb.security.Asn1;
import de.bb.security.SHA;
import de.bb.security.SecureRandom;
import de.bb.util.ByteRef;
import de.bb.util.LogFile;
import de.bb.util.Mime;
import de.bb.util.Misc;
import de.bb.util.SessionManager;
import de.bb.util.XmlFile;

public class LdapProtocol extends Protocol {

	private static final String PAGED_RESULTS_CONTROL = "1.2.840.113556.1.4.319";
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

	private final static byte NEWCONTROLS[] = { (byte) 0xA0, 0x00 };

	// version stuff

	private final LogFile logFile;

	private ByteRef br;

	private int msgId;

	private ByteRef currentUser;
	private String scurrentUser;
	private final ArrayList<String> currentReadPermissions = new ArrayList<String>();
	private final ArrayList<String> currentWritePermissions = new ArrayList<String>();
	private ArrayList<byte[]> currentResponses;
	private int sizeLimit;
	private byte[] data;

	private final HashMap<String, ArrayList<byte[]>> cookie2data = new HashMap<String, ArrayList<byte[]>>();

	LdapProtocol(final LdapFactory hf, final LogFile _logFile) {
		super(hf);
		this.logFile = _logFile;
	}

	private XmlFile getXmlFile() {
		return ((LdapFactory) this.factory).getXmlFile();
	}

	private void saveXmlFile() throws IOException {
		((LdapFactory) this.factory).saveXmlFile();
		LdapFactory.CACHE.clear();
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
			final int avail = br.length();
			if (avail > 1) {
				if (avail > data.length)
					data = new byte[avail];
				br.copy(data);

				sLen = Asn1.getLen(data);
				if (sLen > 0 && sLen <= avail)
					break;
			}
			final ByteRef r = br.update(is);
			if (r == null)
				return false;
		}

		// remove read sequence from buffer
		final byte inc[] = br.substring(0, sLen).toByteArray();
		br = br.substring(sLen);

		final Asn1 in = new Asn1(inc);
		// System.out.println("< " + in);
		// Misc.dump(System.out, inc);
		// get the sequence data
		final Iterator<Asn1> seq = in.children();

		msgId = seq.next().asInt();
		final Asn1 request = seq.next();

		Asn1 control = null;
		if (seq.hasNext())
			control = seq.next();

		final Iterator<Asn1> content = request.children();
		final int requestType = request.getType() & 0x1f;
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
			final ByteRef errorMsg = new ByteRef();
			result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));
			result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

			writeResponse(result);
		}
		return true;
	}

	private void modifyDn(final Iterator<Asn1> content) throws IOException {
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
		final ByteRef errorMsg = new ByteRef();
		result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

		writeResponse(result);
	}

	private void setValue(final XmlFile xml, final String key, final String val) {
		final String l = XmlFile.getLastSegment(key);
		if (l.endsWith("userpassword") || l.endsWith("attributetypes") || l.endsWith("objectclasses")) {
			xml.setString(key, "name", val);
			xml.setString(key, "value", null);
			return;
		}

		// set permissions only if it matches a write permission
		if (l.endsWith("readpermission") || l.endsWith("writepermission")) {
			if (!checkWritePermission(new ByteRef(val)))
				return;
		}

		final String lower = val.toLowerCase();
		xml.setString(key, "name", lower);
		if (lower.equals(val)) {
			xml.setString(key, "value", null);
		} else {
			xml.setString(key, "value", val);
		}
	}

	private void remove(final Asn1 request) throws IOException {
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
		final ByteRef errorMsg = new ByteRef();
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
	private void insert(final Iterator<Asn1> content) throws IOException {
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
					final Map<String, String> existingAttrs = xml
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
		final ByteRef errorMsg = new ByteRef();
		result = Asn1.addTo(result, Asn1.makeASN1(errorMsg.toByteArray(), Asn1.OCTET_STRING));

		writeResponse(result);

	}

	private boolean checkWritePermission(final ByteRef dn) {
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
	private void modify(final Iterator<Asn1> content) throws IOException {
		final ByteRef modifyBaseDn = content.next().asByteRef();
		// check access for the key
		byte[] resultVal = null;

		final boolean access = checkWritePermission(modifyBaseDn);
		final boolean selfAccess = !access && modifyBaseDn.endsWith(currentUser);

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
			for (final Iterator<Asn1> i = modifications.children(); i.hasNext();) {
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
						final String val = value.toString("utf-8");
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
		final ByteRef errorMsg = new ByteRef();
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
	private void extendedOperation(final Iterator<Asn1> content) throws IOException {
		final ByteRef oid = content.next().asByteRef();

		if (oid.equals("1.3.6.1.4.1.4203.1.11.1")) {
			extendedChangePassword(content);
			return;
		}

		sendExOpError("unsupported extended Operation", ERROR_UNSUPORTED_OPERATION);
	}

	private void sendExOpError(final String msg, final byte[] error) throws IOException {
		byte result[] = RESPONSE_EXTENDED_OPERATION;
		result = Asn1.addTo(result, error);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(msg, Asn1.OCTET_STRING));
		writeResponse(result);
	}

	private void extendedChangePassword(final Iterator<Asn1> content) throws IOException {
		final Iterator<Asn1> params = content.next().children().next().children();
		final ByteRef uid = params.next().asByteRef();
		final ByteRef opw = params.next().asByteRef();
		final ByteRef npw = params.next().asByteRef();

		final String key = "/ldap/" + dn2Key(uid);
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
				final byte salt[] = new byte[16];
				SecureRandom.getInstance().nextBytes(salt);

				final SHA sha = new SHA();
				sha.update(newPassword.toByteArray());
				sha.update(salt);
				final byte[] digest = sha.digest();

				final ByteRef all = new ByteRef(digest).append(new ByteRef(salt));

				value = "{ssha}" + new String(Mime.encode(all.toByteArray()), 0);
			}
			xml.setString(key + "userpassword", "name", value);

			saveXmlFile();
		}
	}

	private boolean checkPassword(final String key, final ByteRef password) throws IOException {
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
			final byte[] data = Mime.decode(val.getBytes(), 6, val.length());
			final SHA sha = new SHA();
			sha.update(password.toByteArray());
			sha.update(data, 20, data.length - 20);
			final byte[] digest = sha.digest();
			return Misc.equals(data, 0, digest, 0, 20);
		}

		return false;
	}

	private void search(final Asn1 content) throws IOException {

		final Iterator<Asn1> seq = content.children();
		final ByteRef searchBaseDn = seq.next().asByteRef().toLowerCase();

		final int scope = seq.next().asInt();
		final int deref = seq.next().asInt();
		sizeLimit = seq.next().asInt();
		final int timeLimit = seq.next().asInt();
		final int typesOnly = seq.next().asInt();

		final Asn1 searchAsn = seq.next();
		final Search search = createSearch(searchAsn);

		final Asn1 attributeDescriptionList = seq.next();

		logFile.writeDate("search: ldap:///" + searchBaseDn + "?" + printableAttrs(attributeDescriptionList) + "?"
				+ (scope == 0 ? "base" : scope == 1 ? "one" : "sub") + "?" + search + "?" + sizeLimit);

		final ByteRef cacheKey = currentUser.append(content.asByteRef());

		int criticality = 0;
		String cookie = null;
		if (content.hasNext()) {
			content.next();
			final Iterator<Asn1> extension = content.children();
			final Iterator<Asn1> ext1 = extension.next().children();
			final Asn1 controlType = ext1.next();
			// pagedResultsControl
			if (controlType.asByteRef().equals(PAGED_RESULTS_CONTROL)) {
				criticality = ext1.next().asInt();
				final Iterator<Asn1> controlValueOctet = ext1.next().children();
				final Iterator<Asn1> controlValue = controlValueOctet.next().children();
				final int size = controlValue.next().asInt();
				if (size > 0 && size < sizeLimit)
					sizeLimit = size;

				cookie = controlValue.next().asByteRef().toString();
			}
		}

		if (cookie != null && cookie.length() > 0) {
			currentResponses = cookie2data.remove(cookie);
		} else {
			currentResponses = LdapFactory.CACHE.get(cacheKey);

			// null --> read from data base
			if (currentResponses == null) {
				currentResponses = new ArrayList<byte[]>();

				// System.out.println(search);
				final XmlFile xmlFile = getXmlFile();
				scurrentUser = xmlFile.normalizeSection("/ldap/" + dn2Key(currentUser));

				// apply the baseDn
				String searchRoot = dn2Key(searchBaseDn);
				searchRoot = "/ldap/" + searchRoot;

				final String searchRoot2 = xmlFile.normalizeSection(searchRoot);
//				System.out.println(searchRoot + "==" + searchRoot2);
				if (searchRoot2 != null) {
					if (xmlFile.sections(searchRoot2).hasNext()) {
						findRecursive(xmlFile, searchRoot2, search, attributeDescriptionList, scope);
					}
				} else {
					currentResponses = null;
				}
			}
		}

		// create response
		byte result[] = currentResponses == null ? ERROR_NO_SUCH_OBJECT : RESPONSE_SEARCH_DONE;
		result = Asn1.addTo(result, RESULT_SUCCESS);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));

		if (currentResponses != null) {
			LdapFactory.CACHE.put(cacheKey, currentResponses);

			// write the data
			final ArrayList<byte[]> tmp = currentResponses;
			currentResponses = null;

			int offset = offsetFromCookie(cookie);
			int len = sizeLimit > 0 ? sizeLimit : tmp.size();
			if (offset + len > tmp.size())
				len = tmp.size() - offset;
			for (int i = offset; len > 0; ++i, --len, ++offset) {
				final byte[] b = tmp.get(i);
				writeResponse(b);
			}
			if (cookie != null && offset < tmp.size()) {
				final String newCookie = SessionManager.newKey() + ":" + offset;
				cookie2data.put(newCookie, tmp);

				// create pagedResultsControl response
				byte[] realSearchControlValue = Asn1.addTo(Asn1.newSeq, Asn1.makeASN1(tmp.size(), Asn1.INTEGER));
				realSearchControlValue = Asn1.addTo(realSearchControlValue,
						Asn1.makeASN1(newCookie, Asn1.OCTET_STRING));
				realSearchControlValue = Asn1.makeASN1(realSearchControlValue, Asn1.OCTET_STRING);

				byte[] pagedResultsControl = Asn1.addTo(Asn1.newSeq,
						Asn1.makeASN1(PAGED_RESULTS_CONTROL, Asn1.OCTET_STRING));
				pagedResultsControl = Asn1.addTo(pagedResultsControl, Asn1.makeASN1(criticality, Asn1.BOOLEAN));
				pagedResultsControl = Asn1.addTo(pagedResultsControl, realSearchControlValue);

				final byte[] controls = Asn1.addTo(NEWCONTROLS, pagedResultsControl);

				result = Asn1.addTo(result, controls);
			}
		}

		writeResponse(result);
	}

	private int offsetFromCookie(final String cookie) {
		if (cookie == null)
			return 0;
		final int colon = cookie.lastIndexOf(':');
		if (colon > 0)
			return Integer.parseInt(cookie.substring(colon + 1));
		return 0;
	}

	private String printableAttrs(final Asn1 attributeDescriptionList) {
		final StringBuilder sb = new StringBuilder();
		for (final Iterator<Asn1> i = attributeDescriptionList.children(); i.hasNext();) {
			final Asn1 attr = i.next();
			final ByteRef sattr = attr.asByteRef().toLowerCase();
			if (sb.length() > 0)
				sb.append(",");
			sb.append(sattr.toString());
		}
		return sb.toString();
	}

	private void sendModifyError() throws IOException {
		// create response
		byte result[] = RESPONSE_MODIFY;
		result = Asn1.addTo(result, ERROR_INSUFFICIENT_ACCESS_RIGHTS);
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		result = Asn1.addTo(result, Asn1.makeASN1(NADA, Asn1.OCTET_STRING));
		writeResponse(result);
	}

	private void sendSearchError() throws IOException {
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
	 * @param searchRoot
	 * @param search
	 * @param attributeDescriptionList
	 * @param scope                    level / recursion
	 * @throws IOException
	 * @throws SizeLimitExceededException
	 */
	private void findRecursive(final XmlFile xml, final String searchRoot, final Search search,
			final Asn1 attributeDescriptionList, int scope) throws IOException {

		// access for objects here and below.
		boolean access = searchRoot.startsWith(scurrentUser) || scurrentUser.startsWith(searchRoot);
		for (final String rp : currentReadPermissions) {
//			System.out.println(rp + " ? " + searchRoot);
			if (searchRoot.startsWith(rp) || rp.startsWith(searchRoot)) {
				access = true;
				break;
			}
		}

//		System.out.println(scurrentUser + " :: " + searchRoot + " = " + access);

		if (!access)
			return;

		if (!xml.sections(searchRoot).hasNext())
			return;

		// search only if BASEOBJECT or WHOLESUBTREE
		if (scope != SINGLELEVEL) {
			final int hit = search.match(xml, searchRoot);
			if (hit > 0) {
				if (hit == 1) {
					writeSearchResult(xml, searchRoot, attributeDescriptionList, scope == BASEOBJECT);
				} else if (hit == 2) {
					// discard the last part of the key
					final int slash = searchRoot.lastIndexOf('/', searchRoot.length() - 2);
					final String key2 = searchRoot.substring(0, slash + 1);
					writeSearchResult(xml, key2, attributeDescriptionList, scope == BASEOBJECT);
				}
			}
		}
		if (scope == SINGLELEVEL || scope == WHOLESUBTREE) {
			if (scope == SINGLELEVEL)
				scope = BASEOBJECT;
			for (final Iterator<String> i = xml.sections(searchRoot); i.hasNext();) {
				final String nextkey = i.next();
				findRecursive(xml, nextkey, search, attributeDescriptionList, scope);
			}
		}
	}

	private void writeSearchResult(final XmlFile xml, final String key, final Asn1 attributeDescriptionList,
			boolean appendAllAttributes) throws IOException {
		final String dn = key2Dn(xml, key);

		byte attrResult[] = Asn1.newSeq;
		final String last = XmlFile.getLastSegment(key);

		appendAllAttributes = false;

		// build the list of attributes for the response
		final HashSet<ByteRef> attrsToMatch = new HashSet<ByteRef>();
		if (!appendAllAttributes) {
			// read the supplies attributes
			for (final Iterator<Asn1> i = attributeDescriptionList.children(); i.hasNext();) {
				final Asn1 attr = i.next();
				final ByteRef sattr = attr.asByteRef().toLowerCase();
				attrsToMatch.add(sattr);
				if (sattr.equals("dn")) {
					attrResult = addResult(attrResult, sattr.toByteArray(), dn);
				} else if (sattr.equals("*"))
					appendAllAttributes = true;
			}
		}

		if (appendAllAttributes || attrsToMatch.isEmpty()) {
			// read all attributes
			attrsToMatch.add(new ByteRef(last));
			for (final String s : xml.getSections(key)) {
				// only empty tags are own attributes
				if (!xml.sections(s).hasNext())
					attrsToMatch.add(new ByteRef(XmlFile.getLastSegment(s)));
			}
		}

		for (final ByteRef sattr : attrsToMatch) {
			if (sattr.equals("1.1")) {
				attrResult = addResult(attrResult, "dn".getBytes(), key2Dn(xml, key));
				continue;
			}

			if (sattr.equalsIgnoreCase("ismemberof")) {
				// support "isMemberOf"
				final byte attVal[] = memberOfSearch(xml, NEWSET, "/ldap/", dn.toLowerCase());

				if (attVal.length > NEWSET.length) {
					byte attName[] = Asn1.newSeq;
					attName = Asn1.addTo(attName, Asn1.makeASN1("isMemberOf".getBytes(), Asn1.OCTET_STRING));
					attName = Asn1.addTo(attName, attVal);
					attrResult = Asn1.addTo(attrResult, attName);
				}
				continue;
			}

			if (sattr.equalsIgnoreCase("defaultnamingcontext")) {
				for (final String knc : xml.getSections("/ldap/namingcontexts")) {
					final String d = xml.getString(knc, "name", "<>");
					if (currentUser.endsWith(d)) {
						attrResult = addResult(attrResult, "defaultnamingcontext".getBytes(), d);
						break;
					}
				}
				continue;
			}

			// main attribute => self key
			if (sattr.equals(last)) {
				final String val = getValue(xml, key);
				if (val != null) {
					ByteRef cattr = LdapFactory.L2C.get(sattr);
					if (cattr == null)
						cattr = sattr;

					attrResult = addResult(attrResult, cattr.toByteArray(), val);
				}
				continue;
			}

			boolean hasAttr = false;
			byte attName[] = Asn1.newSeq;
			ByteRef cattr = LdapFactory.L2C.get(sattr);
			if (cattr == null)
				cattr = sattr;
			attName = Asn1.addTo(attName, Asn1.makeASN1(cattr.toByteArray(), Asn1.OCTET_STRING));
			byte attVal[] = NEWSET;
			for (final Iterator<String> j = xml.sections(key + sattr); j.hasNext();) {
				final String k = j.next();
				final String val = getValue(xml, k);
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

	private byte[] addResult(final byte[] attrResult, final byte [] attribute, final String val) {
		byte attName[] = Asn1.newSeq;
		attName = Asn1.addTo(attName, Asn1.makeASN1(attribute, Asn1.OCTET_STRING));
		byte attVal[] = NEWSET;
		attVal = Asn1.addTo(attVal, patchOctetString(Asn1.makeASN1(val, Asn1.UTF8String)));
		attName = Asn1.addTo(attName, attVal);
		final byte[] newAttrResult = Asn1.addTo(attrResult, attName);
		return newAttrResult;
	}

	private String getValue(final XmlFile xml, final String k) {
		String val = xml.getString(k, "value", null);
		if (val == null) {
			val = xml.getString(k, "name", null);
		}
		// TODO remove this - it's only for upgrading existing data
		if (val != null)
			setValue(xml, k, val);
		return val;
	}

	private byte[] patchOctetString(final byte[] b) {
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
	private byte[] memberOfSearch(final XmlFile xml, byte[] attVal, final String key, final String dn) {
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
	private static String key2Dn(final XmlFile xml, final String key) {
		// create a dn
		String dnPath = key.substring(0, key.length() - 1); // skip last /slash
		final StringBuilder dn = new StringBuilder();
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
		final int nr = s.lastIndexOf('#');
		if (nr > 0) {
			s = s.substring(0, nr);
		} else if (s.endsWith("/")) {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String dn2Key(final ByteRef dn) {
		final ByteRef searchBaseDn = dn.clone();
		String dnKey = "";
		for (ByteRef part = searchBaseDn.nextWord(','); part != null; part = searchBaseDn.nextWord(',')) {
			final ByteRef key = part.nextWord('=');
			dnKey = "\\" + key + "\\" + part.trim('"').toString("utf-8") + "/" + dnKey;
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
	private void bind(final Iterator<Asn1> seq) throws IOException {
		final int version = seq.next().asInt();
		final ByteRef name = seq.next().asByteRef();
		final ByteRef auth = seq.next().asByteRef();

		final ByteRef dn = new ByteRef();
		final ByteRef errorMsg = new ByteRef();

		byte[] resultVal = RESULT_SUCCESS;

		final String key = "/ldap/" + dn2Key(name);

		if (name.length() > 0) {
			if (!checkPassword(key, auth)) {
				resultVal = ERROR_INVALID_CREDENTIALS;
			}
		}

		logFile.writeDate("bind: v" + version + " for '" + name + "' "
				+ ((resultVal == RESULT_SUCCESS) ? "login successful" : "login failed"));

		currentUser = name;

		// fetch the read and write permissions.
		final XmlFile xml = getXmlFile();
		currentReadPermissions.clear();
		for (final String rp : xml.getSections(key + "readpermission")) {
			final String val = xml.getString(rp, "name", "$invalid$");
			final String val2 = xml.normalizeSection("/ldap/" + dn2Key(new ByteRef(val)));
//			System.out.println("read: " + val + " -> " + val2);
			currentReadPermissions.add(val2);
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

	private void writeResponse(final byte[] responseContent) throws IOException {
		// support response caching
		if (currentResponses != null) {
			currentResponses.add(responseContent);
			return;
		}

		logResponse(responseContent);

		byte response[] = Asn1.newSeq;
		response = Asn1.addTo(response, Asn1.makeASN1(msgId, Asn1.INTEGER));
		response = Asn1.addTo(response, responseContent);

		os.write(response);

	}

	private void logResponse(final byte[] responseContent) {
		final Iterator<Asn1> msg = new Asn1(responseContent).children();
		final Asn1 cn = msg.next();
		final byte[] data = Asn1.getData(cn.toByteArray());
		if (data.length > 1)
			logFile.writeDate("> " + new ByteRef(data));
		final Asn1 r = msg.next();
		if ((0x1f & r.getType()) == Asn1.SEQUENCE) {
			for (final Iterator<Asn1> j = r.children(); j.hasNext();) {
				final Asn1 r2 = j.next();
				if ((0x1f & r2.getType()) == Asn1.SEQUENCE) {
					Iterator<Asn1> i = r2.children();
					final Asn1 nameAsn1 = i.next();
					final String name = nameAsn1.asByteRef().toString();
					final Asn1 setAsn1 = i.next();
					final StringBuilder vals = new StringBuilder();
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
	}

	Search createSearch(final Asn1 searchAsn) {
		final Iterator<Asn1> i = searchAsn.children();
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

		private final String attribute;

		private final int where;

		private final ArrayList<String> substrings = new ArrayList<String>();

		SubStringFilter(final Iterator<Asn1> i) {
			this.attribute = i.next().asByteRef().toString();
			final Asn1 list = i.next();
			this.where = list.getType() & 3;
			for (final Iterator<Asn1> j = list.children(); j.hasNext();) {
				final ByteRef val = j.next().asByteRef();
				substrings.add(val.toString().toLowerCase());
			}
		}

		
		public int match(final XmlFile xml, final String searchRoot) {

			String id = XmlFile.getLastSegment(searchRoot);
			if (attribute.equalsIgnoreCase(id)) {
				final String value = xml.getString(searchRoot, "name", null);
				for (final String v : substrings) {
					if (value.toLowerCase().indexOf(v) >= 0)
						return 1;
				}
			}

			for (final Iterator<String> i = xml.sections(searchRoot); i.hasNext();) {
				final String key = i.next();

				// skip LDAP objects - identified by nested object class
				if (xml.sections(key + "objectclass").hasNext())
					continue;

				id = getLastSegment(key);
				if (!attribute.equalsIgnoreCase(id))
					continue;

				final String value = xml.getString(key, "name", null);
				if (value == null)
					continue;

				for (final String v : substrings) {
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

		private final ByteRef attribute;

		private final int where;

		private final ArrayList<ByteRef> substrings = new ArrayList<ByteRef>();

		PresentFilter(final Iterator<Asn1> i) {
			this.attribute = i.next().asByteRef().toLowerCase();
			final Asn1 list = i.next();
			this.where = list.getType() & 3;
			for (final Iterator<Asn1> j = list.children(); j.hasNext();) {
				final ByteRef val = j.next().asByteRef();
				substrings.add(val);
			}
		}

		
		public int match(final XmlFile xml, final String searchRoot) {
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

		public OrFilter(final Iterator<Asn1> i) {
			while (i.hasNext()) {
				searches.add(createSearch(i.next()));
			}
		}

		
		public int match(final XmlFile xml, final String key) {
			for (final LdapProtocol.Search s : this.searches) {
				final int r = s.match(xml, key);
				if (r > 0) {
					return r;
				}
			}
			return -1;
		}

		
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("(|");
			for (final Search s : searches) {
				sb.append(s.toString());
			}
			sb.append(")");
			return sb.toString();
		}
	}

	class NotFilter implements LdapProtocol.Search {
		ArrayList<LdapProtocol.Search> searches = new ArrayList<Search>();

		public NotFilter(final Iterator<Asn1> i) {
			while (i.hasNext()) {
				searches.add(createSearch(i.next()));
			}
		}

		
		public int match(final XmlFile xml, final String key) {
			for (final LdapProtocol.Search s : this.searches) {
				final int r = s.match(xml, key);
				return r > 0 ? 0 : 1;
			}
			return -1;
		}

		
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("(!");
			for (final Search s : searches) {
				sb.append(s.toString());
			}
			sb.append(")");
			return sb.toString();
		}
	}

	class AndFilter implements Search {
		ArrayList<Search> searches = new ArrayList<Search>();

		public AndFilter(final Iterator<Asn1> i) {
			while (i.hasNext()) {
				searches.add(createSearch(i.next()));
			}
		}

		
		public int match(final XmlFile xml, final String key) {
			for (final Search s : searches) {
				final int r = s.match(xml, key);
				if (r <= 0)
					return r;
			}
			return 1;
		}

		
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("(&");
			for (final Search s : searches) {
				sb.append(s.toString());
			}
			sb.append(")");
			return sb.toString();

		}
	}

	class EqualityFilter implements Search {
		private final ByteRef what;

		private final String value;

		public EqualityFilter(final Iterator<Asn1> i) {
			what = i.next().asByteRef().toLowerCase();
			final ByteRef v = i.next().asByteRef();
			if (v.startsWith(what) && v.charAt(what.length()) == '=')
				v.nextWord('=');
			value = v.toString().toLowerCase();
		}

		/**
		 * A XML node can reflect an LDAP object or an attribute.
		 *
		 * If the last segment matches, it's a LDAP object. Otherwise it's an attribute.
		 *
		 * Ensure that LDAP objects are not found as attribute.
		 */
		
		public int match(final XmlFile xml, final String searchRoot) {
			final String id = XmlFile.getLastSegment(searchRoot);
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
		final int slash = key.lastIndexOf('/', key.length() - 2) + 1;
		int num = key.indexOf('#', slash);
		if (num < slash)
			num = key.length() - 1;
		return key.substring(slash, num);
	}

	static String base(final String segment) {
		if (!segment.startsWith("\\"))
			return segment;
		final int b2 = segment.indexOf('\\', 1);
		return segment.substring(1, b2);
	}

}
