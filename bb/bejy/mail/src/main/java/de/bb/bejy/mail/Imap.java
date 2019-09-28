/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2016.
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

package de.bb.bejy.mail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeSet;

import de.bb.bejy.Version;
import de.bb.log.Logger;
import de.bb.util.ByteRef;
import de.bb.util.DateFormat;
import de.bb.util.LogFile;
import de.bb.util.Mime;
import de.bb.util.MultiMap;
import de.bb.util.Pair;

final class Imap extends de.bb.bejy.Protocol {
	private final static Logger LOG = Logger.getLogger(Imap.class);

	private final static boolean DEBUG = false;

	boolean VERBOSE = DEBUG;

	// version stuff

	private final static String version;
	static {
		version = Version.getShort() + " IMAP " + V.V + " (c) 2000-" + V.Y
				+ " by BebboSoft, Stefan \"Bebbo\" Franke, all rights reserved";
	}

	/**
	 * return the version number.
	 * 
	 * @return
	 */
	public static String getVersion() {
		return V.V;
	}

	/**
	 * returns a full version String.
	 * 
	 * @return
	 */
	public static String getFull() {
		return version;
	}

	private final static int S_UNAUTHENTICATED = 0, S_AUTH = 3;

	final static ByteRef BAD_COMMANDLINE = new ByteRef("* BAD invalid command line"),
			BAD_COMMAND = new ByteRef("* BAD command unknown or not valid in this state: "), OK = new ByteRef(" OK "),
			COMPLETED = new ByteRef(" completed\r\n"), BAD = new ByteRef(" BAD "),
			UNKNOWN = new ByteRef(" command unknown or arguments invalid\r\n"), NO = new ByteRef(" NO "),
			// FAILURE = new ByteRef(" failure: "),
			CAPABILITY = new ByteRef("CAPABILITY"),
			CAPABILITY_MSG = new ByteRef("* CAPABILITY IMAP4rev1 AUTH=PLAIN QUOTA IDLE MOVE"),
			NOOP = new ByteRef("NOOP"), CHECK = new ByteRef("CHECK"), LOGOUT = new ByteRef("LOGOUT"),
			LOGOUT_MSG = new ByteRef("* BYE IMAP4rev1 Server logging out"), LOGIN = new ByteRef("LOGIN"),
			AUTHENTICATE = new ByteRef("AUTHENTICATE"), PLAIN = new ByteRef("PLAIN"),
			CONTINUE = new ByteRef("+ \"\"\r\n"), LOGIN_NO = new ByteRef("user name/password rejected"),
			LIST = new ByteRef("LIST"), LIST_MSG = new ByteRef("* LIST "), LSUB = new ByteRef("LSUB"),
			LSUB_MSG = new ByteRef("* LSUB "),
			// LSUB_MSG2 = new ByteRef("* LSUB (\\Noselect) \"/\" "),
			CREATE = new ByteRef("CREATE"), RENAME = new ByteRef("RENAME"), DELETE = new ByteRef("DELETE"),
			COPY = new ByteRef("COPY"), MOVE = new ByteRef("MOVE"), EXPUNGE = new ByteRef("EXPUNGE"),
			CLOSE = new ByteRef("CLOSE"), STATUS = new ByteRef("STATUS"), SUBSCRIBE = new ByteRef("SUBSCRIBE"),
			UNSUBSCRIBE = new ByteRef("UNSUBSCRIBE"), EXAMINE = new ByteRef("EXAMINE"),
			// same as SELECT but read only
			SELECT = new ByteRef("SELECT"), SELECT_NO = new ByteRef("cannot select that folder [TRYCREATE]"),
			SELECT_MSG = new ByteRef("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)"),
			APPEND = new ByteRef("APPEND"), FETCH = new ByteRef("FETCH"), SEARCH = new ByteRef("SEARCH"),
			STORE = new ByteRef("STORE"), UID = new ByteRef("UID"), UID_ = new ByteRef("UID "),
			CRLF = new ByteRef("\r\n"), SLASH = new ByteRef("/"), MOREDATA = new ByteRef("+ Ready for more data\r\n"),
			IDLE = new ByteRef("IDLE"), IDLING = new ByteRef("+ idling\r\n"), DONE = new ByteRef("DONE");
	final static ByteRef E_NOFOLDERSELECTED = new ByteRef(" no folder selected");
	final static ByteRef MISSINGPARAMETER = new ByteRef(" not enough parameters specified");
	final static ByteRef MISSINGNUMBER = new ByteRef(" missing message number");
	final static ByteRef INTERNALERROR = new ByteRef(" internal error");
	final static ByteRef READONLY = new ByteRef(" since it is readonly");
	final static ByteRef ONETOSTAR = new ByteRef("1:*");
	static final ByteRef SPACE = new ByteRef(" ");
	static final ByteRef QUOTESPACE = new ByteRef("\" ");
	static final ByteRef QUOTE = new ByteRef("\"");

	private final static byte[] welcomeBytes = ("* OK " + getFull() + " ready " + CRLF).getBytes();
	static final String[] NONAMES = {};

	private static final ByteRef EMPTY = new ByteRef();

	private static final ByteRef SETQUOTA = new ByteRef("SETQUOTA");

	private static final ByteRef GETQUOTA = new ByteRef("GETQUOTA");

	private static final ByteRef GETQUOTAROOT = new ByteRef("GETQUOTAROOT");

	private static final ByteRef PERMISSIONDENIED = new ByteRef("permission denied!");

	// where to log to
	LogFile logFile;

	// used implementations
	MailDBI mDbi;

	// a list of replies
	LinkedList<ByteRef> replies = new LinkedList<ByteRef>();

	// default domain
	ByteRef bServerName;

	ByteRef br, line;

	byte[] data;
	byte[] data2 = new byte[512];

	HashMap<String, ByteRef> header = new HashMap<String, ByteRef>();

	String headerPath = null;

	boolean idle = false;

	// boolean checkStatus = false;
	String mbId = null;

	static MultiMap<String, Imap> mailBoxes = new MultiMap<String, Imap>();

	static Hashtable<String, Object> mailBoxLock = new Hashtable<String, Object>();

	protected Imap(ImapFactory pf, LogFile _logFile) {
		super(pf);
		logFile = _logFile;

		VERBOSE = DEBUG | pf.isVerbose();

		bServerName = new ByteRef(pf.mailCfg.getMainDomain());
		data = new byte[8192];
	}

	// overwrite the trigger method, since clients expects a message first
	protected boolean trigger() throws Exception {
		try {
			if (DEBUG) {
				logFile.writeln(new String(welcomeBytes));
			}
			os.write(welcomeBytes);
			os.flush();
		} catch (Exception e) {
			return false;
		}
		return super.trigger();
	}

	/**
	 * This function is called to notify for STATUS msg on incoming mails.
	 * 
	 * @param msg
	 * @throws IOException
	 */
	void updateStatus(String msg) throws IOException {
		synchronized (os) {
			if (idle) {
				os.write(msg.getBytes());
				CRLF.writeTo(os);
				os.flush();
				if (DEBUG)
					logFile.write(msg + "\r\n");
				return;
			}
			replies.add(new ByteRef(msg));
		}
	}

	/**
	 * Notify an incoming mail.
	 * 
	 * @param mbId
	 * @param msg
	 */
	static void updateStatus(String mbId, String msg) {
		synchronized (mailBoxLock) {
			Iterator<Entry<String, Imap>> i = mailBoxes.tailMap(mbId).entrySet().iterator();
			while (i.hasNext()) {
				Entry<String, Imap> e = i.next();
				if (!e.getKey().equals(mbId)) {
					return;
				}
				Imap imap = e.getValue();
				try {
					imap.updateStatus(msg);
				} catch (Exception ex) {
					i.remove();
					if (mailBoxes.get(mbId) == null) {
						mailBoxLock.remove(mbId);
					}
					return;
				}
			}
		}
	}

	/**
	 * Select the mailbox with given id and register this instance to listen for new
	 * mails.
	 * 
	 * @param mid
	 */
	void selectMailbox(String mid) {
		synchronized (mailBoxLock) {
			// remove from old notification list
			if (mbId != null) {
				mailBoxes.remove(mbId, this);
				if (mailBoxes.get(mbId) == null) {
					mailBoxLock.remove(mbId);
				}
			}

			mbId = mid;

			// add to new notification list
			if (mbId != null) {
				if (mailBoxes.get(mbId) == null) {
					mailBoxLock.put(mbId, new Object());
				}
				mailBoxes.put(mbId, this);
			}
		}
	}

