package com.vitanuova.auth;

/*
 * Inferno public key authentication
 *
 *	Copyright © 2005 Vita Nuova Holdings Limited
 *
 * to do
 *	attr=val keys
 *	secstore interface
 *	Auth.auth
 */

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.StringTokenizer;

import com.vitanuova.lib.Base64;
import com.vitanuova.lib.Encoding;

//java.security.interfaces.RSAKey, RSAPrivateCrtKey, RSAPrivateKey, RSAPublicKey
// use instanceof applied to java.security.PublicKey

//java.security.spec
//	use with java.security.KeyFactory and java.security.AlgorithmParameters
//	RSAKeyGenParameterSpec(modulus, pubexp, privexp, primeP, primeQ, primeExpP, primeExpQ, crtCoeff)
//		getCrtCoefficient, getPrimeExponentP, getPrimeExponentQ, getPrimeP, getPrimeQ, getPublicExponent
//	RSAPrivateKeySpec(modulus, privateexp)
//		getModulus, getPrivateExponent
//	RSAPublicKeySpec(modulus, publicexp)
//		getModulus, getPublicExponent

// NoSuchAlgorithmException

public class Keyring {

	final static int Maxmsg = 4096;

	static final Encoding base64 = new Base64();
	static KeyFactory keyfactory;

	public static class AuthenticationException extends Exception {
		private static final long serialVersionUID = 1L;

		AuthenticationException(String s) {
			super(s);
		}
	}

	public static class LocalAuthErr extends AuthenticationException {
		private static final long serialVersionUID = 1L;

		LocalAuthErr(String s) {
			super(s);
		}
	}

	public static class RemoteAuthErr extends AuthenticationException {
		private static final long serialVersionUID = 1L;

		RemoteAuthErr(String s) {
			super(s);
		}
	}

	public static class InvalidCertificateException extends
			AuthenticationException {
		private static final long serialVersionUID = 1L;

		InvalidCertificateException(String s) {
			super(s);
		}
	}

	public static class InvalidKeyException extends AuthenticationException {
		private static final long serialVersionUID = 1L;

		InvalidKeyException(String s) {
			super(s);
		}
	}

	public Keyring() throws NoSuchAlgorithmException {
		keyfactory = KeyFactory.getInstance("RSA");
	}

	public class InfPublicKey {
		public PublicKey pk;
		public String owner;

		public InfPublicKey(PublicKey pk, String owner) {
			this.pk = pk;
			this.owner = owner;
		}

		public InfPublicKey(String s) throws InvalidKeyException {
			if (s == null)
				throw new InvalidKeyException("missing public key");
			String[] a = tokenize(s, "\n");
			if (a.length < 4)
				throw new InvalidKeyException("bad public key syntax");
			if (!a[0].equals("rsa"))
				throw new InvalidKeyException("unknown key algorithm: " + a[0]);
			BigInteger modulus = s2big(a[2]);
			BigInteger publicexp = s2big(a[3]);
			try {
				pk = keyfactory.generatePublic(new RSAPublicKeySpec(modulus,
						publicexp));
			} catch (InvalidKeySpecException e) {
				throw new InvalidKeyException("bad key spec: " + e.getMessage());
			}
			owner = a[1];
		}

		public String text() {
			RSAPublicKey pk = (RSAPublicKey) this.pk;
			return "rsa\n" + owner + "\n" + b64(pk.getModulus()) + "\n"
					+ b64(pk.getPublicExponent()) + "\n";
		}

		@Override
		public String toString() {
			return pk.toString() + "\nowner=" + owner;
		}
	}

	public class InfPrivateKey {
		PrivateKey sk;
		public String owner;

		public InfPrivateKey(PrivateKey sk, String owner) {
			this.sk = sk;
			this.owner = owner;
		}

