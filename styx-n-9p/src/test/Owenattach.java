package test;

/*
 * test connection to Owen (reattach)
 *	Copyright © 2006 Vita Nuova Holdings Limited
 */

import com.vitanuova.owen.Job;
import com.vitanuova.owen.JobException;
import com.vitanuova.owen.Scheduler;
import com.vitanuova.owen.SchedulerException;

public class Owenattach {

	public static void main(String[] args) throws Exception {
		if (args.length < 3)
			error("usage: Owentest net!address!infsched dir id [certfile]");
		String certfile = "rsacert";
		if (args.length > 3) {
			certfile = args[3];
			if (certfile.equals("-"))
				certfile = null;
		}
		Scheduler sched = new Scheduler();
		try {
			sched.connect(args[0], certfile, null, "");
			Job job = new Job(sched);
			String dir = job.attach(args[1], args[2]);
			System.out.println("job: " + dir + " " + job.uniqueID());
			Job.Status js = job.monitor();
			System.out.println("status: total=" + js.total + ", complete="
					+ js.complete + ", running=" + js.running);
		} catch (SchedulerException e) {
			System.out.println("exception during connect: " + e);
			e.printStackTrace();
			error("die");
		} catch (JobException e) {
			System.out.println("exception during job start: " + e);
			e.printStackTrace();
			error("die");
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
