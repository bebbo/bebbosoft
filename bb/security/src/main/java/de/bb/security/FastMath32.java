/*****************************************************************************
 COPYRIGHT

 (c) 1994-2004 by BebboSoft, Stefan "Bebbo" Franke, all rights reserved

 *****************************************************************************/

package de.bb.security;

/**
 * A small and fast implementation for BigInteger maths. Operates on int[]
 * arrays and all of the 32 bits are used.
 */
public final class FastMath32 {
	static int[] ONE = { 1 };

	// static long start;

	static int[] oddModPow(int[] z, byte[] exp, int[] mod) {
		// long start = System.currentTimeMillis();

		// search highest bit of exponent
		int expLen = exp.length;
		int expi = 0;
		while (expi < expLen) {
			if (exp[expi] != 0)
				break;
			++expi;
		}
		// no bit found --> ONE is the result
		if (expi == expLen)
			return ONE;

		// preload the bits
		int expBitsLeft;
		int expBits = exp[expi++] << 24;

		// preload more bits
		if (expi < expLen) {
			expBitsLeft = 16;
			expBits |= (exp[expi++] & 0xff) << 16;
		} else {
			// exponent == ONE --> return bz
			if (expBits == 1)
				return z;
			expBitsLeft = 8;
		}

		// shift highest bit into sign bit
		while (expBits > 0) {
			expBits <<= 1;
			--expBitsLeft;
		}

		// eat the highest bit since we start with z
		// and load the next bits on underflow
		expBits <<= 1;
		if (--expBitsLeft == 8 && expi < expLen) {
			expBitsLeft = 16;
			expBits |= (exp[expi++] & 0xff) << 16;
		}

		// expBits now contains 1 to 16 bits

		// ok - we have to calculate something
		int modLen = mod.length;
		int muLen = mod.length;
		if (mod[modLen - 1] == 0)
			--muLen;
		int maxLen = modLen + modLen + 1;

		int[][] data = new int[16][];
		int[] t1 = new int[maxLen];

		int n0 = modInverse32Odd(mod[0]);
		// since the highest bit is ONE, we start with Z and just transform it
		// t0 = z << (modLen * 32)
		int[] t0 = new int[z.length + modLen + 1];
		System.arraycopy(z, 0, t0, modLen, z.length);
		mod(t0, mod, t1, t0, t0.length - 1, modLen);

		int[] t = new int[muLen];
		System.arraycopy(t0, 0, t, 0, muLen);
		data[1] = t;
		/*
		 * Since the highest bit is 1 we start with n.
		 * 
		 * then we square while zeros occur
		 * 
		 * if we hit 1 we fill a nibble an determine when to multiply
		 * 
		 * 1000 : square mul*n - loop covers the squares -> n^2, n^3, n^6, n^12 1001 :
		 * square square square square mul*n^9 -> n^2, n^4, n^8 1010 : square square
		 * square mul*n^5 - lcts -> n^2, n^4, n^8 1011 : square square square square
		 * mul*n^11 -> n^2, n^4, n^8 1100 : square square mul*n^3 - lcts -> n^2, n^4,
		 * n^7 1101 ; square square square square mul*n^13 -> n^2, n^4, n^8 1110 ;
		 * square square square mul*n^7 - lcts -> n^2, n^4, n^8 1111 ; square square
		 * square square mul*n^15 -> n^2, n^4, n^8
		 * 
		 * thus the odd exponents are required since we might not need all of them, we
		 * do lazy evaluation using these rules
		 */

		t = new int[maxLen];
		int index = 1;
		while (expBitsLeft > 0) {
			// just square
			if (expBits > 0) {
				square(t1, t0, muLen);
				montgomery(t1, mod, modLen, n0);
				t = t0;
				t0 = t1;
				t1 = t;

				if (index < 16) {
					index += index;
					if (index < 16) {
						t = new int[muLen];
						System.arraycopy(t0, 0, t, 0, muLen);
						data[index] = t;
					}
				}

				// and load the next bits on underflow
				expBits <<= 1;
				if (--expBitsLeft == 8 && expi < expLen) {
					expBitsLeft = 16;
					expBits |= (exp[expi++] & 0xff) << 16;
				}
				continue;
			}

			// sign is set in expBits

			// how many bits to handle?
			int n = 4;
			if (expBitsLeft < n)
				n = expBitsLeft;

			// get the bits
			int nibble = expBits >>> (32 - n);
			while (nibble > 0 && (nibble & 1) == 0) {
				nibble >>>= 1;
				--n;
			}

			// and load new bits
			if (expi < expLen && expBitsLeft - n <= 8) {
				expBits = (expBits << (expBitsLeft - 8)) | ((exp[expi++] & 0xff) << 16);
				expBits <<= 8 + n - expBitsLeft;
				expBitsLeft += 8;
			} else {
				expBits <<= n;
			}
			expBitsLeft -= n;

			// now square n times
			while (n-- > 0) {
				square(t1, t0, muLen);
				montgomery(t1, mod, modLen, n0);
				t = t0;
				t0 = t1;
				t1 = t;
				if (index < 16) {
					index += index;
					if (index < 16) {
						t = new int[muLen];
						System.arraycopy(t0, 0, t, 0, muLen);
						data[index] = t;
					}
				}
			}

			if (nibble > 0) {
				int x[] = data[nibble];
				// load the z^x
				if (x == null) {
					x = fill(nibble, data, muLen, mod, modLen, n0);
				}
				// and multiply
				mul(t1, t0, x, muLen);
				montgomery(t1, mod, modLen, n0);
				t = t0;
				t0 = t1;
				t1 = t;
			}
			if (index < 16) {
				index += nibble;
				if (index < 16) {
					t = new int[muLen];
					System.arraycopy(t0, 0, t, 0, muLen);
					data[index] = t;
				}
			}
		}

		// transform back
		montgomery(t0, mod, modLen, n0);

		// System.out.println("rsa " + expLen*8 + ": " +
		// (System.currentTimeMillis() - start) + "ms");

		return t0;
	}

