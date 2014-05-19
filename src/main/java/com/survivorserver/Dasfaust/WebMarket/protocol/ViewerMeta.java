package com.survivorserver.Dasfaust.WebMarket.protocol;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ViewerMeta {
	
	public String name;
	public String password;
	public int viewType;
	public int page = 1;
	public int pageSize = 40;
	public double balance;
	public String balanceFriendly;
	public String search;
	public boolean isAdmin;
	public int totalSelling;
	public int totalListings;
	public int totalMail;
	public String world;
	
	public ViewerMeta() {}
	
	public static Map<String, Object> serialize() {
		Map<String, Object> fields = new HashMap<String, Object>();
		ViewerMeta meta = new ViewerMeta();
		try {
			for (Field field : ViewerMeta.class.getDeclaredFields()) {
				Object ob = field.get(meta);
				fields.put(field.getName(), ob == null ? "" : ob);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return fields;
	}
	public ViewType getType() {
		return ViewType.fromInt(this.viewType);
	}
	public String getWorld() {
		if(this.world != null) {
			return this.world;
		} 
		return "world";
	}
}
