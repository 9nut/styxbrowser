/*
 * submit a job to Owen
 *	basic version
 *	Copyright © 2005 Vita Nuova Holdings Limited
 */
package com.vitanuova.owen;

import java.util.StringTokenizer;

import com.vitanuova.lib.Strings;
import com.vitanuova.styx.Styx;
import com.vitanuova.styx.StyxClient;

// requests:
// attach to scheduler
// open(.../admin/clone)
//	prereq ...
//	load taskgenerator arg ...
//	start | stop | teardown | delete
//	priority high | low | <otherjobid>
// read job ID from ctl
// read status from monitor
// description file
// duration in ms
// error strings ...

public class Job {
	String uid;
	String dir;
	Scheduler sched;
	StyxClient.FD ctlfd;
	StyxClient.FD monfd;

	public class Status {
		public int total;
		public int complete;
		public int running;
		public int failed;
		public long datain;
		public long dataout;
		public int disconnected;
		public int duplicate;
		public long totaltime;

		Status() {
		}
	}

	public Job(Scheduler sched) {
		this.sched = sched;
	}

	public void load(String tasktype, String[] args) throws JobException {
		if (args == null)
			args = new String[0];
		String s = "load " + tasktype;
		if (args != null)
			for (int i = 0; i < args.length; i++)
				s += " " + Strings.quote(args[i]);
		ctl(s);
	}

	public void job(String sexpr) throws JobException {
		ctl("load job " + Strings.quote(sexpr));
	}

	public void job(Jobspec spec) throws JobException {
		job(spec.toString());
	}

	public void start() throws JobException {
		ctl("start");
	}

	public void stop() throws JobException {
		ctl("stop");
	}

	public void delete() throws JobException {
		ctl("delete");
	}

	public void ctl(String s) throws JobException {
		if (ctlfd == null)
			create();
		if (ctlfd.write(s) < 0)
			throw new JobException("ctl error: " + Strings.quote(s) + ":"
					+ sched.client.errstr());
	}

	public String path() {
		return dir;
	}

	public String uniqueID() {
		return uid;
	}

	public Status monitor() throws JobException {
		if (monfd == null) {
			monfd = sched.fs.open(dir + "/monitor", Styx.OREAD);
			if (monfd == null)
				throw new JobException("can't open scheduler's " + dir
						+ "/monitor: " + sched.client.errstr());
		}
		String s = monfd.reads(4096);
		if (s == null)
			return null;
		StringTokenizer st = new StringTokenizer(s, "()\"\n \t");
		if (st.countTokens() < 9)
			throw new JobException("unexpected format for monitor file");
		try {
			Status status = new Status();
			status.total = Integer.parseInt(st.nextToken());
			status.complete = Integer.parseInt(st.nextToken());
			status.running = Integer.parseInt(st.nextToken());
			status.failed = Integer.parseInt(st.nextToken());
			status.datain = Long.parseLong(st.nextToken());
			status.dataout = Long.parseLong(st.nextToken());
			status.disconnected = Integer.parseInt(st.nextToken());
			status.duplicate = Integer.parseInt(st.nextToken());
			status.totaltime = Long.parseLong(st.nextToken());
			return status;
		} catch (NumberFormatException e) {
			throw new JobException("unexpected content in monitor file");
		}
	}

	public String attach(String olddir, String oldid) throws JobException {
		if (ctlfd != null && dir != null && dir.equals(olddir))
			return dir;
		if (ctlfd != null || dir != null)
			throw new JobException("job already attached");
		StyxClient.FD cfd = sched.fs.open(olddir + "/ctl", Styx.ORDWR);
		if (cfd == null)
			throw new JobException("can't open control file for job directory "
					+ olddir + ": " + sched.client.errstr());
		String newuid = readid(olddir);
		if (!newuid.equals(oldid)) {
			cfd.close();
			throw new JobException("job directory " + olddir
					+ " has unique ID " + newuid + ", required " + oldid);
		}
		ctlfd = cfd;
		dir = olddir;
		uid = newuid;
		return dir;
	}

	public String create() throws JobException {
		if (ctlfd == null) {
			ctlfd = sched.fs.open("/admin/clone", Styx.ORDWR);
			if (ctlfd == null)
				throw new JobException("can't open scheduler's clone file: "
						+ sched.client.errstr());
		}
		if (dir != null)
			return dir;
		String ids = ctlfd.reads(128);
		if (ids == null)
			throw new JobException("can't read job directory number: "
					+ sched.client.errstr());
		try {
			int dirno = Integer.parseInt(ids);
		} catch (NumberFormatException e) {
			throw new JobException("invalid directory number in clone file: "
					+ ids);
		}
		dir = "/admin/" + ids;
		uid = readid(dir);
		return dir;
	}

	String readid(String dir) throws JobException {
		StyxClient.FD fd = sched.fs.open(dir + "/id", Styx.OREAD);
		if (fd == null)
			throw new JobException("cannot open " + dir + "/id: "
					+ sched.client.errstr());
		String uid = fd.reads(128);
		String err = sched.client.errstr();
		fd.close();
		if (uid == null)
			throw new JobException("cannot read unique ID from " + dir + ": "
					+ err);
		return uid;
	}

	public void close() {
		if (ctlfd != null) {
			ctlfd.close();
			ctlfd = null;
		}
		if (monfd != null) {
			monfd.close();
			monfd = null;
		}
		dir = null;
	}

	@Override
	protected void finalize() {
		close();
		if (sched != null)
			sched.close();
	}

}