	private static int[] fill(int nibble, int[][] data, int muLen, int[] mod, int modLen, int n0) {
		int[] x = new int[modLen + modLen + 1];
		int a[], b[];

		// n^3: n * n^2
		// n^5: n^2 * n^3 or n * n^4
		// n^7: n * n^6 or n^4 * n^3 [requires n^3]
		// n^9: n * n^8 or n^2 * n^7 or n^3 * n^6
		// n^11: n^4 * n^7 or n^2 * n^9 [requires n^9]
		// n^13: n * n^12 or n^4 * n^9 [requires n^9]
		// n^15: n^3 * n^12 or n^2 * n^13 [requires n^13 --> n^9]
		switch (nibble) {
		case 3:
			a = data[1];
			b = data[2];
			break;
		case 4:
			a = b = data[2];
			break;
		case 5:
			if (data[4] != null) {
				a = data[1];
				b = data[4];
				break;
			}
			if (data[3] == null)
				fill(3, data, muLen, mod, modLen, n0);

			a = data[2];
			b = data[3];
			break;
		case 7:
			if (data[6] != null) {
				a = data[6];
				b = data[1];
				break;
			}
			if (data[3] == null)
				fill(3, data, muLen, mod, modLen, n0);

			a = data[4];
			b = data[3];
			break;
		// * n^9: n * n^8 or n^2 * n^7 or n^3 * n^6
		case 9:
			if (data[8] != null) {
				a = data[8];
				b = data[1];
				break;
			}
			if (data[6] != null && data[3] != null) {
				a = data[6];
				b = data[3];
			}
			if (data[7] == null)
				fill(7, data, muLen, mod, modLen, n0);
			a = data[7];
			b = data[2];
			break;
		// * n^11: n^4 * n^7 or n^2 * n^9 [requires n^9]
		case 11:
			if (data[7] != null && data[4] != null) {
				a = data[7];
				b = data[4];
				break;
			}
			if (data[9] == null)
				fill(9, data, muLen, mod, modLen, n0);
			a = data[9];
			b = data[2];
			break;
		// * n^13: n * n^12 or n^4 * n^9 [requires n^9]
		case 13:
			if (data[12] != null) {
				a = data[12];
				b = data[1];
				break;
			}
			if (data[4] == null)
				fill(4, data, muLen, mod, modLen, n0);
			if (data[9] == null)
				fill(9, data, muLen, mod, modLen, n0);
			a = data[4];
			b = data[9];
			break;
		// * n^15: n^3 * n^12 or n^2 * n^13 [requires n^13 --> n^9]
		case 15:
			if (data[3] != null && data[12] != null) {
				a = data[3];
				b = data[12];
				break;
			}
			if (data[13] == null)
				fill(13, data, muLen, mod, modLen, n0);
			a = data[2];
			b = data[13];
			break;
		default:
			a = b = null;
		}

		mul(x, a, b, muLen);
		montgomery(x, mod, modLen, n0);
		data[nibble] = x;
		return x;
	}

