package com.survivorserver.Dasfaust.WebMarket.protocol;

import java.util.HashMap;
import java.util.Map;

public enum ViewType {
	LISTINGS(0),
	LISTINGS_OWNED(1),
	MAIL(2),
	CREATE_FROM_INV(3),
	CREATE_FROM_MAIL(4),
	NONE(99);
	
	private final int value;
	
	ViewType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
	public boolean Compare(int i){return value == i;}
	public static ViewType fromInt(int value) {
		ViewType[] As = ViewType.values();
        for(int i = 0; i < As.length; i++)
        {
            if(As[i].Compare(value))
                return As[i];
        }
        return ViewType.NONE;
	}

	public static Map<String,String> serialize() {
		ViewType[] As = ViewType.values();
		Map<String,String> map = new HashMap<String,String>();
		for(int i = 0; i< As.length; i++) {
			String val = As[i].name();
			map.put(val.toLowerCase(), val);
		}
		return map;
	}
}
