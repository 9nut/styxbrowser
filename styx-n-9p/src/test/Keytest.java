package test;

/*
 * Inferno public key authentication test
 */

import java.io.FileInputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import com.vitanuova.auth.Keyring;
import com.vitanuova.lib.Dial;

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

public class Keytest {

	static public class Party extends Thread {
		ReadableByteChannel rfd;
		WritableByteChannel wfd;
		String certfile;

		Party(ReadableByteChannel rfd, WritableByteChannel wfd, String file) {
			this.rfd = rfd;
			this.wfd = wfd;
			certfile = file;
		}

		@Override
		public final void run() {
			try {
				Keyring keyring = new Keyring();
				FileInputStream certfd = new FileInputStream(certfile);
				Keyring.Authinfo info = keyring.readauthinfo(Channels
						.newChannel(certfd));
				Keyring.AuthResult out = keyring.basicauth(rfd, wfd, info);
				keyring.dump(out.secret);
			} catch (Exception e) {
				System.out.println("party exception: " + e);
				e.printStackTrace();
			}
		}
	}

	public static void testauth(String certfile) throws Exception {
		try {
			Pipe c1 = Pipe.open();
			Pipe c2 = Pipe.open();
			Party p1 = new Party(c1.source(), c2.sink(), certfile);
			Party p2 = new Party(c2.source(), c1.sink(), certfile);
			p1.start();
			p2.start();
		} catch (Exception e) {
			System.out.println("exception: " + e);
		}
	}

	public static void main(String[] args) throws Exception {
		String certfile = "rsacert";
		if (args.length > 0)
			certfile = args[0];
		SocketChannel dfd = Dial.dial("tcp!200.1.1.67!9989", null);
		if (dfd == null)
			error("can't dial: " + Dial.errstr());
		try {
			Keyring keyring = new Keyring();
			FileInputStream certfd = new FileInputStream(certfile);
			Keyring.Authinfo info = keyring.readauthinfo(Channels
					.newChannel(certfd));
			Keyring.AuthResult a = keyring.auth(dfd, dfd, "client", info,
					new String[] { "none" });
			keyring.dump(a.secret);
		} catch (Exception e) {
			System.out.println("exception: " + e);
			e.printStackTrace();
		}
	}

	public static void error(String s) {
		System.err.println("Test: " + s);
		System.exit(999);
	}
}
