package de.bb.bejy.j2ee;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

public class MyCDI extends CDI<Object> {

	private BeanManager bm;

	MyCDI(BeanManager bm) {
		this.bm = bm;
	}
	
	@Override
	public void destroy(Object arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isAmbiguous() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUnsatisfied() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Instance<Object> select(Annotation... arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <U> Instance<U> select(Class<U> arg0, Annotation... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <U> Instance<U> select(TypeLiteral<U> arg0, Annotation... arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<Object> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object get() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BeanManager getBeanManager() {
		return bm;
	}

}