		public InfPrivateKey(String s) throws InvalidKeyException {
			if (s == null)
				throw new InvalidKeyException("missing private key");
			String[] a = tokenize(s, "\n");
			if (a.length < 10)
				throw new InvalidKeyException("bad private key syntax");
			if (!a[0].equals("rsa"))
				throw new InvalidKeyException("unknown key algorithm: " + a[0]);
			BigInteger n = s2big(a[2]);
			BigInteger e = s2big(a[3]);
			BigInteger dk = s2big(a[4]);
			BigInteger p = s2big(a[5]);
			BigInteger q = s2big(a[6]);
			BigInteger kp = s2big(a[7]);
			BigInteger kq = s2big(a[8]);
			BigInteger c12 = s2big(a[9]);
			// mind your p's and q's: libsec's p is java's q! (Java follows
			// PKCS#1 in reversing their roles)
			// if using Java's RSA implementation directly, reverse p and q, and
			// kp and kq
			// we can't use it here because it imposes PKCS#1, so we do the
			// calculation ourselves
			try {
				sk = keyfactory.generatePrivate(new RSAPrivateCrtKeySpec(n, e,
						dk, p, q, kp, kq, c12));
			} catch (InvalidKeySpecException ex) {
				throw new InvalidKeyException("bad key spec: "
						+ ex.getMessage());
			}
			owner = a[1];
		}

		public InfPublicKey getpk() throws InvalidKeyException {
			RSAPrivateCrtKey rsk = (RSAPrivateCrtKey) sk;
			try {
				PublicKey pk = keyfactory.generatePublic(new RSAPublicKeySpec(
						rsk.getModulus(), rsk.getPublicExponent()));
				return new InfPublicKey(pk, owner);
			} catch (InvalidKeySpecException e) {
				throw new InvalidKeyException("bad key spec: " + e.getMessage());
			}
		}

		public String text() {
			RSAPrivateCrtKey sk = (RSAPrivateCrtKey) this.sk;
			return "rsa\n" + b64(sk.getModulus()) + "\n"
					+ b64(sk.getPublicExponent()) + "\n"
					+ b64(sk.getPrivateExponent()) + "\n" + b64(sk.getPrimeP())
					+ "\n" + b64(sk.getPrimeQ()) + "\n"
					+ b64(sk.getPrimeExponentP()) + "\n"
					+ b64(sk.getPrimeExponentQ()) + "\n"
					+ b64(sk.getCrtCoefficient()) + "\n";
		}

		@Override
		public String toString() {
			return sk.toString() + "\nowner=" + owner;
		}
	}

	public class Authinfo {
		public InfPrivateKey mysk;
		public InfPublicKey mypk;
		public Certificate cert; // signature of my public key
		public InfPublicKey spk; // signer's public key
		public BigInteger alpha; // diffie-hellman parameters
		public BigInteger p;

		public Authinfo(InfPrivateKey sk, InfPublicKey pk, Certificate cert,
				InfPublicKey spk, BigInteger alpha, BigInteger p) {
			mysk = sk;
			mypk = pk;
			this.cert = cert;
			this.spk = spk;
			this.alpha = alpha;
			this.p = p;
		}
	}

	public final Authinfo readauthinfo(ReadableByteChannel fd) throws Exception {
		// signer's public key, certificate, secret key (use sk.getpk to get
		// public one), alpha, p
		InfPublicKey spk;
		Certificate cert;
		InfPrivateKey mysk;
		BigInteger alpha, p;

		spk = new InfPublicKey(gets(getmsg(fd)));
		cert = new Certificate(gets(getmsg(fd)));
		mysk = new InfPrivateKey(gets(getmsg(fd)));
		alpha = s2big(gets(getmsg(fd)));
		p = s2big(gets(getmsg(fd)));
		return new Authinfo(mysk, mysk.getpk(), cert, spk, alpha, p);
	}

	public class Certificate {
		public String sa; // signature algorithm
		public String ha; // hash algorithm
		public String signer; // name of signer
		public int exp; // expiration date (seconds from Epoch, 0=never)
		BigInteger rsa; // only RSA signatures supported

		public Certificate(String sa, String ha, String signer, int exp,
				BigInteger rsa) {
			this.sa = sa;
			this.ha = ha;
			this.signer = signer;
			this.exp = exp;
			this.rsa = rsa;
		}

		public Certificate(String s) throws InvalidCertificateException {
			if (s == null)
				throw new InvalidCertificateException("missing certificate");
			String[] a = tokenize(s, "\n");
			if (a.length < 5)
				throw new InvalidCertificateException("bad certificate syntax"
						+ ":" + a.length);
			sa = a[0];
			ha = a[1];
			signer = a[2];
			exp = Integer.parseInt(a[3]);
			rsa = s2big(a[4]);
		}

