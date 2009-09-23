package com.vitanuova.styx;

public class Qid {
	public long path;
	public int vers;
	public int qtype;

	// Qid.qtype type bits
	public static final int QTDIR = 0x80; // directory
	public static final int QTAPPEND = 0x40; // append-only file
	public static final int QTEXCL = 0x20; // exclusive-use file
	public static final int QTAUTH = 0x08; // authentication file
	public static final int QTTMP = 0x04; // not-backed-up file (symbolic link
											// in 9P200.u)
	public static final int QTFILE = 0x00; // plain file

	public Qid(long path, int qtype) {
		this.path = path;
		vers = 0;
		this.qtype = qtype;
	}

	public Qid(long path, int vers, int qtype) {
		this.path = path;
		this.vers = vers;
		this.qtype = qtype;
	}

	public boolean equals(Qid q) {
		return q != null && path == q.path && vers == q.vers
				&& qtype == q.qtype;
	}

	@Override
	public int hashCode() {
		return (int) path ^ vers;
	}

	@Override
	public String toString() {
		return "(" + pad(Long.toHexString(path), 16) + " " + vers + " "
				+ pad(Integer.toHexString(qtype), 2) + ")";
	}

	public String qidtype() {
		char[] s = new char[5];
		int i = 0;
		if ((qtype & QTDIR) != 0)
			s[i++] = 'd';
		if ((qtype & QTAPPEND) != 0)
			s[i++] = 'a';
		if ((qtype & QTEXCL) != 0)
			s[i++] = 'l';
		if ((qtype & QTAUTH) != 0)
			s[i++] = 'A';
		if ((qtype & QTTMP) != 0)
			s[i++] = 'T';
		return new String(s, 0, i);
	}

	private static final String pad(String s, int n) {
		while (s.length() < n) {
			int j = n - s.length();
			if (j > 10)
				j = 10;
			s = "0000000000".substring(0, j) + s;
		}
		return s;
	}
}
