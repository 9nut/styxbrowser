/*
 * connect to an Owen scheduler
 *	basic version
 *	Copyright © 2005 Vita Nuova Holdings Limited
 */
package com.vitanuova.owen;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import com.vitanuova.auth.Keyring;
import com.vitanuova.lib.Dial;
import com.vitanuova.styx.StyxClient;

public class Scheduler {

	SocketChannel dfd;
	StyxClient client;
	StyxClient.Conn conn;
	Keyring.Authinfo serverinfo;
	StyxClient.FS fs;

	public Scheduler() {
	}

	public StyxClient.FS connect(String address, String certfile,
			String[] algs, String attachname) throws SchedulerException {
		if (algs == null || algs.length == 0)
			algs = new String[] { "none" };
		if (attachname == null)
			attachname = "";
		SocketChannel dfd = Dial.dial(address, null);
		if (dfd == null)
			throw new SchedulerException("can't dial: " + address + ": "
					+ Dial.errstr());
		String user;
		if (certfile != null) {
			try {
				Keyring keyring = new Keyring();
				FileInputStream certfd = new FileInputStream(certfile);
				Keyring.Authinfo info = keyring.readauthinfo(Channels
						.newChannel(certfd));
				user = info.mypk.owner;
				Keyring.AuthResult a = keyring.auth(dfd, dfd, "client", info,
						algs);
				// secret currently is discarded
				serverinfo = a.info;
			} catch (Exception e) {
				throw new SchedulerException("can't authenticate: " + e);
			}
		} else
			user = "inferno";
		try {
			client = new StyxClient();
			conn = client.new Conn(dfd, dfd);
		} catch (Exception e) {
			throw new SchedulerException("can't prepare Styx connection: " + e);
		}
		// we currently assume Tauth not needed
		fs = conn.attach(null, user, attachname);
		if (fs == null)
			throw new SchedulerException(
					"can't attach to scheduler's name space: "
							+ client.errstr());
		return fs;
	}

	public void close() {
		if (fs != null) {
			fs.close();
			fs = null;
		}
		if (dfd != null) {
			try {
				dfd.close();
			} catch (IOException e) {
				; // don't care
			}
			dfd = null;
		}
	}

	@Override
	protected void finalize() {
		close();
	}
}
