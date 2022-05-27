// CREATED BY de.bb.ws.SD. EDIT AT DISCRETION.
package com.sap;

public class IGroupInfo {
    private String[] GroupMembers;
    private String UniqueName;
    private String Description;
    private String[] UserMembers;
    private String UniqueId;
    private String DisplayName;
    private String[] Roles;

    public String[] getGroupMembers() {
        return this.GroupMembers;
    }

    public void setGroupMembers(String[] GroupMembers) {
        this.GroupMembers = GroupMembers;
    }

    public String getUniqueName() {
        return this.UniqueName;
    }

    public void setUniqueName(String UniqueName) {
        this.UniqueName = UniqueName;
    }

    public String getDescription() {
        return this.Description;
    }

    public void setDescription(String Description) {
        this.Description = Description;
    }

    public String[] getUserMembers() {
        return this.UserMembers;
    }

    public void setUserMembers(String[] UserMembers) {
        this.UserMembers = UserMembers;
    }

    public String getUniqueId() {
        return this.UniqueId;
    }

    public void setUniqueId(String UniqueId) {
        this.UniqueId = UniqueId;
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
}
