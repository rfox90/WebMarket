package com.survivorserver.Dasfaust.WebMarket.protocol;

import java.util.List;

public class ItemList {

	public static int LISTINGS = 0;
	public static int LISTINGS_OWNED = 1;
	public static int MAIL = 2;
	public static int ITEMS_FOR_CREATION = 3;
	
	public int type;
	public int total;
	public int totalPossible = 0;
	public List<WebItem> currentPage;
	
	public ItemList(int type, int total, List<WebItem> currentPage) {
		this.type = type;
		this.total = total;
		this.currentPage = currentPage;
	}
	
	public ItemList(int type, int total, int totalPossible, List<WebItem> currentPage) {
		this.type = type;
		this.total = total;
		this.totalPossible = totalPossible;
		this.currentPage = currentPage;
	}
}