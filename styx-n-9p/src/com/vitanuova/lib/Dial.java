package com.vitanuova.lib;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.StringTokenizer;

public class Dial {

	public static ThreadLocal<String> lasterror = new ThreadLocal<String>();

	public final static String errstr() {
		return lasterror.get();
	}

	public final static void werrstr(String s) {
		lasterror.set(s);
	}

	static public final SocketChannel dial(String dest, String local) {
		StringTokenizer flds = new StringTokenizer(dest, "!");
		int n = flds.countTokens();
		if (n < 3) {
			werrstr("invalid network address for dial: " + dest);
			return null;
		}
		String net = flds.nextToken();
		String host = flds.nextToken();
		String svc = flds.nextToken();
		if (!net.equals("net") && !net.equals("tcp")) { // TO DO: "udp"
			werrstr("no network to host: " + dest);
			return null;
		}
		InetAddress[] addrs;
		try {
			addrs = InetAddress.getAllByName(host);
		} catch (UnknownHostException e) {
			werrstr("unknown host: " + host);
			return null;
		}
		int port = portno(svc);
		if (port < 0) {
			if (svc.equals("styx"))
				port = 6666;
			else if (svc.equals("9fs"))
				port = 564;
			else if (svc.equals("infsched"))
				port = 6678;
			else {
				werrstr("can't translate service: " + svc);
				return null;
			}
		}
		for (int i = 0; i < addrs.length; i++) {
			InetSocketAddress a = new InetSocketAddress(addrs[i], port);
			try {
				SocketChannel sc = SocketChannel.open(a);
				if (sc != null)
					return sc;
			} catch (java.io.IOException e) {
				werrstr(e.getMessage());
			}
		}
		return null;
	}

	static public final String netmkaddr(String addr, String net, String svc) {
		StringTokenizer flds = new StringTokenizer(addr, "!");
		int n = flds.countTokens();
		if (n <= 1) {
			if (svc == null)
				return net + "!" + addr;
			return net + "!" + addr + "!" + svc;
		}
		if (svc != null && n < 2)
			return addr + "!" + svc;
		return addr;
	}

	static private int portno(String s) {
		int n = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (!(c >= '0' && c <= '9'))
				return -1;
			n = n * 10 + (c - '0');
		}
		return n;
	}
}
