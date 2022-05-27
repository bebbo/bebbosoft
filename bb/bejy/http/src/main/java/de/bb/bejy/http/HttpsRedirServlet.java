package de.bb.bejy.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class HttpsRedirServlet extends HttpServlet {
	@Override
	protected void service(final HttpServletRequest request, final HttpServletResponse response)
			throws IOException, ServletException {
		final StringBuffer url = new StringBuffer();
		url.append("https://").append(request.getServerName());
		url.append(request.getRequestURI());
		final String query = request.getQueryString();
		if (query != null)
			url.append("?" + query);
		response.sendRedirect(url.toString());
	}
}
