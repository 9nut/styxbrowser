package com.vitanuova.styx;

/*
 *
 * Styx definitions and message formatting
 *
 * TO DO
 *	simplify
 *	StyxReader
 *
 * Copyright © 2005 Vita Nuova Holdings Limited [C H Forsyth, forsyth@vitanuova.com]
 * Subject to the terms of the MIT-template (google for a copy)
 */

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class Styx {

	/* fundamental constants; don't change */
	public static final String VERSION = "9P2000";
	public static final int MAXWELEM = 16; // limit to elements walked in one
											// Twalk
	public static final int NOTAG = 0xFFFF; // invalid tag
	public static final int NOFID = ~0; // invalid fid

	public static final int IOHDRSZ = 24; // room for Twrite/Rread header
	public static final int MAXFDATA = 8192; // `reasonable' iounit
	public static final int MAXRPC = IOHDRSZ + MAXFDATA; // usable default for
															// fversion and
															// iounit

	public static final int ERRMAX = 128; // protocol's arbitrary limit to error
											// message strings

	/* open/create modes; don't change */
	public static final int OREAD = 0; // open for read
	public static final int OWRITE = 1; // write
	public static final int ORDWR = 2; // read and write
	public static final int OEXEC = 3; // execute, == read but check execute
										// permission
	public static final int OTRUNC = 16; // or'ed in (except for exec), truncate
											// file first
	public static final int ORCLOSE = 64; // or'ed in, remove on close
	public static final int OEXCL = 0x1000; // exclusive-create

	public static class BAD extends Exception {
		private static final long serialVersionUID = 1L;

		BAD(String s) {
			super("Styx: " + s);
		}
	}

	/* message types; don't change */
	static final int MTversion = 100;
	static final int MRversion = MTversion + 1;
	static final int MTauth = 102;
	static final int MRauth = MTauth + 1;
	static final int MTattach = 104;
	static final int MRattach = MTattach + 1;
	static final int MTerror = 106; // illegal
	static final int MRerror = MTerror + 1;
	static final int MTflush = 108;
	static final int MRflush = MTflush + 1;
	static final int MTwalk = 110;
	static final int MRwalk = MTwalk + 1;
	static final int MTopen = 112;
	static final int MRopen = MTopen + 1;
	static final int MTcreate = 114;
	static final int MRcreate = MTcreate + 1;
	static final int MTread = 116;
	static final int MRread = MTread + 1;
	static final int MTwrite = 118;
	static final int MRwrite = MTwrite + 1;
	static final int MTclunk = 120;
	static final int MRclunk = MTclunk + 1;
	static final int MTremove = 122;
	static final int MRremove = MTremove + 1;
	static final int MTstat = 124;
	static final int MRstat = MTstat + 1;
	static final int MTwstat = 126;
	static final int MRwstat = MTwstat + 1;
	static final int MTmax = MRwstat + 1;

	/* size of protocol elements in bytes; don't change */
	static final int BIT8SZ = 1;
	static final int BIT16SZ = 2;
	static final int BIT32SZ = 4;
	static final int BIT64SZ = 8;
	static final int QIDSZ = BIT8SZ + BIT32SZ + BIT64SZ;

	static final int STATFIXLEN = BIT16SZ + QIDSZ + 5 * BIT16SZ + 4 * BIT32SZ
			+ BIT64SZ; // amount of fixed length data in a stat buffer

	/* names for use in a message size formula */
	private static final int STR = BIT16SZ;
	private static final int TAG = BIT16SZ;
	private static final int FID = BIT32SZ;
	private static final int QID = BIT8SZ + BIT32SZ + BIT64SZ;
	private static final int LEN = BIT16SZ; // stat and qid array lengths
	private static final int COUNT = BIT32SZ;
	private static final int OFFSET = BIT64SZ;
	private static final int H = BIT32SZ + BIT8SZ + BIT16SZ; // minimum header
																// length:
																// size[4] type
																// tag[2]

	public abstract class Smsg {
		public abstract int mtype();

		public abstract String mname();

		abstract void pack(ByteBuffer b);

		public void packsize(ByteBuffer b, int n) {
			p32(b, n);
		}

		public abstract int packedsize();

		public abstract boolean isTmsg();
	}

	public abstract class Tmsg extends Smsg {
		public int tag = NOTAG;

		Tmsg() {
		}

		public final Tmsg read(ReadableByteChannel fd, int msize) {
			return null;
		}

		protected final void packtag(ByteBuffer b) {
			p16(b, tag);
		}

		protected Tmsg(ByteBuffer b) {
			tag = g16(b);
		}

		public final boolean isTmsg() {
			return true;
		};
	}

	public class Unpack {
		public final Tmsg unpackT(ByteBuffer b) throws BAD {
			/* length has already been consumed */
			switch (b.get()) {
			case MTversion:
				return new Tversion(b);
			case MTauth:
				return new Tauth(b);
			case MTattach:
				return new Tattach(b);
			case MTflush:
				return new Tflush(b);
			case MTwalk:
				return new Twalk(b);
			case MTopen:
				return new Topen(b);
			case MTcreate:
				return new Tcreate(b);
			case MTread:
				return new Tread(b);
			case MTwrite:
				return new Twrite(b);
			case MTclunk:
				return new Tclunk(b);
			case MTremove:
				return new Tremove(b);
			case MTstat:
				return new Tstat(b);
			case MTwstat:
				return new Twstat(b);
			default:
				throw new BAD("invalid Tmsg type");
			}
		}

		public final Rmsg unpackR(ByteBuffer b) throws BAD {
			/* length has already been consumed */
			switch (b.get()) {
			case MRversion:
				return new Rversion(b);
			case MRauth:
				return new Rauth(b);
			case MRattach:
				return new Rattach(b);
			case MRflush:
				return new Rflush(b);
			case MRwalk:
				return new Rwalk(b);
			case MRopen:
				return new Ropen(b);
			case MRcreate:
				return new Rcreate(b);
			case MRread:
				return new Rread(b);
			case MRwrite:
				return new Rwrite(b);
			case MRclunk:
				return new Rclunk(b);
			case MRremove:
				return new Rremove(b);
			case MRstat:
				return new Rstat(b);
			case MRwstat:
				return new Rwstat(b);
			case MRerror:
				return new Rerror(b);
			default:
				throw new BAD("invalid Rmsg type");
			}
		}
	}

	public class Tversion extends Tmsg {
		public int msize;
		public String version;

		public Tversion(int msize, String version) {
			this.msize = msize;
			this.version = version;
		}

		Tversion(ByteBuffer b) {
			super(b);
			msize = g32(b);
			version = gets(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTversion);
			packtag(b);
			p32(b, msize);
			puts(b, version);
		}

		public final int mtype() {
			return MTversion;
		}

		public final String mname() {
			return "Tversion";
		}

		public final int packedsize() {
			return H + COUNT + STR + utflen(version);
		}

		public final String toString() {
			return "Tversion(" + tag + "," + msize + ",\"" + version + "\")";
		}

		public final Rversion compatible(int msize, String version) {
			if (version == null)
				version = VERSION;
			if (this.msize < msize)
				msize = this.msize;
			String v = this.version;
			if (v.length() < 2 || !v.substring(0, 2).equals("9P"))
				return new Rversion(tag, msize, "unknown");
			for (int i = 2; i < v.length(); i++) {
				char c = v.charAt(i);
				if (c == '.') {
					v = v.substring(0, i);
					break;
				} else if (!(c >= '0' && c <= '9'))
					return new Rversion(tag, msize, "unknown"); // fussier than
																// Plan 9
			}
			if (v.compareTo(VERSION) < 0)
				return new Rversion(tag, msize, "unknown");
			if (v.compareTo(version) < 0)
				version = v;
			return new Rversion(tag, msize, version);
		}
	}

	public class Tauth extends Tmsg {
		public int afid;
		public String uname;
		public String aname;

		public Tauth(int afid, String uname, String aname) {
			this.afid = afid;
			this.uname = uname;
			this.aname = aname;
		}

		Tauth(ByteBuffer b) {
			super(b);
			afid = g32(b);
			uname = gets(b);
			aname = gets(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTauth);
			packtag(b);
			p32(b, afid);
			puts(b, uname);
			puts(b, uname);
		}

		public int mtype() {
			return MTauth;
		}

		public String mname() {
			return "Tauth";
		}

		public final int packedsize() {
			return H + FID + STR + utflen(uname) + STR + utflen(aname);
		}

		public final String toString() {
			return "Tauth(" + tag + "," + afid + ",\"" + uname + "\",\""
					+ aname + "\")";
		}
	}

	public class Tattach extends Tmsg {
		public int fid;
		public int afid;
		public String uname;
		public String aname;

		public Tattach(int fid, int afid, String uname, String aname) {
			if (uname == null)
				uname = "";
			if (aname == null)
				aname = "";
			this.fid = fid;
			this.afid = afid;
			this.uname = uname;
			this.aname = aname;
		}

		Tattach(ByteBuffer b) {
			super(b);
			fid = g32(b);
			afid = g32(b);
			uname = gets(b);
			aname = gets(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTattach);
			packtag(b);
			p32(b, fid);
			p32(b, afid);
			puts(b, uname);
			puts(b, aname);
		}

		public int mtype() {
			return MTattach;
		}

		public String mname() {
			return "Tattach";
		}

		public final int packedsize() {
			return H + 2 * FID + STR + utflen(uname) + STR + utflen(aname);
		}

		public String toString() {
			return "Tattach(" + tag + "," + fid + "," + afid + ",\"" + uname
					+ "\",\"" + aname + "\")";
		}
	}

	public class Tflush extends Tmsg {
		public int oldtag;

		public Tflush(int oldtag) {
			this.oldtag = oldtag;
		}

		Tflush(ByteBuffer b) {
			super(b);
			oldtag = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTflush);
			packtag(b);
			p32(b, oldtag);
		}

		public int mtype() {
			return MTflush;
		}

		public String mname() {
			return "Tflush";
		}

		public final int packedsize() {
			return H + TAG;
		}

		public String toString() {
			return "Tflush(" + tag + "," + oldtag + ")";
		}
	}

	public class Twalk extends Tmsg {
		public int fid;
		public int newfid;
		public String[] names;

		public Twalk(int fid, int newfid, String[] names) {
			this.fid = fid;
			this.newfid = newfid;
			this.names = names;
		}

		Twalk(ByteBuffer b) {
			super(b);
			fid = g32(b);
			newfid = g32(b);
			int n = g16(b);
			names = new String[n];
			for (int i = 0; i < n; i++)
				names[i] = gets(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTwalk);
			packtag(b);
			p32(b, fid);
			p32(b, newfid);
			int n = names.length;
			p16(b, n);
			for (int i = 0; i < n; i++)
				puts(b, names[i]);
		}

		public int mtype() {
			return MTwalk;
		}

		public String mname() {
			return "Twalk";
		}

		public final int packedsize() {
			int n = H + 2 * FID + LEN;
			for (int i = 0; i < names.length; i++)
				n += STR + utflen(names[i]);
			return n;
		}

		public final String toString() {
			String s = "Twalk(" + tag + "," + fid + "," + newfid + ",{";
			for (int i = 0; i < names.length; i++)
				s += "\"" + names[i] + "\",";
			s += "})";
			return s;
		}
	}

	public class Topen extends Tmsg {
		public int fid;
		public int mode;

		public Topen(int fid, int mode) {
			this.fid = fid;
			this.mode = mode;
		}

		Topen(ByteBuffer b) {
			super(b);
			fid = g32(b);
			mode = g8(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTopen);
			packtag(b);
			p32(b, fid);
			b.put((byte) mode);
		}

		public int mtype() {
			return MTopen;
		}

		public String mname() {
			return "Topen";
		}

		public final int packedsize() {
			return H + FID + BIT8SZ;
		}

		public final String toString() {
			return "Topen(" + tag + "," + fid + "," + mode + ")";
		}
	}

	public class Tcreate extends Tmsg {
		public int fid;
		public String name;
		public int perm;
		public int mode;

		public Tcreate(int fid, String name, int perm, int mode) {
			this.fid = fid;
			this.name = name;
			this.perm = perm;
			this.mode = mode;
		}

		Tcreate(ByteBuffer b) {
			super(b);
			fid = g32(b);
			name = gets(b);
			perm = g32(b);
			mode = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTcreate);
			packtag(b);
			p32(b, fid);
			puts(b, name);
			p32(b, perm);
			b.put((byte) mode);
		}

		public int mtype() {
			return MTcreate;
		}

		public String mname() {
			return "Tcreate";
		}

		public final int packedsize() {
			return H + FID + STR + BIT32SZ + BIT8SZ + utflen(name);
		}

		public final String toString() {
			return "Tcreate(" + tag + "," + fid + ",\"" + name + "\","
					+ Integer.toHexString(perm) + "," + mode + ")";
		}
	}

	public class Tread extends Tmsg {
		public int fid;
		public long offset;
		public int count;

		public Tread(int fid, long offset, int count) {
			this.fid = fid;
			this.offset = offset;
			this.count = count;
		}

		Tread(ByteBuffer b) {
			super(b);
			fid = g32(b);
			offset = g64(b);
			count = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTread);
			packtag(b);
			p32(b, fid);
			p64(b, offset);
			p32(b, count);
		}

		public int mtype() {
			return MTread;
		}

		public String mname() {
			return "Tread";
		}

		public final int packedsize() {
			return H + FID + OFFSET + COUNT;
		}

		public final String toString() {
			return "Tread(" + tag + "," + fid + "," + offset + "," + count
					+ ")";
		}
	}

	public class Twrite extends Tmsg {
		public int fid;
		public long offset;
		public ByteBuffer data;

		public Twrite(int fid, long offset, ByteBuffer data) {
			this.fid = fid;
			this.offset = offset;
			this.data = data;
		}

		public Twrite(int fid, long offset, ByteBuffer data, int n) {
			if (n < data.remaining()) {
				data = data.duplicate();
				data.limit(data.position() + n);
				data = data.slice();
			}
			this.fid = fid;
			this.offset = offset;
			this.data = data;
		}

		Twrite(ByteBuffer b) {
			super(b);
			fid = g32(b);
			offset = g64(b);
			int count = g32(b);
			data = b.duplicate();
			data.limit(b.position() + count);
			data = data.slice();
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTwrite);
			packtag(b);
			p32(b, fid);
			p64(b, offset);
			p32(b, data.remaining());
			b.put(data);
		}

		public int mtype() {
			return MTwrite;
		}

		public String mname() {
			return "Twrite";
		}

		public final int packedsize() {
			return H + FID + OFFSET + COUNT + data.remaining();
		}

		public final String toString() {
			return "Twrite(" + tag + "," + fid + "," + offset + ","
					+ data.remaining() + ")";
		}
	}

	public class Tclunk extends Tmsg {
		public int fid;

		public Tclunk(int fid) {
			this.fid = fid;
		}

		Tclunk(ByteBuffer b) {
			super(b);
			fid = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTclunk);
			packtag(b);
			p32(b, fid);
		}

		public int mtype() {
			return MTclunk;
		}

		public String mname() {
			return "Tclunk";
		}

		public final int packedsize() {
			return H + FID;
		}

		public final String toString() {
			return "Tclunk(" + tag + "," + fid + ")";
		}
	}

	public class Tstat extends Tmsg {
		public int fid;

		public Tstat(int fid) {
			this.fid = fid;
		}

		Tstat(ByteBuffer b) {
			super(b);
			fid = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTstat);
			packtag(b);
			p32(b, fid);
		}

		public int mtype() {
			return MTstat;
		}

		public String mname() {
			return "Tstat";
		}

		public final int packedsize() {
			return H + FID;
		}

		public final String toString() {
			return "Tstat(" + tag + "," + fid + ")";
		}
	}

	public class Tremove extends Tmsg {
		public int fid;

		public Tremove(int fid) {
			this.fid = fid;
		}

		Tremove(ByteBuffer b) {
			super(b);
			fid = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTremove);
			packtag(b);
			p32(b, fid);
		}

		public int mtype() {
			return MTremove;
		}

		public String mname() {
			return "Tremove";
		}

		public final int packedsize() {
			return H + FID;
		}

		public final String toString() {
			return "Tremove(" + tag + "," + fid + ")";
		}
	}

	public class Twstat extends Tmsg {
		public int fid;
		public Dir stat;

		public Twstat(int fid, Dir stat) {
			this.fid = fid;
			this.stat = stat;
		}

		Twstat(ByteBuffer b) throws BAD {
			super(b);
			fid = g32(b);
			int n = g16(b); // TO DO: check this
			stat = unpackdir(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MTwstat);
			packtag(b);
			p32(b, fid);
			p16(b, packdirsize(stat));
			packdir(b, stat);
		}

		public int mtype() {
			return MTwstat;
		}

		public String mname() {
			return "Twstat";
		}

		public final int packedsize() {
			return H + FID + LEN + packdirsize(stat);
		}

		public final String toString() {
			return "Twstat(" + tag + "," + fid + "," + stat + ")";
		}
	}

	public abstract class Rmsg extends Smsg {
		public int tag;

		Rmsg(int tag) {
			this.tag = tag;
		}

		protected final void packtag(ByteBuffer b) {
			p16(b, tag);
		}

		protected Rmsg(ByteBuffer b) {
			tag = g16(b);
		}

		public final boolean isTmsg() {
			return false;
		}

		// read: fn(fd: ref Sys->FD, msize: int): ref Rmsg;
		// unpack: fn(a: array of byte): (int, ref Rmsg);
		// pack: fn(nil: self ref Rmsg): array of byte;
		// packedsize: fn(nil: self ref Rmsg): int;
		// text: fn(nil: self ref Rmsg): string;
	}

	public class Rversion extends Rmsg {
		public int msize;
		public String version;

		public Rversion(int tag, int msize, String version) {
			super(tag);
			this.msize = msize;
			this.version = version;
		}

		Rversion(ByteBuffer b) {
			super(b);
			msize = g32(b);
			version = gets(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRversion);
			packtag(b);
			p32(b, msize);
			puts(b, version);
		}

		public int mtype() {
			return MRversion;
		}

		public String mname() {
			return "Rversion";
		}

		public final int packedsize() {
			return H + BIT32SZ + STR + utflen(version);
		}

		public final String toString() {
			return "Rversion(" + tag + "," + msize + "," + version + ")";
		}
	}

	public class Rauth extends Rmsg {
		public Qid aqid;

		public Rauth(int tag, Qid aqid) {
			super(tag);
			this.aqid = aqid;
		}

		Rauth(ByteBuffer b) {
			super(b);
			aqid = gqid(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRauth);
			packtag(b);
			pqid(b, aqid);
		}

		public int mtype() {
			return MRauth;
		}

		public String mname() {
			return "Rauth";
		}

		public final int packedsize() {
			return H + QID;
		}

		public final String toString() {
			return "Rauth(" + tag + "," + aqid + ")";
		}
	}

	public class Rattach extends Rmsg {
		public Qid qid;

		public Rattach(int tag, Qid qid) {
			super(tag);
			this.qid = qid;
		}

		Rattach(ByteBuffer b) {
			super(b);
			qid = gqid(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRattach);
			packtag(b);
			pqid(b, qid);
		}

		public int mtype() {
			return MRattach;
		}

		public String mname() {
			return "Rattach";
		}

		public final int packedsize() {
			return H + QID;
		}

		public final String toString() {
			return "Rattach(" + tag + "," + qid + ")";
		}
	}

	public class Rflush extends Rmsg {

		public Rflush(int tag) {
			super(tag);
		}

		Rflush(ByteBuffer b) {
			super(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRflush);
			packtag(b);
		}

		public int mtype() {
			return MRflush;
		}

		public String mname() {
			return "Rflush";
		}

		public final int packedsize() {
			return H;
		}

		public final String toString() {
			return "Rflush(" + tag + ")";
		}
	}

	public class Rerror extends Rmsg {
		public String ename;

		public Rerror(int tag, String ename) {
			super(tag);
			this.ename = ename;
		}

		Rerror(ByteBuffer b) {
			super(b);
			ename = gets(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRerror);
			packtag(b);
			puts(b, ename);

		}

		public int mtype() {
			return MRerror;
		}

		public String mname() {
			return "Rerror";
		}

		public final int packedsize() {
			return H + STR + utflen(ename);
		}

		public final String toString() {
			return "Rerror(" + tag + ",\"" + ename + "\"";
		}
	}

	public class Rclunk extends Rmsg {
		public Rclunk(int tag) {
			super(tag);
		}

		Rclunk(ByteBuffer b) {
			super(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRclunk);
			packtag(b);
		}

		public int mtype() {
			return MRclunk;
		}

		public String mname() {
			return "Rclunk";
		}

		public final int packedsize() {
			return H;
		}

		public final String toString() {
			return "Rclunk(" + tag + ")";
		}
	}

	public class Rremove extends Rmsg {
		public Rremove(int tag) {
			super(tag);
		}

		Rremove(ByteBuffer b) {
			super(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRremove);
			packtag(b);
		}

		public int mtype() {
			return MRremove;
		}

		public String mname() {
			return "Rremove";
		}

		public final int packedsize() {
			return H;
		}

		public final String toString() {
			return "Rremove(" + tag + ")";
		}
	}

	public class Rwstat extends Rmsg {
		public Rwstat(int tag) {
			super(tag);
		}

		Rwstat(ByteBuffer b) {
			super(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRwstat);
			packtag(b);
		}

		public int mtype() {
			return MRwstat;
		}

		public String mname() {
			return "Rwstat";
		}

		public final int packedsize() {
			return H;
		}

		public final String toString() {
			return "Rwstat(" + tag + ")";
		}
	}

	public class Rwalk extends Rmsg {
		public Qid[] qids;

		public Rwalk(int tag, Qid[] qids) {
			super(tag);
			this.qids = qids;
		}

		Rwalk(ByteBuffer b) {
			super(b);
			int n = g16(b);
			qids = new Qid[n];
			for (int i = 0; i < n; i++)
				qids[i] = gqid(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRwalk);
			packtag(b);
			int n = qids.length;
			p16(b, n);
			for (int i = 0; i < n; i++)
				pqid(b, qids[i]);
		}

		public int mtype() {
			return MRwalk;
		}

		public String mname() {
			return "Rwalk";
		}

		public final int packedsize() {
			return H + LEN + qids.length * QID;
		}

		public final String toString() {
			String s = "Rwalk({" + tag;
			for (int i = 0; i < qids.length; i++)
				s += "," + qids[i];
			s += "})";
			return s;
		}
	}

	public class Rcreate extends Rmsg {
		public Qid qid;
		public int iounit;

		public Rcreate(int tag, Qid qid) {
			super(tag);
			this.qid = qid;
			iounit = MAXFDATA;
		}

		public Rcreate(int tag, Qid qid, int iounit) {
			super(tag);
			this.qid = qid;
			this.iounit = iounit;
		}

		Rcreate(ByteBuffer b) {
			super(b);
			qid = gqid(b);
			iounit = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRcreate);
			packtag(b);
			pqid(b, qid);
			p32(b, iounit);
		}

		public int mtype() {
			return MRcreate;
		}

		public String mname() {
			return "Rcreate";
		}

		public final int packedsize() {
			return H + QID + COUNT;
		}

		public final String toString() {
			return "Rcreate(" + tag + "," + qid + "," + iounit + ")";
		}
	}

	public class Ropen extends Rmsg {
		public Qid qid;
		public int iounit;

		public Ropen(int tag, Qid qid) {
			super(tag);
			this.qid = qid;
			iounit = MAXFDATA;
		}

		public Ropen(int tag, Qid qid, int iounit) {
			super(tag);
			this.qid = qid;
			this.iounit = iounit;
		}

		Ropen(ByteBuffer b) {
			super(b);
			qid = gqid(b);
			iounit = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRopen);
			packtag(b);
			pqid(b, qid);
			p32(b, iounit);
		}

		public int mtype() {
			return MRopen;
		}

		public String mname() {
			return "Ropen";
		}

		public final int packedsize() {
			return H + QID + COUNT;
		}

		public final String toString() {
			return "Ropen(" + tag + "," + qid + "," + iounit + ")";
		}
	}

	public class Rread extends Rmsg {
		public ByteBuffer data;

		public Rread(int tag, ByteBuffer data) {
			super(tag);
			this.data = data;
		}

		Rread(ByteBuffer b) {
			super(b);
			int n = g32(b);
			data = b.duplicate();
			int limit = b.position() + n;
			data.limit(limit);
			data = data.slice();
			b.position(limit);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRread);
			packtag(b);
			if (data != null) {
				p32(b, data.remaining());
				b.put(data);
			} else
				p32(b, 0);
		}

		public int mtype() {
			return MRread;
		}

		public String mname() {
			return "Rread";
		}

		public final int packedsize() {
			return H + COUNT + data.remaining();
		}

		public final String toString() {
			return "Rread(" + tag + "," + data.remaining() + ")";
		}
	}

	public class Rwrite extends Rmsg {
		public int count;

		public Rwrite(int tag, int count) {
			super(tag);
			this.count = count;
		}

		Rwrite(ByteBuffer b) {
			super(b);
			count = g32(b);
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRwrite);
			packtag(b);
			p32(b, count);
		}

		public int mtype() {
			return MRwrite;
		}

		public String mname() {
			return "Rwrite";
		}

		public final int packedsize() {
			return H + COUNT;
		}

		public final String toString() {
			return "Rwrite(" + tag + "," + count + ")";
		}
	}

	public class Rstat extends Rmsg {
		public Dir stat;

		public Rstat(int tag, Dir stat) {
			super(tag);
			this.stat = stat;
		}

		Rstat(ByteBuffer b) throws BAD {
			super(b);
			int n = g16(b); // TO DO: consistency check
			int pos = b.position();
			stat = unpackdir(b);
			if (b.position() != pos + n)
				throw new BAD("bad Styx stat count"); // TO DO: throw exception
		}

		final void pack(ByteBuffer b) {
			b.put((byte) MRstat);
			packtag(b);
			p16(b, packdirsize(stat));
			packdir(b, stat);
		}

		public int mtype() {
			return MRstat;
		}

		public String mname() {
			return "Rstat";
		}

		public final int packedsize() {
			return H + LEN + packdirsize(stat);
		}

		public final String toString() {
			return "Rstat(" + tag + "," + stat + ")";
		}
	}

	public final boolean isTmsg(ByteBuffer b) {
		return b.remaining() >= H && (b.get(BIT32SZ) & 1) == 0;
	}

	public final boolean isRmsg(ByteBuffer b) {
		return b.remaining() >= H && (b.get(BIT32SZ) & 1) != 0;
	}

	private static final Smsg readmsg(ReadableByteChannel chan, ByteBuffer b) {
		int l = b.remaining();
		if (l < BIT32SZ)
			return null;
		int p = b.position();
		int n = g32(b);
		// TO DO
		return null;
	}

	protected static final Dir unpackdir(ByteBuffer b) throws BAD {
		int n = g16(b); // TO DO: consistency check on format
		int pos = b.position();
		Dir d = new Dir();
		d.dtype = g16(b);
		d.dev = g32(b);
		d.qid = gqid(b);
		d.mode = g32(b);
		d.atime = g32(b);
		d.mtime = g32(b);
		d.length = g64(b);
		d.name = gets(b);
		d.uid = gets(b);
		d.gid = gets(b);
		d.muid = gets(b);
		if (b.position() != pos + n)
			throw new BAD("Dir badly packed");
		return d;
	}

	public int packdirsize(Dir d) {
		return STATFIXLEN + utflen(d.name) + utflen(d.uid) + utflen(d.gid)
				+ utflen(d.muid);
	}

	public final void packdir(ByteBuffer b, Dir d) {
		p16(b, packdirsize(d) - LEN);
		p16(b, d.dtype);
		p32(b, d.dev);
		pqid(b, d.qid);
		p32(b, d.mode);
		p32(b, d.atime);
		p32(b, d.mtime);
		p64(b, d.length);
		puts(b, d.name);
		puts(b, d.uid);
		puts(b, d.gid);
		puts(b, d.muid);
	}

	private final void pqid(ByteBuffer b, Qid q) {
		b.put((byte) q.qtype);
		p32(b, q.vers);
		p64(b, q.path);
	}

	private static final Qid gqid(ByteBuffer b) {
		int qtype = g8(b);
		int vers = g32(b);
		long path = g64(b);
		return new Qid(path, vers, qtype);
	}

	private static final void puts(ByteBuffer b, String s) {
		byte[] a = bytes(s);
		p16(b, a.length);
		b.put(a);
	}

	private static final String gets(ByteBuffer b) {
		int n = g16(b);
		byte[] a = new byte[n];
		b.get(a);
		try {
			return new String(a, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling";
		}
	}

	private static final int g8(ByteBuffer b) {
		return (int) b.get() & 0xFF;
	}

	private static final void p16(ByteBuffer b, int n) {
		b.put((byte) n);
		b.put((byte) (n >> 8));
	}

	private static final int g16(ByteBuffer b) {
		int b0 = g8(b);
		return (g8(b) << 8) | b0;
	}

	private static final void p32(ByteBuffer b, int v) {
		b.put((byte) v);
		b.put((byte) (v >> 8));
		b.put((byte) (v >> 16));
		b.put((byte) (v >> 24));
	}

	private static final int g32(ByteBuffer b) {
		int b0 = g8(b);
		int b1 = g8(b);
		int b2 = g8(b);
		return (g8(b) << 24) | (b2 << 16) | (b1 << 8) | b0;
	}

	private static final void p64(ByteBuffer b, long v) {
		int n = (int) v;
		b.put((byte) n);
		b.put((byte) (n >> 8));
		b.put((byte) (n >> 16));
		b.put((byte) (n >> 24));
		n = (int) (v >> 32);
		b.put((byte) n);
		b.put((byte) (n >> 8));
		b.put((byte) (n >> 16));
		b.put((byte) (n >> 24));
	}

	private static final long g64(ByteBuffer b) {
		int n0 = g32(b);
		return ((long) g32(b) << 32) | ((long) n0 & 0xFFFFFFFF);
	}

	// misc support functions

	public static final int utflen(String s) { // 16-bit unicode only
		int n, l;

		if (s == null)
			return 0;
		n = l = s.length();
		for (int i = 0; i < l; i++) {
			int c;
			if ((c = s.charAt(i)) > 0x7F) {
				n++;
				if (c > 0x7FF)
					n++;
			}
		}
		return n;
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

	public static final byte[] bytes(ByteBuffer b) {
		int n = b.remaining();
		if (b.hasArray() && b.arrayOffset() == 0) {
			byte[] a = b.array();
			if (a.length == n)
				return a;
		}
		byte[] a = new byte[b.remaining()];
		b.get(a);
		return a;
	}

	public static final void dump(ByteBuffer b) {
		String s = "buffer " + b.toString() + ":";
		int n = 0;
		for (int i = b.position(); i < b.limit() && ++n < 64; i++)
			s += " " + Integer.toString((int) b.get(i) & 0xFF, 16);
		System.out.println(s);
	}

	public static final void dump(byte[] b, int i, int e, int max) {
		String s = "buffer " + b + ":";
		if (max < 0)
			max = 64;
		if (e > b.length)
			e = b.length;
		int n = 0;
		for (; i < e && ++n < max; i++)
			s += " " + Integer.toString((int) b[i] & 0xFF, 16);
		System.out.println(s);
	}

	public static final void dump(byte[] b, int max) {
		dump(b, 0, b.length, max);
	}

	public static String S(byte[] a) {
		try {
			return new String(a, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling";
		}
	}

	public static String S(byte[] a, int o, int l) {
		try {
			return new String(a, o, l, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling";
		}
	}

	public static String S(ByteBuffer a) {
		try {
			return new String(a.array(), a.arrayOffset() + a.position(), a
					.remaining(), "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling";
		}
	}
}
