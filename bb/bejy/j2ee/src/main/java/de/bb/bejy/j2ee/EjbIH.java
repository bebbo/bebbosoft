package de.bb.bejy.j2ee;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJBException;

class EjbIH implements InvocationHandler {

    Object ejb;
    Object proxy;
    private Map<Method, HashSet<String>> method2roles = new ConcurrentHashMap<Method, HashSet<String>>();

    EjbIH(Object ejb) {
        this.ejb = ejb;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final Thread t = Thread.currentThread();
        final ClassLoader cl = t.getContextClassLoader();
        t.setContextClassLoader(ejb.getClass().getClassLoader());
        try {
            checkAccess(method);
            try {
                return method.invoke(ejb, args);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new EJBException(ex);
            }
        } finally {
            t.setContextClassLoader(cl);
        }
    }

    private void checkAccess(Method method) {
        HashSet<String> roles = method2roles.get(method);
        if (roles == null) {
            roles = new HashSet<String>();
            Class<?> clazz = ejb.getClass();
            RolesAllowed rolesAllowed = clazz.getAnnotation(RolesAllowed.class);

            Outer: for (; clazz != null; clazz = clazz.getSuperclass()) {
                Loop: for (final Method m : clazz.getDeclaredMethods()) {
                    if (!m.getName().equals(method.getName()))
                        continue;

                    if (!m.getReturnType().equals(method.getReturnType()))
                        continue;

                    /* Avoid unnecessary cloning */
                    Class<?>[] params1 = m.getParameterTypes();
                    Class<?>[] params2 = method.getParameterTypes();
                    if (params1.length != params2.length)
                        continue;

                    for (int i = 0; i < params1.length; i++) {
                        if (params1[i] != params2[i])
                            continue Loop;
                    }

                    // found
                    final RolesAllowed methodRolesAllowed = m.getAnnotation(RolesAllowed.class);
                    if (methodRolesAllowed != null)
                        rolesAllowed = methodRolesAllowed;
                    break Outer;
                }
            }

            if (rolesAllowed != null) {
                for (final String role : rolesAllowed.value()) {
                    roles.add(role);
                }
            }
            method2roles.put(method, roles);
        }
        if (roles.isEmpty())
            return;

        if (SCWrapper.getSessionContext().isCallerInOneRoleOf(roles))
            return;

        throw new AccessControlException("no access to " + method + " - "
                + ThreadContext.currentUserPrincipal().getName() + " not in " + roles);
    }
}
