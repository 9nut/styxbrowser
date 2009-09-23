package com.vitanuova.styxbrowser;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import com.vitanuova.lib.Dial;
import com.vitanuova.styx.Dir;
import com.vitanuova.styx.Styx;
import com.vitanuova.styx.StyxClient;

public class AndroidStyx {

	private StyxClient client;
	private StyxClient.FS fs;

	public AndroidStyx(String address) {
		client = new StyxClient();

		SocketChannel dfd = Dial.dial(address, null);
		if (dfd == null)
			error("can't dial: " + Dial.errstr());

		StyxClient.Conn conn = client.new Conn(dfd, dfd);
		fs = conn.attach(null, "", "");
		if (fs == null)
			error("can't attach: " + client.errstr());
	}

	public String file(String file) {
		StyxClient.FD fd = fs.open(file, Styx.OREAD);
		if (fd == null)
			error("can't open " + file + ": " + client.errstr());
		ByteBuffer b;
		String out = "";
		while ((b = fd.read(4096)) != null && b.remaining() != 0)
			out = out + S(com.vitanuova.styx.Styx.bytes(b));
		fd.close();
		return out;
	}

	public String[] dir(String dir) {
		/* Function fs.open doesn't like "/", but accept "/.". */
		if ("/".equals(dir))
			dir = dir + ".";

		StyxClient.FD fd = fs.open(dir, Styx.OREAD);
		if (fd == null)
			error("can't open " + dir + ": " + client.errstr());

		String[] tmp = new String[4096];

		Dir[] db;

		int i = 0;
		tmp[i] = "../";
		i++;

		while ((db = fd.dirread()) != null) {
			for (; i < db.length && i < 4096; i++)
				if (db[i - 1].modefmt().startsWith("d"))
					tmp[i] = db[i - 1].name + "/";
				else
					tmp[i] = db[i - 1].name;
		}

		fd.close();

		String ls[] = new String[i];

		for (int j = 0; j < i; j++)
			ls[j] = tmp[j];

		Arrays.sort(ls);

		return ls;
	}

	public void create(String path) {
		if (!"".equals(path)) {
			StyxClient.FD fd = fs.create(path, Styx.OWRITE, 0666);
			if (fd == null)
				error("can't create " + path + " :" + client.errstr());
		}
	}

	private String S(byte[] a) {
		try {
			return new String(a, "UTF-8");
		} catch (java.io.UnsupportedEncodingException e) {
			return "Egosling";
		}
	}

	private void error(String s) {
		System.err.println("Test: " + s);
		System.exit(999);
	}
}
