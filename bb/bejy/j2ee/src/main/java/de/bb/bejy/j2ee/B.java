package de.bb.bejy.j2ee;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

public class B implements Bean<Object> {

	private Object object;

	public B(Object object) {
		this.object = object;
	}

	@Override
	public Object create(CreationalContext<Object> arg0) {
		return object;
	}

	@Override
	public void destroy(Object arg0, CreationalContext<Object> arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Type> getTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAlternative() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Class<?> getBeanClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isNullable() {
		// TODO Auto-generated method stub
		return false;
	}

	public Object getObject() {
		return object;
	}

}