	/**
	 * 
	 * @param src
	 * @param dst
	 */
	static byte[] int2Byte(int[] src, byte[] dst) {
		if (dst == null) {
			int n = src.length;
			while (n > 0 && src[n - 1] == 0)
				--n;
			dst = new byte[n * 4];
		}
		for (int i = 0, j = dst.length - 1; i < src.length; ++i) {
			int v = src[i];
			if (j < 0)
				break;
			dst[j--] = (byte) v;
			v >>>= 8;
			if (j < 0)
				break;
			dst[j--] = (byte) v;
			v >>>= 8;
			if (j < 0)
				break;
			dst[j--] = (byte) v;
			v >>>= 8;
			if (j < 0)
				break;
			dst[j--] = (byte) v;
		}
		return dst;
	}

	/**
	 * Convert the byte array into an int array, also convert the order: byte[0] =
	 * high byte ... byte[n] = low byte int[0] = low int ... int[k] = high int The
	 * highest int has always the highest bit zero. To ensure this an additional int
	 * is appended.
	 * 
	 * @param src
	 */
	static int[] byte2Int(byte[] src, int minLen) {
		int i = 0;
		// skip zero bytes
		while (i < src.length && src[i] == 0) {
			++i;
		}
		// fix len to keep highest bit int highest int zero
		int len = src.length - i;
		if ((len & 3) == 0 && src[i] < 0)
			++len;
		len = (len + 3) / 4;
		// ensure minimal length
		if (len < minLen)
			len = minLen;
		int d[] = new int[len];

		// copy the data
		for (int k = 0, j = src.length - 1; k < len && j >= i; ++k) {
			int v = (src[j--] & 0xff);
			if (j >= 0)
				v |= ((src[j--] & 0xff) << 8);
			if (j >= 0)
				v |= ((src[j--] & 0xff) << 16);
			if (j >= 0)
				v |= ((src[j--] & 0xff) << 24);
			d[k] = v;
		}
		return d;
	}

	/**
	 * sub WITHOUT handling underflows!!!!
	 * 
	 * @return true on underflow
	 * @param res
	 * @param a
	 * @param b
	 * @param bl
	 */
	static boolean sub(int[] res, int[] a, int[] b, int bl) {
		long carry = 0;
		int i;
		for (i = 0; i < bl; ++i) {
			carry += (a[i] & 0xffffffffL) - (b[i] & 0xffffffffL);
			res[i] = (int) carry;
			carry >>= 32;
		}
		for (; carry != 0 && i < bl; ++i) {
			carry += a[i] & 0xffffffffL;
			res[i] = (int) carry;
			carry >>= 32;
		}
		return (int) carry != 0;
	}

