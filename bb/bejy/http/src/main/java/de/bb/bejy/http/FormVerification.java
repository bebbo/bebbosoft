package de.bb.bejy.http;

import java.util.Collection;

import de.bb.bejy.UserGroupDbi;

public class FormVerification implements UserGroupDbi {

    String loginPage;
    String loginErrorPage;
    ServletHandler handler;

    public FormVerification(final String loginPage, final String loginErrorPage, ServletHandler h) {
        this.loginPage = loginPage;
        this.loginErrorPage = loginErrorPage == null ? null : loginErrorPage.trim();
        this.handler = h;
    }

    public Collection<String> verifyUserGroup(String user, String pass) {
        return null;
    }

}