		public final String text() {
			return sa + "\n" + ha + "\n" + signer + "\n" + exp + "\n"
					+ b64(rsa) + "\n";
		}
	}

	public class AuthResult {
		public Authinfo info;
		public byte[] secret;

		AuthResult(Authinfo info, byte[] secret) {
			this.info = info;
			this.secret = secret;
		}
	}

	public final AuthResult basicauth(ReadableByteChannel rfd,
			WritableByteChannel wfd, Authinfo info)
			throws AuthenticationException {
		BigInteger low, r0, alphar0, alphar1, alphar0r1;
		Certificate hiscert, alphacert;
		byte[] buf, hispkbuf, alphabuf;
		InfPublicKey hispk;
		byte[] secret;
		int vers;

		try {
			sendmsg(wfd, bytes("1"));
			buf = getmsg(rfd);
			vers = Integer.parseInt(gets(buf));
			if (vers != 1 || buf.length > 4)
				throw new LocalAuthErr("incompatible authentication protocol");
			if (info == null)
				throw new LocalAuthErr("no authentication information");
			if (info.p == null)
				throw new LocalAuthErr("missing diffie hellman mod");
			if (info.alpha == null)
				throw new LocalAuthErr("missing diffie hellman base");
			if (info.mysk == null || info.mypk == null || info.cert == null
					|| info.spk == null) // could check key details
				throw new LocalAuthErr("invalid authentication information");
			if (info.p.compareTo(BigInteger.ZERO) <= 0)
				throw new LocalAuthErr("-ve modulus");

			low = info.p.shiftRight(info.p.bitLength() / 4);
			r0 = rand(low, info.p);
			alphar0 = info.alpha.modPow(r0, info.p);
			sendmsg(wfd, bytes(b64(alphar0)));
			sendmsg(wfd, bytes(info.cert.text()));
			sendmsg(wfd, bytes(info.mypk.text()));

			alphar1 = s2big(gets(getmsg(rfd)));
			if (info.p.compareTo(alphar1) <= 0)
				throw new LocalAuthErr("implausible parameter value");
			if (alphar0.compareTo(alphar1) == 0)
				throw new LocalAuthErr("possible replay attack");
			hiscert = new Certificate(gets(getmsg(rfd)));
			hispkbuf = getmsg(rfd);
			hispk = new InfPublicKey(gets(hispkbuf));
			if (!verify(info.spk, hiscert, hispkbuf))
				throw new LocalAuthErr("pk doesn't match certificate");
			if (hiscert.exp != 0 && hiscert.exp <= now())
				throw new LocalAuthErr("certificate expired");

			alphabuf = bytes(b64(alphar0) + b64(alphar1));
			alphacert = sign(info.mysk, 0, alphabuf);
			sendmsg(wfd, bytes(alphacert.text()));
			alphacert = new Certificate(gets(getmsg(rfd)));
			alphabuf = bytes(b64(alphar1) + b64(alphar0));
			if (!verify(hispk, alphacert, alphabuf))
				throw new LocalAuthErr("signature did not match pk");

			alphar0r1 = alphar1.modPow(r0, info.p);
			secret = trim0(alphar0r1.toByteArray());

			sendmsg(wfd, bytes("OK"));
		} catch (IOException e) {
			throw new LocalAuthErr("i/o error: " + e.getMessage()); // could
																	// distinguish
																	// a few
																	// cases
		} catch (InvalidCertificateException e) {
			senderrmsg(wfd, "remote: " + e.getMessage());
			throw e;
		} catch (InvalidKeyException e) {
			senderrmsg(wfd, "remote: " + e.getMessage());
			throw e;
		} catch (NoSuchAlgorithmException e) {
			String msg = "unsupported algorithm: " + e.getMessage();
			senderrmsg(wfd, "remote: " + msg);
			throw new AuthenticationException(msg);
		} catch (LocalAuthErr e) {
			senderrmsg(wfd, "remote: " + e.getMessage());
			throw e;
		} catch (RemoteAuthErr e) {
			senderrmsg(wfd, "missing your authentication data"); // strange but
																	// true
			throw new AuthenticationException(e.getMessage());
		}
		try {
			String s;
			do {
				s = gets(getmsg(rfd));
			} while (!s.equals("OK"));
		} catch (IOException e) {
			throw new AuthenticationException("i/o error: " + e.getMessage());
		}
		return new AuthResult(new Authinfo(null, hispk, hiscert, info.spk,
				info.alpha, info.p), secret);
	}