	/**
	 * Return true on owerflow.
	 * 
	 * @param res
	 * @param a
	 * @param al
	 * @param b
	 * @param bl
	 * @return
	 */
	static boolean add(int[] res, int[] a, int al, int[] b, int bl) {
		long carry = 0;
		int i;
		for (i = 0; i < bl; ++i) {
			carry += (a[i] & 0xffffffffL) + (b[i] & 0xffffffffL);
			res[i] = (int) carry;
			carry >>= 32;
		}
		for (; carry != 0 && i < al; ++i) {
			carry += a[i] & 0xffffffffL;
			res[i] = (int) carry;
			carry >>= 32;
		}
		return (int) carry != 0;
	}

	/**
	 * aIn = aIn mod bIn
	 * 
	 * @param aInOut
	 * @param bIn
	 * @param temp1  a temp buffer to shift align aIn
	 * @param temp2  a temp buffer to shift align bIn
	 * @param al     length of a
	 * @param bl     length of b
	 */
	static void mod(int[] aInOut, int[] bIn, int temp1[], int temp2[], int al, int bl) {
		int a[] = aInOut;
		int mod[] = bIn;

		while (bl > 0) {
			if (mod[bl - 1] != 0)
				break;
			--bl;
		}

		int shift = 0;
		{
			int x = mod[bl - 1];
			if (x < 0)
				shift = 31;
			else {
				while (x + x > 0) {
					x += x;
					++shift;
				}
			}

			if (shift > 0) {
				shiftLeft(temp1, bIn, bl, shift);
				mod = temp1;
				shiftLeft(temp2, aInOut, al, shift);
				a = temp2;

				if (mod[bl] != 0)
					++bl;
				if (a[al] != 0)
					++al;
			}
		}
		int divlen = bl - 1;

		// modMSW aligned that highest bit is zero and 2nd highest bit is set
		long modMSW = mod[divlen] & 0xffffffffL;
		long modMSW32 = modMSW << 32;
		long modLSW = divlen > 0 ? mod[divlen - 1] & 0xffffffffL : 0;
		for (int i = al - bl; i >= 0;) {
			int ai;
			long carry;
			long a0 = al == a.length ? 0 : a[al] & 0xffffffffL;
			long a01 = (a0 << 32) | (a[--al] & 0xffffffffL);
			if (a01 != 0) {
				long c0;
				// calculate: c0 = a0|a1 / b0
				// long c0 = a0 == modMSW ? 0xffffffffL : (a01 / modMSW) &
				// 0xffffffffL;
				if (a0 == modMSW) {
					c0 = 0xffffffffL; // necessary
				} else // if (a01 >= 0) // always true since the highest int <= modMSW 
				{
					c0 = (a01 / modMSW) & 0xffffffffL;
					
					
					if (al > 0 && modLSW != 0) {
						// a0|a1 -= c0*b0 // a0 part always gets zero!
						a01 -= c0 * modMSW;

						long c0b1 = c0 * modLSW;
						// add a2 => a0|a1|a2 - c0*b0|0
						a01 = (a01 << 32) | (a[al - 1] & 0xffffffffL);
						while ((a01 >= 0 && (c0b1 < 0 || (c0b1 >= 0 && a01 < c0b1)))
								|| (a01 < 0 && c0b1 < 0 && a01 < c0b1)) {
							--c0;
							long t = a01 + modMSW32;
							c0b1 -= modLSW;
							if (c0b1 < 0 && a01 < 0 && t >= 0)
								break;
							a01 = t;
						}
					}
				}

				carry = 0;
				if (c0 > 0) {
					// start of mulSub
					ai = al - divlen;
					for (int bi = 0; bi <= divlen; ++bi) {
						carry += (mod[bi] & 0xffffffffL) * c0;
						int t = a[ai];
						a[ai++] = t - (int) carry;
						carry = (carry >>> 32) + (((int) (carry & 0xffffffffL) + 0x80000000) > (t + 0x80000000) ? 1 : 0);
					}
					if (carry != 0) {
						carry = a[ai] - carry;
						a[ai] = (int) carry;
					} 
					carry = a[ai];
				}

				// if sub created an underflow, add mod
				if (carry < 0 ) {
//						--c0;

					carry = 0;
					ai = al - divlen;
					for (int bi = 0; bi <= divlen; bi++) {
						carry += (mod[bi] & 0xffffffffL);
						carry += (a[ai] & 0xffffffffL);
						a[ai++] = (int) carry;
						carry >>>= 32;
					}
					if (carry > 0)
						a[ai] = a[ai] + (int) carry;
				}

				// do some subtractions until the result is smaller than mod
				while (overflow( a, mod, al, divlen)) {
//					 ++ c0;

					carry = 0;
					ai = al - divlen;
					for (int bi = 0; bi <= divlen; bi++) {
						carry += (mod[bi] & 0xffffffffL);
						int t = a[ai];
						a[ai++] = t - (int) carry;
						carry = (carry >>> 32) + (((int) (carry & 0xffffffffL) + 0x80000000) > (t + 0x80000000) ? 1 : 0);
					}
					if (carry != 0) {
						carry = a[ai] - carry;
						a[ai] = (int) carry;
					} 
				}
			}
			
			
			--i; // c[i--] += (int) c0;
		}

		if (shift > 0) {
			shiftRight(aInOut, a, aInOut.length, shift);
		}
	}

