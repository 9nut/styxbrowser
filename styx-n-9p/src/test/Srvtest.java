package test;

/*
 * authentication and styx service test
 *	Copyright © 2005 Vita Nuova Holdings Limited
 */

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import com.vitanuova.auth.Keyring;
import com.vitanuova.lib.Dial;
import com.vitanuova.styx.Dir;
import com.vitanuova.styx.Styx;
import com.vitanuova.styx.StyxClient;

public class Srvtest {

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
			System.out.println("exception during auth: " + e);
			e.printStackTrace();
			error("die");
		}

		try {
			StyxClient client = new StyxClient();
			StyxClient.Conn conn = client.new Conn(dfd, dfd);
			StyxClient.FS fs = conn.attach(null, "forsyth", "");
			if (fs == null)
				error("can't attach: " + client.errstr());
			StyxClient.FD fd = fs.open("/usr/world/this/is/text", Styx.OREAD);
			if (fd == null) {
				System.out.println("can't open ...: " + client.errstr());
				// return;
			}
			fd = fs.open("/usr/inferno/namespace", Styx.OREAD);
			if (fd == null)
				error("can't open /usr/inferno/namespace: " + client.errstr());
			System.out.println("name = " + fd.path());
			fd.close();
			fd = fs.open("/LICENCE", Styx.OREAD);
			if (fd == null)
				error("can't open /LICENCE: " + client.errstr());
			System.out.println("name = " + fd.path());
			ByteBuffer b;
			while ((b = fd.read(8192)) != null && b.remaining() != 0)
				System.out.print(S(Styx.bytes(b)));
			fd.close();
			fd = fs.create("/tmp/burble", Styx.OWRITE, 0666);
			if (fd == null)
				error("can't create /tmp/burble: " + client.errstr());
			System.out.println("name = " + fd.path());
			fd.write(ByteBuffer.wrap(Styx.bytes("hello world!\n")));
			fd.close();
			fd = fs.open("/usr", Styx.OREAD);
			if (fd == null)
				error("can't open /usr: " + client.errstr());
			Dir[] db;
			while ((db = fd.dirread()) != null) {
				for (int i = 0; i < db.length; i++)
					System.out.println(db[i]);
			}
			fd.close();
			System.out.println("cwd = " + fs.getwd());
			if (!fs.chdir("/usr/inferno"))
				error("can't chdir to /usr/inferno: " + client.errstr());
			System.out.println("chdir' = " + fs.getwd());
			fd = fs.open("LICENCE", Styx.OREAD);
			if (fd == null)
				error("can't open LICENCE: " + client.errstr());
			System.out.println("name = " + fd.path());
			fd.close();
		} catch (Exception e) {
			System.out.println("exception during styx: " + e);
			e.printStackTrace();
		}
	}

	public static String S(byte[] a) {
		try {
			return new String(a, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling";
		}
	}

	public static void error(String s) {
		System.err.println("Test: " + s);
		System.exit(999);
	}
}
