package de.bb.bejy.j2ee;

import java.security.Principal;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Element;

import de.bb.bejy.ServerThread;
import de.bb.bejy.http.HttpProtocol;
import de.bb.bejy.http.HttpRequest;
import de.bb.bejy.http.WebAppContext;

public class WSC implements WebServiceContext {

    WebAppContext wac;

    public WSC(WebAppContext wac) {
        this.wac = wac;
    }

    public MessageContext getMessageContext() {
        return new MC(this);
    }

    public Principal getUserPrincipal() {
        return ThreadContext.currentUserPrincipal();
    }

    public boolean isUserInRole(String role) {
        // TODO Auto-generated method stub
        return false;
    }

    public EndpointReference getEndpointReference(Element... referenceParameters) {
        // TODO Auto-generated method stub
        return null;
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz, Element... referenceParameters) {
        // TODO Auto-generated method stub
        return null;
    }

}
