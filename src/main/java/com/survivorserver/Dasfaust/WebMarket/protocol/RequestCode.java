package com.survivorserver.Dasfaust.WebMarket.protocol;

public enum RequestCode {
	LOGIN(0),
	LOGIN_XENFORO(3),
	LOGOUT(1),
	UPDATE_VIEW(2),
	BUY(3),
	CANCEL(5),
	SEND(6),
	CREATE_LISTING(7),
	PICKUP(8),
	NOOP(99);
	private final int value;
	
	RequestCode(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
	public boolean Compare(int i){return value == i;}
	public static RequestCode fromInt(int value) {
		RequestCode[] As = RequestCode.values();
        for(int i = 0; i < As.length; i++)
        {
            if(As[i].Compare(value))
                return As[i];
        }
        return RequestCode.NOOP;
	}
	public String toString() {
		return "Request ID is "+this.getValue();
	}
}
