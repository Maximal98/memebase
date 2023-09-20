package net.memebase.Auth;

import java.util.ArrayList;

public class User {
	public int id;
	public String name;
	public String hashedPassword;
	public byte[] salt;
	public ArrayList<AuthToken> validTokens;
}
