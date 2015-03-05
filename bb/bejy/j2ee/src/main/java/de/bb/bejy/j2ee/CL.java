/*****************************************************************************
 * Copyright (c) by Stefan Bebbo Franke 1999-2015.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *****************************************************************************/

package de.bb.bejy.j2ee;

class CL extends ClassLoader {
    static CL cl = new CL();

    private CL() {
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
        }
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }

    Class loadClass(String name, byte[] bits) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if (c != null)
            return c;

        try {
            c = findSystemClass(name);
        } catch (ClassNotFoundException e) {
        }

        if (c == null) {
            //    System.out.println("define class: " + name);
            c = defineClass(name, bits, 0, bits.length);
        }

        if (c == null)
            throw new ClassNotFoundException(name);

        resolveClass(c);

        return c;
    }
}

