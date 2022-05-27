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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import de.bb.log.Logger;
import de.bb.security.Pkcs5;
import de.bb.security.SHA256;
import de.bb.util.ByteRef;
import de.bb.util.DateFormat;
import de.bb.util.Mime;
import de.bb.util.MimeFile;
import de.bb.util.MimeFile.Info;
import de.bb.util.Misc;
import de.bb.util.Pair;
import de.bb.util.TimedLRUCache;

/**
 * Common SQL DBI with methods for most databases.
 * 
 * @author sfranke
 */
public abstract class MailDBI {

	private static Logger LOG = Logger.getLogger(MailDBI.class);

	private static final ByteRef NTO = new ByteRef("TO:");

	private static final ByteRef NCC = new ByteRef("CC:");

	private static final ByteRef NFROM = new ByteRef("FROM:");

	static final Object LOCK = new Object();

	private static final TimedLRUCache<String, Long> LOGINDELAY = new TimedLRUCache<String, Long>(3600 * 1000L);

	private static final TimedLRUCache<String, String> AUTORESPONSESENT = new TimedLRUCache<String, String>(
			24 * 3600 * 1000L);

	static volatile boolean isRecovering = false;

	String concat1;

	String concat2;

	String concat3;

	String dateFx;

	private String dbUser = null, dbPass = null;

	/** the Connection to the DB */
	de.bb.sql.Connection jdbcCon;

	/** the JDBC String for DB connection */
	String jdbcUrl;

	// instance vars
	/** the log file to write to */

	IMailFile mailFile;

	String passwdFx1;
	String passwdFx2;

	/** JDBC statement to perform update and deletes */
	HashMap<String, PreparedStatement> q2ps = new HashMap<String, PreparedStatement>();

	String deleteEnd = "";

	private Statement stmt;

	/**
	 * Check whether connection is alive or connection can be reused.
	 * 
	 * @throws SQLException
	 */
	protected void checkConnection() throws SQLException {
		// LOG.debug("checkConnection");
		try {
			if (jdbcCon != null && !jdbcCon.isClosed())
				return;
		} catch (Throwable t1) {
		}
		// close old
		close();

		Connection con = dbUser != null ? DriverManager.getConnection(jdbcUrl, dbUser, dbPass)
				: DriverManager.getConnection(jdbcUrl);

		jdbcCon = new de.bb.sql.Connection(con);
		stmt = jdbcCon.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
	}

