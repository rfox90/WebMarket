package com.survivorserver.Dasfaust.WebMarket;

import com.survivorserver.Dasfaust.WebMarket.netty.WebSocketSession;
import com.survivorserver.Dasfaust.WebMarket.protocol.CreateRequest;
import com.survivorserver.Dasfaust.WebMarket.protocol.Protocol;
import com.survivorserver.Dasfaust.WebMarket.protocol.Reply;
import com.survivorserver.Dasfaust.WebMarket.protocol.SendRequest;
import com.survivorserver.Dasfaust.WebMarket.protocol.ViewerMeta;
import com.survivorserver.GlobalMarket.Listing;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.MarketStorage;

public class WebViewer {

	private ViewerMeta meta;
	private WebSocketSession session;

	public WebViewer(WebSocketSession session, ViewerMeta meta) {
		this.meta = meta;
		this.session = session;
	}

	public String getName() {
		return meta.name;
	}

	public WebSocketSession getSession() {
		return session;
	}

	public int getPage() {
		return meta.page;
	}

	public int getPageSize() {
		return meta.pageSize;
	}
	
	public ViewerMeta getMeta() {
		return meta;
	}

	public Reply onRequestListings(InterfaceHandler handler) {
		return new Reply(Protocol.REPLY_UPDATE_VIEW, meta, handler.getListings(meta));
	}
	
	public Reply onLogout() {
		session.getContext().disconnect();
		return new Reply(Protocol.REPLY_GENERAL_SUCCESS, null, Protocol.STATUS_LOGGED_OUT);
	}
	
	public Reply onRequestListingsOwned(InterfaceHandler handler) {
		return new Reply(Protocol.REPLY_UPDATE_VIEW, meta, handler.getOwnedListings(meta));
	}
	
	public Reply onRequestMail(InterfaceHandler handler) {
		return new Reply(Protocol.REPLY_UPDATE_VIEW, meta, handler.getMail(meta));
	}
	
	public Reply onRequestListingsCreate(InterfaceHandler handler) {
		return meta.viewType == 3 ? handler.getItemsForCreation(meta) : new Reply(Protocol.REPLY_UPDATE_VIEW, meta, handler.getMailForCreation(meta));
	}
	
	public void notify(String message) {
		session.send(new Reply(Protocol.REPLY_NOTIFICATION, meta, message));
	}
	
	public void updateMeta(Market market, ViewerMeta with) {
		updateMeta(market);
		meta.page = with.page;
		meta.pageSize = with.pageSize;
		meta.search = with.search;
		meta.viewType = with.viewType;
	}
	
	public void updateMeta(Market market) {
		meta.balance = market.getEcon().getBalance(meta.name);
		meta.balanceFriendly = market.getEcon().format(meta.balance);
		meta.isAdmin = market.getInterfaceHandler().isAdmin(meta.name);
		meta.totalListings = market.getStorage().getNumListings("");
		meta.totalMail = market.getStorage().getNumMail(meta.name, "");
		meta.totalSelling = market.getStorage().getNumListingsFor(meta.name, "");
	}
	
	public Reply buy(Market market, int id) {
		MarketStorage storage = market.getStorage();
		synchronized(storage.getAllListings()) {
			Listing listing = storage.getListing(id);
			if (listing == null) {
				return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_FOUND);
			}
			if (market.getCore().buyListing(listing, meta.name, true, true)) {
				updateMeta(market);
				return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, id);
			} else {
				return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_INSUFFICIENT_FUNDS);
			}
		}
	}
	
	public Reply cancel(Market market, int id) {
		Listing listing = market.getStorage().getListing(id);
		if (listing == null) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_FOUND);
		}
		if (!listing.seller.equalsIgnoreCase(meta.name) && market.getInterfaceHandler().isAdmin(meta.name)) {
			return new Reply(Protocol.REPLY_GENERAL_FAILURE, meta, Protocol.STATUS_BAD_REQUEST);
		}
		market.getCore().removeListing(listing, meta.name);
		return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, id);
	}
	
	public Reply send(InterfaceHandler handler, SendRequest request) {
		return handler.send(meta, request);
	}
	
	public Reply create(InterfaceHandler handler, CreateRequest request) {
		return handler.create(meta, request);
	}
	
	public Reply pickup(InterfaceHandler handler, int id) {
		return handler.retrieveMail(meta, id);
	}
}
