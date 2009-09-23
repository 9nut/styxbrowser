package com.vitanuova.styx;

public class Dir {
	public String name; // last element of path name
	public String uid; // user name
	public String gid; // group name
	public String muid; // last modifier name
	public Qid qid; // unique ID from seerver
	public int mode; // permissions (see below)
	public int atime; // last access time in seconds from Epoch
	public int mtime; // last modification time in seconds from Epoch
	public long length; // length in bytes (0 for directory)

	// ``system-modified data'' (leave them 0 or ~0)
	public int dtype; // server type
	public int dev; // server subtype

	// mode bits in Dir.mode used by the protocol
	public static final int DMDIR = 1 << 31; // directory
	public static final int DMAPPEND = 1 << 30; // append-only file
	public static final int DMEXCL = 1 << 29; // exclusive use file
	// public static final int DMMOUNT = 1<<28; // mounted channel (internal
	// use)
	public static final int DMAUTH = 1 << 27; // authentication file
	public static final int DMTMP = 1 << 26; // not-backed-up

	// permission bits (for others; use <<3 for group, <<6 for owner)
	public static final int DMREAD = 1 << 2; // read
	public static final int DMWRITE = 1 << 1; // write
	public static final int DMEXEC = 1 << 0; // execute

	private static final String[] rwx = { "---", "--x", "-w-", "-wx", "r--",
			"r-x", "rw-", "rwx", };

	public Dir() {
	} // all zero is fine

	public Dir(String name, Qid qid, long length, int mode) {
		this.name = name;
		this.qid = qid;
		this.length = length;
		this.mode = mode;
	}

	@Override
	public String toString() {
		String ms = Integer.toOctalString(mode);
		if (mode != 0)
			ms = "0" + ms;
		return "Dir(\"" + name + "\",\"" + uid + "\",\"" + gid + "\",\"" + muid
				+ "\"," + qid + "," + ms + "," + atime + "," + mtime + ","
				+ length + ",0x" + Integer.toHexString(dev) + "," + dev + ")";
	}

	public String modefmt() {
		return modefmt(mode);
	}

	public static String modefmt(int m) {
		String p0, p1;
		if ((m & DMDIR) != 0)
			p0 = "d";
		else if ((m & DMAPPEND) != 0)
			p0 = "a";
		else if ((m & DMAUTH) != 0)
			p0 = "A";
		else
			p0 = "-";
		if ((m & DMEXCL) != 0)
			p1 = "l";
		else
			p1 = "-";
		return p0 + p1 + rwx[(m >> 6) & 7] + rwx[(m >> 3) & 7] + rwx[m & 7];
	}

	public static final Dir nulldir() { // used for `no change' prototype in
										// wstat calls
		Dir d = new Dir();
		d.qid = new Qid(~0L, ~0, ~0);
		d.mode = ~0;
		d.atime = d.mtime = ~0;
		d.length = ~0L;
		d.dtype = d.dev = ~0;
		// names remain null
		return d;
	}
}
