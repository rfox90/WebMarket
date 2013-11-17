package com.survivorserver.Dasfaust.WebMarket.protocol;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Request {
	
	public Object data;
	public int req;
	public ViewerMeta meta;
	
	public Request() {}
	
	public static Map<String, Object> serialize() {
		Map<String, Object> fields = new HashMap<String, Object>();
		Request req = new Request();
		try {
			for (Field field : Request.class.getDeclaredFields()) {
				Object ob = field.get(req);
				fields.put(field.getName(), ob == null ? "" : ob);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return fields;
	}
}