	private static int now() {
		return (int) ((new Date()).getTime() / 1000);
	}

	static SecureRandom prng;

	public static final BigInteger rand(BigInteger p, BigInteger q)
			throws NoSuchAlgorithmException, IllegalArgumentException {
		if (prng == null) // race is rare and doesn't matter
			prng = SecureRandom.getInstance("SHA1PRNG");
		if (p.compareTo(q) > 0) {
			BigInteger t = p;
			p = q;
			q = t;
		}
		BigInteger diff = q.subtract(p);
		BigInteger two = BigInteger.ONE.add(BigInteger.ONE);
		if (diff.compareTo(two) < 0)
			throw new IllegalArgumentException("range must be at least two");
		int l = diff.bitLength();
		BigInteger t = BigInteger.ONE.shiftLeft(l);
		l = (l + 7) & ~7; // nearest byte
		BigInteger slop = t.mod(diff);
		BigInteger r;
		do {
			byte[] buf = new byte[l];
			prng.nextBytes(buf);
			r = new BigInteger(1, buf);
		} while (r.compareTo(slop) < 0);
		return r.mod(diff).add(p);
	}

	public final void setlinecrypt(ReadableByteChannel rfd,
			WritableByteChannel wfd, String role, String[] algs)
			throws IOException {
		String alg;

		if (role.equals("client")) {
			if (algs != null && algs.length > 0)
				alg = algs[0];
			else
				alg = "none"; // alg = "md5/rc4_256"; // no idea how to make use
								// of SSL without its handshake
			sendmsg(wfd, bytes(alg));
		} else if (role.equals("server")) {
			try {
				alg = gets(getmsg(rfd));
			} catch (RemoteAuthErr e) {
				throw new IOException("remote: " + e.getMessage()); // can't
																	// happen
			}
			if (alg != "none")
				throw new IOException("unsupported algorithm: " + alg);
		} else
			throw new IOException("invalid role: " + role);
	}

	public final AuthResult auth(ReadableByteChannel rfd,
			WritableByteChannel wfd, String role, Authinfo info, String[] algs)
			throws AuthenticationException, IOException {
		AuthResult a;
		a = basicauth(rfd, wfd, info);
		setlinecrypt(rfd, wfd, role, algs);
		return a;
	}

	public static final String b64(BigInteger b) {
		return base64.enc(b.toByteArray()); // toByteArray can add a leading
											// zero if top byte has top bit set
	}

	public static final BigInteger s2big(String s) {
		return new BigInteger(1, base64.dec(s)); // note decoded value is
													// magnitude (unsigned)
	}

