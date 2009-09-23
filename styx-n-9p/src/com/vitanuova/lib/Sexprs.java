package com.vitanuova.lib;

/*
 * SDSI/SPKI S-expression reader (eventually)
 *
 * Copyright © 2005 Vita Nuova Holdings Limited, C H Forsyth
 */

public class Sexprs {

	private static final int Maxtoken = 1024 * 1024; // should be more than
														// enough

	private class Syntax extends Exception {
		private static final long serialVersionUID = 1L;

		Syntax(String s, long l) {
			super(s);
		}
	}

	private static final long Here = -1;

	public abstract class Sexp {
		public Sexp parse(String s) {
			return null;
		} // needs a way of adjusting stream?

		public Sexp unpack(byte[] a) {
			return null;
		} // needs a way of adjusting stream?

		@Override
		public abstract String toString();

		public abstract int packedsize();

		public abstract byte[] pack();

		public abstract String b64text();

		public abstract boolean islist();

		public abstract Sexp[] els();

		public abstract String op();

		public abstract Sexp[] args();

		public abstract boolean eq(Sexp t); // or equals?

		public abstract Sexp copy();

		public abstract byte[] asdata();

		public abstract String astext();
	}

	// just quoting functions for the moment

	public static String quote(String s) {
		if (istoken(s))
			return s;
		for (int i = 0; i < s.length(); i++) {
			String v;
			if ((v = esc(s.charAt(i))) != null) {
				StringBuffer os = new StringBuffer();
				os.append('"');
				os.append(s.substring(0, i));
				os.append(v);
				while (++i < s.length()) {
					if ((v = esc(s.charAt(i))) != null)
						os.append(v);
					else
						os.append(s.charAt(i));
				}
				os.append('"');
				return new String(os);
			}
		}
		return "\"" + s + "\"";
	}

	private static final String esc(char c) {
		switch (c) {
		case '"':
			return "\\\"";
		case '\\':
			return "\\\\";
		case '\b':
			return "\\b";
		case '\f':
			return "\\f";
		case '\n':
			return "\\n";
		case '\t':
			return "\\t";
		case '\r':
			return "\\r";
		case 0x0b:
			return "\\v";
		default:
			if (c < ' ' || c >= 0x7F) {
				String s = Integer.toHexString(c & 0xFF);
				if (s.length() < 2)
					s = "0" + s;
				return "\\x" + s;
			}
		}
		return null;
	}

	// An octet string that meets the following conditions may be given
	// directly as a "token".
	//
	// -- it does not begin with a digit
	//
	// -- it contains only characters that are
	// -- alphabetic (upper or lower case),
	// -- numeric, or
	// -- one of the eight "pseudo-alphabetic" punctuation marks:
	// - . / _ : * + =
	// (Note: upper and lower case are not equivalent.)
	// (Note: A token may begin with punctuation, including ":").

	public static boolean istokenc(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A'
				&& c <= 'Z' || c == '-' || c == '.' || c == '/' || c == '_'
				|| c == ':' || c == '*' || c == '+' || c == '=';
	}

	public static boolean istoken(String s) {
		if (s == null || s.length() == 0)
			return false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= '0' && c <= '9' && i == 0 || !istokenc(c))
				return false;
		}
		return true;
	}

	private static boolean istextual(byte[] a) {
		for (int i = 0; i < a.length;) {
			int c = a[i++];
			if (c < ' ' && !isspace(c) || c >= 0x7F)
				return false;
		}
		return true;
	}

	private static boolean isspace(int c) {
		return c == ' ' || c == '\r' || c == '\t' || c == '\n';
	}

	private static int hex(int c) {
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'a' && c <= 'f')
			return 10 + (c - 'a');
		if (c >= 'A' && c <= 'F')
			return 10 + (c - 'A');
		return -1;
	}
}
