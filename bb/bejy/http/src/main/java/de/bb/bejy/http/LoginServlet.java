package de.bb.bejy.http;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import de.bb.bejy.UserGroupDbi;

class LoginServlet extends ServletHandler {
	private UserGroupDbi vfy;

	public void setVerify(UserGroupDbi vfy) {
		this.vfy = vfy;
	}

	public LoginServlet() {
	}

	public void service(ServletRequest in, ServletResponse out) throws IOException, ServletException {
		if (vfy == null)
			return;
		final String userName = in.getParameter("j_username");
		final String password = in.getParameter("j_password");
		final Collection<String> permissions = vfy.verifyUserGroup(userName, password);
		final HttpServletRequest hsr = (HttpServletRequest) in;
		final javax.servlet.http.HttpSession session = hsr.getSession(true);
		if (permissions != null) {
			session.setAttribute("j_username", userName);
			session.setAttribute("j_user_roles", permissions);
		}
	}

}