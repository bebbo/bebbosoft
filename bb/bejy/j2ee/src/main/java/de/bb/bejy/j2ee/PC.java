package de.bb.bejy.j2ee;

import java.lang.annotation.Annotation;
import java.util.ArrayList;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceProperty;
import javax.persistence.SynchronizationType;

public class PC implements PersistenceContext {
	private static final PersistenceProperty[] X = {};
	ArrayList<PersistenceProperty> props = new ArrayList<>();

	@Override
	public Class<? extends Annotation> annotationType() {
		return PersistenceContext.class;
	}

	@Override
	public String name() {
		return "default";
	}

	@Override
	public PersistenceProperty[] properties() {
		return props.toArray(X);
	}

	@Override
	public SynchronizationType synchronization() {
		return SynchronizationType.UNSYNCHRONIZED;
	}

	@Override
	public PersistenceContextType type() {
		return PersistenceContextType.TRANSACTION;
	}

	@Override
	public String unitName() {
		return "default";
	}

}
