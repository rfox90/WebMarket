package com.survivorserver.Dasfaust.WebMarket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {

	private WebMarket web;
	private Map<String, String> forumAuth;
	
	public AuthManager(WebMarket web) {
		this.web = web;
		forumAuth = new ConcurrentHashMap<String, String>();
	}
	
	public void addUser(String user, String session) {
		if (forumAuth.containsKey(user)) {
			forumAuth.remove(user);
		}
		forumAuth.put(user, session);
	}
	
	public boolean check(String user, String password) {
		// TODO regular authentication system
		if (forumAuth.containsKey(user)) {
			return forumAuth.get(user).equals(password);
		}
		return false;
	}
	
	public boolean canAuthorize(String address) {
		return address.split(":")[0].equals("/" + web.getConfig().getString("server.forumAuth.allowFrom"));
	}
}
