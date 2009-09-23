package test;

import com.vitanuova.lib.Base64;

public class D64 {
	public static void main(String[] args) {
		Base64 encoding = new Base64();
		byte[] b = encoding.dec(args[0]);
		System.out.println(b.length + ": " + new String(b));
	}
}
