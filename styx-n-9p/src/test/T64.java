package test;

import com.vitanuova.lib.Base64;

public class T64 {
	public static void main(String[] args) {
		Base64 encoding = new Base64();
		String s = encoding.enc(args[0].getBytes());
		System.out.println(s.length() + ": " + s);
	}
}
