package org.aquila.test.apps;
import org.aquila.crypto.BrokenMD160;

import com.google.common.hash.HashCode;

@SuppressWarnings("deprecation")
public class brokenmd160 {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("usage: broken-md160 <hex>\noutputs: hex");
			System.exit(1);
		}

		byte[] raw = HashCode.fromString(args[0]).asBytes();
		BrokenMD160 brokenMD160 = new BrokenMD160();
		byte[] digest = brokenMD160.digest(raw);

		System.out.println(HashCode.fromBytes(digest).toString());
	}

}
