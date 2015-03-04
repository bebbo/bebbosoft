/******************************************************************************
 *
 * Helper class for pairs
 *
 * Copyright (c) by Stefan Bebbo Franke 1999-2008.
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

package de.bb.util;

import java.io.Serializable;

/**
 * STL like helper class for pairs.
 * 
 * Immutable.
 * 
 * @author stefan franke
 */
public class Pair<A, B> implements Serializable {

	private static final long serialVersionUID = 6076372730727364826L;
	
	private A first;
	private B second;

	public Pair(A first, B second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * CT. creates a Pair for the given values.
	 * 
	 * @param first
	 *            the first parameter
	 * @param second
	 *            the second parameter
	 */
	public static <T, V> Pair<T, V> makePair(T first, V second) {
		return new Pair<T, V>(first, second);
	}

	/**
	 * Get the first parameter.
	 * 
	 * @return the first parameter.
	 */
	public A getFirst() {
		return first;
	}

	/**
	 * Get the second parameter.
	 * 
	 * @return the second parameter.
	 */
	public B getSecond() {
		return second;
	}

	/**
	 * Compares this pair to another pair
	 * 
	 * @param obj
	 *            the other pair.
	 * @return true if both pairs match.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pair<?, ?>))
			return false;
		Pair<?, ?> p = (Pair<?, ?>) obj;
		if (first == null && p.first != null)
			return false;
		if (second == null && p.second != null)
			return false;
		return (first == null || first.equals(p.first))
				&& (second == null || second.equals(p.second));
	}

	/**
	 * Calculate a hashcode.
	 * 
	 * @return the hashcode.
	 */
	@Override
	public int hashCode() {
		int hashCode = 314159265;
		if (first != null)
			hashCode += first.hashCode();
		hashCode = hashCode * 17;
		if (second != null)
			hashCode += second.hashCode();
		return hashCode;
	}

	/**
	 * Provide a text representation.
	 * 
	 * @return a String to display some info about this pair.
	 */
	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}
}
