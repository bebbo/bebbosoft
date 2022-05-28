package de.bb.mejb;

import javax.ejb.*;
import javax.xml.rpc.handler.MessageContext;

class SC extends C implements SessionContext {

    SC(SimpleHomeBean c, EJBObject ejbobject) {
        super(c);
        Code = ejbobject;
    }

    public EJBLocalObject getEJBLocalObject() throws IllegalStateException {
        return null;
    }

    public EJBObject getEJBObject() throws IllegalStateException {
        return Code;
    }

    private EJBObject Code;

    @Override
    public MessageContext getMessageContext() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public <T> T getBusinessObject(Class<T> arg0) throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class getInvokedBusinessInterface() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean wasCancelCalled() throws IllegalStateException {
		// TODO Auto-generated method stub
		return false;
	}
}
