package com.vitanuova.lib;

public class Base64 extends Encoding {

	@Override
	public final String enc(byte[] a) {
		int n = a.length;
		if (n == 0)
			return "";
		char[] out = new char[4 * n / 3 + 4];
		int j = 0;
		for (int i = 0; i < n;) {
			int x = (a[i++] & 255) << 16;
			if (i < n)
				x |= (a[i++] & 255) << 8;
			if (i < n)
				x |= (a[i++] & 255);
			out[j++] = c64(x >> 18);
			out[j++] = c64(x >> 12);
			out[j++] = c64(x >> 6);
			out[j++] = c64(x);
		}
		int nmod3 = n % 3;
		if (nmod3 != 0) {
			out[j - 1] = '=';
			if (nmod3 == 1)
				out[j - 2] = '=';
		}
		return new String(out, 0, j);
	}

	static final char[] cmap = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
			'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
			'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
			'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', '+', '/' };

	private static char c64(int c) {
		return cmap[c & 63];
	}

	private static final short INVAL = 255;

	private static final short[] t64d = { INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, 62, INVAL, INVAL, INVAL, 63, 52, 53, 54, 55, 56, 57,
			58, 59, 60, 61, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, 0,
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
			20, 21, 22, 23, 24, 25, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
			43, 44, 45, 46, 47, 48, 49, 50, 51, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL, INVAL,
			INVAL, INVAL, INVAL };

	@Override
	public final byte[] dec(String s) {
		int b24 = 0;
		int i = 0;
		int l = s.length();
		byte[] out = new byte[(3 * l + 3) / 4]; // upper bound, especially if s
												// contains white space
		int o = 0;
		for (int n = 0; n < l; n++) {
			int c;
			if ((c = s.charAt(n)) > 0xFF || (c = t64d[c]) == INVAL)
				continue;
			switch (i++) {
			case 0:
				b24 = c << 18;
				break;
			case 1:
				b24 |= c << 12;
				break;
			case 2:
				b24 |= c << 6;
				break;
			case 3:
				b24 |= c;
				out[o++] = (byte) (b24 >> 16);
				out[o++] = (byte) (b24 >> 8);
				out[o++] = (byte) b24;
				i = 0;
				break;
			}
		}
		switch (i) {
		case 2:
			out[o++] = (byte) (b24 >> 16);
			break;
		case 3:
			out[o++] = (byte) (b24 >> 16);
			out[o++] = (byte) (b24 >> 8);
			break;
		}
		if (o != out.length) {
			byte[] trimmed = new byte[o];
			System.arraycopy(out, 0, trimmed, 0, o);
			out = trimmed;
		}
		return out;
	}
}
