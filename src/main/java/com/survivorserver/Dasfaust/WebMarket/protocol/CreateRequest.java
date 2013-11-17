package com.survivorserver.Dasfaust.WebMarket.protocol;

public class CreateRequest {

	public int id;
	public int amount;
	public double price;
	
	public CreateRequest(int id, int amount, double price) {
		this.id = id;
		this.amount = amount;
		this.price = price;
	}
}
