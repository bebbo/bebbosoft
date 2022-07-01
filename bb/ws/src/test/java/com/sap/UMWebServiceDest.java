package com.sap;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;

public class UMWebServiceDest {

    private final static String NAMESPACE = "prt:service:com.sapportals.portal.prt.webservice.usermanagement.UMWebService";
    private final static QName UM_WEBSERVICE_NAME = new QName(NAMESPACE, "UMWebServiceService");
    private final static QName UM_PORT_NAME = new QName(NAMESPACE, "UMWebService");

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            URL url = new URL("https://sabatina.ikornet.de:57001/irj/servlet/prt/soap/UMWebService");
            Map<String, Object> auth = new HashMap<String, Object>();
            auth.put(BindingProvider.USERNAME_PROPERTY, "duke");
            auth.put(BindingProvider.PASSWORD_PROPERTY, "test1234");

            Service service = Service.create(url, UM_WEBSERVICE_NAME);
            UMWebService umWebService = service.getPort(UM_PORT_NAME, UMWebService.class);

            BindingProvider bindingProvider = (BindingProvider) umWebService;
            bindingProvider.getRequestContext().putAll(auth);

            System.out.println("==== USERS ====");
            String[] users = umWebService.searchUsers("s*", UMWebService.SEARCH_LIKE, false, 100);
            for (String user : users) {
                System.out.println(user);
            }
            System.out.println("==== USER GROUPS ====");
            String groups[] = umWebService.getGroupsOfUser("duke", true);
            for (String group : groups) {
                System.out.println(group);
                System.out.println("==== GROUP ROLES ====");
                String[] rolesOfGroup = umWebService.getRolesOfGroup(group, true);
                for (String roleOfGroup : rolesOfGroup) {
                    System.out.println(roleOfGroup);
                }
                System.out.println("==== ====");
            }

            System.out.println("==== USER ROLES ====");
            String roles[] = umWebService.getRolesOfUser("duke", false);
            for (String role : roles) {
                System.out.println(role);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
