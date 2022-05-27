/* 
 * Created on 19.10.2004
 * written by Stefan Bebbo Franke
 * (c) by BebboSoft 2004
 */
package de.bb.web.user;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ResourceBundle;

import de.bb.util.DateFormat;
import de.bb.util.Mime;

/**
 * @author sfranke
 */
public class Mailer
{
  /**
   * @param name
   * @param email
   * @param password
   * @throws Exception
   */
  public static void sendPasswordMail(String name, String email, String password)
      throws Exception
  {
    ResourceBundle bundle = ResourceBundle.getBundle("usermsg");

    String mydomain = bundle.getString("de.bb.web.user.mydomain");
    String host = bundle.getString("de.bb.web.user.smtphost");
    String mailUser = bundle.getString("de.bb.web.user.mail.user");
    String mailPassword = bundle.getString("de.bb.web.user.mail.password");
    String mailMessage = bundle.getString("de.bb.web.user.mail.message");
    String message = "From: "
        + mailUser
        + "\r\nTo: "
        + email
        + "\r\nSubject: "
        + mailMessage
        + "\r\nDate: "
        + DateFormat
            .EEE__dd_MMM_yyyy_HH_mm_ss__zzzz(System.currentTimeMillis())
        + "\r\nContent-Type: text/plain;\r\n        charset=\"iso-8859-1\"\r\nContent-Transfer-Encoding: 7bit\r\n\r\n"
        + "Hello " + name + ",\r\n" + mailMessage + ": " + password + "\r\n";

    Socket socket = new Socket(host, 25);
    DataInputStream is = new DataInputStream(socket.getInputStream());
    DataOutputStream os = new DataOutputStream(socket.getOutputStream());

    String line = is.readLine();
    if (!line.startsWith("220"))
      throw new Exception("error.sendmail.helo");

    os.writeBytes("EHLO " + mydomain + "\r\n");
    os.flush();

    line = is.readLine();
    while (line.charAt(3) == '-')
      line = is.readLine();

    if (!line.startsWith("250"))
      throw new Exception("error.sendmail.helo");

    os.write(("AUTH LOGIN\r\n").getBytes());
    os.flush();

    line = is.readLine();
    if (!line.startsWith("334"))
      throw new Exception("error.sendmail.authlogin");
    if (line.indexOf("VXNlcm5hbWU6") < 0)
      throw new Exception("error.sendmail.authlogin");

    byte b[] = Mime.encode(mailUser.getBytes());
    os.write(b);
    os.write(13);
    os.write(10);
    os.flush();

    line = is.readLine();
    if (!line.startsWith("334"))
      throw new Exception("error.sendmail.authlogin");
    if (line.indexOf("UGFzc3dvcmQ6") < 0)
      throw new Exception("error.sendmail.authlogin");

    b = Mime.encode(mailPassword.getBytes());
    os.write(b);
    os.write(13);
    os.write(10);
    os.flush();

    line = is.readLine();
    if (!line.startsWith("235"))
      throw new Exception("error.sendmail.authlogin");

    os.writeBytes("MAIL FROM:<" + mailUser + ">\r\n");
    os.flush();

    line = is.readLine();
    if (!line.startsWith("250"))
      throw new Exception("error.sendmail.authlogin");

    os.writeBytes("RCPT TO:<" + email + ">\r\n");
    os.flush();

    line = is.readLine();
    if (!line.startsWith("250"))
      throw new Exception("error.sendmail.recipient");

    os.writeBytes("DATA\r\n");
    os.flush();

    line = is.readLine();
    if (!line.startsWith("354"))
      throw new Exception("error.sendmail.nodata");

    os.writeBytes(message);
    os.writeBytes(".\r\n");
    os.flush();

    line = is.readLine();
    if (!line.startsWith("250"))
      throw new Exception("error.sendmail.nocommmit");
  }
}