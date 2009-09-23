package com.vitanuova.owen;

import java.util.Vector;

import com.vitanuova.lib.Sexprs;

//	(job ...)
//	 (task name [arg ...])
//	 (output kind) kind="data" | "bundle"
//	 (file (path <path>) [(name <name>)][(size N)][(split "lines"|"files")][(kind "data"|"stdin"|"bundle")])
//	 (value <name> <value>)
//	  <value> ::= (range [from] to [by]) | (for <value> ...)
//	 (script <name> [arg ...])

public class Jobspec {
	String task;
	String[] taskargs;
	String outkind;
	Vector<Value> values = new Vector<Value>();
	Vector<File> files = new Vector<File>();
	String script;
	String[] scriptargs;

	public Jobspec(String task, String[] taskargs) {
		this.task = task;
		this.taskargs = taskargs;
	}

	public void setoutputkind(String outkind) throws JobException {
		if (!outkind.equals("data") && !outkind.equals("bundle"))
			throw new JobException("invalid output kind in spec: " + outkind);
		this.outkind = outkind;
	}

	public void setscript(String script, String[] scriptargs) {
		this.script = script;
		this.scriptargs = scriptargs;
	}

	public class File {
		String path;
		String name;
		long size;
		String split;
		String kind;

		File(String path, String name, long size, String split, String kind)
				throws JobException {
			if (kind != null)
				if (!kind.equals("data") && !kind.equals("stdin")
						&& !kind.equals("bundle"))
					throw new JobException("invalid file `kind': " + kind);
			if (split != null)
				if (!split.equals("lines") && !split.equals("files"))
					throw new JobException("invalid split type: " + split);
			this.path = path;
			this.name = name;
			this.size = size;
			this.split = split;
			this.kind = kind;
		}

		@Override
		public String toString() {
			StringBuffer os = new StringBuffer();
			os.append("(file (path " + Sexprs.quote(path) + ")");
			if (name != null)
				os.append("(name " + Sexprs.quote(name) + ")");
			if (size != 0)
				os.append("(size \"" + size + "\")");
			if (split != null)
				os.append("(split " + Sexprs.quote(split) + ")");
			if (kind != null)
				os.append("(kind " + Sexprs.quote(kind) + ")");
			os.append(')');
			return new String(os);
		}
	}

	abstract class Value {
		String name;

		Value(String name) {
			this.name = name;
		}

		@Override
		public abstract String toString();
	}

	class Range extends Value {
		long from;
		long to;
		long by;

		Range(String name, long to) {
			super(name);
			from = 0;
			this.to = to;
			by = 1;
		}

		Range(String name, long from, long to) {
			super(name);
			this.from = from;
			this.to = to;
			by = 1;
		}

		Range(String name, long from, long to, long by) {
			super(name);
			this.from = from;
			this.to = to;
			this.by = by;
		}

		@Override
		public String toString() {
			String sf = "", st = "", sb = "";
			if (from != 0)
				sf = " \"" + from + "\"";
			if (by != 1)
				sb = " \"" + by + "\"";
			return "(value " + Sexprs.quote(name) + " (range" + sf + " \"" + to
					+ "\"" + sb + "))";
		}
	}

	class Enum extends Value {
		String[] items;

		Enum(String name, String[] items) {
			super(name);
			this.items = items;
		}

		@Override
		public String toString() {
			StringBuffer os = new StringBuffer();
			os.append("(value ");
			os.append(Sexprs.quote(name));
			os.append(" (for");
			if (items != null)
				for (int i = 0; i < items.length; i++)
					os.append(" " + Sexprs.quote(items[i]));
			os.append("))");
			return new String(os);
		}
	}

	public void addrange(String name, long to) {
		values.add(new Range(name, to));
	}

	public void addrange(String name, long from, long to) {
		values.add(new Range(name, from, to));
	}

	public void addrange(String name, long from, long to, long by) {
		values.add(new Range(name, from, to, by));
	}

	public void addenum(String name, String[] items) {
		values.add(new Enum(name, items));
	}

	public void addfile(String path, String kind) throws JobException {
		files.add(new File(path, null, 0, null, kind));
	}

	public void addfile(String path, String name, String kind)
			throws JobException {
		files.add(new File(path, name, 0, null, kind));
	}

	public void addsplitfile(String path, String kind, String how)
			throws JobException {
		files.add(new File(path, null, 0, how, kind));
	}

	public void addsplitfile(String path, String name, String kind, String how)
			throws JobException {
		files.add(new File(path, name, 0, how, kind));
	}

	@Override
	public String toString() {
		StringBuffer os = new StringBuffer();
		os.append("(job (task " + Sexprs.quote(task));
		if (taskargs != null)
			for (int i = 0; i < taskargs.length; i++)
				os.append(" " + Sexprs.quote(taskargs[i]));
		os.append(')');
		if (outkind != null)
			os.append("(output " + outkind + ")");
		if (script != null) {
			os.append("(script " + Sexprs.quote(script));
			if (scriptargs != null)
				for (int i = 0; i < scriptargs.length; i++)
					os.append(" " + Sexprs.quote(scriptargs[i]));
			os.append(')');
		}
		for (int i = 0; i < values.size(); i++) {
			Value v = values.elementAt(i);
			os.append(v.toString());
		}
		for (int i = 0; i < files.size(); i++) {
			File f = files.elementAt(i);
			os.append(f.toString());
		}
		os.append(")");
		return new String(os);
	}
}