	/**
	 * Determine if a subtraction is needed.
	 * @param a the number.
	 * @param mod the modulo.
	 * @param al the highest int.
	 * @param divlen ints used in deivision. 
	 * @return true if a > mod at the given position.
	 */
	private static boolean overflow(int[] a, int[] mod, int al, int divlen) {
		if (al + 1 < a.length && a[al + 1] != 0)
			return true;
		for (; divlen >= 0; --divlen, --al) {
			if (a[al] != mod[divlen])
				break;
		}
		boolean r = divlen < 0 || a[al] + 0x80000000 > mod[divlen] + 0x80000000;
		return r;
	}

	/**
	 * @param dst
	 * @param src
	 * @param len
	 */
	static void square(int[] dst, int[] src, int len) {
		// delay(75);
		// calc the squares a1*a1, a2*a2, ... - also erases the buffer
		int i = len - 1;
		int j = i + len;
		dst[j] = 0;
		for (; i >= 0; --i) {
			long p = src[i] & 0xffffffffL;
			p *= p;
			dst[j--] = (int) (p >>> 32);
			dst[j--] = (int) p;
		}

		// add the mixed terms
		for (i = 0; i < len; ++i) {
			long l = src[i] & 0xffffffffL;
			long carry = 0;
			for (j = i + 1; j < len; ++j) {
				long m = l * (src[j] & 0xffffffffL);
				carry += m + m + (dst[i + j] & 0xffffffffL);
				dst[i + j] = (int) carry;
				// if (carry + 0x8000000000000000L < m + 0x8000000000000000L)
				if (m < 0) // overflow is only caused by m!
					carry = 0x100000000L | (carry >>> 32);
				else
					carry = (carry >>> 32);
			}
			for (; carry != 0; ++j) {
				carry += (dst[i + j] & 0xffffffffL);
				dst[i + j] = (int) carry;
				carry >>>= 32;
			}
		}
	}

	/**
	 * dst needs at least size of len+len
	 * 
	 * @param dst destination of result.
	 * @param a   multiplicant a.
	 * @param b   mulitplicant b.
	 * @param len length used if a and b.
	 */
	static void mul(int[] dst, int[] a, int[] b, int len) {
		long carry = 0;
		long temp = a[0] & 0xffffffffL;
		int i = 0;
		for (; i < len; ++i) {
			carry += (b[i] & 0xffffffffL) * temp;
			dst[i] = (int) carry;
			carry >>>= 32;
		}
		dst[i] = (int) carry;

		for (int q = 1; q < len; ++q) {
			carry = 0;
			temp = a[q] & 0xffffffffL;
			i = q;
			for (int bi = 0; bi < len; ++bi) {
				carry += (b[bi] & 0xffffffffL) * temp;
				carry += dst[i] & 0xffffffffL;

				dst[i++] = (int) carry;
				carry >>>= 32;
			}
			dst[i] = (int) carry;
		}
	}

