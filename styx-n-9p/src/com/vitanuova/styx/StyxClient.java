package com.vitanuova.styx;

/*
 * styx client
 *
 *	Java forces a daemon process for Channel reading to allow
 *	waiting requests to be interrupted (an InterruptableChannel exists,
 *	but cannot be associated with old java.io, and furthermore, closes
 *	the channel on the interrupt!)
 *
 * TO DO
 *	full walk (chunks of NWELEM, ..)
 *	version compatibility
 *	StyxReader?
 *
 * Copyright © 2005 Vita Nuova Holdings Limited [C H Forsyth, forsyth@vitanuova.com]
 * Subject to the terms of the MIT-template (google for a copy)
 */

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class StyxClient {

	public boolean debug = false;

	static public final int SEEKSTART = 0;
	static public final int SEEKRELA = 1;
	static public final int SEEKEND = 2;

	static public final String Enomem = "out of memory";
	static public final String Eexists = "file already exists";
	static public final String Eisdir = "file is a directory";
	static public final String Enegoff = "negative i/o offset";
	static public final String Etoosmall = "read or write too small";
	static public final String Ebadusefd = "inappropriate use of fd";
	static public final String Ehungup = "i/o on hungup channel";
	static public final String Eversion = "version not established for mount channel";
	static public final String Eintr = "interrupted";
	static public final String Enotdir = "not a directory";
	static public final String Edoesnotexist = "does not exist";

	private static ThreadLocal<String> lasterror = new ThreadLocal<String>();

	public final String errstr() {
		return lasterror.get();
	}

	public final static void werrstr(String s) {
		lasterror.set(s);
	}

	private static Styx styx = new Styx();

	public StyxClient() {
		settag(0);
		settag(Styx.NOTAG);
	}

	private class Request {
		Styx.Tmsg t;
		Styx.Rmsg r;
		Request flushed;
		Request next;

		Request(Styx.Tmsg t) {
			this.t = t;
		}

		Request(Styx.Tmsg t, Request old) {
			this.t = t;
			flushed = old;
		}

		synchronized final void awaitreply() throws InterruptedException {
			while (r == null)
				this.wait();
		}
	}

	private class Requests {
		Request set; // set of requests; tags are unique
		String err;

		synchronized final void add(Request req) {
			if (err == null) {
				req.next = set;
				set = req;
			} else if (req.t instanceof Styx.Tflush)
				req.r = styx.new Rflush(req.t.tag);
			else
				req.r = styx.new Rerror(req.t.tag, err);
		}

		final void completed(Styx.Rmsg r) {
			synchronized (this) {
				Request prev = null;
				for (Request req = set; req != null; req = req.next) {
					if (req.t.tag == r.tag) {
						synchronized (req) {
							if (prev != null)
								prev.next = req.next;
							else
								set = req.next;
							req.next = null;
							req.r = r;
							req.notify();
							puttag(r.tag); // assumes Request won't be recycled
											// but freed
						}
						return;
					}
					prev = set;
				}
			}
			System.out.println("unexpected reply tag " + r.tag + " type "
					+ r.mtype() + "(" + r.mname() + ")");
		}

		final void diagnose(String why) {
			System.out.println("Styx message error: " + why);
		}

		final void shutdown(String why) {
			System.out.println("shut down: " + why);
			synchronized (this) {
				if (err == null) // first error might be most accurate
					err = why;
				Request r;
				while ((r = set) != null) {
					set = r.next;
					r.next = null;
					synchronized (r) {
						if (r.t instanceof Styx.Tflush)
							r.r = styx.new Rflush(r.t.tag);
						else
							r.r = styx.new Rerror(r.t.tag, why);
						r.notify();
						System.out.println("notify: " + r.t.tag + " "
								+ r.t.mname());
					}
				}
			}
		}
	}

	public class Conn {
		private int inuse = 1;
		private ReadableByteChannel rfd;
		private WritableByteChannel wfd;

		private int msize;
		private int fidgen;
		private FD fids;
		private FD freefids;

		private Requests active = new Requests();

		private Object versioning = new Object(); // Tversion queue lock
		private String versioned;

		public Conn(ReadableByteChannel rfd, WritableByteChannel wfd) {
			this.rfd = rfd;
			this.wfd = wfd;
			(new Reader()).start();
		}

		public String version(int msize, String v) {
			if (msize == 0)
				msize = Styx.MAXRPC;
			if (v == null)
				v = Styx.VERSION;
			synchronized (versioning) { // only one Tversion active at once on a
										// connection
				if (versioned != null)
					return versioned; // already done; TO DO: check
										// compatibility
				Styx.Rversion r = (Styx.Rversion) styxreq(styx.new Tversion(
						msize, v));
				if (r == null)
					return null;
				// check compatibility
				if (r.msize < 256 || r.msize > 1024 * 1024) {
					werrstr("nonsense value of remote msize in version");
					return null;
				}
				if (r.msize > msize) {
					werrstr("server tries to increase msize in version");
					return null;
				}
				int n = r.version.length();
				if (!v.substring(0, n).equals(r.version)) {
					werrstr("bad 9P version received from server");
					return null;
				}
				this.msize = r.msize;
				versioned = r.version;
			}
			return versioned;
		}

		public FD auth(String uname, String aname) {
			if (versioned == null && version(0, null) == null)
				return null;
			FD fd = newfd();
			Styx.Rauth r = (Styx.Rauth) styxreq(styx.new Tauth(fd.fid, uname,
					aname));
			if (r == null) {
				fd.free();
				return null;
			}
			fd.qid = r.aqid;
			fd.mode = Styx.ORDWR;
			return fd;
		}

		public FS attach(FD afd, String uname, String aname) {
			if (versioned == null && version(0, null) == null)
				return null;
			FD fd = newfd();
			int afid = Styx.NOFID;
			if (afd != null)
				afid = afd.fid;
			Styx.Rattach r = (Styx.Rattach) styxreq(styx.new Tattach(fd.fid,
					afid, uname, aname));
			if (r == null) {
				fd.free();
				return null;
			}
			fd.name = "/";
			fd.qid = r.qid;
			return new FS(this, fd, aname);
		}

		public void close() {
			/* wouldn't need this if Java had predictable gc */
			synchronized (this) {
				if (--inuse != 0)
					return;
			}
			fids = null;
			freefids = null;
			if (rfd != null) {
				try {
					rfd.close(); // will have important side-effect of stopping
									// Reader
				} catch (IOException e) {
				}
				rfd = null;
			}
			if (wfd != null) {
				try {
					wfd.close();
				} catch (IOException e) {
				}
				wfd = null;
			}
		}

		protected Styx.Rmsg styxreq(Styx.Tmsg f) {
			int otype = f.mtype();
			int tag;
			if (otype == Styx.MTversion)
				f.tag = tag = Styx.NOTAG;
			else if ((tag = gettag()) != Styx.NOTAG)
				f.tag = tag;
			else {
				werrstr("out of Styx message tags"); // unlikely: there are
														// 2^16-2
				return null;
			}
			int size = f.packedsize();
			ByteBuffer buf = ByteBuffer.allocate(size);
			f.packsize(buf, size);
			f.pack(buf);
			buf.flip();
			if (debug)
				System.out.println("-> " + f.toString());
			int oldtag = f.tag;
			Request req = new Request(f);
			for (;;) {
				active.add(req);
				try {
					/*
					 * we rely on Channels non-interleaving semantics (TO DO:
					 * test)
					 */
					/* channel must not be non-blocking (short writes) */
					if (wfd.write(buf) != size)
						throw new IOException("short write on Styx channel");
					req.awaitreply();
					break;
				} catch (InterruptedException e) {
					req = new Request(styx.new Tflush(oldtag), req); // ie,
																		// flush
																		// original
																		// request,
																		// not
																		// last
																		// Tflush
				} catch (Exception e) { // anything else is fatal
					flushout(req);
					werrstr("i/o error: " + e.getMessage());
					return null;
				}
			}
			if (req.flushed != null)
				req = flushout(req);
			Styx.Rmsg r = req.r;
			if (debug)
				System.out.println("<- " + r.toString());
			if (r instanceof Styx.Rerror) {
				werrstr(((Styx.Rerror) r).ename);
				return null; // use exception?
			}
			if (r instanceof Styx.Rflush) {
				werrstr(Eintr);
				return null; // use exception?
			}
			if (r.mtype() != otype + 1) {
				werrstr("mismatched Styx reply");
				return null; // use exception?
			}
			return r;
		}

		private Request flushout(Request req) {
			for (;;) {
				if (req.r == null)
					active.completed(styx.new Rflush(req.t.tag));
				if (req.flushed == null)
					break;
				req = req.flushed;
			}
			return req;
		}

		protected final FD newfd() {
			FD fd;
			synchronized (this) {
				fd = freefids;
				if (fd == null) {
					fd = new FD();
					fd.fid = ++fidgen;
				} else {
					freefids = fd.next;
					fd.next = null;
				}
			}
			fd.inuse = 1;
			fd.conn = this;
			return fd;
		}

		protected final void freefd(FD fd) {
			if (fd == null)
				return;
			// assert fd.inuse <= 1;
			fd.conn = null;
			fd.mode = -1;
			fd.offset = 0;
			fd.iounit = 0;
			fd.qid = null;
			synchronized (this) {
				fd.next = freefids;
				freefids = fd;
			}
		}

		private class Reader extends Thread {
			Reader() {
				setDaemon(true);
			}

			private final void fillbuf(ReadableByteChannel fd, ByteBuffer b)
					throws IOException {
				while (b.remaining() > 0 && fd.read(b) > 0) {
					/* skip */
				}
				b.flip();
				if (b.remaining() != b.capacity())
					throw new IOException("Styx message truncated");
			}

			@Override
			public final void run() {
				try {
					Styx.Unpack unpacker = styx.new Unpack();
					ByteBuffer sizeb = ByteBuffer.allocate(Styx.BIT32SZ);
					for (;;) {
						int msglim = msize;
						if (msglim == 0)
							msglim = Styx.MAXRPC;
						fillbuf(rfd, sizeb);
						int ml = sizeb.get() & 0xFF;
						ml |= (sizeb.get() & 0xFF) << 8;
						ml |= (sizeb.get() & 0xFF) << 16;
						ml |= (sizeb.get() & 0xFF) << 24;
						sizeb.clear();
						if (ml > msglim) {
							active.shutdown("Styx message longer than agreed");
							return;
						}
						if ((ml -= Styx.BIT32SZ) <= 0) {
							active.shutdown("invalid Styx message size");
							return;
						}
						ByteBuffer b = ByteBuffer.allocate(ml);
						fillbuf(rfd, b);
						try {
							Styx.Rmsg r = unpacker.unpackR(b);
							if (r != null)
								active.completed(r);
							else
								active.diagnose("can't unpack: bad format");
						} catch (Styx.BAD e) {
							active.diagnose("can't unpack: bad type");
						}
					}
				} catch (EOFException e) {
					active.shutdown(Ehungup);
				} catch (Exception e) {
					active.shutdown("error reading Styx message: "
							+ e.getMessage());
				}
			}
		}
	}

	public class FS {
		int inuse = 1;
		Conn conn;
		String aname;
		FD root;
		private ThreadLocal<FD> dot;

		private final FD getdot() {
			return dot.get();
		}

		private final void setdot(FD fd) {
			FD ofd = getdot();
			dot.set(fd);
			if (ofd != null)
				ofd.close();
		}

		protected FS(Conn conn, FD root, String aname) {
			this.conn = conn;
			this.root = root;
			this.aname = aname;
			dot = new ThreadLocal<FD>();
		}

		public FS use() {
			synchronized (this) {
				inuse++;
			}
			return this;
		}

		public void close() {
			synchronized (this) {
				if (--inuse != 0)
					return;
			}
			root.close();
			root = null;
			aname = null;
			FD fd = getdot();
			if (fd != null) {
				fd.close();
				fd = null;
			}
			conn.close();
		}

		public FD open(String name, int mode) {
			FD fd = conn.newfd();
			if (!walk(root, fd, new Parse(name))) {
				fd.free();
				return null;
			}
			Styx.Ropen r = (Styx.Ropen) conn.styxreq(styx.new Topen(fd.fid,
					mode));
			if (r == null) {
				fd.close();
				return null;
			}
			fd.mode = openmode(mode);
			fd.iounit = r.iounit;
			fd.qid = r.qid;
			return fd;
		}

		public FD create(String name, int mode, int perm) {
			Parse p = new Parse(name);
			if (p.els.length == 0) {
				werrstr(Eexists);
				return null;
			}
			FD fd = conn.newfd();
			if (!walk(root, fd, new Parse(p, 0, p.els.length - 1))) { // walk to
																		// penultimate
																		// entry
				fd.free();
				return null;
			}
			/* try to walk to final entry */
			switch (opentrunc(fd, p, mode)) {
			case -1:
				fd.close();
				return null;
			case 1:
				return fd;
			}
			/* must create */
			String entry = p.els[p.els.length - 1];
			Styx.Rcreate r = (Styx.Rcreate) conn.styxreq(styx.new Tcreate(
					fd.fid, entry, perm, mode));
			if (r != null) {
				fd.name = addname(fd.name, entry);
				fd.mode = openmode(mode);
				fd.iounit = r.iounit;
				fd.qid = r.qid;
				return fd;
			}
			/* to allow for create/create race, attempt a second open here */
			String s = errstr();
			if (opentrunc(fd, p, mode) <= 0) {
				fd.close();
				werrstr(s); /* restore original diagnostic */
				return null;
			}
			return fd;
		}

		public Dir stat(String name) {
			FD fd = walk(name);
			if (fd == null)
				return null;
			Dir d = fd.stat();
			fd.close();
			return d;
		}

		public int wstat(String name, Dir db) {
			FD fd = walk(name);
			if (fd == null)
				return -1;
			int r = fd.wstat(db);
			fd.close();
			return r;
		}

		public int remove(String name) {
			FD fd = walk(name);
			if (fd == null)
				return -1;
			Styx.Rremove r = (Styx.Rremove) conn.styxreq(styx.new Tremove(
					fd.fid));
			fd.close();
			if (r == null)
				return -1;
			return 0;
		}

		public boolean chdir(String name) {
			if (name == null) {
				setdot(null);
				return true;
			}
			FD fd = walk(name);
			if (fd == null)
				return false;
			setdot(fd);
			return true;
		}

		public String getwd() {
			FD fd = getdot();
			if (fd == null)
				return root.name;
			return fd.name;
		}

		/* implementation */

		protected class Parse {
			String name;
			String[] els;
			boolean abs;

			Parse(String name) {
				abs = name.length() != 0 && name.charAt(0) == '/';
				this.name = name;
				StringTokenizer st = new StringTokenizer(name, "/");
				int n = st.countTokens();
				String[] names = new String[n];
				int dots = 0;
				for (int i = 0; i < n;) {
					names[i] = st.nextToken();
					if (names[i] == ".")
						dots++;
					else
						i++;
				}
				if (dots == 0) {
					els = names;
					return;
				}
				n -= dots;
				els = new String[n];
				System.arraycopy(names, 0, els, 0, n);
				names = null;
			}

			Parse(Parse p, int s, int e) { // slice
				abs = p.abs && s == 0;
				name = p.name;
				els = new String[e - s];
				for (int i = 0; i < els.length; i++)
					els[i] = p.els[s + i];
			}
		}

		protected boolean walk(FD ofd, FD fd, Parse p) {
			Styx.Rwalk r = (Styx.Rwalk) conn.styxreq(styx.new Twalk(ofd.fid,
					fd.fid, p.els));
			if (r == null)
				return false;
			if (r.qids.length != p.els.length) { /* TO DO: chunks of NWELEM */
				String s;
				if (p.abs)
					s = "/";
				else
					s = "";
				int i;
				for (i = 0; i < r.qids.length; i++) {
					if (i != 0)
						s += "/";
					s += p.els[i];
				}
				if (i == 0 || (r.qids[i - 1].qtype & Qid.QTDIR) != 0) {
					if (s.length() != 0)
						s += "/";
					s += p.els[i];
					werrstr("'" + s + "'" + " " + Edoesnotexist); // could be
																	// permission
																	// problem
				} else
					werrstr("'" + s + "'" + " " + Enotdir);
				return false;
			}
			fd.name = ofd.name;
			for (int i = 0; i < p.els.length; i++)
				fd.name = addname(fd.name, p.els[i]);
			fd.qid = r.qids[r.qids.length - 1];
			return true;
		}

		protected boolean walk(FD fd, String name) {
			return walk(root, fd, new Parse(name));
		}

		protected FD walk(String name) {
			FD fd = conn.newfd();
			Parse p = new Parse(name);
			FD ofd = root;
			if (!p.abs) {
				FD cwd = getdot();
				if (cwd != null)
					ofd = cwd; // the reference count in dot will hold it for
								// this process
			}
			if (!walk(ofd, fd, p)) {
				fd.free();
				fd = null;
			}
			return fd;
		}

		protected int opentrunc(FD fd, Parse p, int mode) {
			/* if able to walk one level, open and truncate */
			if (!walk(fd, fd, new Parse(p, p.els.length - 1, p.els.length)))
				return 0; // doesn't exist
			if ((mode & Styx.OEXCL) != 0) {
				werrstr(Eexists);
				return -1;
			}
			/* try an open */
			Styx.Ropen r = (Styx.Ropen) conn.styxreq(styx.new Topen(fd.fid,
					mode | Styx.OTRUNC));
			if (r == null)
				return -1;
			fd.mode = openmode(mode);
			fd.iounit = r.iounit;
			fd.qid = r.qid;
			return 1;
		}

		protected final String addname(String n1, String n2) {
			if (n1 == null)
				n1 = "";
			if (n2 == null || n2.length() == 0)
				return n1;
			int l1 = n1.length();
			if (l1 == 0 || n1.charAt(l1 - 1) != '/')
				return n1 + "/" + n2;
			return n1 + n2;
		}
	}

	public class FD {
		int inuse = 1;
		Conn conn;
		int fid;
		int mode = -1;
		int iounit;
		Qid qid;
		long offset;
		String name; // not yet used
		FD next; // free list

		public ByteBuffer read(int n, long off) {
			if (!checkfd(Styx.OREAD))
				return null;
			if (n < 0) {
				werrstr(Etoosmall);
				return null;
			}
			if (off < 0) {
				werrstr(Enegoff);
				return null;
			}
			Styx.Rread r = (Styx.Rread) conn
					.styxreq(styx.new Tread(fid, off, n));
			if (r == null)
				return null;
			int nr = r.data.remaining();
			if (nr > n)
				r.data.limit(r.data.position() + n); // guard against broken
														// servers
			return r.data;
		}

		public int read(ByteBuffer buf, int n, long off) {
			ByteBuffer rb = read(n, off);
			if (rb == null)
				return -1;
			n = rb.remaining();
			buf.put(rb);
			return n;
		}

		public ByteBuffer read(int n) {
			long off;
			synchronized (this) {
				off = offset;
			}
			ByteBuffer rb = read(n, off);
			if (rb == null)
				return null;
			n = rb.remaining();
			synchronized (this) {
				offset += n;
			}
			return rb;
		}

		public int read(ByteBuffer buf, int n) {
			long off;
			synchronized (this) {
				off = offset;
			}
			n = read(buf, n, off);
			if (n < 0)
				return -1;
			synchronized (this) {
				offset += n;
			}
			return n;
		}

		public String reads(int lim) {
			ByteBuffer b = read(lim);
			if (b == null)
				return null;
			return Styx.S(b);
		}

		public String reads(int lim, long off) {
			ByteBuffer b = read(lim, off);
			if (b == null)
				return null;
			return Styx.S(b);
		}

		public int write(ByteBuffer buf, int n, long off) {
			if (!checkfd(Styx.OWRITE))
				return -1;
			if (n < 0) {
				werrstr(Etoosmall);
				return -1;
			}
			if (off < 0) {
				werrstr(Enegoff);
				return -1;
			}
			Styx.Rwrite r = (Styx.Rwrite) conn.styxreq(styx.new Twrite(fid,
					off, buf, n));
			if (r == null)
				return -1;
			return r.count;
		}

		public int write(ByteBuffer buf, int n) {
			long off;
			if ((qid.qtype & Qid.QTDIR) != 0) {
				werrstr(Eisdir);
				return -1;
			}
			synchronized (this) {
				off = offset;
				offset += n;
			} // assume it all goes
			int r = write(buf, n, off);
			if (r < 0) {
				synchronized (this) {
					offset -= n;
				}
				return -1;
			}
			if (r < n)
				synchronized (this) {
					offset -= n - r;
				}
			return r;
		}

		public int write(ByteBuffer buf) {
			return write(buf, buf.remaining());
		}

		public int write(byte[] buf) {
			return write(ByteBuffer.wrap(buf));
		}

		public int write(byte[] buf, long offset) {
			return write(ByteBuffer.wrap(buf), buf.length, offset);
		}

		public int write(String buf) {
			return write(Styx.bytes(buf));
		}

		public Dir[] dirread() {
			ByteBuffer b;
			b = read(4096); // arbitrary value bigger than largest single
							// directory entry
			if (b == null)
				return null;
			ArrayList<Dir> v = new ArrayList<Dir>(b.remaining()
					/ Styx.STATFIXLEN);
			dirunpack(b, v);
			return dirents(v);
		}

		public Dir[] dirreadall() {
			ByteBuffer b;
			ArrayList<Dir> v = new ArrayList<Dir>(256); // arbitrary
			while ((b = read(4096)) != null)
				if (!dirunpack(b, v))
					break;
			return dirents(v);
		}

		public Dir stat() {
			Styx.Rstat r = (Styx.Rstat) conn.styxreq(styx.new Tstat(fid));
			if (r == null)
				return null;
			return r.stat;
		}

		public int wstat(Dir d) {
			Styx.Rwstat r = (Styx.Rwstat) conn.styxreq(styx.new Twstat(fid, d));
			if (r == null)
				return -1;
			return 0;
		}

		public void close() {
			synchronized (this) {
				if (--inuse != 0)
					return;
			}
			conn.styxreq(styx.new Tclunk(fid));
			/* defined to succeed */
			conn.freefd(this);
		}

		private void free() {
			conn.freefd(this);
		}

		public long seek(long off, int whence) {
			switch (whence) {
			case SEEKSTART:
				if ((qid.qtype & Qid.QTDIR) != 0 && off != 0) {
					werrstr(Eisdir);
					return -1;
				}
				synchronized (this) {
					offset = off;
				}
				break;
			case SEEKRELA:
				if ((qid.qtype & Qid.QTDIR) != 0) {
					werrstr(Eisdir);
					return -1;
				}
				synchronized (this) {
					off += offset;
					if (off < 0) {
						werrstr(Enegoff);
						return -1;
					}
					offset = off;
				}
				break;
			case SEEKEND:
				if ((qid.qtype & Qid.QTDIR) != 0) {
					werrstr(Eisdir);
					return -1;
				}
				Dir d = stat();
				if (d == null) {
					werrstr("stat error in seek");
					return -1;
				}
				off += d.length;
				if (off < 0) {
					werrstr(Enegoff);
					return -1;
				}
				synchronized (this) {
					offset = off;
				}
				break;
			default:
				werrstr("invalid argument");
				return -1;
			}
			return off; // not offset, that might already have changed
		}

		public String path() {
			return name;
		}

		protected boolean checkfd(int m) {
			if (mode < 0)
				return false; // not opened
			if (mode != Styx.ORDWR) {
				if ((m & Styx.OTRUNC) != 0 && mode == Styx.OREAD
						|| (m & ~Styx.OTRUNC) != mode) {
					werrstr(Ebadusefd);
					return false;
				}
			}
			return true;
		}

	}

	static final protected int openmode(int m) {
		m &= 3;
		if (m == Styx.OEXEC)
			return Styx.OREAD;
		return m;
	}

	protected static final boolean dirunpack(ByteBuffer buf, ArrayList<Dir> v) {
		boolean found = false;
		while (buf.remaining() > 0) {
			try {
				v.add(Styx.unpackdir(buf));
				found = true;
			} catch (Styx.BAD e) {
				// server error?
				break;
			}
		}
		return found;
	}

	protected static final Dir[] dirents(ArrayList<Dir> v) {
		if (v.size() == 0)
			return null;
		return v.toArray(new Dir[0]); // don't ask: it's java
	}

	/* tags are shared by all clients and all connections */
	static private final int Tagshift = 5;
	static private final int Tagmask = (1 << Tagshift) - 1;
	static private final int Tagwords = (64 * 1024) >> Tagshift;
	static int[] tagbits = new int[Tagwords]; // might need to be done in
												// constructor

	static protected int gettag() {
		synchronized (tagbits) {
			for (int i = 0; i < Tagwords; i++) {
				int v = tagbits[i];
				if (v != ~0) {
					for (int j = 0; j <= Tagmask; j++)
						if ((v & (1 << j)) == 0) {
							tagbits[i] &= ~(1 << j);
							return i * (1 << Tagshift) + j;
						}
				}
			}
		}
		return Styx.NOTAG;
	}

	static protected void puttag(int tag) {
		if (tag != Styx.NOTAG)
			synchronized (tagbits) {
				tagbits[tag >> Tagshift] &= ~(1 << (tag & Tagmask));
			}
	}

	static private void settag(int tag) {
		// assert tag != Styx.NOTAG;
		synchronized (tagbits) {
			tagbits[tag >> Tagshift] |= 1 << (tag & Tagmask);
		}
	}
}
