package com.vitanuova.lib;

public class Strings {

	public static final String quote(String s) {
		return quote(s, null);
	}

	public static final String quote(String s, String ccl) {
		return quote(new String[] { s }, ccl);
	}

	public static final String quote(String[] args, String ccl) {
		StringBuffer s = new StringBuffer();
		for (int a = 0; a < args.length; a++) {
			String arg = args[a];
			int i;
			for (i = 0; i < arg.length(); i++) {
				char c = arg.charAt(i);
				if (c == ' ' || c == '\t' || c == '\n' || c == '\''
						|| ccl != null && ccl.indexOf(c) >= 0)
					break;
			}
			if (i < arg.length() || arg.length() == 0) {
				s.append('\'');
				s.append(arg.substring(0, i));
				for (; i < arg.length(); i++) {
					char c = arg.charAt(i);
					if (c == '\'')
						s.append('\'');
					s.append(c);
				}
				s.append('\'');
			} else
				s.append(arg);
			if (a < args.length - 1)
				s.append(' ');
		}
		return new String(s);
	}

	public static final byte[] bytes(String s) {
		if (s == null)
			return new byte[0];
		try {
			return s.getBytes("UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling".getBytes();
		}
	}
}