	/**
	 * Main worker loop.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected boolean doit() throws Exception {
		ByteRef user = null, domain = null, passwd = null;
		String userId = null, unitId = null;
		mbId = null;
		idle = false;
		boolean readOnly = true;

		int state = S_UNAUTHENTICATED;

		logFile.writeDate("connect from " + remoteAddress);
		replies.clear();

		br = readFirst();
		boolean run = true;
		try {
			while (run) {
				// if (DEBUG) logFile.write(br.toString());
				os.flush();

				idle = true;
				line = readLine(br);
				synchronized (os) {
					idle = false;
				}

				if (line == null) {
					return false;
				}

				if (VERBOSE) {
					if (line.toString().toUpperCase().indexOf("LOGIN") >= 0) {
						logFile.writeln("1 LOGIN ...");
					} else {
						logFile.writeln(line.toString());
					}
				}
				boolean useUid = false;
				ByteRef cmdId = line.nextWord();
				ByteRef cmd = line.nextWord();
				if (cmd == null) {
					BAD_COMMANDLINE.writeTo(os);
					CRLF.writeTo(os);
					os.flush();
					continue;
				}
				cmd = cmd.toUpperCase();

				ByteRef retVal = null;
				int ret = -1;
				// cmdId = commandID
				// cmd = command
				// line = parameters

				mDbi = ((MailFactory) factory).getDbi(this);
				try {
					command:
					// check command that are always allowed
					{
						if (cmdId == null) {
							// replies.add(BAD_COMMANDLINE);
							retVal = BAD_COMMANDLINE;
							break command;
						}

						if (cmd.equals(CAPABILITY)) {
							replies.add(CAPABILITY_MSG);
							ret = 0;
							break command;
						}

						if (cmd.equals(NOOP) || cmd.equals(CHECK)) {
							if (unitId != null && mbId != null) {
								String fc = mDbi.selectCountFromImapData(mbId);
								if (fc != null) {
									replies.add(new ByteRef("* " + fc + " EXISTS"));
									String rc = mDbi.selectRecentCountFromImapData(mbId);
									replies.add(new ByteRef("* " + rc + " RECENT"));
								}
							}
							ret = 0;
							break command;
						}

						if (cmd.equals(IDLE)) {
							// release the DBI otherwise it's locked while the client is waiting.
							((MailFactory) factory).releaseDbi(this, mDbi);
							mDbi = null;

							IDLING.writeTo(os);
							os.flush();
							if (VERBOSE)
								logFile.write("enter IDLE\r\n");
							idle = true;
							do {
								line = readLine(br);
								if (VERBOSE && line != null)
									logFile.write(line.toString() + "\r\n");
							} while (!DONE.equals(line) && line != null);
							if (VERBOSE)
								logFile.write("leave IDLE\r\n");

							synchronized (os) {
								idle = false;
							}
							ret = 0;
							break command;
						}

						if (cmd.equals(LOGOUT)) {
							replies.add(LOGOUT_MSG);
							ret = 0;
							run = false; // good bye now!
							break command;
						}

						switch (state) {
						case S_UNAUTHENTICATED:
							if (cmd.equals(LOGIN) || cmd.equals(AUTHENTICATE)) {
								ret = 2;
								if (cmd.equals(LOGIN)) {
									user = nextAString();
									passwd = nextAString();

									if (VERBOSE) {
										logFile.writeln("user=" + user);
									}

								} else {
									// AUTHENTICATE
									final ByteRef kind = nextAString();
									if (VERBOSE)
										logFile.writeln("using AUTHENTICATE " + kind);
									if (!PLAIN.equals(kind))
										break command;

									ByteRef data = nextAString();
									if (VERBOSE)
										logFile.writeln("AUTHENTICATE data:" + data);
									if (data == null) {
										if (VERBOSE)
											logFile.write(CONTINUE.toString());
										CONTINUE.writeTo(os);
										os.flush();
										line = null;
										data = nextAString();
										if (VERBOSE)
											logFile.writeln("data=" + data);
										if (data != null)
											data = new ByteRef(Mime.decode(data.toByteArray(), 0, data.length()));
									}
									if (data != null) {
										final ByteRef authcid = data.nextWord(0);
										user = data.nextWord(0);
										passwd = data;
									}
									if (VERBOSE) {
										logFile.writeln("user=" + user);
									}
									if (passwd == null || user == null) {
										passwd = new ByteRef();
										if (user == null)
											user = new ByteRef();
									}
								}

								user = user.toLowerCase();
								domain = bServerName;
								int idx = user.indexOf('@');
								if (idx == -1) {
									idx = user.indexOf('%');
								}
								if (idx > 0) {
									domain = user.substring(idx + 1);
									user = user.substring(0, idx);
								}
								if (user.length() == 0) {
									break command;
								}

								ret = 1;
								retVal = LOGIN_NO;

								// release the DBI otherwise it's locked while the client is waiting.
								((MailFactory) factory).releaseDbi(this, mDbi);
								mDbi = null;
								if (MailDBI.delayLogin(user.toString(), domain.toString(), remoteAddress)) {
									// verify user/domain/password
									mDbi = ((MailFactory) factory).getDbi(this);
									userId = mDbi.verifyUser(user.toString(), domain.toString(), passwd.toString(),
											remoteAddress, null);

									if (userId == null) {
										MailDBI.handleLoginFailure(user.toString(), domain.toString(), remoteAddress);
										logFile.writeDate(
												"login FAILURE for " + user + "@" + domain + " from " + remoteAddress);
										break command;
									}
									unitId = mDbi.getOrCreateUnitId(userId);
								}

								if (unitId == null) {
									break command;
								}

								state = S_AUTH;
								ret = 0;
								logFile.writeDate(
										"login SUCCESS for " + user + "@" + domain + " from " + remoteAddress);
								break command;
							}
							break;
						// commands with login
						case S_AUTH:

							if (cmd.equals(SETQUOTA)) {
								ret = 1;
								retVal = PERMISSIONDENIED;
								break command;
							}
							if (cmd.equals(GETQUOTAROOT) || cmd.equals(GETQUOTA)) {
								ByteRef ref = nextAString();
								if (cmd.equals(GETQUOTAROOT)) {
									replies.add(new ByteRef("* QUOTAROOT \"" + ref + "\" \"\""));
									ref = EMPTY;
								}

								// atm we support only ""
								if (ref.length() == 0) {
									String sUser = user.toString();
									String sDomain = domain.toString();
									long quota = mDbi.getPoBoxQuota(sUser, sDomain);
									if (quota > 0) {
										long usage = mDbi.getPoBoxSize(sUser, sDomain);
										replies.add(new ByteRef("* QUOTA \"\" (STORAGE " + (usage / 1024) + " "
												+ (quota / 1024) + ")"));
									}
								}
								ret = 0;
								break command;
							}

							if (cmd.equals(LIST)) {
								ByteRef ref = nextAString();
								if (ref.equalsIgnoreCase("INBOX")) {
									ref.toUpperCase();
								}
								ByteRef mask = nextAString();
								ret = list(unitId, ref.toString(), mask.toString()) ? 0 : 1;
								break command;
							}

							if (cmd.equals(LSUB)) {
								ByteRef ref = nextAString();

								if (ref.equalsIgnoreCase("INBOX")) {
									ref.toUpperCase();
								}
								ByteRef mask = nextAString();
								ret = lsub(userId, ref.toString(), mask.toString()) ? 0 : 1;
								break command;
							}

							if (cmd.equals(CREATE)) {
								ByteRef mb = nextAString().trim('/');

								if (mb.equalsIgnoreCase("INBOX")) {
									ret = 1;
								} else {
									ret = create(unitId, mb.toString()) ? 0 : 1;
								}
								break command;
							}

							if (cmd.equals(RENAME)) {
								ByteRef oldName = nextAString().trim('/');
								ByteRef newName = nextAString().trim('/');

								if (oldName.equalsIgnoreCase("INBOX") || newName.equalsIgnoreCase("INBOX")) {
									ret = 1;
								} else {
									ret = rename(unitId, oldName.toString(), newName.toString()) ? 0 : 1;
								}
								break command;
							}

							if (cmd.equals(DELETE)) {
								ByteRef mb = nextAString().trim('/');

								if (mb.equalsIgnoreCase("INBOX")) {
									ret = 1;
								} else {
									ret = delete(unitId, mb.toString()) ? 0 : 1;
								}
								break command;
							}

							if (cmd.equals(SUBSCRIBE)) {
								ByteRef mb = nextAString().trim('/');
								if (mb.equalsIgnoreCase("INBOX")) {
									mb.toUpperCase();
								}

								ret = subscribe(userId, mb.toString()) ? 0 : 1;
								break command;
							}

							if (cmd.equals(UNSUBSCRIBE)) {
								ByteRef mb = nextAString().trim('/');
								if (mb.equalsIgnoreCase("INBOX")) {
									mb.toUpperCase();
								}

								ret = unsubscribe(userId, mb.toString()) ? 0 : 1;
								break command;
							}

							if (cmd.equals(SELECT) || cmd.equals(EXAMINE)) {
								readOnly = false;
								readOnly = cmd.equals(EXAMINE);
								ret = 1;
								ByteRef mb = nextAString().trim('/');
								if (mb.equalsIgnoreCase("INBOX")) {
									mb.toUpperCase();
								}

								selectMailbox(mDbi.mailboxId(unitId, mb.toString()));
								// add [TRYCREATE]
								if (mbId != null) {
									ret = select(mbId) ? 0 : 1;
								}
								if (ret != 0) {
									retVal = SELECT_NO;
								}

								replies.add(SELECT_MSG);
								break command;
							}

							if (cmd.equals(APPEND)) {
								ByteRef mb = nextAString().trim('/');
								if (mb.equalsIgnoreCase("INBOX")) {
									mb.toUpperCase();
								}

								ByteRef flags = new ByteRef();
								ByteRef date = new ByteRef();

								line = line.trim();

								if (line.charAt(0) == '(') {
									int idx = line.indexOf(')');
									flags = line.substring(1, idx);
									line = line.substring(idx + 1).trim();
								}

								if (line.charAt(0) == '"') {
									int idx = line.indexOf('"', 1);
									date = line.substring(1, idx).trim();
									line = line.substring(idx + 1).trim();
								}

								if (line.charAt(0) != '{') {
									break command;
								}
								int idx = line.indexOf('}');
								int len = line.substring(1, idx).toInteger();

								MOREDATA.writeTo(os);
								os.flush();

								MailEntry me = mDbi.createNewMail();
								OutputStream fos = mDbi.getOutputStream(me.mailId);
								try {
									// collect the mail
									while (len > 0) {
										int b0 = is.read();
										if (b0 < 0)
											throw new IOException("no data in APPEND");
										fos.write(b0);
										--len;
										if (len == 0 || is.available() == 0)
											continue;
										
										int read = data.length < len ? data.length : len;
										read = is.read(data, 0, read);
										if (read == 0)
											throw new IOException("no data in APPEND");
										if (read > 0) {
											fos.write(data, 0, read);
										}
										len -= read;
									}

									// eat CRLF
									is.read();
									is.read();

									fos.flush();
								} finally {
									fos.close();
								}

								ret = mDbi.append(unitId, mb.toString(), flags.toString(),
										DateFormat.parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(date.toString()), me) ? 0 : 1;

								break command;
							}

							if (cmd.equals(STATUS)) {
								ByteRef mb = nextAString().trim('/');
								if (mb.equalsIgnoreCase("INBOX")) {
									mb.toUpperCase();
								}

								String s = status(unitId, mb.toString(), line.toUpperCase().toString());

								ret = s != null ? 0 : 1;
								if (s == null) {
									break command;
								}

								replies.add(new ByteRef(s));

								break command;
							}

							// selected state
							if (mbId != null) {
								if (cmd.equals(UID)) {
									useUid = true;
									cmd = line.nextWord().trim().toUpperCase();
								} else {
									useUid = false;
								}

								if (cmd.equals(FETCH)) {
									ByteRef messages = line.nextWord();
									ByteRef what = line.trim();
									ret = fetch(mbId, messages.toString(), useUid, what.toString()) ? 0 : 1;
									break command;
								}

								if (cmd.equals(STORE)) {
									if (readOnly) {
										ret = 1;
										retVal = READONLY;
										break command;
									}
									ByteRef messages = line.nextWord();
									ByteRef subcmd = line.nextWord().toUpperCase();
									ByteRef flags = line.trim().trim('(').trim(')').trim().toUpperCase();
									ret = store(mbId, messages.toString(), useUid, subcmd.toString(), flags.toString())
											? 0
											: 1;
									break command;
								}

								if (cmd.equals(COPY)) {
									if (readOnly) {
										ret = 1;
										retVal = READONLY;
										break command;
									}
									ByteRef messages = line.nextWord();
									ByteRef destBox = nextAString().trim('/');
									ret = copyMove(unitId, mbId, messages.toString(), useUid, destBox.toString(), false)
											? 0
											: 1;
									break command;
								}

								if (cmd.equals(MOVE)) {
									if (readOnly) {
										ret = 1;
										retVal = READONLY;
										break command;
									}
									ByteRef messages = line.nextWord();
									ByteRef destBox = nextAString().trim('/');
									ret = copyMove(unitId, mbId, messages.toString(), useUid, destBox.toString(), true)
											? 0
											: 1;
									break command;
								}

								if (cmd.equals(SEARCH)) {
									ret = search(mbId, useUid) ? 0 : 1;
									break command;
								}

								if (cmd.equals(EXPUNGE)) {
									if (readOnly) {
										ret = 1;
										retVal = READONLY;
										break command;
									}
									ret = expunge(mbId) ? 0 : 1;
									break command;
								}

								if (cmd.equals(CLOSE)) {
									ret = 0;
									if (!readOnly) {
										ret = close(mbId) ? 0 : 1;
									}
									mbId = null; // unselect!
									break command;
								}
							}

							break;
						}
						if (useUid) {
							cmd = UID_.append(cmd);
						}
						useUid = false;

						// replies.add(BAD_COMMAND.append(cmd));
						retVal = BAD_COMMAND;
						ret = 1;
					} // command:
				} catch (Exception e) {
					if (e instanceof NullPointerException) {
						retVal = MISSINGPARAMETER;
					} else if (e instanceof NumberFormatException) {
						retVal = MISSINGNUMBER;
					} else {
						if (e instanceof IOException)
							LOG.debug(e.getMessage(), e);
						else
							LOG.error(e.getMessage(), e);
						retVal = INTERNALERROR;
					}
					ret = 1;
				} finally {
					((MailFactory) factory).releaseDbi(this, mDbi);
					mDbi = null;
				}

				// send all replies
				while (replies.size() > 0) {
					ByteRef msg = replies.remove(0);
					if (VERBOSE) {
						logFile.writeln(msg.toString());
					}

					msg.writeTo(os);
					CRLF.writeTo(os);
				}

				if (useUid) {
					cmd = UID_.append(cmd);
				}

				// send result of command
				switch (ret) {
				case 0:
					if (VERBOSE) {
						logFile.write(cmdId.toString() + " OK " + cmd + COMPLETED);
					}
					cmdId.writeTo(os);
					OK.writeTo(os);
					cmd.writeTo(os);
					COMPLETED.writeTo(os);
					break;
				case 1:
					if (VERBOSE) {
						logFile.writeln(cmdId.toString() + " NO " + cmd + ' ' + retVal);
					}
					cmdId.writeTo(os);
					NO.writeTo(os);
					cmd.writeTo(os);
					if (retVal != null) {
						os.write(32);
						retVal.writeTo(os);
					}
					CRLF.writeTo(os);
					break;
				case 2:
					if (VERBOSE) {
						logFile.writeln(cmdId.toString() + " BAD " + cmd + UNKNOWN);
					}
					cmdId.writeTo(os);
					BAD.writeTo(os);
					cmd.writeTo(os);
					UNKNOWN.writeTo(os);
					break;
				}
			}
			os.flush();
		} catch (Exception ex) {
			if (!(ex instanceof IOException))
				LOG.error(ex.getMessage(), ex);
			throw ex;
		} finally {
			selectMailbox(null);
		}
		return false; // don't reuse socket
	}

	private static String handleWildchar(String s) {
		// change stars into %, probably a wrong handling...
		// first the % at end handling should be performed
		byte b[] = s.getBytes();
		for (int i = 0; i < b.length; ++i) {
			if (b[i] == '*') {
				b[i] = '%';
			}
		}
		return new String(b);
	}

	/**
	 * IMAP list function
	 * 
	 * @param unitId
	 * @param reference
	 * @param mask
	 * @return
	 * @throws Exception
	 */
	boolean list(String unitId, String reference, String mask) throws Exception {
		if (DEBUG) {
			logFile.writeln("list(" + unitId + ", " + reference + ", " + mask + ")");
		}

		// handle empty mask argument
		if (mask.length() == 0) {
			LIST_MSG.writeTo(os);
			// l.add("(\\Noselect) \"/\" \"\"");
			os.write("(\\Noselect) \"/\" \"\"\r\n".getBytes());
			if (DEBUG) {
				logFile.writeln("* LIST (\\Noselect) \"/\" \"\"");
			}
			return true;
		}

		// reference is ignored right now...
		// insert code here

		// get the unitId if the resulting mask starts with a '#'
		if (mask.charAt(0) == '#') {
			int idx = mask.indexOf('/');
			if (idx == -1) {
				return false;
			}
			++idx;
			String unitName = mask.substring(0, idx);
			mask = mask.substring(idx);

			ResultSet rs = mDbi.selectFromImapUnit(unitName);
			if (!rs.next()) {
				return false;
			}

			// use THIS unitId!
			unitId = rs.getString(1);
		}

		// check whether only current level is queried
		boolean all = mask.endsWith("*");

		mask = handleWildchar(mask);

		ResultSet rs;
		if (all) {
			rs = mDbi.selectAllLevelsFromImapFolder(unitId, mask);
		} else {
			rs = mDbi.selectThisLevelFromImapFolder(unitId, mask);
		}

		// String current;
		while (rs.next()) {
			String name = rs.getString(1);
			// String here = name;
			/*
			 * // add \Noselect for (;;) { int idx = here.lastIndexOf('/'); if (idx < 0)
			 * break;
			 * 
			 * here = here.substring(0, idx); if (current.startsWith(here)) break; //
			 * l.add("(\\Noselect) \"/\" \"" + here + '"'); LIST_MSG.writeTo(os);
			 * os.write(("(\\Noselect) \"/\" \"" + here + "\"\r\n").getBytes()); if
			 * (VERBOSE) logFile.write("* LIST (\\Noselect) \"/\" \"" + here + "\""); }
			 */
			// l.add("() \"/\" \"" + name + '"');
			LIST_MSG.writeTo(os);
			os.write(("() \"/\" \"" + name + "\"\r\n").getBytes());
			if (VERBOSE) {
				logFile.writeln("* LIST () \"/\" \"" + name + "\"");
				// current = name;
			}
		}
		rs.close();
		return true;
	}

