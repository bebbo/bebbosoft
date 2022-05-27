package de.bb.bejy.j2ee;

import java.lang.annotation.Annotation;

import javax.persistence.PersistenceProperty;

public class PP implements PersistenceProperty {

	private String name;
	private String value;

	public PP(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String value() {
		return value;
	}

}
