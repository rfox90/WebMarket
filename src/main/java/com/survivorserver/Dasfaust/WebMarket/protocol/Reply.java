package com.survivorserver.Dasfaust.WebMarket.protocol;

public class Reply {
	
	public int rep;
	public ViewerMeta meta;
	public Object data;
	
	public Reply() {}
	
	public Reply(int rep, ViewerMeta meta, Object data) {
		this.rep = rep;
		this.meta = meta;
		this.data = data;
	}
}