	boolean create(String unitId, String mailbox) {
		if (DEBUG) {
			logFile.writeln("create(" + unitId + ", " + mailbox + ")");
		}
		try {
			return mDbi.insertIntoImapFolder(unitId, mailbox);
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * IMAP lsub function
	 * 
	 * @param userId
	 * @param reference
	 * @param mask
	 * @return
	 * @throws Exception
	 */
	boolean lsub(String userId, String reference, String mask) {
		if (DEBUG) {
			logFile.writeln("lsub(" + reference + ", " + mask + ")");
		}

		try {
			if (mask.length() == 0) {
				return true;
			}

			mask = handleWildchar(mask);

			ResultSet rs = mDbi.selectFromImapSubsLike(userId, mask);
			while (rs.next()) {
				// String id = rs.getString(1);
				String name = rs.getString(3);
				// l.add(" () \"/\" \"" + name + '"');
				LSUB_MSG.writeTo(os);
				os.write(("() \"/\" \"" + name + "\"\r\n").getBytes());
				if (VERBOSE) {
					logFile.writeln("* LSUB () \"/\" \"" + name + "\"");
				}
			}
			rs.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void readLine() throws Exception {
		// read more data if necessary
		if (line == null) {
			line = readLine(br);
		}
		if (line == null) {
			throw new IOException("EOF in nextString()");
		}
	}

	private ByteRef nextAString() throws Exception {
		readLine();
		if ("{\"".indexOf(line.charAt(0)) >= 0) {
			return nextString();
		}

		return line.nextWord();
	}

	private ByteRef nextAStringNoRead() throws Exception {
		if (line == null) {
			return null;
		}
		if ("{\"".indexOf(line.charAt(0)) >= 0) {
			return nextString();
		}

		return line.nextWord();
	}

	private ByteRef nextString() throws Exception {
		readLine();

		// literal
		if (line.charAt(0) == '{') {
			ByteRef bsz = line.substring(1).nextWord('}');
			int sz = bsz.toInteger();
			if (sz > data.length) {
				data = new byte[sz];
			}
			MOREDATA.writeTo(os);
			os.flush();

			int pos = 0;

			if (br != null && br.length() > 0) {
				int copy = sz;
				if (br.length() < copy) {
					copy = br.length();
				}
				br.copy(data, pos, copy);
				br = br.substring(copy);
				pos = copy;
			}

			while (pos < sz) {
				int n = is.read(data, pos, sz - pos);
				if (n <= 0) {
					throw new IOException("EOF in nextString()");
				}
				pos += n;
			}
			ByteRef r = new ByteRef(data, 0, pos);
			int ch;
			line = null;
			if (br.length() > 0) {
				ch = br.charAt(0);
				br = br.substring(1);
			} else {
				ch = is.read();
			}
			if (ch == ' ') {
				readLine();
			} else {
				if (br.length() > 0) {
					br = br.substring(1);
				} else {
					is.read();
				}
			}
			// logFile.write("nextString: " + r);
			return r;
		}

		// quoted char
		line = line.substring(1);
		ByteRef r = line.nextWord('"');
		line = line.trim();
		// logFile.write("nextString: " + r);
		return r;
	}

	boolean subscribe(String userId, String mailbox) throws Exception {
		if (DEBUG) {
			logFile.writeln("subsribe(" + userId + ", " + mailbox + ")");
		}

		// return true, if mb already exists
		ResultSet rs = mDbi.selectFromImapSubs(userId, mailbox);
		try {
			if (rs.next()) {
				return true;
			}

			return mDbi.insertIntoImapSubs(userId, mailbox);
		} finally {
			rs.close();
		}
	}

	boolean unsubscribe(String userId, String mailbox) throws Exception {
		if (DEBUG) {
			logFile.writeln("unsubscribe(" + userId + ", " + mailbox + ")");
		}
		return 0 != mDbi.deleteFromImapSubs(userId, mailbox);
	}

	boolean select(String mbId) throws Exception {
		if (DEBUG) {
			logFile.writeln("select(" + mbId + ")");
		}

		// List l = new LinkedList();

		// get count of files
		String fc = mDbi.selectCountFromImapData(mbId);
		if (fc == null) {
			return false;
		}

		// l.add("* " + fc + " EXISTS");
		os.write(("* " + fc + " EXISTS\r\n").getBytes());
		if (DEBUG) {
			logFile.writeln("* " + fc + " EXISTS");
		}

		// get count of recent files
		String rc = mDbi.selectRecentCountFromImapData(mbId);

		// l.add("* " + rc + " RECENT");
		os.write(("* " + rc + " RECENT\r\n").getBytes());
		if (DEBUG) {
			logFile.writeln("* " + rc + " RECENT");
		}

		String first = mDbi.selectFirstUnseenIDFromImapData(mbId);
		if (first != null) {
			os.write(("* OK [UNSEEN " + first + "] Message " + first + " is the first unseen\r\n").getBytes());
			if (DEBUG) {
				logFile.writeln("* OK [UNSEEN " + first + "] Message " + first + " is the first unseen");
			}
		}

		// UIDValidity
		// l.add("* OK [UIDVALIDITY " + mbId + "] UIDs valid");
		os.write(("* OK [UIDVALIDITY " + mbId + "] UIDs valid\r\n").getBytes());
		if (DEBUG) {
			logFile.writeln("* OK [UIDVALIDITY " + mbId + "] UIDs valid");
		}

		return true;
	}

	boolean rename(String unitId, String oldName, String newName) throws Exception {
		if (DEBUG) {
			logFile.writeln("rename(" + unitId + ", " + oldName + ", " + newName + ")");
		}
		// rename
		return mDbi.updateImapFolder(unitId, oldName, newName);
	}

	boolean delete(String unitId, String mailbox) throws Exception {
		if (DEBUG) {
			logFile.writeln("select(" + unitId + ", " + mailbox + ")");
		}

		// get mailbox id
		String mbId = mDbi.mailboxId(unitId, mailbox);

		return 0 != mDbi.deleteImapFolder(mbId);
	}

	HashMap<String, ByteRef> readHeader(RandomAccessFile raf, String fileName, String path) throws Exception {
		// use last cached data
		if (path.equals(headerPath)) {
			return header;
		}

		header.clear();
		headerPath = path;
		ResultSet rs2 = mDbi.selectFromImapMime(fileName, path);
		try {
			if (!rs2.next()) {
				return header;
			}

			int offset = rs2.getInt(4);
			int len = rs2.getInt(5) - offset;
			raf.seek(offset);

			if (data2.length < len) {
				data2 = new byte[len];
			}
			for (int off = 0; off < len;) {
				int sz = raf.read(data2, off, len - off);
				if (sz <= 0) {
					throw new IOException("read error");
				}
				off += sz;
			}

			ByteRef all = new ByteRef(data2, 0, len);

			// parse the header
			ByteRef l = new ByteRef();
			while (all.length() > 0) {
				l = l.append(all.nextWord(0xa));
				l = l.trim();
				while ("\r\n".indexOf(all.charAt(0)) >= 0) {
					all = all.substring(1);
				}
				if (all.charAt(0) > 0 && all.charAt(0) <= 32) {
					l = l.append(CRLF);
					continue;
				}

				if (l.indexOf(':') > 0) {
					String key = l.substring(0).nextWord(':').toString().toLowerCase();
					header.put(key, l);
				}
				l = new ByteRef();
			}
			return header;
		} finally {
			rs2.close();
		}
	}

	interface MsgHandler {
		/**
		 * @param pos
		 *            current position
		 * @param uid
		 *            unique message id
		 * @param mbId
		 *            message box id
		 * @param rs
		 *            ResultSet to query current message String fileName =
		 *            rs.getString(3); long last = rs.getTimestamp(4).getTime(); boolean
		 *            f1 = rs.getBoolean(5); boolean f2 = rs.getBoolean(6); boolean f3 =
		 *            rs.getBoolean(7); boolean f4 = rs.getBoolean(8); boolean f5 =
		 *            rs.getBoolean(9); String size = rs.getString(10);
		 * @param o
		 *            a parameter object passed to the handler
		 * @throws Exception
		 */
		void call(long pos, String uid, String mbId, ResultSet rs, Object o) throws Exception;

	}

	static class Fetch {
		boolean body = false, bodyStructure = false, envelope = false, flags = false, internalDate = false,
				size = false, uid = false, seen = false;

		String names[], parts[];

		int offsets[], sizes[];

		HashMap<?, ?>[] hashs;

		void get(ArrayList<String> vNames, ArrayList<String> vParts, ArrayList<String> vOffsets,
				ArrayList<String> vSizes, ArrayList<HashMap<?, ?>> vHashtables) {
			names = new String[vNames.size()];
			parts = new String[names.length];
			offsets = new int[names.length];
			sizes = new int[names.length];
			hashs = new HashMap<?, ?>[names.length];

			for (int i = 0; i < names.length; ++i) {
				names[i] = vNames.get(i);
				parts[i] = vParts.get(i);
				String s = vOffsets.get(i);
				offsets[i] = s != null ? Integer.parseInt(s) : -1;
				s = vSizes.get(i);
				sizes[i] = s != null ? Integer.parseInt(s) : -1;
				hashs[i] = vHashtables.get(i);
			}
		}
	}

	final static ByteRef
	// ONE = new ByteRef("1"),
	ALL = new ByteRef("ALL"), BODY = new ByteRef("BODY"), BODYSTRUCTURE = new ByteRef("BODYSTRUCTURE"),
			ENVELOPE = new ByteRef("ENVELOPE"), FAST = new ByteRef("FAST"), FLAGS = new ByteRef("FLAGS"),
			FULL = new ByteRef("FULL"), INTERNALDATE = new ByteRef("INTERNALDATE"), SIZE = new ByteRef("RFC822.SIZE"),
			RFC822 = new ByteRef("RFC822"), RFC822HEADER = new ByteRef("RFC822.HEADER"),
			RFC822TEXT = new ByteRef("RF822.TEXT"), PEEK = new ByteRef(".PEEK"),
			// FIELDSNOT = new ByteRef(".FIELDS.NOT"),
			FIELDS = new ByteRef(".FIELDS");

	static final ByteRef CHARSET = new ByteRef("CHARSET");

	static final ByteRef NOT = new ByteRef("NOT");

	static final ByteRef OR = new ByteRef("OR");

	static final ByteRef KET = new ByteRef(")");

	static final ByteRef BRA = new ByteRef("(");

	static final ByteRef BCC = new ByteRef("BCC");

	// private static final ByteRef BODY = new ByteRef("BODY");
	static final ByteRef BEFORE = new ByteRef("BEFORE");

	static final ByteRef CC = new ByteRef("CC");

	static final ByteRef FROM = new ByteRef("FROM");

	static final ByteRef ON = new ByteRef("ON");

	static final ByteRef SENTBEFORE = new ByteRef("SENTBEFORE");

	static final ByteRef SENTON = new ByteRef("SENTON");

	static final ByteRef SENTSINCE = new ByteRef("SENTSINCE");

	static final ByteRef SINCE = new ByteRef("SINCE");

	static final ByteRef SMALLER = new ByteRef("SMALLER");

	static final ByteRef SUBJECT = new ByteRef("SUBJECT");

	static final ByteRef TEXT = new ByteRef("TEXT");

	static final ByteRef TO = new ByteRef("TO");

	static final ByteRef ANSWERED = new ByteRef("ANSWERED");

	static final ByteRef DELETED = new ByteRef("DELETED");

	static final ByteRef DRAFT = new ByteRef("DRAFT");

	static final ByteRef FLAGGED = new ByteRef("FLAGGED");

	static final ByteRef SEEN = new ByteRef("SEEN");

	static final ByteRef UNANSWERED = new ByteRef("UNANSWERED");

	static final ByteRef UNDELETED = new ByteRef("UNDELETED");

	static final ByteRef UNDRAFT = new ByteRef("UNDRAFT");

	static final ByteRef UNFLAGGED = new ByteRef("UNFLAGGED");

	static final ByteRef UNSEEN = new ByteRef("UNSEEN");

	static final ByteRef NEW = new ByteRef("NEW");

	static final ByteRef OLD = new ByteRef("OLD");

	static final ByteRef HEADER = new ByteRef("HEADER");

	static HashSet<ByteRef> SEARCH0PARAM = new HashSet<ByteRef>();

	static HashSet<ByteRef> SEARCH1PARAM = new HashSet<ByteRef>();

	static {
		SEARCH0PARAM.add(ANSWERED);
		SEARCH0PARAM.add(DELETED);
		SEARCH0PARAM.add(DRAFT);
		SEARCH0PARAM.add(FLAGGED);
		SEARCH0PARAM.add(SEEN);
		SEARCH0PARAM.add(NEW);
		SEARCH0PARAM.add(OLD);
		SEARCH0PARAM.add(UNANSWERED);
		SEARCH0PARAM.add(UNDELETED);
		SEARCH0PARAM.add(UNDRAFT);
		SEARCH0PARAM.add(UNFLAGGED);
		SEARCH0PARAM.add(UNSEEN);

		SEARCH1PARAM.add(BCC);
		SEARCH1PARAM.add(BODY);
		SEARCH1PARAM.add(BEFORE);
		SEARCH1PARAM.add(CC);
		SEARCH1PARAM.add(FROM);
		SEARCH1PARAM.add(ON);
		SEARCH1PARAM.add(SENTBEFORE);
		SEARCH1PARAM.add(SENTON);
		SEARCH1PARAM.add(SENTSINCE);
		SEARCH1PARAM.add(SINCE);
		SEARCH1PARAM.add(SMALLER);
		SEARCH1PARAM.add(SUBJECT);
		SEARCH1PARAM.add(TEXT);
		SEARCH1PARAM.add(TO);
	}

	boolean fetch(String mbId, String msgNo, boolean uid, String what) {
		if (DEBUG) {
			logFile.writeln("fetch(" + mbId + ", " + msgNo + ", " + uid + ", " + what + ")");
		}

		// parse the what string
		Fetch f = new Fetch();
		f.uid = uid;
		{
			ArrayList<String> vNames = new ArrayList<String>(), vParts = new ArrayList<String>(),
					vOffsets = new ArrayList<String>(), vSizes = new ArrayList<String>();
			ArrayList<HashMap<?, ?>> vHashtables = new ArrayList<HashMap<?, ?>>();
			ByteRef x = new ByteRef(what).toUpperCase().trim('(').trim(')');
			for (ByteRef w = x.nextWord(); w != null; w = x.nextWord()) {
				if (w.equals(ALL)) {
					f.flags = f.internalDate = f.size = f.envelope = true;
					continue;
				}
				if (w.equals(BODY)) {
					f.body = true;
					continue;
				}
				if (w.equals(BODYSTRUCTURE)) {
					f.bodyStructure = true;
					continue;
				}
				if (w.equals(ENVELOPE)) {
					f.envelope = true;
					continue;
				}
				if (w.equals(FAST)) {
					f.flags = f.internalDate = f.size = true;
					continue;
				}
				if (w.equals(FLAGS)) {
					f.flags = true;
					continue;
				}
				if (w.equals(FULL)) {
					f.flags = f.internalDate = f.size = f.envelope = f.body = true;
					continue;
				}
				if (w.equals(INTERNALDATE)) {
					f.internalDate = true;
					continue;
				}
				if (w.equals(SIZE)) {
					f.size = true;
					continue;
				}
				if (w.equals(UID)) {
					f.uid = true;
					continue;
				}
				String aNames, aPart, aOffset, aSize;
				aNames = aPart = aOffset = aSize = null;
				HashMap<?, ?> aHashtable = null;

				// it is BODY[]<>, BODY.PEEK[]<>, RFC822, RFC822.HEADER, RFC822.TEXT
				if (w.equals(RFC822)) {
					aNames = "RFC822";
					aPart = "";
					f.seen = true;
				} else if (w.equals(RFC822HEADER)) {
					aNames = "RFC822.HEADER";
					aPart = "HEADER";
				} else if (w.equals(RFC822TEXT)) {
					aNames = "RFC822.TEXT";
					aPart = "TEXT";
					f.seen = true;
				} else if (w.startsWith(BODY)) {
					// get flags, if any
					int ief = w.indexOf(FIELDS);
					if (ief > 0) {
						ByteRef flags = x.nextWord(']');
						w = w.append(" ").append(flags).append("]");

						x = x.trim();
						if (DEBUG) {
							logFile.writeln("flags are: " + flags);
						}
						// parse flags
						flags = flags.trim('(').trim(')');

						// TODO handle flags
					}

					int idx = w.indexOf(PEEK);
					if (idx < 0) {
						f.seen = true;
					} else {
						w = w.substring(0, idx).append(w.substring(idx + 5));
					}
					aNames = w.toString();

					// ByteRef txt =
					w.nextWord('[');

					ByteRef ref = w.nextWord(']');
					aPart = ref.toString();
					if (w.charAt(0) == '<') {
						w = w.trim('<').trim('>');
						ByteRef n1 = w.nextWord('.');
						ByteRef n2 = w.nextWord();
						aOffset = n1.toString();
						aSize = n2.toString();

						String soa = aNames;
						soa = soa.substring(0, soa.length() - n2.length() - 2) + ">";
						aNames = soa;
					}
				}
				// ignore it
				if (aNames != null || aPart != null || aOffset != null || aSize != null || aHashtable != null) {
					vNames.add(aNames);
					vParts.add(aPart);
					vOffsets.add(aOffset);
					vSizes.add(aSize);
					vHashtables.add(aHashtable);
				}
			}
			// convert Vectors into arrays
			// Vector vNames, Vector vParts, Vector cOffsets, Vector vSizes, Vector
			// vHashtables
			f.get(vNames, vParts, vOffsets, vSizes, vHashtables);
		}

		// loop over all msgNo's - later it makes sense to parse the what String first
		return handleMessages(mbId, msgNo, uid, f, subFetch);
	}

	private SubFetch subFetch = new SubFetch();

	class SubFetch implements MsgHandler {

		/**
		 * (non-Javadoc)
		 * 
		 * @see de.bb.bejy.mail.Imap.MsgHandler#call(int, java.lang.String,
		 *      java.lang.String, java.sql.ResultSet, java.lang.Object)
		 */
		public void call(long pos, String uid, String mbId, ResultSet rs, Object o) throws Exception {
			RandomAccessFile raf = null;
			try {
				// ResultSet rs2;
				Fetch fetch = (Fetch) o;
				// rs is attached to stmt, so use stmt2 in this function
				ByteRef res = new ByteRef();
				String fileName = rs.getString(3);
				Timestamp ts = rs.getTimestamp(4);
				long last = ts != null ? ts.getTime() : 0;
				boolean fAnswered = rs.getBoolean(5);
				boolean fFlagged = rs.getBoolean(6);
				boolean fDeleted = rs.getBoolean(7);
				boolean fSeen = rs.getBoolean(8);
				boolean fDraft = rs.getBoolean(9);
				String size = rs.getString(10);

				if (!fSeen && fetch.seen) {
					fetch.flags = fSeen = true;

					mDbi.updateImapData(uid, fAnswered, fFlagged, fDeleted, fSeen, fDraft);
				}

				if (fetch.flags) {
					res = res.append(" FLAGS (");
					if (fAnswered) {
						res = res.append("\\Answered ");
					}
					if (fFlagged) {
						res = res.append("\\Flagged ");
					}
					if (fDeleted) {
						res = res.append("\\Deleted ");
					}
					if (fSeen) {
						res = res.append("\\Seen ");
					}
					if (fDraft) {
						res = res.append("\\Draft ");
					}
					res = res.trim().append(")");
				}
				if (fetch.uid) {
					res = res.append(" UID ");
					res = res.append(uid);
				}
				if (fetch.internalDate) {
					res = res.append(" INTERNALDATE ");
					res = res.append(QUOTE);
					String dt = DateFormat.dd_MMM_yyyy_HH_mm_ss_zzzz(last);
					byte db[] = dt.getBytes();
					db[2] = '-';
					db[6] = '-';
					db[11] = ' ';
					dt = new String(db);
					res = res.append(dt);
					res = res.append(QUOTE);
				}
				if (fetch.size) {
					res = res.append(" RFC822.SIZE ");
					res = res.append(size);
				}

				headerPath = null;
				// do we touch the mail file?

				boolean bodyStructure = fetch.bodyStructure;
				boolean envelope = fetch.envelope;
				String[] names = fetch.names;
				if (bodyStructure || envelope || names.length > 0) {
					// open the file
					try {
						raf = mDbi.openRandomAccessFile(fileName);
						readHeader(raf, fileName, "");
					} catch (IOException ioe) {
						logFile.writeDate(ioe.getMessage());

						bodyStructure = false;
						envelope = false;
						names = NONAMES;
					}
				}

				if (envelope) {
					res = res.append(" ENVELOPE (");

					res = addValue(res, "date");
					res = addValue(res, "subject");

					res = addList(res, "from");
					res = addList(res, "sender");
					res = addList(res, "reply-to");
					res = addList(res, "to");
					res = addList(res, "cc");
					res = addList(res, "bcc");
					res = addValue(res, "in-reply-to");

					res = addValue(res, "message-id");

					res = res.trim().append(")");
				}

				if (bodyStructure) {
					ResultSet rs2 = mDbi.selectFromImapMime(fileName);
					LinkedList<String[]> ll = new LinkedList<String[]>();
					while (rs2.next()) {
						String s1[] = new String[4];
						s1[0] = rs2.getString(2);
						s1[1] = rs2.getString(3);
						// s1[2] = rs2.getString(7); // header lines
						s1[2] = "" + (rs2.getInt(6) - rs2.getInt(5));
						s1[3] = rs2.getString(8); // body lines
						if (s1[0] == null) {
							s1[0] = ""; // for oracle
						}
						if (s1[1] == null) {
							s1[1] = ""; // for oracle
						}
						ll.addLast(s1);
					}
					rs2.close();

					res = res.append(" BODYSTRUCTURE ");

					while (ll.size() > 0) {
						res = appendStructure(raf, fileName, res, ll);
					}
				}

				// ...
				os.write(("* " + pos + " FETCH (").getBytes());
				res = res.trim();
				res.writeTo(os);
				if (VERBOSE) {
					logFile.write("* " + pos + " FETCH (" + res);
				}

				if (names.length > 0) {
					boolean addSpace = res.length() > 0;

					// if (DEBUG) logFile.write("open file: " + fileName);
					for (int i = 0; i < names.length; ++i) {
						if (addSpace) {
							if (VERBOSE) {
								logFile.write(" ");
							}
							os.write(' ');
						}
						addSpace = true;

						ByteRef part = new ByteRef(fetch.parts[i]);
						int includeHeader = part.indexOf("HEADER");
						if (includeHeader >= 0) {
							ByteRef hdr = part.substring(includeHeader);
							part = part.substring(0, includeHeader).trim('.');
							// start of better header handling, if only a partial header is requested
							if (hdr.startsWith("HEADER.FIELDS")) {
								hdr = hdr.substring(13);

								boolean not = false;
								if (hdr.startsWith(".NOT")) {
									not = true;
									hdr = hdr.substring(4);
								}
								hdr = hdr.toUpperCase();
								HashMap<String, ByteRef> header = readHeader(raf, fileName, part.toString());
								if (DEBUG) {
									System.out.println("header wanted:" + hdr);
									System.out.println("header parsed:" + header);
								}

								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								for (Iterator<Entry<String, ByteRef>> j = header.entrySet().iterator(); j.hasNext();) {
									Entry<String, ByteRef> e = j.next();
									String key = e.getKey();
									if ((hdr.indexOf(key.toUpperCase()) > 0) ^ not) {
										ByteRef val = e.getValue();
										val.writeTo(bos);
										CRLF.writeTo(bos);
									}
								}
								CRLF.writeTo(bos);
								if (VERBOSE) {
									logFile.write(names[i]);
									logFile.writeln(" {" + bos.size() + "}");
									logFile.write(bos.toString());
								}
								os.write(names[i].getBytes());
								os.write((" {" + bos.size() + "}\r\n").getBytes());
								os.write(bos.toByteArray());
								continue;
							}
							// end of better header handling
						} else {
							includeHeader = part.indexOf("MIME");
							if (includeHeader >= 0) {
								part = part.substring(0, includeHeader).trim('.');
							}
						}

						int includeBody = part.indexOf("TEXT");
						if (includeBody >= 0) {
							part = part.substring(0, includeBody).trim('.');
						}

						if (DEBUG) {
							logFile.writeln("p=" + part.toString());
						}
						ResultSet rs2;
						if (includeBody >= 0) {
							rs2 = mDbi.selectFromImapMimeTextPlain(fileName);
						} else {
							rs2 = mDbi.selectFromImapMime(fileName, part.toString());
						}
						try {
							if (rs2.next()) {
								// String ct = rs2.getString(3);
								if (part.length() > 0 && includeHeader < 0) {
									includeBody = 1;
								}

								int begin, end;
								if (includeHeader >= 0) {
									begin = rs2.getInt(4);
									end = rs2.getInt(5);
								} else if (includeBody >= 0) {
									begin = rs2.getInt(5);
									end = rs2.getInt(6);
								} else {
									begin = rs2.getInt(4);
									end = rs2.getInt(6);
								}
								if (fetch.offsets[i] != -1) {
									begin += fetch.offsets[i];
								}
								if (begin > end) {
									begin = end;
								}
								if (fetch.sizes[i] != -1 && begin + fetch.sizes[i] < end) {
									end = begin + fetch.sizes[i];
								}

								ByteRef name = new ByteRef(fetch.names[i]);
								end -= begin;
								raf.seek(begin);
								name.writeTo(os);
								os.write((" {" + end + "}\r\n").getBytes());
								if (VERBOSE) {
									logFile.writeln(name.toString() + " {" + end + "}");
								}

								// copy content
								while (end > 0) {
									int sz = end > data.length ? data.length : end;
									sz = raf.read(data, 0, sz);
									if (sz <= 0) {
										break; // should NEVER occur!
									}
									end -= sz;
									os.write(data, 0, sz);
									if (VERBOSE) {
										logFile.write(new String(data, 0, sz));
									}
								}
								while (end > 0) {
									os.write(32);
									--end;
								}
								// CRLF.writeTo(os);
							}
						} finally {
							rs2.close();
						}
					}
				}

				os.write(')');
				CRLF.writeTo(os);

				if (VERBOSE) {
					logFile.writeln(")");
				}
			} finally {
				if (raf != null) {
					try {
						raf.close();
					} catch (Exception ex2) {
					}
				}
			}
		}

		/*
		 * * Method readHeader.
		 * 
		 * @param raf
		 * 
		 * @param begin
		 * 
		 * @param end
		 * 
		 * @return Hashtable / private Hashtable readHeader(RandomAccessFile raf, int
		 * begin, int end) { return null; } /
		 **/
		private ByteRef addValue(ByteRef res, String key) {
			ByteRef val = header.get(key);
			if (val == null) {
				res = res.append("NIL ");
				return res;
			}

			// logFile.write(val.toString());

			val.nextWord(':');
			val = stripCRLF(val);

			res = appendEscaped(res, val);

			res = res.append(SPACE);
			return res;
		}

		private ByteRef appendEscaped(ByteRef res, ByteRef val) {
			// escape the value
			res = res.append(QUOTE);

			byte buf[] = new byte[val.length() * 2];
			int j = 0;
			for (int i = 0; i < val.length(); ++i) {
				int b = val.charAt(i);
				if (b == '"' || b == '\\') {
					buf[j++] = '\\';
				}
				buf[j++] = (byte) b;
			}

			res = res.append(new ByteRef(buf, 0, j)).append(QUOTE);
			return res;
		}

		// converts a list of mail addresses
		private ByteRef addList(ByteRef res, String key) {
			List<ByteRef> addresses = null;
			ByteRef val = header.get(key);
			if (val != null) {
				val.nextWord(':');
				val = stripCRLF(val);
				if (DEBUG) {
					logFile.writeln(val.toString());
				}
				addresses = parseMailAddresses(val);
			}
			if (val == null || addresses.size() == 0) {
				res = res.append("NIL ");
				return res;
			}

			// logFile.write(val.toString());

			res = res.append("(");

			for (Iterator<ByteRef> i = addresses.iterator(); i.hasNext();) {
				res = res.append("(");
				val = i.next();
				if (val.charAt(val.length() - 1) == '>') {
					int gt = val.lastIndexOf('<');
					ByteRef name = val.substring(0, gt).trim();
					res = res.append(QUOTE).append(name.trim()).append(QUOTESPACE);
					val = val.substring(gt + 1, val.length() - 1).trim();
				} else {
					res = res.append("NIL ");
				}

				res = res.append("NIL ");

				ByteRef name = val.nextWord('@');
				if (name != null && name.length() > 0) {
					res = res.append(QUOTE).append(name.trim()).append(QUOTESPACE);
				} else {
					res = res.append("NIL ");
				}

				if (val.length() > 0) {
					res = res.append(QUOTE).append(val.trim()).append(QUOTE);
				} else {
					res = res.append("NIL");
				}

				res = res.append(")");
			}

			res = res.append(") ");
			return res;
		}

		/**
		 * Insert a BODY element.
		 * 
		 * @param raf
		 * @param fileName
		 * @param res2
		 * @param ll
		 * @return
		 * @throws Exception
		 */
		private ByteRef appendStructure(RandomAccessFile raf, String fileName, ByteRef res2, LinkedList<String[]> ll)
				throws Exception {
			ByteRef res = new ByteRef("(");
			String s1[] = ll.removeFirst();

			// check if subelements exists
			int nested = 0;
			while (ll.size() > 0) {
				String s2[] = ll.getFirst();

				if (!s2[0].startsWith(s1[0])) {
					break;
				}
				{
					res = appendStructure(raf, fileName, res, ll);
					++nested;
				}
			}

			ByteRef b = new ByteRef(s1[1]);
			ByteRef k = b.nextWord('/'); // media_type
			ByteRef t = b.nextWord(';'); // media_subtype
			if (k == null) {
				k = new ByteRef("text");
			}
			if (t == null) {
				t = new ByteRef("plain");
			}

			// boolean isRfc822message = k.equalsIgnoreCase("message") &&
			// t.equalsIgnoreCase("rfc822");
			boolean isText = k.equalsIgnoreCase("text");

			if (nested < 1) {
				res = res.append(QUOTE + k.toString() + QUOTESPACE);
				res = res.append(QUOTE + t.toString() + QUOTE);
			} else if (nested > 1 || !isText) {
				res = res.append(" \"" + t.toString() + QUOTE);
				return res2.append(res.append(")"));
			} else {
				return res2.append(res.substring(1));
			}

			// body_type_mpart: 1*body SPACE media_subtype

			// add body parameter list
			if (b.length() > 0) {
				res = res.append(" (");
				do {
					k = b.nextWord('=');
					ByteRef v = b.nextWord(';');
					if (k == null) {
						k = new ByteRef("");
					}
					if (v == null) {
						v = new ByteRef("");
					}

					res = res.append(QUOTE).append(k.trim()).append(QUOTESPACE).append(QUOTE).append(v.trim().trim('"'))
							.append(QUOTESPACE);
				} while (b.length() > 0);
				res = res.trim().append(") ");
			} else {
				res = res.append(" NIL ");
			}

			readHeader(raf, fileName, s1[0]);

			// body id
			res = addValue(res, "message-id");

			// body description
			res = res.append("NIL ");

			// body encoding
			res = addValue(res, "content-transfer-encoding");

			res = res.append(s1[2]); // length

			if (isText) {
				res = res.append(" " + s1[3]); // number of lines
			}

			ByteRef contentDisposition = header.get("content-disposition");
			if (contentDisposition != null) {
				contentDisposition.nextWord(':');
				contentDisposition = stripCRLF(contentDisposition);
				if (contentDisposition.length() == 0)
					contentDisposition = null;
			}
			if (contentDisposition != null) {
				// extension data
				res = res.append(" NIL"); // md5
				res = res.append(" (\"").append(contentDisposition.nextWord(';').trim()).append(QUOTESPACE);
				contentDisposition = contentDisposition.trim();
				if (contentDisposition.length() == 0) {
					res = res.append("NIL");
				} else {
					res = res.append("(");
					for (;;) {
						// FIXME - detect also ';' inside of a value
						ByteRef nameVal = contentDisposition.nextWord(';');
						ByteRef name = nameVal.nextWord('=');
						res = res.append(QUOTE).append(name).append(QUOTESPACE);
						if (nameVal.charAt(0) == '"') {
							res = res.append(nameVal);
						} else {
							res = appendEscaped(res, nameVal);
						}
						contentDisposition = contentDisposition.trim();
						if (contentDisposition.length() == 0)
							break;
						res = res.append(SPACE);
					}
					res = res.append(")");
				}
				res = res.append(")");
			}

			res = res.append(")");
			return res2.append(res);
		}

	}

	private static class Search {
		boolean returnUid;

		boolean not;

		ByteRef q;

		ByteRef val;

		ByteRef charSet;

		public ByteRef val2;

		public boolean searchUid;

		TreeSet<Long> result;

		Search(boolean uid) {
			this.returnUid = uid;
		}

		public String toString() {
			return "S(" + (not ? "NOT " : "") + q + ", v=" + val + (returnUid ? " UID " : " ") + ")";
		}
	}

	boolean search(String mbId, boolean uid) throws Exception {
		if (DEBUG) {
			logFile.writeln("search(" + mbId + ", " + uid + ", " + line + ")");
		}

		if (line == null) {
			return false;
		}

		Search s = new Search(uid);
		ByteRef q = line.nextWord().toUpperCase();
		if (q.equals(CHARSET)) {
			s.charSet = nextAString();
			if (line == null) {
				return false;
			}
			q = line.nextWord().toUpperCase();
		}

		// the range can be specified at start or end or elsewhere
		TreeSet<Long> result = searchAnd("1:*", q, s);

		if (result == null) {
			return false;
		}

		if (VERBOSE) {
			logFile.write("* SEARCH");
		}
		os.write(("* SEARCH").getBytes());
		if (!result.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (final Long ii : result) {
				sb.append(" ").append(ii);
			}
			if (VERBOSE) {
				logFile.writeln(sb.toString());
			}
			os.write(sb.toString().getBytes());
		}
		if (VERBOSE) {
			logFile.write("\r\n");
		}
		CRLF.writeTo(os);

		return true;
	}

	static ByteRef stripCRLF(ByteRef val) {
		val = val.trim();

		// remove line breaks
		ByteRef t = new ByteRef();
		for (;;) {
			ByteRef l = val.nextLineCRLF();
			if (l == null)
				break;
			t = t.append(SPACE).append(l);
			val = val.substring(1); // eat the tab or space
		}
		val = t.append(val);
		return val;
	}

	private TreeSet<Long> searchAnd(String range, ByteRef q, Search s) throws Exception {
		TreeSet<Long> andResult = null;
		do {
			if (q.equals(KET)) {
				if (andResult == null)
					return new TreeSet<Long>();
				return andResult;
			}
			TreeSet<Long> result = searchCmd(range, q, s);
			if (andResult != null) {
				andResult = andResult(andResult, result);
			} else {
				andResult = result;
			}
			if (line == null) {
				break;
			}
			q = line.nextWord();
		} while (q != null && !andResult.isEmpty());
		while (q != null) {
			q = this.nextAStringNoRead();
		}
		return andResult;
	}

	private TreeSet<Long> searchCmd(String searchRange, ByteRef q, Search search) throws Exception {
		search.q = null;
		search.not = false;
		search.result = new TreeSet<Long>();

		if (q.equals(BRA)) {
			if (line == null) {
				return null;
			}
			q = line.nextWord();
			if (q == null) {
				return null;
			}
			q.toUpperCase();
			return searchAnd(searchRange, q, search);
		}

		if (q.equals(UID)) {
			search.searchUid = true;
			q = line.nextWord().toUpperCase();
			if (q == null) {
				return null;
			}
		}
		if (q.equals(ALL)) {
			q = ONETOSTAR;
		}

		if (q.equals(OR)) {
			if (line == null) {
				return null;
			}
			q = line.nextWord();
			if (q == null) {
				return null;
			}
			q.toUpperCase();
			TreeSet<Long> r1 = searchCmd("1:*", q, search);
			if (line == null) {
				return r1;
			}
			q = line.nextWord();
			if (q == null) {
				return r1;
			}
			q.toUpperCase();
			TreeSet<Long> r2 = searchCmd("1:*", q, search);
			r1.addAll(r2);
			return r1;
		}

		int q0 = q.charAt(0);
		if (q0 == '*' || (q0 >= '0' && q0 <= '9')) {
			searchRange = q.toString();
			q = null;
		}

		if (NOT.equals(q)) {
			search.not = true;
			q = line.nextWord();
			if (q == null) {
				return null;
			}
			q.toUpperCase();
		}

		if (q != null) {
			if (SEARCH1PARAM.contains(q)) {
				search.val = nextAString();
			} else if (q.equals(HEADER)) {
				search.val = nextAString();
				search.val2 = nextAString();
			}
		}

		// loop over all msgNo's - later it makes sense to parse the what String first
		search.q = q;
		if (DEBUG)
			logFile.writeln(search.toString());
		boolean r = handleMessages(mbId, searchRange, search.searchUid, search, subSearch);
		if (DEBUG)
			logFile.writeln("searchresult: " + r + " " + search.result);
		if (!r) {
			return null;
		}
		return search.result;
	}

	private static TreeSet<Long> andResult(TreeSet<Long> left, TreeSet<Long> right) {
		TreeSet<Long> and = new TreeSet<Long>();
		for (final Long ii : left) {
			if (right.contains(ii))
				and.add(ii);
		}
		return and;
	}

	private SubSearch subSearch = new SubSearch();

	class SubSearch implements MsgHandler {

		/**
		 * (non-Javadoc)
		 * 
		 * @see de.bb.bejy.mail.Imap.MsgHandler#call(int, java.lang.String,
		 *      java.lang.String, java.sql.ResultSet, java.lang.Object)
		 */
		public void call(long pos, String uid, String mbId, ResultSet rs, Object o) throws Exception {
			Search s = (Search) o;
			boolean add = true; // default add it to the result
			if (DEBUG)
				logFile.writeln("uid=" + s.searchUid + ", not=" + s.not + ", attr=" + s.q);

			ByteRef attribute = s.q;
			if (attribute != null)
				attribute = attribute.toUpperCase();
			ByteRef value = s.val;
			if (attribute != null) {
				if (attribute.equals(ANSWERED)) {
					add = rs.getBoolean(5);
				} else if (attribute.equals(FLAGGED)) {
					add = rs.getBoolean(6);
				} else if (attribute.equals(DELETED)) {
					add = rs.getBoolean(7);
				} else if (attribute.equals(SEEN) || attribute.equals(OLD)) {
					add = rs.getBoolean(8);
				} else if (attribute.equals(DRAFT)) {
					add = rs.getBoolean(9);
				} else if (attribute.equals(UNANSWERED)) {
					add = !rs.getBoolean(5);
				} else if (attribute.equals(UNFLAGGED)) {
					add = !rs.getBoolean(6);
				} else if (attribute.equals(UNDELETED)) {
					add = !rs.getBoolean(7);
				} else if (attribute.equals(UNSEEN) || attribute.equals(NEW)) {
					add = !rs.getBoolean(8);
				} else if (attribute.equals(UNDRAFT)) {
					add = !rs.getBoolean(9);
				} else if (attribute.equals(SINCE) || attribute.equals(SENTSINCE)) {
					long t0 = rs.getTimestamp(4).getTime();
					long t1 = DateFormat.parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(value.toString());
					if (DEBUG)
						logFile.write("search since: " + t0 + " >= " + t1);
					add = t0 >= t1;
				} else if (attribute.equals(BEFORE) || attribute.equals(SENTBEFORE)) {
					add = rs.getTimestamp(4).getTime() < DateFormat
							.parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(value.toString());
				} else if (attribute.equals(ON) || attribute.equals(SENTON)) {
					long t0 = rs.getTimestamp(4).getTime();
					long t1 = DateFormat.parse_dd_MMM_yyyy_HH_mm_ss_GMT_zz_zz(value.toString());
					add = t0 >= t1 && t0 <= t1 + DateFormat.TIME_PER_DAY;
				} else {
					RandomAccessFile raf = null;
					ByteRef header = null;
					if (attribute.equals(BCC) || attribute.equals(CC) || attribute.equals(FROM) || attribute.equals(TO)
							|| attribute.equals(SUBJECT)) {
						header = attribute;
					} else if (attribute.equals(HEADER)) {
						header = s.val;
						value = s.val2;
					}
					try {
						String fileName = rs.getString(3);
						raf = mDbi.openRandomAccessFile(fileName);
						if (attribute.equals(BODY)) {
							String charSet = "iso-8851-1";
							if (s.charSet != null) {
								charSet = s.charSet.toString();
							}
							String searchVal;
							try {
								searchVal = new String(s.val.toByteArray(), charSet);
							} catch (Exception ex) {
								searchVal = new String(s.val.toByteArray(), "iso-8859-1");
							}
							ResultSet rs2 = mDbi.selectFromImapMime(fileName);
							add = false;
							while (!add && rs2.next()) {
								String contentType = rs2.getString(3);
								if (contentType.startsWith("text/")) {
									int start = rs2.getInt(5);
									int end = rs2.getInt(6);
									byte data[] = new byte[end - start];
									raf.seek(start);
									raf.read(data);

									int cs = contentType.indexOf("charset=");
									charSet = "iso-8859-1";
									if (cs > 0) {
										charSet = contentType.substring(cs + 8);
										int semi = charSet.indexOf(';');
										if (semi > 0) {
											charSet = charSet.substring(0, semi);
										}
										charSet = charSet.trim();
										if (charSet.charAt(0) == '"') {
											charSet = charSet.substring(1, charSet.length() - 1);
										}
									}
									String content;
									try {
										content = new String(data, charSet);
									} catch (Exception ex) {
										content = new String(data, "iso-8859-1");
									}
									add = content.toUpperCase().indexOf(searchVal.toUpperCase()) >= 0;
								}
							}

							rs2.close();

						} else if (header != null) {

							headerPath = null;
							HashMap<String, ByteRef> hdr = readHeader(raf, fileName, "");

							ByteRef ahdr = hdr.get(header.toString().toLowerCase());
							if (ahdr != null) {
								// logFile.write(ahdr.toUpperCase() + " ?? " + val.toUpperCase());
								add = ahdr.toUpperCase().indexOf(value.toUpperCase()) >= 0;
							}
						}
					} finally {
						if (raf != null) {
							raf.close();
						}
					}
				}
			}
			if (add ^ s.not) {
				if (s.returnUid) {
					s.result.add(Long.parseLong(uid));
				} else {
					s.result.add(pos);
				}
			}
		}
	}

	private boolean handleMessages(String mbId, final String msgNumbers, boolean useUid, Object o, MsgHandler mh) {
		try {
			final ArrayList<Pair<Long, Long>> ranges = new ArrayList<Pair<Long, Long>>();

			// parse the provided message numbers
			for (final StringTokenizer st = new StringTokenizer(msgNumbers, ","); st.hasMoreElements();) {
				final String part = st.nextToken();
				final int colon = part.indexOf(':');
				if (colon < 0) {
					// next is single number if no colon or colon > komma
					if (part.equals("*")) {
						ranges.add(Pair.makePair(1L, Long.MAX_VALUE));
					} else {
						final Long n = Long.parseLong(part);
						ranges.add(Pair.makePair(n, n));
					}
				} else {
					// next is a range
					final String sfrom = part.substring(0, colon);
					final String sto = part.substring(colon + 1);
					// convert to int
					try {
						final Long from = sfrom.equals("*") ? 1 : Long.parseLong(sfrom);
						final Long to = sto.equals("*") ? Integer.MAX_VALUE : Long.parseLong(sto);
						ranges.add(Pair.makePair(from, to));
					} catch (Exception ex) {
						ranges.add(Pair.makePair(1L, Long.MAX_VALUE));
					}
				}
			}

			Collections.sort(ranges, new Comparator<Pair<Long, Long>>() {
				public int compare(Pair<Long, Long> o1, Pair<Long, Long> o2) {
					long diff = o1.getFirst() - o2.getFirst();
					if (diff > 0) return 1;
					if (diff < 0) return -1;
					return 0;
				}
			});

			// merge overlapping ranges
			for (int i = 1; i < ranges.size();) {
				Pair<Long, Long> a = ranges.get(i - 1);
				Pair<Long, Long> b = ranges.get(i);
				if (a.getSecond() < b.getFirst()) {
					++i;
					continue;
				}
				Pair<Long, Long> c = Pair.makePair(a.getFirst(), Long.max(a.getSecond(), b.getSecond()));
				ranges.set(i - 1, c);
				ranges.remove(i);
			}

			if (DEBUG)
				logFile.writeln(msgNumbers + " -> " + ranges);

			if (useUid) {
				// get count of elements
				String stot = mDbi.selectCountFromImapData(mbId);
				if (DEBUG)
					logFile.write(stot + " in mbox id=" + mbId);
				if (stot == null) {
					return false;
				}

				int total = Integer.parseInt(stot);
				if (total == 0) {
					return true;
				}

				// perform one select for all uids in the lists
				ResultSet rs;
				rs = mDbi.selectFromImapData(mbId);
				try {
					if (!rs.next()) {
						return true;
					}
					int pos = 1;

					for (Pair<Long, Long> p : ranges) {
						final long from = p.getFirst();
						final long to = p.getSecond();
						if (DEBUG) {
							logFile.writeln("[" + from + ":" + to + "] in total " + total);
						}

						long diff = total - pos;
						long iuid = rs.getLong(1);
						if (from - iuid < diff) {
							diff = from - iuid;
						}

						int inc = 1;
						while (2 * inc <= diff) {
							inc *= 2;
						}

						while (iuid != from && inc > 0) {
							if (DEBUG) {
								logFile.writeln("id =" + iuid + ", pos=" + pos + ", inc=" + inc);
							}

							if (iuid < from) {
								if (pos + inc <= total) {
									pos += inc;
									rs.relative(inc);
									iuid = rs.getInt(1);
								}
							} else {
								if (pos - inc > 0) {
									pos -= inc;
									rs.relative(-inc);
									iuid = rs.getInt(1);
								}
							}
							inc /= 2;
						}
						while (iuid < from && pos < total) {
							if (DEBUG) {
								logFile.writeln("id =" + iuid + ", pos=" + pos + " linear");
							}
							rs.next();
							iuid = rs.getInt(1);
							++pos;
						}

						while (iuid >= from && iuid <= to) {
							if (DEBUG) {
								logFile.writeln("call with id =" + iuid);
							}
							mh.call(pos, "" + iuid, mbId, rs, o);

							if (iuid >= to) {
								break;
							}
							if (!rs.next()) {
								return true;
							}
							++pos;
							iuid = rs.getInt(1);
						}
					}
				} finally {
					rs.close();
				}

			} else {
				// perform one select and move in the resultset according to the list
				ResultSet rs = mDbi.selectFromImapData(mbId);
				try {
					if (rs.next()) {
						long pos = 1;
						Outer:
						for (Pair<Long, Long> p : ranges) {
							final long from = p.getFirst();
							final long to = p.getSecond();
							if (from > pos) {
								long seek = from - pos;
								while (seek > Integer.MAX_VALUE) {
									seek -= Integer.MAX_VALUE;
									if (!rs.relative(Integer.MAX_VALUE)) {
										break Outer;
									}
								}
								if (!rs.relative((int)seek)) {
									break;
								}
								pos = from;
							}
							do { // handle this file
								String iuid = rs.getString(1);
								mh.call(pos, iuid, mbId, rs, o);
								if (pos <= to) {
									if (!rs.next()) {
										break;
									}
									++pos;
								}
							} while (pos <= to);
						}
					}
				} finally {
					rs.close();
				}

			}
			return true;
		} catch (Exception e) {
			if ((e instanceof IOException))
				LOG.debug(e.getMessage(), e);
			else
				LOG.error(e.getMessage(), e);
		}
		return false;
	}

	static class Store {
		int action; // -1 = DELETE where true, 0 STORE where true, 1 ADD where true

		boolean quiet;

		boolean f1, f2, f3, f4, f5;
	}

	boolean store(String mbId, String messages, boolean uid, String subcmd, String flags) throws Exception {
		if (DEBUG) {
			logFile.writeln("store(" + mbId + ", " + messages + ", " + uid + ", " + subcmd + ", " + flags + ")");
		}

		Store st = new Store();

		if (subcmd.charAt(0) == '-') {
			st.action = -1;
		} else if (subcmd.charAt(0) == '+') {
			st.action = 1;
		} else {
			st.action = 0;
		}

		st.quiet = subcmd.endsWith("SILENT");

		st.f1 = flags.indexOf("\\ANSWERED") >= 0;
		st.f2 = flags.indexOf("\\FLAGGED") >= 0;
		st.f3 = flags.indexOf("\\DELETED") >= 0;
		st.f4 = flags.indexOf("\\SEEN") >= 0;
		st.f5 = flags.indexOf("\\DRAFT") >= 0;

		boolean rc = handleMessages(mbId, messages, uid, st, subStore);
		if (st.quiet) {
			return rc;
		}
		return fetch(mbId, messages, uid, "FLAGS");
	}

	private SubStore subStore = new SubStore();

	class SubStore implements MsgHandler {
		/**
		 * (non-Javadoc)
		 * 
		 * @see de.bb.bejy.mail.Imap.MsgHandler#call(int, java.lang.String,
		 *      java.lang.String, java.sql.ResultSet, java.lang.Object)
		 */
		public void call(long pos, String uid, String mbId, ResultSet rs, Object o) throws Exception {
			Store st = (Store) o;

			boolean f1, f2, f3, f4, f5;
			f1 = f2 = f3 = f4 = f5 = false;

			if (st.action != 0) {
				ResultSet rs2 = mDbi.selectFromImapDataById(uid);
				if (!rs2.next()) {
					rs2.close();
					return;
				}
				f1 = rs2.getBoolean(5);
				f2 = rs2.getBoolean(6);
				f3 = rs2.getBoolean(7);
				f4 = rs2.getBoolean(8);
				f5 = rs2.getBoolean(9);
				rs2.close();
			}
			switch (st.action) {
			case -1:
				if (st.f1) {
					f1 = false;
				}
				if (st.f2) {
					f2 = false;
				}
				if (st.f3) {
					f3 = false;
				}
				if (st.f4) {
					f4 = false;
				}
				if (st.f5) {
					f5 = false;
				}
				break;
			case 0:
				f1 = st.f1;
				f2 = st.f2;
				f3 = st.f3;
				f4 = st.f4;
				f5 = st.f5;
				break;
			case 1:
				if (st.f1) {
					f1 = true;
				}
				if (st.f2) {
					f2 = true;
				}
				if (st.f3) {
					f3 = true;
				}
				if (st.f4) {
					f4 = true;
				}
				if (st.f5) {
					f5 = true;
				}
				break;
			}

			mDbi.updateImapData(uid, f1, f2, f3, f4, f5);
		}
	}

	boolean copyMove(String unitId, String mbId, String messages, boolean useUid, String destBox, boolean isMove)
			throws Exception {
		if (DEBUG) {
			logFile.writeln("copy(" + unitId + ", " + mbId + ", " + messages + ", " + useUid + ", " + destBox + ")");
		}

		String destId = mDbi.mailboxId(unitId, destBox);
		if (destId == null) {
			return false;
		}

		SubCopy subCopy = new SubCopy(isMove);
		boolean r = handleMessages(mbId, messages, useUid, destId, subCopy);

		String fc = mDbi.selectCountFromImapData(destId);
		if (fc != null) {
			Imap.updateStatus(destId, "* " + fc + " EXISTS");
		}

		return r;
	}

	class SubCopy implements MsgHandler {
		private boolean isMove;

		public SubCopy(boolean isMove) {
			this.isMove = isMove;
		}

		/**
		 * (non-Javadoc)
		 * 
		 * @see de.bb.bejy.mail.Imap.MsgHandler#call(int, java.lang.String,
		 *      java.lang.String, java.sql.ResultSet, java.lang.Object)
		 */
		public void call(long pos, String uid, String mbId, ResultSet r, Object o) throws Exception {
			final String destId = (String) o;
			try {
				// copy imap_data entry
				ResultSet rs = mDbi.selectFromImapDataById(uid);
				if (rs.next()) {
					String filename = rs.getString(3);
					mDbi.insertIntoImapData(destId, filename, rs.getTimestamp(4).getTime(), rs.getBoolean(5),
							rs.getBoolean(6), rs.getBoolean(7), rs.getBoolean(8), rs.getBoolean(9), rs.getString(10));

					if (isMove) {
						if (mDbi.deleteFromImapData(uid) > 0) {
							// on change notify all
							updateStatus(mbId, "* " + pos + " EXPUNGE");
						} else {
							// otherwise notify only current connection
							updateStatus("* " + pos + " EXPUNGE");
						}
						if (DEBUG) {
							logFile.writeln("* " + pos + " EXPUNGE");
						}
					}

				}
				rs.close();
			} catch (Exception e) {
			} finally {
			}
		}
	}

	boolean expunge(String mbId) throws Exception {
		if (DEBUG) {
			logFile.writeln("expunge(" + mbId + ")");
		}

		idle = true;

		// sync on mailbox level
		synchronized (mailBoxLock.get(mbId)) {
			ResultSet rs = mDbi.selectDeletedFromImapData(mbId);
			for (int pos = 1; rs.next();) {
				if (rs.getBoolean(2)) {
					String id = rs.getString(1);
					if (mDbi.deleteFromImapData(id) > 0) {
						// on change notify all
						updateStatus(mbId, "* " + pos + " EXPUNGE");
					} else {
						// otherwise notify only current connection
						updateStatus("* " + pos + " EXPUNGE");
					}
					if (DEBUG) {
						logFile.writeln("* " + pos + " EXPUNGE");
					}
				} else {
					++pos;
				}
			}
			rs.close();
		}

		synchronized (os) {
			idle = false;
		}

		return true;
	}

	boolean close(String mbId) throws Exception {
		if (DEBUG) {
			logFile.writeln("close(" + mbId + ")");
		}

		idle = true;

		// sync on mailbox level
		synchronized (mailBoxLock.get(mbId)) {
			ResultSet rs = mDbi.selectDeletedFromImapData(mbId);
			for (int pos = 1; rs.next();) {
				if (rs.getBoolean(2)) {
					String id = rs.getString(1);
					mDbi.deleteFromImapData(id);

					// updateStatus(mbId, "* " + pos + " EXPUNGE");
					if (DEBUG) {
						logFile.writeln("* " + pos + " CLOSE");
					}
				} else {
					++pos;
				}
			}
			rs.close();
		}

		synchronized (os) {
			idle = false;
		}

		return true;
	}

	String status(String unitId, String mailbox, String what) throws Exception {
		if (DEBUG) {
			logFile.writeln("status(" + unitId + ", " + mailbox + ", " + what + ")");
		}

		String mbId = mDbi.mailboxId(unitId, mailbox);
		if (mbId == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		// do it
		if (what.indexOf("MESSAGES") >= 0) {
			String count = mDbi.selectCountFromImapData(mbId);
			if (count != null) {
				sb.append("MESSAGES ");
				sb.append(count);
			}
		}
		if (what.indexOf("RECENT") >= 0) {
			// get count of recent files
			String count = mDbi.selectRecentCountFromImapData(mbId);
			if (count != null) {
				sb.append(" RECENT ");
				sb.append(count);
			}
		}
		if (what.indexOf("UIDNEXT") >= 0) {
			int uidnext = mDbi.getNextIdFromImapData(mbId);
			sb.append(" UIDNEXT ");
			sb.append(uidnext);
		}
		if (what.indexOf("UIDVALIDITY") >= 0) {
			sb.append(" UIDVALIDITY ");
			sb.append(mbId);
		}
		if (what.indexOf("UNSEEN") >= 0) {
			String count = mDbi.selectUnseenCountFromImapData(mbId);
			if (count != null) {
				sb.append(" UNSEEN ");
				sb.append(count);
			}
		}

		String r = sb.toString().trim();
		return "* STATUS \"" + mailbox + "\" (" + r + ")";
	}

	/**
	 * Method parseMailAddresses parses the given byte array for mail addresse. -
	 * quotes " are unquoted - <xxx> is taken if available - (comments) are removed
	 * - white spaces are removed
	 * 
	 * @param br
	 * @return a List containing the found addresses
	 */
	static List<ByteRef> parseMailAddresses(ByteRef br) {
		LinkedList<ByteRef> ll = new LinkedList<ByteRef>();
		br = br.trim();
		ByteRef eat = new ByteRef();
		while (br.length() > 0) {
			int komma = br.indexOf(',');
			int quote = br.indexOf('"');
			if (komma < 0) {
				komma = quote < 0 ? br.length() : quote + 1;
			}
			if (quote < komma && quote >= 0) {
				eat = eat.append(br.substring(0, quote));
				++quote;
				int q2 = quote;
				for (;;) {
					int t = q2;
					q2 = br.indexOf('"', t);

					if (q2 < 0) {
						q2 = br.length();
						br = br.append("\"");
						break;
					}

					int esc = br.indexOf('\\', t);
					if (esc < 0 || esc > q2) {
						break;
					}
					if ("\"\\".indexOf(br.charAt(esc + 1)) >= 0) {
						q2 = esc + 2;
					} else {
						br = br.substring(0, esc).append(br.substring(esc + 1));
						q2 = esc;
						--komma;
					}
				}

				eat = eat.append(br.substring(quote, q2));
				br = br.substring(q2 + 1);
				continue;
			}
			eat = eat.append(br.substring(0, komma)).trim();
			int gt = 0;
			if (eat.charAt(eat.length() - 1) == '>') {
				gt = eat.lastIndexOf('<');
			}

			// remove comments from address part
			for (int bra = eat.indexOf('(', gt); bra >= 0; bra = eat.indexOf('(', bra)) {
				int ket = eat.indexOf(')', bra);
				eat = eat.substring(0, bra).trim().append(eat.substring(ket + 1).trim());
			}

			// remove src routes from address part
			for (int dot = eat.indexOf(':', gt); dot >= 0; dot = eat.indexOf(':', gt)) {
				eat = eat.substring(0, gt + 1).append(eat.substring(dot + 1));
			}

			ll.add(eat);

			br = br.substring(komma + 1);
			eat = new ByteRef();
		}
		return ll;
	}

}
