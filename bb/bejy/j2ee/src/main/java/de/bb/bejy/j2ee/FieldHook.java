package de.bb.bejy.j2ee;

import java.lang.reflect.Field;

public interface FieldHook {
	void handle(Object bean, Field f);
}