	/**
	 * Method close.
	 */
	void close() {

		try {
			stmt.close();
		} catch (Exception ex) {
		}

		for (final PreparedStatement ps : q2ps.values()) {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
		q2ps.clear();

		try {
			if (jdbcCon != null) {
				jdbcCon.close();
				jdbcCon = null;
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Get or create a prepared statemnt for a given query.
	 *
	 * @param query
	 * @return the prepared statement
	 * @throws SQLException
	 */
	PreparedStatement getPreparedStatement(final String query) throws SQLException {
		PreparedStatement ps = q2ps.get(query);
		if (ps != null)
			return ps;

		ps = jdbcCon.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
		q2ps.put(query, ps);
		return ps;
	}

	/**
	 * Weak verify of an user, by time interval to last access with password.
	 * 
	 * @param user
	 *            the user name without mail_domain, e.g. massa
	 * @param mail_domain
	 *            the users mail_domain name, e.g. here.com
	 * @param ipAddress
	 * @param last
	 * @return true when user and password matches, otherwise false
	 * @throws Exception
	 */
	public String allowUser(String user, String mail_domain, String ipAddress, long[] last) throws Exception {

		LOG.debug("allowUser");

		ResultSet rs = selectFromMailUserIp(user, mail_domain, ipAddress);
		try {
			if (rs.next()) {
				String userId = rs.getString(1);
				if (last != null) {
					Timestamp ts = rs.getTimestamp(5);
					last[0] = ts != null ? ts.getTime() : 0;
				}

				updateMailUser(userId, ipAddress);
				return userId;
			}
			return null;
		} finally {
			rs.close();
		}
	}

	/**
	 * Add a new mail to the specified mailbox.
	 * 
	 * @param unitId
	 * @param mailbox
	 * @param flags
	 * @param date
	 * @param me
	 * @return
	 * @throws Exception
	 */
	public boolean append(String unitId, String mailbox, String flags, long date, MailEntry me) throws Exception {

		LOG.debug("append(" + unitId + ", " + mailbox + ", " + flags + ", " + date + ", ...)");

		String mbId = mailboxId(unitId, mailbox);

		storeMail(me); // update length field

		// extract the MIME stuff
		InputStream is = null;
		ResultSet rs = null;
		try {
			is = getInputStream(me.mailId);
			jdbcCon.setAutoCommit(false);

			LOG.debug("checkin " + me.mailId);
			rs = selectFromImapMime(me.mailId, "");
			if (!rs.next()) {

				LOG.debug("OK, not there " + me.mailId);
				// insert mime stuff
				ArrayList<Info> mime = MimeFile.parseMime(is);
				for (Iterator<Info> e = mime.iterator(); e.hasNext();) {
					MimeFile.Info mi = e.next();
					if (!insertIntoImapMime(me.mailId, mi.path, mi.contentType, mi.hBegin, mi.bBegin, mi.end, mi.hLines,
							mi.bLines)) {
						LOG.debug("FATAL insertIntoMime failed: " + me.mailId);
						return false;
					}
				}
			}

			if (null == insertIntoImapData(mbId, me.mailId, date, flags.indexOf("\\Answered") >= 0,
					flags.indexOf("\\Flagged") >= 0, flags.indexOf("\\Deleted") >= 0, flags.indexOf("\\Seen") >= 0,
					flags.indexOf("\\Draft") >= 0, me.size)) {
				LOG.debug("FATAL insertIntoMime failed: " + me.mailId);
				return false;
			}

			jdbcCon.commit();

			LOG.debug("SUCCESS stored: " + me.mailId);

			String fc = selectCountFromImapData(mbId);
			if (fc != null) {
				Imap.updateStatus(mbId, "* " + fc + " EXISTS");
			}

			return true;

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception ex2) {
			}
			try {
				if (rs != null)
					rs.close();
			} catch (Exception ex2) {
			}
			jdbcCon.rollback();
			jdbcCon.setAutoCommit(true);
		}
	}

	/**
	 * Change a PO box's password.
	 * 
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @param oldPasswd
	 *            the old password
	 * @param newPasswd
	 *            the new password
	 * @return true on success
	 * @throws Exception
	 */
	public boolean changePoBoxPass(String user, String mail_domain, String oldPasswd, String newPasswd)
			throws Exception {

		LOG.debug("changePoBoxPass as " + user + '@' + mail_domain);
		if (verifyUser(user, mail_domain, oldPasswd, null, null) == null)
			return false;
		return updateMailUser(user, mail_domain, newPasswd);
	}

	void commit() throws SQLException {
		jdbcCon.commit();
	}

	/**
	 * @param mail_domain
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public boolean createDomain(String mail_domain, String owner, boolean guessPermission) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM mail_domain WHERE mail_domain=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, mail_domain);

		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			q = "UPDATE mail_domain set owner=?, guessPermission=? WHERE mail_domain=?";
		} else {
			q = "INSERT INTO mail_domain (owner, guessPermission, mail_domain) VALUES (?, ?, ?)";
		}
		rs.close();

		ps = getPreparedStatement(q);
		ps.setString(1, owner);
		ps.setInt(2, guessPermission ? 1 : 0);
		ps.setString(3, mail_domain);
		return 0 != ps.executeUpdate();
	}

	/**
	 * Create a new Forwarder.
	 * 
	 * @param owner
	 *            owner of the mail_domain
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @param passwd
	 *            users password if owner is null
	 * @param forward
	 * @param notify
	 *            the send notifications instead of forward the mail
	 * @return true on success.
	 * @throws Exception
	 */
	public boolean createForwarder(String owner, String user, String mail_domain, String passwd, String forward,
			boolean notify) throws Exception {

		LOG.debug("createForwarder as " + owner + " for " + user + '@' + mail_domain);

		ResultSet rs = selectFromDomain(mail_domain, owner);
		try {
			if (!rs.next()) {
				if (null == verifyUser(user, mail_domain, passwd, null, null)) {
					return false;
				}
			}

			rs.close();
			rs = selectFromMailUser(user, mail_domain);
			if (!rs.next())
				return false;

			return insertIntoForward(rs.getString(1), forward, notify);
		} finally {
			rs.close();
		}
	}

	/**
	 * @param userId
	 * @param base
	 * @return
	 * @throws SQLException
	 */
	public boolean createMailUserImapUnit(String userId, String base) throws SQLException {
		checkConnection();
		String q = "INSERT INTO imap_unit (base) VALUES (?)";

		String unitId = insertAtomic("imap_unit", q, base);
		if (unitId == null)
			return false;

		q = "INSERT INTO mail_user_imap_unit VALUES (?, ?, 1, 1)";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		ps.setLong(2, Long.parseLong(unitId));
		if (0 == ps.executeUpdate())
			return false;

		insertIntoImapFolder(unitId, "INBOX");
		return true;
	}

	private String insertAtomic(String table, String query, String... params) throws SQLException {
		synchronized (LOCK) {
			PreparedStatement ps = getPreparedStatement(query);
			int i = 0;
			for (String p : params) {
				ps.setString(++i, p);
			}
			ps.executeUpdate();

			return getLastInsertId(table);
		}
	}

	protected abstract String getLastInsertId(final String tableName) throws SQLException;

	/**
	 * Create a new MailEntry.
	 * 
	 * @return a new MailElement
	 * @throws Exception
	 */
	public MailEntry createNewMail() throws Exception {

		LOG.debug("createNewMail");
		return mailFile.createNewMail();
	}

	/**
	 * Create a new PO box.
	 * 
	 * @param owner
	 *            owner of the mail_domain
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @return true on success.
	 * @throws Exception
	 */
	public boolean createPoBox(String owner, String user, String mail_domain) throws Exception {

		LOG.debug("createPoBox as " + owner + " for " + user + '@' + mail_domain);

		ResultSet rs = selectFromDomain(mail_domain, owner);
		try {
			if (!rs.next())
				return false;

			return insertIntoMailUser(user, mail_domain);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		} finally {
			rs.close();
		}
	}

	/**
	 * Deletes the mail_domain and all mail users.
	 * 
	 * @param mailDomain
	 * @return true if mail_domain was deleted.
	 * @throws SQLException
	 */
	public boolean deleteFromDomain(String mailDomain) throws SQLException {
		checkConnection();
		String q = "DELETE FROM mail_user WHERE mail_domain=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, mailDomain);
		boolean ok = 0 != ps.executeUpdate();

		q = "DELETE FROM mail_domain WHERE mail_domain=?";
		ps = getPreparedStatement(q);
		ps.setString(1, mailDomain);
		ok &= 0 != ps.executeUpdate();

		return ok;
	}

	/**
	 * @param mail_user_id
	 * @param forward_id
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromForward(String mail_user_id, String forward_id) throws SQLException {
		checkConnection();
		String q = "DELETE FROM forward WHERE id=? AND mail_user_id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, forward_id);
		ps.setString(2, mail_user_id);
		return ps.executeUpdate();
	}

	/**
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromImapData(String id) throws SQLException {
		checkConnection();
		String q = "DELETE FROM imap_data WHERE id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(id));
		return ps.executeUpdate();
	}

	/**
	 * @param mbId
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromImapData(String mbId, String filename) throws SQLException {
		checkConnection();
		String q = "DELETE FROM imap_data WHERE imap_folder_id=? AND imap_mime_filename=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		ps.setString(2, filename);
		return ps.executeUpdate();
	}

	/**
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromImapMime(String filename) throws SQLException {
		checkConnection();
		String q = "DELETE FROM imap_mime WHERE filename=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		return ps.executeUpdate();
	}

	/**
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromImapSubs(String userId) throws SQLException {
		checkConnection();
		String q = "DELETE FROM imap_subs WHERE mail_user_id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		return ps.executeUpdate();
	}

	/**
	 * @param userId
	 * @param mailbox
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromImapSubs(String userId, String mailbox) throws SQLException {
		checkConnection();
		String q = "DELETE FROM imap_subs WHERE mail_user_id=? AND path=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		ps.setString(2, mailbox);
		return ps.executeUpdate();
	}

	/**
	 * @param name
	 * @param mail_domain
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromMailUser(String name, String mail_domain) throws SQLException {
		checkConnection();
		String q = "DELETE FROM mail_user WHERE name=? AND mail_domain=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, name);
		ps.setString(2, mail_domain);
		return ps.executeUpdate();
	}

	/**
	 * @param msgId
	 * @return
	 * @throws SQLException
	 */
	public int deleteFromSpool(String msgId) throws SQLException {
		checkConnection();
		String q = "DELETE FROM spool WHERE id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, msgId);
		return ps.executeUpdate();
	}

	/**
	 * @param mbId
	 * @return
	 * @throws SQLException
	 */
	public int deleteImapFolder(String mbId) throws SQLException {
		checkConnection();
		// remove all mime information
		String
		// q = "DELETE FROM imap_mime FROM imap_data d WHERE imap_data_id=d.id
		// AND d.imap_folder_id=" + mbId;
		// stmt.executeUpdate(q);

		// remove all files
		q = "DELETE FROM imap_data WHERE imap_folder_id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		ps.executeUpdate();

		// remove the folder
		q = "DELETE FROM imap_folder WHERE id=?";
		ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		return ps.executeUpdate();
	}

	/**
	 * Delete a mail element from the default mail storage.
	 * 
	 * @param userId
	 *            the userId
	 * @param me
	 *            the mail element
	 * @throws SQLException
	 */
	public void deleteMail(String userId, MailEntry me) throws SQLException {

		LOG.debug("deleteMail");
		String unitId = getOrCreateUnitId(userId);
		if (unitId == null)
			return;
		String mbId = this.mailboxId(unitId, "INBOX");
		if (mbId == null)
			return;
		this.deleteFromImapData(mbId, me.mailId);
	}

	/**
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @see de.bb.bejy.mail.MailDBI#executeSQL(java.lang.String)
	 */
	public String executeSQL(String fileName) throws IOException {
		StringBuffer sb = new StringBuffer();

		BufferedReader br = null;
		try {
			FileInputStream fis = new FileInputStream(fileName);
			InputStreamReader isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			StringBuffer qb = new StringBuffer();
			for (;;) {
				String line = br.readLine();
				if (line != null && line.length() != 0) {
					qb.append(line);
					continue;
				}

				String q = qb.toString().trim();
				if (q.endsWith("/"))
					q = q.substring(0, q.length() - 1).trim();
				if (q.endsWith(";"))
					q = q.substring(0, q.length() - 1).trim();
				if (q.length() > 0 && !q.startsWith("--"))
					try {
						checkConnection();
						stmt.execute(q);
					} catch (Exception e) {
						sb.append(e.getMessage() + "\r\n");
					}
				if (line == null)
					break;
				qb = new StringBuffer();
			}

		} finally {
			if (br != null)
				br.close();
		}
		return sb.toString();
	}

	/**
	 * extract user and password from jdbcUrl
	 */
	protected void extractUserPass() {
		int i = jdbcUrl.indexOf('?');
		if (i < 0)
			return;
		String q = jdbcUrl.substring(i + 1);
		jdbcUrl = jdbcUrl.substring(0, i);
		i = q.indexOf('&');
		if (i < 0)
			return;

		String p = q.substring(i + 1);
		q = q.substring(0, i);
		if (q.charAt(0) == 'u') {
			String t = q;
			q = p;
			p = t;
		}

		if (p.startsWith("user="))
			dbUser = p.substring(5);
		if (q.startsWith("password="))
			dbPass = q.substring(9);
	}

	/**
	 * Get an InputStream for the selected mail element.
	 * 
	 * @param mailId
	 *            the mail ID
	 * @return an InputStream to read the mail content
	 */
	public InputStream getInputStream(String mailId) {

		LOG.debug("getInputStream");
		try {
			return mailFile.getInputStream(mailId);
		} catch (FileNotFoundException e) {
			return new ByteArrayInputStream(new byte[] {});
		}
	}

	/**
	 * Get domains for owner.
	 * 
	 * @param owner
	 *            name of owner for domains
	 * @return a Vector containing his mail_domain names.
	 * @throws Exception
	 */
	public Vector<String> getMailDomains(String owner) throws Exception {

		LOG.debug("getMailDomains for " + owner);

		Vector<String> v = new Vector<String>();
		//
		ResultSet rs = selectFromDomainByOwner(owner);
		try {
			while (rs.next()) {
				v.add(rs.getString(1));
			}
			return v;
		} finally {
			rs.close();
		}
	}

	/**
	 * Get domains for owner.
	 * 
	 * @param owner
	 *            name of owner for domains
	 * @return a Vector containing his mail_domain names.
	 * @throws Exception
	 */
	public Vector<String> getMailDomainsLike(String owner, String filter, boolean up) throws Exception {

		LOG.debug("getMailDomains for " + owner);

		Vector<String> v = new Vector<String>();
		//
		ResultSet rs = selectFromDomainByOwnerLike(owner, filter, up);
		try {
			while (rs.next()) {
				v.addElement(rs.getString(1));
			}
			return v;
		} finally {
			rs.close();
		}
	}

	/**
	 * @param mbId 
	 * @return
	 * @throws SQLException
	 */
	public int getNextIdFromImapData(String mbId) throws SQLException {
		checkConnection();
		String q = "SELECT MAX(id) FROM imap_data WHERE imap_folder_id=" + mbId;
		ResultSet rs = stmt.executeQuery(q);
		try {
			if (!rs.next())
				return 0;
			return rs.getInt(1) + 1;
		} finally {
			rs.close();
		}
	}

	/**
	 * IMAP support: get default unit id. Tries to create unit and folder id
	 * necessary.
	 * 
	 * @param userId
	 *            the user id
	 * @return the unit id or null on error
	 * @throws SQLException
	 *             on fatal error
	 */
	public String getOrCreateUnitId(String userId) throws SQLException {

		LOG.debug("getOrCreateUnitId(" + userId + ")");

		ResultSet rs = null;
		synchronized (LOCK) {
			try {
				rs = selectFromMailUserImapUnit(userId);
				if (!rs.next()) {
					if (!createMailUserImapUnit(userId, ""))
						return null;
					rs.close();
					rs = selectFromMailUserImapUnit(userId);
					if (!rs.next())
						return null;
				}
				String unitId = rs.getString(1);
				return unitId;
			} catch (SQLException e) {
				throw e;
			} finally {
				if (rs != null)
					rs.close();
			}
		}
	}

	/**
	 * Get an OutputStream for the selected mail element.
	 * 
	 * @param mailId
	 *            the mail ID
	 * @return an OutputStream to write the mail content
	 * @throws Exception
	 */
	public OutputStream getOutputStream(String mailId) throws Exception {

		LOG.debug("getOutputStream");
		return mailFile.getOutputStream(mailId);
	}

	/**
	 * Get PO boxes for mail_domain.
	 * 
	 * @param mail_domain
	 *            the mail_domain name
	 * @return a Vector with the PO boxes.
	 * @throws Exception
	 */
	public Vector<String> getPoBoxes(String mail_domain) throws Exception {

		LOG.debug("getPoBoxes for " + mail_domain);

		Vector<String> v = new Vector<String>();
		//
		ResultSet rs = null;
		try {
			rs = selectFromMailUser(mail_domain);
			while (rs.next()) {
				v.addElement(rs.getString(2));
			}
			return v;
		} catch (SQLException e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * @param owner
	 * @param user
	 * @param mail_domain
	 * @param passwd
	 * @return
	 * @throws Exception
	 */
	public String getPoBoxReplytext(String owner, String user, String mail_domain, String passwd) throws Exception {

		LOG.debug("getPoBoxReplytext as " + owner + " for " + user + '@' + mail_domain);

		String id = null;
		ResultSet rs = selectFromDomain(mail_domain, owner);
		if (!rs.next()) {
			rs.close();
			id = verifyUser(user, mail_domain, passwd, "", null);
			if (null == id) {
				return null;
			}
		} else {
			rs.close();
			rs = selectFromMailUser(user, mail_domain);
			if (!rs.next()) {
				rs.close();
				return null;
			}
			id = rs.getString(1);
			rs.close();
		}

		String res = selectFromResponse(id);

		return res;
	}

	/**
	 * Get size for PO box.
	 * 
	 * @param user
	 * @param mail_domain
	 * @return
	 * @throws Exception
	 */
	public long getPoBoxSize(String user, String mail_domain) throws SQLException {

		LOG.debug("getPoBoxSize for " + user + '@' + mail_domain);

		ResultSet rs = selectFromMailUser(user, mail_domain);
		try {
			if (!rs.next())
				return 0;
			String userId = rs.getString(1);
			rs.close();

			rs = selectFromMailUserImapUnit(userId);
			if (!rs.next())
				return 0;
			String unitId = rs.getString(1);

			return selectSumFromImapFolder(unitId);
		} finally {
			rs.close();
		}
	}

	/**
	 * Create a new entry in the forwarder table.
	 * 
	 * @param mail_user_id
	 *            the user id
	 * @param forward
	 *            the forwarder
	 * @param notify
	 *            notification flag
	 * @return true on success.
	 * @throws SQLException
	 */
	public boolean insertIntoForward(String mail_user_id, String forward, boolean notify) throws SQLException {
		checkConnection();
		String q = "INSERT INTO forward (mail_user_id, forward, notify) VALUES (?, ?, ?)";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, mail_user_id);
		ps.setString(2, forward.trim());
		ps.setInt(3, (notify ? 1 : 0));
		return 0 != ps.executeUpdate();
	}

	/**
	 * @param mbId
	 * @param filename
	 * @param date
	 * @param f1
	 * @param f2
	 * @param f3
	 * @param f4
	 * @param f5
	 * @param size
	 * @return
	 * @throws SQLException
	 */
	public String insertIntoImapData(String mbId, String filename, long date, boolean f1, boolean f2, boolean f3,
			boolean f4, boolean f5, String size) throws SQLException {
		checkConnection();
		String q = "insert into imap_data (imap_folder_id, imap_mime_filename, last, f_answered, f_flagged, f_deleted, f_seen, f_draft, filesize) values(?, ?, ?, ?, ?, ?, ?, ?, ?)";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		ps.setString(2, filename);
		ps.setTimestamp(3, new java.sql.Timestamp(date));
		// set the flags
		ps.setInt(4, f1 ? 1 : 0);
		ps.setInt(5, f2 ? 1 : 0);
		ps.setInt(6, f3 ? 1 : 0);
		ps.setInt(7, f4 ? 1 : 0);
		ps.setInt(8, f5 ? 1 : 0);
		ps.setLong(9, Long.parseLong(size));
		ps.executeUpdate();

		return getLastInsertId("imap_data");
	}

	/**
	 * @param unitId
	 * @param mailbox
	 * @return
	 * @throws SQLException
	 */
	public boolean insertIntoImapFolder(String unitId, String mailbox) throws SQLException {
		checkConnection();
		String q = "INSERT INTO imap_folder (imap_unit_id, path) VALUES (?, ?)";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(unitId));
		ps.setString(2, mailbox);
		return 0 != ps.executeUpdate();
	}

	/**
	 * @param filename
	 * @param path
	 * @param contentType
	 * @param hBegin
	 * @param bBegin
	 * @param end
	 * @param hLines
	 * @param bLines
	 * @return
	 * @throws SQLException
	 */
	public boolean insertIntoImapMime(String filename, String path, String contentType, int hBegin, int bBegin, int end,
			int hLines, int bLines) throws SQLException {
		checkConnection();
		String q = "insert into imap_mime (filename, path, contentType, b_begin, b_body, b_end, l_header, l_body) values(?, ?, ?, ?, ?, ?, ?, ?)";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		ps.setString(2, path);
		ps.setString(3, contentType);
		ps.setInt(4, hBegin);
		ps.setInt(5, bBegin);
		ps.setInt(6, end);
		ps.setInt(7, hLines);
		ps.setInt(8, bLines);
		return 0 != ps.executeUpdate();
	}

	/**
	 * @param userId
	 * @param mailbox
	 * @return
	 * @throws SQLException
	 */
	public boolean insertIntoImapSubs(String userId, String mailbox) throws SQLException {
		checkConnection();
		String q = "INSERT INTO imap_subs (mail_user_id, path) VALUES(?, ?)";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		ps.setString(2, mailbox);
		return 0 != ps.executeUpdate();
	}

	/**
	 * @param name
	 * @param mail_domain
	 * @return
	 * @throws SQLException
	 */
	public boolean insertIntoMailUser(String name, String mail_domain) throws SQLException {
		checkConnection();
		String q = "INSERT INTO mail_user (name, mail_domain, passwd, address, keep, reply) VALUES (?, ?, '*', null, 1, 0)";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, name.trim());
		ps.setString(2, mail_domain.trim());
		return 0 != ps.executeUpdate();
	}

	/**
	 * @param filename
	 * @param name
	 * @param mail_domain
	 * @param s_name
	 * @param s_domain
	 * @return
	 * @throws SQLException
	 */
	public boolean insertIntoSpool(String filename, String name, String mail_domain, String s_name, String s_domain)
			throws SQLException {
		checkConnection();
		String q = "INSERT INTO spool (filename, name, mail_domain, next_send, retry, s_name, s_domain) VALUES (?, ?, ?, "
				+ dateFx + ", 0, ?, ?)";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		ps.setString(2, name);
		ps.setString(3, mail_domain);
		ps.setString(4, s_name);
		ps.setString(5, s_domain);
		return 0 != ps.executeUpdate();
	}

	/**
	 * Check mail_domain name against local mail_domain names.
	 * 
	 * @param mail_domain
	 *            the checked mail_domain name
	 * @return true, if the mail_domain is a local mail_domain, false either.
	 * @throws Exception
	 */
	public boolean isLocalDomain(String mail_domain) {

		LOG.debug("isLocalDomain");
		ResultSet rs = null;
		try {
			try {
				rs = selectFromDomain(mail_domain);
				return rs.next();
			} finally {
				if (rs != null)
					rs.close();
			}
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Check that specified user is a local user.
	 * 
	 * @param user
	 *            the user name
	 * @param mail_domain
	 *            the mail_domain name
	 * @return true, if the user is a local user, false either.
	 * @throws Exception
	 */
	public boolean isLocalUser(String user, String mail_domain) throws SQLException {

		LOG.debug("isLocalUser");
		ResultSet rs = null;
		try {
			rs = selectFromMailUser(user, mail_domain);
			return rs.next();
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * check whether there is forward entry from s_name@s_domain to name@domain.
	 * 
	 * @param nameFrom
	 *            local user name
	 * @param domainFrom
	 *            local user domain
	 * @param nameTo
	 *            forwarded user name
	 * @param domainTo
	 *            forwarded domain name
	 * @return true if there is a forwarder configuration, false either
	 */
	public boolean isForwarder(String nameFrom, String domainFrom, String nameTo, String domainTo) throws SQLException {
		checkConnection();

		LOG.debug("isForwarder");
		ResultSet rs = null;
		try {
			String q = "SELECT * from mail_user, forward " + "WHERE mail_user.id=forward.mail_user_id "
					+ "AND mail_user.name=? " + "AND mail_user.mail_domain=? AND forward.forward=?";
			PreparedStatement ps = getPreparedStatement(q);
			ps.setString(1, nameFrom);
			ps.setString(2, domainFrom);
			ps.setString(3, nameTo + "@" + domainTo);
			rs = ps.executeQuery();
			return rs.next();
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * Returns the id for a mailbox.
	 * 
	 * @param unitId
	 *            the unit, containing mail folders
	 * @param mailbox
	 *            a mail folder
	 * @return
	 * @throws SQLException
	 */
	public String mailboxId(String unitId, String mailbox) throws SQLException {

		LOG.debug("mailboxId(" + unitId + ", " + mailbox + ")");

		// get mailbox id
		ResultSet rs = null;
		try {
			rs = selectFromImapFolder(unitId, mailbox);
			if (!rs.next())
				return null;

			String mbId = rs.getString(1);
			return mbId;
		} catch (SQLException e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * Get an RandomAccessFile for the selected mail element.
	 * 
	 * @param mailId
	 *            the ID of the mail element
	 * @return a RandomAccessFile to read the mail element
	 * @throws Exception
	 */
	public RandomAccessFile openRandomAccessFile(String mailId) throws Exception {
		return mailFile.openRandomAccessFile(mailId);
	}

	/**
	 * Global cleanup.
	 * 
	 * @throws Exception
	 */
	public void recoverFiles(boolean createUsers, boolean createDomains) throws Exception {
		synchronized (LOCK) {
			if (isRecovering)
				return;
			isRecovering = true;
		}
		try {

			LOG.debug("recoverFiles");

			LOG.debug("start recovering lost mails");

			for (Iterator<File> i = mailFile.files(); i.hasNext();) {
				File file = i.next();
				String fileName = file.toString();

				// is the file user by some folder?
				ResultSet rs = selectFromImapDataByFilename(fileName);
				boolean f = rs.next();
				rs.close();

				if (f) {
					continue;
				}

				// is the file used by the spooler?
				rs = selectFromSpool(fileName);
				f = rs.next();
				rs.close();
				if (f) {
					continue;
				}

				try {
					LOG.debug("analysing lost mail: " + fileName);

					// remove it from Mime store, if it is used there
					deleteFromImapMime(fileName);

					ArrayList<Info> v = null;

					InputStream is = this.getInputStream(fileName);
					try {
						// parse the file
						v = MimeFile.parseMime(is);
					} finally {
						is.close();
						is = null;
					}
					MimeFile.Info info = v.get(0);
					LOG.debug("analyze mail range: " + info.hBegin + " - " + info.bBegin);

					// read the complete header
					byte b[] = new byte[info.bBegin];
					is = this.getInputStream(fileName);
					try {
						is.read(b);
					} finally {
						is.close();
					}

					ByteRef br = new ByteRef(b);
					ByteRef to = new ByteRef();
					ByteRef from = new ByteRef();
					int collect = 0;
					for (ByteRef line = br.nextLine(); line != null; line = br.nextLine()) {
						if (line.length() == 0)
							continue;
						if (line.charAt(0) <= 32) {
							if (collect == 1) {
								to = to.append(line);
							} else if (collect == 2) {
								from = from.append(line);
							}
							continue;
						}
						if (line.length() < 3)
							continue;

						collect = 0;
						ByteRef start = line.substring(0, 3).toUpperCase();
						if (start.equals(NCC) || start.equals(NTO)) {
							collect = 1;
							to = to.append(line.substring(3));
							continue;
						}

						start = line.substring(0, 5).toUpperCase();
						if (start.equals(NFROM)) {
							collect = 2;
							from = from.append(line.substring(5));
						}
					}

					LOG.debug("analyze mail from: " + from + " - " + to);

					String user = "postmaster";
					String mail_domain = "localhost";
					// parse from:
					{
						ByteRef ud = from;
						int j = from.indexOf('<');
						if (j >= 0) {
							int k = from.indexOf('>', j);
							ud = from.substring(j + 1, k);
						}
						int k = ud.indexOf('@');
						if (k >= 0) {
							user = ud.substring(0, k).trim().toString();
							mail_domain = ud.substring(k + 1).trim().toString();
						}
					}
					ArrayList<String> rcpt = new ArrayList<String>();
					// parse from:
					for (ByteRef ud = to.nextWord(','); ud != null; ud = to.nextWord()) {
						int j = ud.indexOf('<');
						if (j >= 0) {
							int k = ud.indexOf('>', j);
							ud = ud.substring(j + 1, k);
						}
						ud = ud.trim();
						while (ud.indexOf(' ') > 0) {
							ud = ud.substring(ud.indexOf(' ')).trim();
						}
						int l = ud.indexOf('@');
						if (l >= 0) {
							String todomain = ud.substring(l + 1).toString();
							rs = selectFromDomain(todomain);
							boolean exi = rs.next();
							if (!exi && createDomains) {
								rs.close();
								createDomain(todomain, "admin", false);
								LOG.debug("creating domain: " + todomain);
								rs = selectFromDomain(todomain);
								f = rs.next();
							}
							if (exi) {
								rcpt.add(ud.toString());
							}
							rs.close();
						}
					}

					ArrayList<String> rcpt2 = new ArrayList<String>();
					for (Iterator<String> e = rcpt.iterator(); e.hasNext();) {
						String arcpt = e.next();
						int l = arcpt.indexOf('@');
						String ar = arcpt.substring(0, l);
						String cpt = arcpt.substring(l + 1);
						rs = selectFromMailUser(ar, cpt);
						boolean exi = rs.next();
						if (!exi && createUsers) {
							rs.close();
							createPoBox("admin", ar, cpt);
							LOG.debug("creating mail user: " + arcpt);
							rs = selectFromMailUser(ar, cpt);
							exi = rs.next();
						}
						if (exi)
							rcpt2.add(arcpt);
						rs.close();
					}

					if (rcpt2.size() > 0) {
						LOG.debug("resending mail from: " + user + "@" + mail_domain + " to: " + rcpt2);

						MailEntry me = new MailEntry(fileName, null, null);
						storeMail(me);
						sendMultiMail(me, user, mail_domain, rcpt2);
					} else {
						LOG.debug("skipping mail from: " + user + "@" + mail_domain + " to: " + rcpt);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				Thread.sleep(1);
				Thread.yield();
			}
			LOG.debug("finished recovering lost mails");
		} finally {
			isRecovering = false;
		}
	}

	/**
	 * @param owner
	 * @param user
	 * @param mail_domain
	 * @param passwd
	 * @param fwid
	 * @return
	 * @throws Exception
	 */
	public boolean removeForwarder(String owner, String user, String mail_domain, String passwd, String fwid)
			throws Exception {

		LOG.debug("removeForwarder as " + owner + " for " + user + '@' + mail_domain);

		ResultSet rs = null;
		try {
			rs = selectFromDomain(mail_domain, owner);
			if (!rs.next()) {
				if (null == verifyUser(user, mail_domain, passwd, "", null)) {
					return false;
				}
			}

			rs.close();
			rs = selectFromMailUser(user, mail_domain);
			if (!rs.next())
				return false;

			return 0 != deleteFromForward(rs.getString(1), fwid);

		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * Remove a PO box.
	 * 
	 * @param owner
	 *            owner of the mail_domain
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @return true on success.
	 * @throws Exception
	 */
	public boolean removePoBox(String owner, String user, String mail_domain) throws Exception {

		LOG.debug("removePoBox as " + owner + " for " + user + '@' + mail_domain);

		ResultSet rs = selectFromDomain(mail_domain, owner);
		try {
			if (!rs.next())
				return false;

			rs.close();
			rs = selectFromMailUser(user, mail_domain);
			if (!rs.next()) {
				return false;
			}
			String userId = rs.getString(1);
			rs.close();
			rs = null;

			// remove responses
			deleteFromMailUserReply(userId);

			// remove subscriptions
			deleteFromImapSubs(userId);

			// remove unit
			String q = makeDelete("imap_data") + " imap_folder, imap_unit, mail_user_imap_unit WHERE "
					+ "imap_data.imap_folder_id=imap_folder.id AND " + "imap_folder.imap_unit_id=imap_unit.id AND "
					+ "mail_user_imap_unit.imap_unit_id=imap_unit.id AND " + "mail_user_imap_unit.mail_user_id=?"
					+ deleteEnd;
			PreparedStatement ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(userId));
			ps.execute();

			q = makeDelete("imap_folder") + " imap_unit, mail_user_imap_unit "
					+ "WHERE imap_folder.imap_unit_id=imap_unit.id AND mail_user_imap_unit.imap_unit_id=imap_unit.id "
					+ "AND mail_user_imap_unit.mail_user_id=?" + deleteEnd;
			ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(userId));
			ps.execute();

			q = "DELETE FROM mail_user_imap_unit WHERE mail_user_id=?";
			ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(userId));
			ps.execute();

			q = makeDelete("imap_unit") + " mail_user_imap_unit "
					+ "WHERE mail_user_imap_unit.imap_unit_id=imap_unit.id " + "AND mail_user_imap_unit.mail_user_id=?"
					+ deleteEnd;
			ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(userId));
			ps.execute();

			return 0 != deleteFromMailUser(user, mail_domain);
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	protected String makeDelete(final String tableName) {
		return "DELETE " + tableName + " FROM " + tableName + ", ";
	}

	void rollback() throws SQLException {
		jdbcCon.rollback();
	}

	/**
	 * @param unitId
	 * @param mask
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectAllLevelsFromImapFolder(String unitId, String mask) throws SQLException {
		checkConnection();
		String q = "SELECT " + concat1 + "base" + concat2 + "path" + concat3
				+ " AS x FROM imap_folder f, imap_unit u WHERE imap_unit_id= u.id AND u.id =? AND path LIKE ? ORDER BY x";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(unitId));
		ps.setString(2, mask);
		return ps.executeQuery();
	}

	/**
	 * @param mbId
	 * @return
	 * @throws SQLException
	 */
	public String selectCountFromImapData(String mbId) throws SQLException {
		checkConnection();
		String q = "SELECT DISTINCT COUNT(id) FROM imap_data WHERE imap_folder_id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		ResultSet rs = ps.executeQuery();
		try {
			if (!rs.next())
				return null;
			return rs.getString(1);
		} finally {
			rs.close();
		}
	}

	/**
	 * @param mbId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectDeletedFromImapData(String mbId) throws SQLException {
		checkConnection();
		String q = "SELECT id, f_deleted FROM imap_data WHERE imap_folder_id=? ORDER BY ID";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		return ps.executeQuery();
	}

	/**
	 * Get all existing domains.
	 * 
	 * @return all existing domains.
	 * @throws SQLException
	 */
	public ResultSet selectFromDomain() throws SQLException {
		checkConnection();
		String q = "SELECT * FROM mail_domain";
		PreparedStatement ps = getPreparedStatement(q);
		return ps.executeQuery();
	}

	/**
	 * Checks for a specified mail_domain.
	 * 
	 * @param mail_domain
	 * @return the mail_domain if it exists.
	 * @throws SQLException
	 */
	public ResultSet selectFromDomain(String mail_domain) throws SQLException {
		checkConnection();
		String query = "SELECT * FROM mail_domain WHERE mail_domain=?";
		PreparedStatement ps = getPreparedStatement(query);
		ps.setString(1, mail_domain);
		return ps.executeQuery();
	}

	/**
	 * @param mail_domain
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromDomain(String mail_domain, String owner) throws SQLException {
		checkConnection();
		String query = "SELECT * FROM mail_domain WHERE mail_domain=? AND owner=?";
		PreparedStatement ps = getPreparedStatement(query);
		ps.setString(1, mail_domain);
		ps.setString(2, owner);
		return ps.executeQuery();
	}

	/**
	 * @param mail_domain
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromDomainByOwnerLike(String owner, String filter, boolean up) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM mail_domain WHERE owner=? AND mail_domain LIKE ? ORDER BY mail_domain ";
		q += up ? "ASC" : "DESC";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, owner);
		ps.setString(2, filter + "%");
		return ps.executeQuery();
	}

	/**
	 * @param owner
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromDomainByOwner(String owner) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM mail_domain WHERE owner=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, owner);
		return ps.executeQuery();
	}

	/**
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromForward(String userId) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM forward WHERE mail_user_id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		return ps.executeQuery();
	}

	/**
	 * @param mbId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapData(String mbId) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_data WHERE imap_folder_id=? ORDER BY ID";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		return ps.executeQuery();
	}

	/**
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapDataByFilename(String filename) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_data WHERE imap_mime_filename=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		return ps.executeQuery();
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapDataOrdered() throws SQLException {
		checkConnection();
		String q = "SELECT DISTINCT imap_mime_filename FROM imap_data ORDER BY imap_mime_filename";
		PreparedStatement ps = getPreparedStatement(q);
		return ps.executeQuery();
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	private ResultSet selectOrphanedImapUnits() throws SQLException {
		checkConnection();
		String q = "select id from imap_unit where id not in (select imap_unit_id from mail_user_imap_unit)";
		PreparedStatement ps = getPreparedStatement(q);
		return ps.executeQuery();
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectAllFilesFromImapMime() throws SQLException {
		checkConnection();
		String q = "SELECT DISTINCT filename FROM imap_mime";
		PreparedStatement ps = getPreparedStatement(q);
		return ps.executeQuery();
	}

	/**
	 * @param files
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapDataByFilenames(HashMap<String, ?> files) throws SQLException {
		checkConnection();
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT * FROM imap_data WHERE");
		if (files.size() == 0) {
			sb.append(" 1=2");
		} else {
			boolean or = false;
			for (Iterator<String> i = files.keySet().iterator(); i.hasNext();) {
				String fileName = i.next();
				if (or)
					sb.append(" OR");
				sb.append(" imap_mime_filename='" + fileName + "'");
				or = true;
			}
		}
		String q = sb.toString();
		return stmt.executeQuery(q);
	}

	/**
	 * @param id
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapDataById(String id) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_data WHERE id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(id));
		return ps.executeQuery();
	}

	/**
	 * @param unitId
	 * @param mailbox
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapFolder(String unitId, String mailbox) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_folder WHERE imap_unit_id=? AND path=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(unitId));
		ps.setString(2, mailbox);
		return ps.executeQuery();
	}

	/**
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapMime(String filename) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_mime WHERE filename=? ORDER BY path";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		return ps.executeQuery();
	}

	/**
	 * @param filename
	 * @param path
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapMime(String filename, String path) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_mime WHERE filename=? AND path=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		ps.setString(2, path);
		return ps.executeQuery();
	}

	/**
	 * @param filename
	 * @param path
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapMimeTextPlain(String filename) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_mime WHERE filename=? AND contentType like 'text/%' ORDER BY path";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		return ps.executeQuery();
	}

	/**
	 * @param userId
	 * @param mailbox
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapSubs(String userId, String mailbox) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_subs WHERE mail_user_id=? AND path=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		ps.setString(2, mailbox);
		return ps.executeQuery();
	}

	/**
	 * @param userId
	 * @param mask
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapSubsLike(String userId, String mask) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM imap_subs WHERE mail_user_id=? AND path LIKE ? ORDER BY PATH";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		ps.setString(2, mask);
		return ps.executeQuery();
	}

	/**
	 * @param unitName
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromImapUnit(String unitName) throws SQLException {
		checkConnection();
		String q;
		if (unitName.length() == 0)
			q = "SELECT * FROM imap_unit WHERE base IS NULL OR base=''";
		else
			q = "SELECT * FROM imap_unit WHERE base=?";
		PreparedStatement ps = getPreparedStatement(q);
		if (unitName.length() > 0)
			ps.setString(1, unitName);
		return ps.executeQuery();
	}

	/**
	 * @param mail_domain
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromMailUser(String mail_domain) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM mail_user WHERE mail_domain=? ORDER BY name";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, mail_domain);
		return ps.executeQuery();
	}

	/**
	 * @param user
	 * @param mail_domain
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromMailUser(String user, String mail_domain) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM mail_user WHERE name=? AND mail_domain=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, user);
		ps.setString(2, mail_domain);
		return ps.executeQuery();
	}

	/**
	 * @param user
	 * @param mail_domain
	 * @param passwd
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromMailUser(final String user, final String mail_domain, final String passwd)
			throws SQLException {

		ResultSet rs = null;
		try {
			rs = selectFromMailUser(user, mail_domain);
			// user does not exist
			if (!rs.next()) {
				return rs;
			}

			String storedEncodedPassword = rs.getString(4);
			if (storedEncodedPassword == null)
				storedEncodedPassword = "";

			if (storedEncodedPassword.startsWith("{SSHA}")) {
				final byte salt[] = Misc.hex2Bytes(storedEncodedPassword.substring(6, 38));
				final SHA256 sha256 = new SHA256();
				sha256.update(salt);
				try {
					final byte[] sha256Digest = sha256.digest(passwd.getBytes("utf-8"));
					final String encodedPassword = "{SSHA}" + Misc.bytes2Hex(salt) + Misc.bytes2Hex(sha256Digest);
					if (!encodedPassword.equals(storedEncodedPassword))
						return rs;
				} catch (UnsupportedEncodingException e) {
					throw new SQLException(e);
				}

				// success -> update the password to PBKDF2
				updateMailUser(user, mail_domain, passwd);
			} else if (storedEncodedPassword.startsWith("{P5")) {
				if (!Pkcs5.verifyPbkdf2(storedEncodedPassword, passwd))
					return rs;
				// success -> update the password to PBKDF2
				updateMailUser(user, mail_domain, passwd);
			} else if (storedEncodedPassword.startsWith("{P")) {
				if (!Pkcs5.verifyPbkdf2(storedEncodedPassword, passwd))
					return rs;

				if (storedEncodedPassword.startsWith("{PKCS5"))
					updateMailUser(user, mail_domain, passwd);

			} else {
				// if the old format is used, check it
				rs.close();
				rs = null;
				String q = "SELECT * FROM mail_user WHERE name=? AND mail_domain=? AND passwd=" + passwdFx1 + "?"
						+ passwdFx2;
				PreparedStatement ps = getPreparedStatement(q);
				ps.setString(1, user);
				ps.setString(2, mail_domain);
				ps.setString(3, passwd);
				rs = ps.executeQuery();

				// failed login
				if (!rs.next()) {
					return rs;
				}

				// success -> update the password to PBKDF2
				updateMailUser(user, mail_domain, passwd);
			}
			// new format is used.
			rs.beforeFirst();
			return rs;

		} catch (SQLException se) {
			if (rs != null)
				rs.close();
			throw se;
		}
	}

	/**
	 * @param user
	 * @param mail_domain
	 * @param ipAddress
	 * @param when
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromMailUserIp(String user, String mail_domain, String ipAddress) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM mail_user WHERE name=? AND mail_domain=? AND address=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, user);
		ps.setString(2, mail_domain);
		ps.setString(3, ipAddress);
		// ps.setTimestamp(4, new Timestamp(when));
		return ps.executeQuery();
	}

	/**
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromMailUserImapUnit(String userId) throws SQLException {
		return selectFromMailUserImapUnit(userId, "");
	}

	/**
	 * @param userId
	 * @param base
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromMailUserImapUnit(String userId, String base) throws SQLException {
		checkConnection();
		String q;
		if (base.length() == 0) {
			q = "SELECT m.imap_unit_id FROM mail_user_imap_unit m, imap_unit u WHERE m.imap_unit_id=u.id AND m.mail_user_id=? AND (u.base IS NULL OR u.base='')";
		} else {
			q = "SELECT m.imap_unit_id FROM mail_user_imap_unit m, imap_unit u WHERE m.imap_unit_id=u.id AND m.mail_user_id=? AND u.base=?";
		}
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(userId));
		if (base.length() > 0)
			ps.setString(2, base);
		return ps.executeQuery();
	}

	/**
	 * @param mail_user_id
	 * @return
	 * @throws SQLException
	 */
	public String selectFromResponse(String mail_user_id) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM response WHERE mail_user_id=?";
		ResultSet rs = null;
		try {
			PreparedStatement ps = getPreparedStatement(q);
			ps.setInt(1, Integer.parseInt(mail_user_id));
			rs = ps.executeQuery();
			if (rs.next())
				return unescape(rs.getString(3));
		} finally {
			if (rs != null)
				rs.close();
		}
		return null;
	}

	/**
	 * @param files
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromSpool(HashMap<String, ?> files) throws SQLException {
		checkConnection();
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT * FROM spool WHERE");
		if (files.size() == 0) {
			sb.append(" 1=2");
		} else {
			boolean or = false;
			for (Iterator<String> i = files.keySet().iterator(); i.hasNext();) {
				String fileName = i.next();
				if (or)
					sb.append(" OR");
				sb.append(" filename='" + fileName + "'");
				or = true;
			}
		}
		String q = sb.toString();
		return stmt.executeQuery(q);
	}

	/**
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromSpool(String filename) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM spool WHERE filename=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, filename);
		return ps.executeQuery();
	}

	/**
	 * @param next
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectFromSpool(Timestamp next) throws SQLException {
		checkConnection();
		String q = "SELECT * FROM spool WHERE next_send=? AND retry >=0";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setTimestamp(1, next);
		return ps.executeQuery();
	}

	/**
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectNextFromSpool() throws SQLException {
		checkConnection();
		String q = "SELECT MIN(next_send)," + dateFx + " FROM spool WHERE retry>=0";
		PreparedStatement ps = getPreparedStatement(q);
		return ps.executeQuery();
	}

	/**
	 * Reset all hanging entries. Caused by a restart.
	 * 
	 * @throws SQLException
	 */
	public void resetSpoolEntries() throws SQLException {
		checkConnection();
		String q = "UPDATE spool set retry=0, next_send=" + dateFx + " WHERE retry<0";
		PreparedStatement ps = getPreparedStatement(q);
		ps.executeUpdate();
	}

	/**
	 * @param mbId
	 * @return
	 * @throws SQLException
	 */
	public String selectRecentCountFromImapData(String mbId) throws SQLException {
		checkConnection();
		String q = "SELECT DISTINCT COUNT(d.id) FROM imap_data d, imap_folder f WHERE f.imap_unit_id=? AND d.imap_folder_id=f.id AND d.last>f.last";
		PreparedStatement ps = getPreparedStatement(q);
		ResultSet rs = null;
		try {
			ps.setLong(1, Long.parseLong(mbId));
			rs = ps.executeQuery();
			if (!rs.next())
				return null;

			String ret = rs.getString(1);
			q = "UPDATE imap_folder SET last=" + dateFx + " WHERE id=?";
			ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(mbId));
			ps.executeUpdate();
			return ret;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * @param unitId
	 * @return
	 * @throws SQLException
	 */
	public long selectSumFromImapFolder(String unitId) throws SQLException {
		checkConnection();
		String q = "SELECT SUM(d.filesize) FROM imap_folder f, imap_data d WHERE f.imap_unit_id=?"
				+ " AND d.imap_folder_id=f.id";

		ResultSet rs = null;
		try {
			PreparedStatement ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(unitId));
			rs = ps.executeQuery();
			if (!rs.next())
				return 0;
			return rs.getLong(1);
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * @param unitId
	 * @param mask
	 * @return
	 * @throws SQLException
	 */
	public ResultSet selectThisLevelFromImapFolder(String unitId, String mask) throws SQLException {
		checkConnection();
		String q = "SELECT " + concat1 + "base" + concat2 + "path" + concat3
				+ " AS x FROM imap_folder f, imap_unit u WHERE imap_unit_id= u.id AND u.id =?"
				+ " AND path LIKE ? AND path NOT LIKE ? ORDER BY x";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(unitId));
		ps.setString(2, mask);
		ps.setString(3, mask + "/%");
		return ps.executeQuery();
	}

	/**
	 * Select the count of unseen mails in the given mailbox
	 * 
	 * @param mbId
	 *            the mailbox id
	 * @return a String containing the count.
	 * @throws SQLException
	 */
	public String selectUnseenCountFromImapData(String mbId) throws SQLException {
		checkConnection();
		String q = "SELECT DISTINCT COUNT(id) FROM imap_data WHERE f_seen=0 AND imap_folder_id=?";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		ResultSet rs = ps.executeQuery();
		try {
			if (!rs.next())
				return null;
			return rs.getString(1);
		} finally {
			rs.close();
		}
	}

	/**
	 * Select the id of the first unseen mail in the specified mailbox
	 * 
	 * @param mbId
	 *            the mailbox id
	 * @return a String containing the id or null.
	 * @throws SQLException
	 */
	public String selectFirstUnseenIDFromImapData(String mbId) throws SQLException {
		checkConnection();
		String q = "SELECT id FROM imap_data WHERE f_seen=0 AND imap_folder_id=? ORDER BY id";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(mbId));
		ResultSet rs = ps.executeQuery();
		try {
			if (!rs.next())
				return null;
			return rs.getString(1);
		} finally {
			rs.close();
		}
	}

	void sendAMail(MailEntry me, String fUser, String fDomain, String toUser, String toDomain) throws Exception {
		LOG.debug("mail from: " + fUser + "@" + fDomain + " to: " + toUser + "@" + toDomain);

		ResultSet rs = null;
		try {
			rs = selectFromMailUser(toUser, toDomain);

			// is it a local user?
			if (rs.next()) {
				LOG.debug(toUser + '@' + toDomain + " is a local user: storing mail " + me.mailId);
				// local mail -> insert into mail table
				String userId = rs.getString(1);

				LOG.debug("insert into mail: " + me.mailId);
				// insertIntoMail(me.mailId, me.size, userId);
				// insert into imap_folder
				boolean r = append(getOrCreateUnitId(userId), "INBOX", "", System.currentTimeMillis(), me);
				LOG.debug((r ? "SUCCESS" : "FAILURE") + " delivering mail for " + toUser + "@" + toDomain + " from "
						+ fUser + "@" + fDomain);
			} else {
				LOG.debug(toUser + '@' + toDomain + " is not local: spooling mail " + me.mailId);

				// custom mail -> insert into spool table

				LOG.debug("insert into spool: " + toUser + '@' + toDomain + " - " + me.mailId);
				boolean r = insertIntoSpool(me.mailId, toUser, toDomain, fUser, fDomain);
				LOG.debug((r ? "SUCCESS" : "FAILURE") + " storing mail for " + toUser + "@" + toDomain + " from "
						+ fUser + "@" + fDomain);
			}
		} catch (SQLException e) {
			throw e;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * send a mail element from a to b.
	 * 
	 * @param me
	 *            the mailelement
	 * @param fUser
	 * @param fDomain
	 * @param tUser
	 * @param tDomain
	 * @throws Exception
	 */
	public void sendMail(MailEntry me, String fUser, String fDomain, String tUser, String tDomain) throws Exception {

		LOG.debug("sendMail");

		LOG.debug("send a mail from " + fUser + '@' + fDomain + " to " + tUser + '@' + tDomain);
		ArrayList<String> v = new ArrayList<String>();
		v.add(tUser + '@' + tDomain);
		sendMultiMail(me, fUser, fDomain, v);
	}

	/**
	 * Send a mail (MailEntry me) from the specified user to a list of
	 * recipients.
	 * 
	 * @param me
	 *            the mail
	 * @param _sendingUser
	 *            from user
	 * @param _sendingDomain
	 *            from mail_domain
	 * @param rcp
	 *            list of recipients
	 * @throws Exception
	 *             on error
	 */
	public void sendMultiMail(final MailEntry me, String sendingUser, String sendingDomain,
			ArrayList<String> recipients) throws Exception {

		LOG.debug("sendMultiMail");

		appendToSentFolder(me, sendingUser, sendingDomain);

		// create an ArrayList for senders and fill it with the original sender
		ArrayList<Pair<String, String>> senders = new ArrayList<Pair<String, String>>();
		for (int i = 0; i < recipients.size(); ++i) {
			senders.add(Pair.makePair(sendingUser, sendingDomain));
		}

		// only the original recipients will receive automatic responses!
		int autoReplyCount = recipients.size();

		LOG.debug("resolv " + autoReplyCount + " addresses");

		HashMap<String, String> resolved = new HashMap<String, String>();

		// prevent double forwarding / notify
		HashSet<String> forwardedTo = new HashSet<String>(recipients);
		HashSet<String> notifiedTo = new HashSet<String>();

		// resolve all recipients
		while (recipients.size() > 0) {
			// get next recipient
			String recipient = recipients.remove(recipients.size() - 1);
			Pair<String, String> p = senders.remove(senders.size() - 1);

			// get user and mail_domain
			int idx = recipient.indexOf('@');
			if (idx <= 0) {
				LOG.error("INVALID RECIPIENT: " + recipient);
				continue;
			}

			String fromUser = p.getFirst();
			String fromDomain = p.getSecond();

			String toDomain = recipient.substring(idx + 1);
			String toUser = recipient.substring(0, idx);

			if (!isLocalDomain(fromDomain) && !isLocalDomain(toDomain)) {
				LOG.error("ERROR: won't send for domain " + fromDomain + " to domain " + toDomain);
				continue;
			}

			ResultSet rs = selectFromMailUser(toUser, toDomain);

			// == 0 : dont keep, != 0 : keep
			// a local address? check forwarder!
			if (rs.next()) {
				String userId = rs.getString(1);
				boolean keep = rs.getBoolean(7);
				boolean reply = false;
				try {
					reply = rs.getBoolean(8);
				} catch (Exception exxx) {
				}

				rs.close();
				// not yet resolved
				if (null == resolved.get(userId)) {
					resolved.put(userId, recipient);

					// send the mail if it is a real account
					if (keep) {
						LOG.debug("send mail from " + fromUser + "@" + fromDomain + " to local " + recipient);
						// now send the stuff
						sendAMail(me, fromUser, fromDomain, toUser, toDomain);

						// send auto responses but not to postmasters
						if (--autoReplyCount >= 0 && reply && !fromUser.equals("postmaster")) {
							final String key = toUser + "@" + toDomain + "->" + fromUser + "@" + fromDomain;

							// do not send auto responses to often
							if (AUTORESPONSESENT.put(key, key) == null) {
								String res = selectFromResponse(userId);
								if (res != null) {
									MailEntry replyMe = createNewMail();
									OutputStream fos = getOutputStream(replyMe.mailId);
									try {
										String msg = "From: " + recipient + "\r\nTo: " + fromUser + '@' + fromDomain
												+ "\r\nContent-Type: text/plain; charset=iso-8859-1"
												+ "\r\nContent-Transfer-Encoding: base64" + "\r\nSubject: auto response"
												+ "\r\nDate: "
												+ DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(System.currentTimeMillis())
												+ "\r\n\r\n";
										fos.write(msg.getBytes());
										fos.write(Mime.encode(res.getBytes("ISO-8859-1")));
										fos.write(0xd);
										fos.write(0xa);
										fos.flush();
										storeMail(replyMe); // update internal
															// values
										sendAMail(replyMe, toUser, toDomain, fromUser, fromDomain);
									} finally {
										fos.close();
									}
								}
							}
						}
					}

					rs = selectFromForward(userId);
					if (rs.next()) {
						do {
							String to = rs.getString(3); // forward
							boolean notify = rs.getBoolean(4); // notify

							idx = to.indexOf('@');
							if (idx > 0) {
								String fwdUser = to.substring(0, idx);
								String fwdDomain = to.substring(idx + 1);

								// forward only
								// - to local recipients
								// - to remote recipients if the sender is not a
								// postmaster
								if (isLocalDomain(fwdDomain) || !fromUser.equals("postmaster")) {
									// send a notification mail
									if (notify) {
										if (!forwardedTo.contains(to) && !notifiedTo.contains(to)) {
											notifiedTo.add(to);

											LOG.debug("notification for " + recipient + " to " + to
													+ ", using mail_domain: " + toDomain);
											OutputStream fos = null;
											try {
												MailEntry notifyMe = createNewMail();
												fos = getOutputStream(notifyMe.mailId);
												String msg = "From: postmaster@" + toDomain + "\r\nTo: " + to
														+ "\r\nSubject: mail notification: new mail from " + fromUser
														+ '@' + fromDomain + "\r\nDate: "
														+ DateFormat.EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(
																System.currentTimeMillis())
														+ "\r\n\r\nnew mail from " + fromUser + '@' + fromDomain
														+ "\r\ncheck your account: " + recipient + "\r\n";
												fos.write(msg.getBytes());
												fos.flush();
												storeMail(notifyMe); // update
																		// internal
																		// values
												sendAMail(notifyMe, "postmaster", toDomain, fwdUser, fwdDomain);
											} catch (Exception ex) {
												LOG.debug("notification for " + recipient + " to " + to + " failed: "
														+ ex);
											} finally {
												try {
													if (fos != null)
														fos.close();
												} catch (Throwable tw) {
												}
											}
										}
									} else {
										LOG.debug("forwarding for " + recipient + " to " + to);
										if (!forwardedTo.contains(to)) {
											forwardedTo.add(to);
											recipients.add(to);
											senders.add(Pair.makePair(toUser, toDomain));
										}
									}
								}
							}
						} while (rs.next());
					}
					rs.close();
				}
			} else { // not a local user -> deliver mail!
				rs.close();
				LOG.debug("sending from " + fromUser + "@" + fromDomain + " to " + recipient);
				// now send the stuff
				sendAMail(me, fromUser, fromDomain, toUser, toDomain);
			}
		}
	}

	/**
	 * Put the sent email into the senders SENT folder, if the folder exists.
	 * 
	 * @param me
	 *            the MailEntry
	 * @param sendingUser
	 *            the sending user
	 * @param sendingDomain
	 *            the sending domain
	 * @throws SQLException
	 * @throws Exception
	 */
	private void appendToSentFolder(final MailEntry me, String sendingUser, String sendingDomain)
			throws SQLException, Exception {
		// check if it is a local user and if the local user has a SENT folder.
		ResultSet rs = selectFromMailUser(sendingUser, sendingDomain);
		String userId = rs.next() ? rs.getString(1) : null;
		rs.close();

		if (userId != null) {
			// SENT folder found. Add mail to SENT folder.
			String unitId = getOrCreateUnitId(userId);
			if (unitId == null) {
				return;
			}
			String mbId = mailboxId(unitId, "SENT");
			if (mbId != null) {
				// mark mails as read in SENT folder
				append(unitId, "SENT", "\\Seen", System.currentTimeMillis(), me);
			}
		}
	}

	void setAutoCommit(boolean b) throws SQLException {
		jdbcCon.setAutoCommit(b);
	}

	/**
	 * Set the JDBC Url.
	 * 
	 * @param jdbcUrl
	 *            the JDBC Url.
	 */
	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
		extractUserPass();
	}

	/**
	 * @param path
	 */
	public void setMailPath(String path) {
		mailFile = new MailFile(path);
	}

	/**
	 * Set the keep state for the mail box.
	 * 
	 * @param owner
	 *            owner of the mail_domain
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @param passwd
	 * @param keep
	 *            the keep state
	 * @return
	 * @throws Exception
	 */
	public boolean setPoBoxKeep(String owner, String user, String mail_domain, String passwd, boolean keep)
			throws Exception {

		LOG.debug("setPoBoxKeep as " + owner + " for " + user + '@' + mail_domain);

		ResultSet rs = selectFromDomain(mail_domain, owner);
		try {
			if (!rs.next()) {
				if (null == verifyUser(user, mail_domain, passwd, "", null)) {
					return false;
				}
			}

			return updateMailUser(user, mail_domain, keep);
		} finally {
			rs.close();
		}
	}

	/**
	 * Check the keep state for the mail box.
	 * 
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @return the keep state
	 * @throws SQLException 
	 * @throws Exception
	 */
	public boolean keepMail(String user, String mail_domain) throws SQLException {
		ResultSet rs = selectFromMailUser(user, mail_domain);
		try {
			if (rs.next()) {
				boolean keep = rs.getBoolean(7);
				return keep;
			}
		} finally {
			rs.close();
		}
		return false;
	}

	
	/**
	 * Set the quota for the mail box.
	 * 
	 * @param owner
	 *            owner of the mail_domain
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @param passwd
	 * @param quota
	 *            the quota
	 * @return
	 * @throws Exception
	 */
	public boolean setPoBoxQuota(String owner, String user, String mail_domain, String passwd, String squota)
			throws Exception {
		ResultSet rs = selectFromDomain(mail_domain, owner);
		try {
			if (!rs.next()) {
				if (null == verifyUser(user, mail_domain, passwd, "", null)) {
					return false;
				}
			}

			long quota = 0;
			long scale = 1;
			if (squota != null) {
				squota = squota.toLowerCase();
				if (squota.endsWith("k")) {
					scale = 1024;
					squota = squota.substring(0, squota.length() - 1);
				} else if (squota.endsWith("m")) {
					scale = 1024 * 1024;
					squota = squota.substring(0, squota.length() - 1);
				}
				try {
					quota = Long.parseLong(squota);
				} catch (Throwable t) {
				}
			}
			quota *= scale;

			String q = "UPDATE mail_user SET quota=? WHERE name=? AND mail_domain=?";

			PreparedStatement ps = getPreparedStatement(q);
			ps.setLong(1, quota);
			ps.setString(2, user);
			ps.setString(3, mail_domain);

			return 0 != ps.executeUpdate();
		} finally {
			rs.close();
		}
	}

	/**
	 * Set a PO box's password.
	 * 
	 * @param owner
	 *            owner of the mail_domain
	 * @param user
	 *            user part of mail address
	 * @param mail_domain
	 *            mail_domain part of mail address
	 * @param passwd
	 *            the new password
	 * @return
	 * @throws Exception
	 */
	public boolean setPoBoxPass(String owner, String user, String mail_domain, String passwd) throws Exception {

		LOG.debug("setPoBoxPass as " + owner + " for " + user + '@' + mail_domain);

		ResultSet rs = selectFromDomain(mail_domain, owner);
		try {
			if (!rs.next())
				return false;

			return updateMailUser(user, mail_domain, passwd);
		} finally {
			rs.close();
		}
	}

	/**
	 * @param owner
	 * @param user
	 * @param mail_domain
	 * @param passwd
	 * @param reply
	 * @param replytext
	 * @return
	 * @throws Exception
	 */
	public boolean setPoBoxReply(String owner, String user, String mail_domain, String passwd, boolean reply,
			String replytext) throws Exception {

		LOG.debug("setPoBoxReply as " + owner + " for " + user + '@' + mail_domain);

		String id = null;
		ResultSet rs = selectFromDomain(mail_domain, owner);
		if (!rs.next()) {
			rs.close();
			id = verifyUser(user, mail_domain, passwd, "", null);
			if (null == id) {
				return false;
			}
		} else {
			rs.close();
			rs = selectFromMailUser(user, mail_domain);
			if (!rs.next()) {
				rs.close();
				return false;
			}
			id = rs.getString(1);
			rs.close();
		}

		return updateMailUserReply(id, reply, replytext);
	}

	/**
	 * Store a mail element into the mail storage.
	 * 
	 * @param me
	 *            the mailelement
	 * @return
	 * @throws Exception
	 */
	public File storeMail(MailEntry me) throws Exception {

		LOG.debug("storeMail");
		return mailFile.storeMail(me);
	}

	/**
	 * @param id
	 * @param f1
	 * @param f2
	 * @param f3
	 * @param f4
	 * @param f5
	 * @return
	 * @throws SQLException
	 */
	public boolean updateImapData(String id, boolean f1, boolean f2, boolean f3, boolean f4, boolean f5)
			throws SQLException {
		checkConnection();
		String q = "update imap_data set f_answered=?, f_flagged=?, f_deleted=?, f_seen=?, f_draft=? where id=?";

		PreparedStatement ps = getPreparedStatement(q);
		ps.setInt(1, f1 ? 1 : 0);
		ps.setInt(2, f2 ? 1 : 0);
		ps.setInt(3, f3 ? 1 : 0);
		ps.setInt(4, f4 ? 1 : 0);
		ps.setInt(5, f5 ? 1 : 0);
		ps.setLong(6, Long.parseLong(id));

		return 0 != ps.executeUpdate();
	}

	/**
	 * @param unitId
	 * @param oldName
	 * @param newName
	 * @return
	 * @throws SQLException
	 */
	public boolean updateImapFolder(String unitId, String oldName, String newName) throws SQLException {
		checkConnection();
		String q = "update imap_folder set path=? where imap_unit_id=? and path=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, newName);
		ps.setLong(2, Long.parseLong(unitId));
		ps.setString(3, oldName);

		return 0 != ps.executeUpdate();
	}

	/**
	 * @param userId
	 * @param ipAddress
	 * @return
	 * @throws SQLException
	 */
	public boolean updateMailUser(String userId, String ipAddress) throws SQLException {
		checkConnection();
		String q = "UPDATE mail_user SET last=" + dateFx + ", address=? WHERE id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, ipAddress);
		ps.setLong(2, Long.parseLong(userId));

		return 0 != ps.executeUpdate();
	}

	/**
	 * @param name
	 * @param mail_domain
	 * @param keep
	 * @return
	 * @throws SQLException
	 */
	public boolean updateMailUser(String name, String mail_domain, boolean keep) throws SQLException {
		checkConnection();
		String q = "UPDATE mail_user SET keep=? WHERE name=? AND mail_domain=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setInt(1, keep ? 1 : 0);
		ps.setString(2, name);
		ps.setString(3, mail_domain);

		return 0 != ps.executeUpdate();
	}

	/**
	 * @param name
	 * @param mail_domain
	 * @param passwd
	 * @return
	 * @throws SQLException
	 */
	public boolean updateMailUser(String name, String mail_domain, String passwd) throws SQLException {
		checkConnection();
		String q = "UPDATE mail_user SET passwd=? WHERE name=? AND mail_domain=?";
		String encodedPassword;
		if (passwd.length() > 0) {
			encodedPassword = Pkcs5.encodePbkdf2("SHA256", passwd, 11);
		} else {
			encodedPassword = "*";
		}

		PreparedStatement ps = getPreparedStatement(q);
		ps.setString(1, encodedPassword);
		ps.setString(2, name);
		ps.setString(3, mail_domain);
		return 0 != ps.executeUpdate();
	}

	/**
	 * @param id
	 * @param reply
	 * @param replytext
	 * @return
	 * @throws SQLException
	 */
	public boolean updateMailUserReply(String id, boolean reply, String replytext) throws SQLException {
		checkConnection();
		String q = "UPDATE mail_user SET reply=? WHERE id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setInt(1, (reply ? 1 : 0));
		ps.setLong(2, Long.parseLong(id));
		if (0 == ps.executeUpdate())
			return false;

		deleteFromMailUserReply(id);

		q = "INSERT INTO response (mail_user_id, response) values (?, ?)";
		ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(id));
		ps.setString(2, escape(replytext));
		if (0 == ps.executeUpdate())
			return false;

		AUTORESPONSESENT.clear();
		return true;
	}

	private void deleteFromMailUserReply(String id) throws SQLException {
		checkConnection();
		String q = "DELETE FROM response WHERE mail_user_id=?";
		PreparedStatement ps = getPreparedStatement(q);
		ps.setLong(1, Long.parseLong(id));
		ps.executeUpdate();
	}

	/**
	 * @param next
	 * @param retry
	 * @param msgId
	 * @return
	 * @throws SQLException
	 */
	public boolean updateSpool(long next, int retry, String msgId) throws SQLException {
		checkConnection();
		ResultSet rs = null;
		try {
			PreparedStatement ps = getPreparedStatement("SELECT " + dateFx + " FROM mail_user");
			rs = ps.executeQuery();
			if (!rs.next())
				return false;
			Timestamp ts = rs.getTimestamp(1);
			String q = "UPDATE spool SET next_send=?, retry=? WHERE id=?";
			ps = getPreparedStatement(q);
			ps.setTimestamp(1, new Timestamp(ts.getTime() + next));
			ps.setInt(2, retry);
			ps.setLong(3, Long.parseLong(msgId));
			return 0 != ps.executeUpdate();
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * Verify an user.
	 * 
	 * @param user
	 *            the username without mail_domain, e.g. massa
	 * @param mail_domain
	 *            the users mail_domain name, e.g. here.com
	 * @param passwd
	 *            a password as cleartext
	 * @param ipAddress
	 * @param last
	 * @return the userId as String when user and password matches, otherwise
	 *         null
	 * @throws Exception
	 */
	public String verifyUser(String user, String mail_domain, String passwd, String ipAddress, long[] last)
			throws Exception {

		LOG.debug("verifyUser");

		// this yields an unique object - now serialize logins from the same
		// ipAddress
		ResultSet rs = null;
		try {
			rs = selectFromMailUser(user, mail_domain, passwd);
			if (rs.next()) {
				final String userId = rs.getString(1);

				LOG.debug("verifyUser: " + userId);

				if (last != null) {
					Timestamp ts = rs.getTimestamp(5);
					last[0] = ts != null ? ts.getTime() : 0;
				}

				if (ipAddress != null) {
					updateMailUser(userId, ipAddress);
					final String key = (user + "@" + mail_domain + ":" + ipAddress);
					LOGINDELAY.remove(key);
				}
				return userId;
			}
		} finally {
			try {
				if (rs != null)
					rs.close();
			} catch (Exception ex) {
			}
		}
		return null;
	}

	/**
	 * If a failed login is pending, wait until the delay is done. If a delay is
	 * already pending, return false. Return true if verfiyUser should be called
	 * 
	 * @param user
	 * @param mail_domain
	 * @param ipAddress
	 * @return
	 */
	public static boolean delayLogin(String user, String mail_domain, String ipAddress) {
		final String key = (user + "@" + mail_domain + ":" + ipAddress).intern();
		synchronized (key) {
			Long delay = LOGINDELAY.get(key);
			if (delay != null) {
				// fail immediate if while someone waits
				if (LOGINDELAY.get(key + "#") != null)
					return false;
				try {
					LOGINDELAY.put(key + "#", delay);
					Thread.sleep(delay);
				} catch (InterruptedException e) {
				} finally {
					LOGINDELAY.remove(key + "#");
				}
			}
		}
		return true;
	}

	public static void handleLoginFailure(String user, String mail_domain, String ipAddress) {
		final String key = (user + "@" + mail_domain + ":" + ipAddress);
		Long delay = LOGINDELAY.get(key);
		if (delay == null) {
			delay = 1000L;
		} else if (delay < 1000L * 3600) {
			delay += delay;
		}
		LOGINDELAY.put(key, delay);
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
	}

	public boolean useGuessPermission(String domain) throws SQLException {
		checkConnection();
		ResultSet rs = null;
		try {
			String q = "SELECT * FROM mail_domain WHERE mail_domain=?";
			PreparedStatement ps = getPreparedStatement(q);
			ps.setString(1, domain);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getBoolean(3);
			}
		} finally {
			if (rs != null)
				rs.close();
		}
		return false;
	}

	public long getPoBoxQuota(String user, String domain) throws SQLException {
		checkConnection();
		ResultSet rs = null;
		try {
			rs = selectFromMailUser(user, domain);
			if (rs.next()) {
				return rs.getLong(9);
			}
		} finally {
			if (rs != null)
				rs.close();
		}
		return -1;
	}

	public boolean isPOBoxFull(String user, String domain) throws SQLException {
		checkConnection();
		ResultSet rs = null;
		try {
			rs = selectFromMailUser(user, domain);
			if (rs.next()) {
				long quota = rs.getLong(9);
				if (quota <= 0)
					return false;

				rs.close();
				rs = null;

				long sz = getPoBoxSize(user, domain);
				return sz > quota;
			}
		} finally {
			if (rs != null)
				rs.close();
		}
		return false;
	}

	public String getPatchlevel() throws SQLException {
		checkConnection();
		ResultSet rs = null;
		try {
			PreparedStatement ps = getPreparedStatement("select * from dbproperty where propname='version'");
			rs = ps.executeQuery();
			if (!rs.next())
				return null;
			return rs.getString(3);
		} catch (Exception ex) {
			return null;
		} finally {
			if (rs != null)
				rs.close();
		}
	}

	/**
	 * Global cleanup.
	 * 
	 * @throws Exception
	 */
	public void globalCleanup() throws Exception {

		LOG.debug("globalCleanup");

		// pass 1 - remove orphaned imap_units
		String q = "delete from mail_user_imap_unit where mail_user_id not in (select id from mail_user)";
		PreparedStatement ps = getPreparedStatement(q);
		ps.execute();

		ResultSet rs2 = selectOrphanedImapUnits();
		while (rs2.next()) {
			String unitId = rs2.getString(1);
			q = makeDelete("imap_data") + " imap_folder, imap_unit WHERE "
					+ "imap_data.imap_folder_id=imap_folder.id AND " + "imap_folder.imap_unit_id=? " + deleteEnd;
			ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(unitId));
			ps.execute();

			q = makeDelete("imap_folder") + " imap_unit " + "WHERE imap_folder.imap_unit_id=? " + deleteEnd;
			ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(unitId));
			ps.execute();

			q = "DELETE FROM imap_unit WHERE id=?";
			ps = getPreparedStatement(q);
			ps.setLong(1, Long.parseLong(unitId));
			ps.execute();

			LOG.debug("cleanup: delete imap_unit " + unitId);
		}
		rs2.close();

		/**/
		// new implementation with 2 parallel Iterators
		Iterator<File> f = mailFile.files();
		rs2 = selectFromImapDataOrdered();

		File file = null;
		String dbFileName = null;
		for (;;) {

			// get next file
			if (file == null && f.hasNext()) {
				file = f.next();
			}

			// no files left --> done
			if (file == null) {
				// dump errors for non existent mail files
				if (dbFileName != null) {
					LOG.debug("FATAL: missing mail file <" + dbFileName + ">");
					while (rs2.next()) {
						LOG.debug("FATAL: missing mail file <" + rs2.getString(1).trim() + ">");
					}
				}
				break;
			}

			// get next filename from DB
			if (dbFileName == null && rs2.next()) {
				dbFileName = rs2.getString(1).trim();
			}

			String fileName = file.toString();
			// is file not in DB?
			if (dbFileName == null || fileName.compareTo(dbFileName) < 0) {
				// LOG.debug("cleanup: WOULD delete mail file " + fileName);

				if (mailFile.removeMail(file)) {
					LOG.debug("cleanup: delete mail file " + fileName);
				}

				file = null;
				continue;
			}

			// database contains not existent file !?
			if (fileName.compareTo(dbFileName) > 0) {
				LOG.debug("FATAL: missing mail file " + dbFileName);
				dbFileName = null;
				continue;
			}

			// identical -> skip
			file = null;
			dbFileName = null;
		}
		rs2.close();

		// pass3 clean imap_mime
		q = "delete from imap_mime where filename not in (select imap_mime_filename from imap_data)";
		ps = getPreparedStatement(q);
		ps.execute();
	}

	/**
	 * Convert illegal characters into % quoted representation. "foo bar" ->
	 * "foo%20bar" No support for UTF yet.
	 * 
	 * @param url
	 *            the url
	 * @return the escaped url
	 */
	public static String escape(String url) {
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

	// could be faster...
	private static String unescape(String s) {
		ByteRef b = new ByteRef(s);
		// replace + by space
		for (int i = b.indexOf('+'); i >= 0; i = b.indexOf('+', i + 1)) {
			b = b.substring(0, i).append(" ").append(b.substring(i + 1));
		}

		// decode %xx
		for (int i = b.indexOf('%'); i >= 0; i = b.indexOf('%', i + 1)) {
			if (i + 3 > b.length()) {
				break;
			}
			byte[] c = new byte[] { (byte) Integer.parseInt(b.substring(i + 1, i + 3).toString(), 16) };
			b = b.substring(0, i).append(new ByteRef(c)).append(b.substring(i + 3));
		}

		// LOG.debug(o.toString() + " => " + b.toString());
		return b.toString();
	}

	private final static String ILLEGAL = "<>'\"+&";
}