	/**
	 * @param dst
	 * @param src
	 * @param len
	 * @param shift
	 */
	static void shiftLeft(int[] dst, int[] src, int len, int shift) {
		long carry = 0;
		int j = shift >>> 5;
		shift &= 31;
		for (int i = 0; i < len; ++i) {
			carry >>>= 32;
			carry |= (src[i] & 0xffffffffL) << shift;
			dst[j++] = (int) carry;
		}
		if ((carry >>= 32) != 0)
			dst[j] = (int) carry;
	}

	static void shiftRight(int[] dst, int[] src, int len, int shift) {
		long carry = 0;
		int j = len - (shift >>> 5) - 1;
		shift &= 31;
		for (int i = len - 1; j >= 0; --i) {
			carry <<= 32;
			carry |= (src[i] & 0xffffffffL);
			dst[j--] = (int) (carry >>> shift);
		}
	}

	/**
	 * Internal function to calculate a modInverse for the lowest int.
	 * 
	 * @param _x lowest int of n.
	 * @return A result where: x*result mod 2^16 = 1
	 */
	private static int modInverse32Odd(int val) {
		// Newton's iteration!
		int t = val;
		t *= 2 - val * t;
		t *= 2 - val * t;
		t *= 2 - val * t;
		t *= 2 - val * t;
		return -t;
	}

	/**
	 * @param t
	 * @param mod
	 * @param ml
	 * @param n0
	 */
	static void montgomery(int[] t, int[] mod, int ml, int n0) {
		/**
		 * / short[] mods = new short[mod.length * 2]; short[] ts = new short[t.length *
		 * 2]; copyInt2Short(mods, mod); copyInt2Short(ts, t); montgomery(ts, mods, ml *
		 * 2, (short) n0); copyShort2Int(t, ts);
		 * 
		 * /
		 **/
		// t = (t + (t*n' mod r) * n )
		int ml2 = ml + ml;
		long carry = 0;
		for (int i = 0; i < ml; ++i) {
			long m = (n0 * t[i]) & 0xffffffffL;
			int k = i;
			for (int j = 0; j < ml; ++j, ++k) {
				carry += (t[k] & 0xffffffffL) + m * (mod[j] & 0xffffffffL);
				t[k] = (int) carry;
				carry >>>= 32;
			}
			for (; carry != 0 && k < ml2; ++k) {
				carry += (t[k] & 0xffffffffL);
				t[k] = (int) carry;
				carry >>>= 32;
			}
		}

		// t = t / r = shift right
		{
			int i = 0;
			for (; i < ml; ++i)
				t[i] = t[i + ml];
			for (; i < ml2; ++i)
				t[i] = 0;
		}

		boolean sub = carry != 0;
		if (!sub) {
			sub = isGreater(mod, t, ml);
		}
		if (sub) {
			sub(t, t, mod, ml);
			sub = isGreater(mod, t, ml);
		}
		/**/
	}

	static boolean isGreater(int[] mod, int[] t, int ml) {
		int i = ml - 1;
		for (; i >= 0; --i) {
			if (t[i] != mod[i])
				break;
		}
		boolean r = i < 0 || t[i] + 0x80000000 > mod[i] + 0x80000000;
		return r;
	}

	public static byte[] reverse(byte[] bIn) {
		byte[] b = bIn.clone();
		for (int i = 0, j = b.length - 1; i < j; ++i, --j) {
			byte t = b[i];
			b[i] = b[j];
			b[j] = t;
		}
		return b;
	}

	static void mulmont(int[] a, int x[], int y[], int[] m, int ms, int len) {
		if (x == y)
			square(a, x, len);
		else
			mul(a, x, y, len);
		montgomery(a, m, len, ms);
	}

