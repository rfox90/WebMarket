package com.survivorserver.Dasfaust.WebMarket;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;

import com.survivorserver.Dasfaust.WebMarket.mojang.profiles.HttpProfileRepository;
import com.survivorserver.Dasfaust.WebMarket.mojang.profiles.Profile;
import com.survivorserver.Dasfaust.WebMarket.mojang.profiles.ProfileCriteria;
import com.survivorserver.GlobalMarket.SQL.Database;
import com.survivorserver.GlobalMarket.SQL.MarketResult;

public class AuthManager {

	private WebMarket web;
	private Map<String, String> forumAuth;
	private Map<String, Profile> profileCache;
	private Database db;
	
	public AuthManager(WebMarket web) {
		this.web = web;
		forumAuth = new ConcurrentHashMap<String, String>();
		profileCache = new ConcurrentHashMap<String, Profile>();
		db = new Database(web.log, "", "", "", "users", web.getDataFolder().getAbsolutePath());
		db.connect();
		try {
			db.createStatement("CREATE TABLE IF NOT EXISTS market_users (player TINYTEXT NOT NULL, password MEDIUMTEXT NOT NULL)").execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void addUser(String user, String session) {
		if (forumAuth.containsKey(user)) {
			forumAuth.remove(user);
		}
		forumAuth.put(user, session);
	}
	
	public synchronized boolean check(String user, String password) {
		if (forumAuth.containsKey(user)) {
			return forumAuth.get(user).equals(password);
		}
		try {
			MarketResult res = db.createStatement("SELECT * FROM market_users WHERE player = ?").setString(user).query();
			if (res.next()) {
				if (decodePassword(res.getString("password")).equals(password)) {
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean store(String user, String password) {
		try {
			boolean exists = false;
			if (db.createStatement("SELECT * FROM market_users WHERE player = ?").setString(user).query().next()) {
				exists = true;
				db.createStatement("DELETE FROM market_users WHERE player = ?").setString(user).execute();
			}
			db.createStatement("INSERT INTO market_users (player, password) VALUES (?, ?)").setString(user).setString(encodePassword(password)).execute();
			return exists;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean canAuthorize(String address) {
		return address.split(":")[0].equals("/" + web.getConfig().getString("server.forumAuth.allowFrom"));
	}
	
	private String encodePassword(String pass) {
		return Base64.encodeBase64String(pass.getBytes());
	}
	
	private String decodePassword(String encoded) {
		return new String(Base64.decodeBase64(encoded.getBytes()));
	}
	
	public Profile getProfile(String player) {
		if (profileCache.containsKey(player.toLowerCase())) {
			return profileCache.get(player.toLowerCase());
		}
		HttpProfileRepository repo = new HttpProfileRepository();
		Profile[] profiles = repo.findProfilesByCriteria(new ProfileCriteria(player, "Minecraft"));
		if (profiles == null || profiles.length == 0) {
			return null;
		}
		Profile profile = profiles[0];
		profileCache.put(player.toLowerCase(), profile);
		return profile;
	}
}
