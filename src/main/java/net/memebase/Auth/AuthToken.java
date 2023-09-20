package net.memebase.Auth;

import java.security.SecureRandom;

public class AuthToken {
	public String uniqueString;
	public int associatedUser;
	public AuthToken() {
		uniqueString = "";
		associatedUser = 0;
	}
	public String genUnique() {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[20];
		random.nextBytes( bytes );
		uniqueString = new String( bytes );
		return uniqueString;
	}
}