	/*
	 * static void xmulmont(int[] a, int x[], int y[], int[] m, int ms, int len) {
	 * 
	 * int y00 = y[0];
	 * long y0 = y00 & 0xffffffffL;
	 * int m00 = m[0];
	 * long m0 = m00 & 0xffffffffL;
	 * 
	 * long carry;
	 * // initial step initializes a[]
	 * {
	 * int x00 = x[0];
	 * long x0 = x00 & 0xffffffffL;
	 * long ui = (x00 * y00 * ms) & 0xffffffffL;
	 * 
	 * long t = x0 * y0;
	 * carry = t + ui * m0;
	 * if (t + 0x8000000000000000L > carry + 0x8000000000000000L)
	 * carry = 0x100000000L + (carry >>> 32);
	 * else
	 * carry >>>= 32;
	 * for (int j = 1; j <= len; ++j) {
	 * // carry += (a[j] & 0xffffffffL) + xi * (y[j] & 0xffffffffL) +
	 * // ui * (m[j] & 0xffffffffL);
	 * t = x0 * (y[j] & 0xffffffffL);
	 * carry += t + ui * (m[j] & 0xffffffffL);
	 * a[j - 1] = (int) carry;
	 * if (t + 0x8000000000000000L > carry + 0x8000000000000000L)
	 * carry = 0x100000000L + (carry >>> 32);
	 * else
	 * carry >>>= 32;
	 * }
	 * a[len] = (int) carry;
	 * }
	 * 
	 * // now work additive
	 * for (int i = 1; i < len; ++i) {
	 * int a00 = a[0];
	 * long a0 = a00 & 0xffffffffL;
	 * int xi0 = x[i];
	 * long xi = xi0 & 0xffffffffL;
	 * long ui = ((a00 + xi0 * y00) * ms) & 0xffffffffL;
	 * long t = a0 + xi * y0;
	 * carry = t + ui * m0;
	 * if (t + 0x8000000000000000L > carry + 0x8000000000000000L)
	 * carry = 0x100000000L + (carry >>> 32);
	 * else
	 * carry >>>= 32;
	 * for (int j = 1; j <= len; ++j) {
	 * // carry += (a[j] & 0xffffffffL) + xi * (y[j] & 0xffffffffL) +
	 * // ui * (m[j] & 0xffffffffL);
	 * t = xi * (y[j] & 0xffffffffL) + (a[j] & 0xffffffffL);
	 * carry += t + ui * (m[j] & 0xffffffffL);
	 * a[j - 1] = (int) carry;
	 * if (t + 0x8000000000000000L > carry + 0x8000000000000000L)
	 * carry = 0x100000000L + (carry >>> 32);
	 * else
	 * carry >>>= 32;
	 * }
	 * a[len] = (int) carry;
	 * if (carry >>> 32 != 0) {
	 * carry++;
	 * }
	 * }
	 * 
	 * int i = len;
	 * for (; i >= 0; --i) {
	 * if (a[i] != m[i])
	 * break;
	 * }
	 * if (i < 0 || a[i] + 0x80000000 > m[i] + 0x80000000) {
	 * sub(a, a, m, len);
	 * }
	 * }
	 */

	static int[] invertP2(int z0[], int[] z1, int z2[], int[] t3, int[] t4, byte[] exp, int[] P) {
		int len = z0.length & ~1;
		int modLen = len >> 1;
		System.arraycopy(z0, 0, z1, 0, modLen);
		int index = 1;
		int b0 = exp[0];
		while (b0 > 0) {
			++index;
			b0 = (byte)(b0 + b0);
		}
		for (; index < exp.length * 8; ++index) {
			int t[];
			square(z2, z1, modLen);
			mod(z2, P, t3, t4, len, modLen);
			if (1 == (1 & exp[index >> 3] >> ((7 - index) & 7))) {
				mul(z1, z2, z0, modLen);
				mod(z1, P, t3, t4, len, modLen);
			} else {
				t = z1;
				z1 = z2;
				z2 = t;
			}
		}
		return z1;
	}

}