// CREATED BY de.bb.ws.SD. EDIT AT DISCRETION.
package com.sap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebParam.Mode;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService
public interface UMWebService {

    public final static int SEARCH_EQUAL = 0;
    public final static int SEARCH_LIKE = 1;
    public final static int SEARCH_GREATERTHAN = 2;
    public final static int SEARCH_LESSTHAN = 3;

    @WebMethod(operationName = "getUser")
    @WebResult(name = "response")
    public IUserInfo getUser(@WebParam(name = "getUser_1_param2", mode = Mode.IN) String userName);

    @WebMethod(operationName = "getUsersOfRole")
    @WebResult(name = "response")
    public String[] getUsersOfRole(
            @WebParam(name = "getUsersOfRole_2_param37", mode = Mode.IN) Boolean getUsersOfRole_2_param37,
            @WebParam(name = "getUsersOfRole_2_param36", mode = Mode.IN) String getUsersOfRole_2_param36);

    @WebMethod(operationName = "getRoleByUniqueName")
    @WebResult(name = "response")
    public IRoleInfo getRoleByUniqueName(
            @WebParam(name = "getRoleByUniqueName_1_param40", mode = Mode.IN) String getRoleByUniqueName_1_param40);

    /**
     * Search user names.
     * 
     * @param nameMask
     *            e.g. * to get all with LIKE ...
     * @param how
     *            0 = EQ, 1 = LIKE, 2 = GT, 3 = LT
     * @param caseSensitive
     *            true if search is case sensitive
     * @param maxCount
     *            max count of results
     * @return
     */
    @WebMethod(operationName = "searchUsers")
    @WebResult(name = "response")
    public String[] searchUsers(@WebParam(name = "searchUsers_4_param14", mode = Mode.IN) String nameMask,
            @WebParam(name = "searchUsers_4_param15", mode = Mode.IN) Integer how,
            @WebParam(name = "searchUsers_4_param16", mode = Mode.IN) Boolean caseSensitive,
            @WebParam(name = "searchUsers_4_param17", mode = Mode.IN) Integer maxCount);

    /**
     * Get the roles of a group.
     * 
     * @param groupName
     *            the group name.
     * @param unknown
     *            ?
     * @return an array containing the role names.
     */
    @WebMethod(operationName = "getRolesOfGroup")
    @WebResult(name = "response")
    public String[] getRolesOfGroup(@WebParam(name = "getRolesOfGroup_2_param22", mode = Mode.IN) String groupName,
            @WebParam(name = "getRolesOfGroup_2_param23", mode = Mode.IN) Boolean unknown);

    @WebMethod(operationName = "getGroupByUniqueName")
    @WebResult(name = "response")
    public IGroupInfo getGroupByUniqueName(
            @WebParam(name = "getGroupByUniqueName_1_param31", mode = Mode.IN) String getGroupByUniqueName_1_param31);

    @WebMethod(operationName = "getUserEmail")
    @WebResult(name = "response")
    public String[] getUserEmail(
            @WebParam(name = "getUserEmail_1_param24", mode = Mode.IN) String getUserEmail_1_param24);

    /**
     * Get the groups for the specified user.
     * 
     * @param userName
     *            the case insensitive user name.
     * @param unknown
     *            ?
     * @return
     */
    @WebMethod(operationName = "getGroupsOfUser")
    @WebResult(name = "response")
    public String[] getGroupsOfUser(@WebParam(name = "getGroupsOfUser_2_param18", mode = Mode.IN) String userName,
            @WebParam(name = "getGroupsOfUser_2_param19", mode = Mode.IN) Boolean unknown);

    @WebMethod(operationName = "getUsersOfGroup")
    @WebResult(name = "response")
    public String[] getUsersOfGroup(
            @WebParam(name = "getUsersOfGroup_2_param30", mode = Mode.IN) Boolean getUsersOfGroup_2_param30,
            @WebParam(name = "getUsersOfGroup_2_param29", mode = Mode.IN) String getUsersOfGroup_2_param29);

    @WebMethod(operationName = "searchRoles")
    @WebResult(name = "response")
    public String[] searchRoles(@WebParam(name = "searchRoles_4_param32", mode = Mode.IN) String searchRoles_4_param32,
            @WebParam(name = "searchRoles_4_param33", mode = Mode.IN) Integer searchRoles_4_param33,
            @WebParam(name = "searchRoles_4_param34", mode = Mode.IN) Boolean searchRoles_4_param34,
            @WebParam(name = "searchRoles_4_param35", mode = Mode.IN) Integer searchRoles_4_param35);

    @WebMethod(operationName = "getGroupsOfRole")
    @WebResult(name = "response")
    public String[] getGroupsOfRole(
            @WebParam(name = "getGroupsOfRole_2_param38", mode = Mode.IN) String getGroupsOfRole_2_param38,
            @WebParam(name = "getGroupsOfRole_2_param39", mode = Mode.IN) Boolean getGroupsOfRole_2_param39);

    @WebMethod(operationName = "modifyUser")
    @WebResult(name = "response")
    public Boolean modifyUser(@WebParam(name = "modifyUser_9_param5", mode = Mode.IN) String modifyUser_9_param5,
            @WebParam(name = "modifyUser_9_param6", mode = Mode.IN) String modifyUser_9_param6,
            @WebParam(name = "modifyUser_9_param7", mode = Mode.IN) String modifyUser_9_param7,
            @WebParam(name = "modifyUser_9_param8", mode = Mode.IN) String modifyUser_9_param8,
            @WebParam(name = "modifyUser_9_param9", mode = Mode.IN) String modifyUser_9_param9,
            @WebParam(name = "modifyUser_9_param10", mode = Mode.IN) String modifyUser_9_param10,
            @WebParam(name = "modifyUser_9_param11", mode = Mode.IN) String modifyUser_9_param11,
            @WebParam(name = "modifyUser_9_param12", mode = Mode.IN) String modifyUser_9_param12,
            @WebParam(name = "modifyUser_9_param13", mode = Mode.IN) String modifyUser_9_param13);

    @WebMethod(operationName = "searchGroups")
    @WebResult(name = "response")
    public String[] searchGroups(
            @WebParam(name = "searchGroups_4_param26", mode = Mode.IN) Integer searchGroups_4_param26,
            @WebParam(name = "searchGroups_4_param25", mode = Mode.IN) String searchGroups_4_param25,
            @WebParam(name = "searchGroups_4_param28", mode = Mode.IN) Integer searchGroups_4_param28,
            @WebParam(name = "searchGroups_4_param27", mode = Mode.IN) Boolean searchGroups_4_param27);

    @WebMethod(operationName = "getMapping")
    @WebResult(name = "response")
    public Map getMapping(@WebParam(name = "getMapping_2_param3", mode = Mode.IN) String getMapping_2_param3,
            @WebParam(name = "getMapping_2_param4", mode = Mode.IN) String getMapping_2_param4);

    /**
     * Get the role names for the specified user.
     * 
     * @param userName
     *            the case insensitive user name.
     * @param includeGroups
     *            if true the group names are returned too
     * @return an array of Strings containing the role names.
     */
    @WebMethod(operationName = "getRolesOfUser")
    @WebResult(name = "response")
    public String[] getRolesOfUser(@WebParam(name = "getRolesOfUser_2_param20", mode = Mode.IN) String userName,
            @WebParam(name = "getRolesOfUser_2_param21", mode = Mode.IN) Boolean includeGroups);

}
