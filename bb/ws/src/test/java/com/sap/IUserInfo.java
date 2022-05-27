// CREATED BY de.bb.ws.SD. EDIT AT DISCRETION.
package com.sap;

import java.util.Locale;

public class IUserInfo {
    private String UniqueName;
    private String[] Groups;
    private String[] Email;
    private String FirstName;
    private String UniqueId;
    private Locale Locale;
    private String Company;
    private String LastName;
    private String DisplayName;
    private String[] Roles;
    private String Title;

    public String getUniqueName() {
        return this.UniqueName;
    }

    public void setUniqueName(String UniqueName) {
        this.UniqueName = UniqueName;
    }

    public String[] getGroups() {
        return this.Groups;
    }

    public void setGroups(String[] Groups) {
        this.Groups = Groups;
    }

    public String[] getEmail() {
        return this.Email;
    }

    public void setEmail(String[] Email) {
        this.Email = Email;
    }

    public String getFirstName() {
        return this.FirstName;
    }

    public void setFirstName(String FirstName) {
        this.FirstName = FirstName;
    }

    public String getUniqueId() {
        return this.UniqueId;
    }

    public void setUniqueId(String UniqueId) {
        this.UniqueId = UniqueId;
    }

    public Locale getLocale() {
        return this.Locale;
    }

    public void setLocale(Locale Locale) {
        this.Locale = Locale;
    }

    public String getCompany() {
        return this.Company;
    }

    public void setCompany(String Company) {
        this.Company = Company;
    }

    public String getLastName() {
        return this.LastName;
    }

    public void setLastName(String LastName) {
        this.LastName = LastName;
    }

    public String getDisplayName() {
        return this.DisplayName;
    }

    public void setDisplayName(String DisplayName) {
        this.DisplayName = DisplayName;
    }

    public String[] getRoles() {
        return this.Roles;
    }

    public void setRoles(String[] Roles) {
        this.Roles = Roles;
    }

    public String getTitle() {
        return this.Title;
    }

    public void setTitle(String Title) {
        this.Title = Title;
    }
}