	public static final String[] tokenize(String s, String delim) {
		StringTokenizer st = new StringTokenizer(s, delim);
		String[] a = new String[st.countTokens()];
		for (int i = 0; i < a.length; i++)
			a[i] = st.nextToken();
		return a;
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

	public static final byte[] trim0(byte[] a) {
		if (a.length == 0 || a[0] != 0)
			return a;
		for (int i = 0; i < a.length; i++)
			if (a[i] != 0) {
				byte[] ta = new byte[a.length - i];
				System.arraycopy(a, i, ta, 0, ta.length);
				return ta;
			}
		return a;
	}

	public final void dump(byte[] b) {
		String s = b.length + ":";
		int n = 0;
		for (int i = 0; i < b.length && ++n < 100; i++)
			s += " " + Integer.toString(b[i] & 0xFF, 16);
		if (n == 100)
			s += "...";
		System.out.println(s);
	}

	public static final void dump(ByteBuffer b) {
		String s = b.remaining() + ":";
		int n = 0;
		for (int i = b.position(); i < b.limit() && ++n < 100; i++)
			s += " " + Integer.toString(b.get(i) & 0xFF, 16);
		if (n == 100)
			s += "...";
		System.out.println(s);
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

	private static final BigInteger rsaencrypt(RSAPublicKey pk, BigInteger data) {
		return data.modPow(pk.getPublicExponent(), pk.getModulus());
	}

	public static final void sendmsg(WritableByteChannel fd, byte[] data)
			throws IOException {
		ByteBuffer b = ByteBuffer.allocate(5 + data.length);
		b.put(bytes(pad(Integer.toString(data.length), 4) + "\n"));
		b.put(data);
		b.flip();
		fd.write(b);
	}

	public static final void senderrmsg(WritableByteChannel fd, String s) {
		try {
			byte[] a = bytes(s);
			ByteBuffer b = ByteBuffer.allocate(5 + a.length);
			b.put(bytes("!" + pad(Integer.toString(a.length), 3) + "\n"));
			b.put(a);
			b.flip();
			fd.write(b);
		} catch (Exception e) {
		} // we don't care if it doesn't get there; we're done
	}

	public static final byte[] getmsg(ReadableByteChannel fd)
			throws IOException, RemoteAuthErr {
		ByteBuffer num;
		int i, n;

		num = ByteBuffer.allocate(5);
		fillbuf(fd, num);
		if (num.get(4) != (byte) '\n')
			throw new IOException("bad message syntax");
		boolean iserr = false;
		if (num.get(0) == (byte) '!') {
			iserr = true;
			i = 1;
		} else
			i = 0;
		for (n = 0; i < 4; i++)
			n = n * 10 + (num.get(i) - '0');
		if (n < 0 || n > Maxmsg)
			throw new IOException("message syntax");
		ByteBuffer z = ByteBuffer.allocate(n);
		fillbuf(fd, z);
		if (iserr)
			throw new RemoteAuthErr(gets(z));
		return z.array();
	}

	private static final void fillbuf(ReadableByteChannel fd, ByteBuffer b)
			throws IOException {
		while (b.remaining() > 0 && fd.read(b) > 0) {
			/* skip */
		}
		b.flip();
		if (b.remaining() != b.capacity())
			throw new IOException("message truncated");
	}

	private static final String gets(byte[] b) {
		try {
			return new String(b, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling";
		}
	}

	private static final String gets(ByteBuffer b) {
		byte[] a = new byte[b.remaining()];
		b.get(a);
		return gets(a);
	}

	public final Certificate sign(InfPrivateKey sk, int exp, byte a[])
			throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		BigInteger sig, digest;
		sha1.update(a);
		// add signer's name and expiration to hash
		sha1.update(bytes(sk.owner + " " + exp)); // "%s %d"
		digest = new BigInteger(1, sha1.digest());
		sig = rsadecrypt(digest, sk.sk);
		return new Certificate("rsa", "sha1", sk.owner, exp, sig);
	}

	public final boolean verify(InfPublicKey pk, Certificate c, byte a[])
			throws NoSuchAlgorithmException {
		if (!c.sa.equals("rsa") || !c.ha.equals("sha1") && !c.ha.equals("sha"))
			return false;
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		sha1.update(a);
		sha1.update(bytes(c.signer + " " + c.exp));
		return rsaverify(new BigInteger(1, sha1.digest()), c.rsa,
				(RSAPublicKey) pk.pk);
	}

	public static final BigInteger rsadecrypt(BigInteger n, PrivateKey rsa1) {
		RSAPrivateCrtKey rsa = (RSAPrivateCrtKey) rsa1;
		BigInteger p, q, v1, v2;
		p = rsa.getPrimeP();
		v1 = n.mod(p);
		q = rsa.getPrimeQ();
		v2 = n.mod(q);
		v1 = v1.modPow(rsa.getPrimeExponentP(), p);
		v2 = v2.modPow(rsa.getPrimeExponentQ(), q);
		// out = v1 + p*((v2-v1)*c2 mod q)
		return v2.subtract(v1).multiply(rsa.getCrtCoefficient()).mod(q)
				.multiply(p).add(v1);
	}

	public static final boolean rsaverify(BigInteger m, BigInteger sig,
			RSAPublicKey key) {
		return rsaencrypt(key, sig).equals(m);
	}
}
