package com.survivorserver.Dasfaust.WebMarket.protocol;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class Protocol {

	// Reply types
	public static int REPLY_GENERAL_FAILURE = 0;
	public static int REPLY_GENERAL_SUCCESS = 1;
	public static int REPLY_UPDATE_VIEW = 2;
	public static int REPLY_NOTIFICATION = 4;
	public static int REPLY_TRANSACTION_FAILURE = 5;
	public static int REPLY_TRANSACTION_SUCCESS = 6;
	
	// Status codes
	public static int STATUS_INVALID_JSON = 0;
	public static int STATUS_LOGIN_EXPECTED = 1;
	public static int STATUS_INVALID_CREDENTIALS = 2;
	public static int STATUS_VIEWER_ALREADY_ACTIVE = 3;
	public static int STATUS_UPDATE_VIEWER = 4;
	public static int STATUS_LOGGED_OUT = 5;
	public static int STATUS_SERVER_ERROR = 6;
	public static int STATUS_TIMED_OUT = 7;
	public static int STATUS_UNKNOWN_REQUEST = 8;
	public static int STATUS_PLAYER_ONLINE = 9;
	public static int STATUS_NO_INVENTORY = 10;
	public static int STATUS_NOT_FOUND = 11;
	public static int STATUS_INSUFFICIENT_FUNDS = 12;
	public static int STATUS_BAD_REQUEST = 13;
	public static int STATUS_PLAYER_NOT_FOUND = 14;
	public static int STATUS_PRICE_TOO_HIGH = 15;
	public static int STATUS_SELLING_TOO_MANY_ITEMS = 16;
	public static int STATUS_ITEM_BACLKLISTED = 17;
	public static int STATUS_NOT_ENOUGH_OF_ITEM = 18;
	public static int STATUS_FULL_INVENTORY = 19;
	public static int STATUS_DISABLED_BY_SERVER = 20;
	
	// Request codes
	//replaced with RequestCode
	
	// View types
	//replaced with ViewType
	
	public Protocol() {}
	
	public static Map<String, Integer> serialize() {
		Map<String, Integer> fields = new HashMap<String, Integer>();
		Protocol prot = new Protocol();
		try {
			for (Field field : Protocol.class.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					fields.put(field.getName(), field.getInt(prot));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return fields;
	}
}
