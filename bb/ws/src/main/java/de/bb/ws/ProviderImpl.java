package de.bb.ws;

import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.Element;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.spi.Provider;
import jakarta.xml.ws.spi.ServiceDelegate;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;

public class ProviderImpl extends Provider {

    @Override
    public Endpoint createAndPublishEndpoint(String address, Object implementor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Endpoint createEndpoint(String bindingId, Object implementor) {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
	@Override
    public ServiceDelegate createServiceDelegate(URL wsdlDocumentLocation, QName serviceName, Class serviceClass) {
        return new SD(wsdlDocumentLocation, serviceClass);
    }

    @Override
    public W3CEndpointReference createW3CEndpointReference(String address, QName serviceName, QName portName,
            List<Element> metadata, String wsdlDocumentLocation, List<Element> referenceParameters) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface,
            WebServiceFeature... features) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EndpointReference readEndpointReference(Source eprInfoset) {
        // TODO Auto-generated method stub
        return null;
    }

}
