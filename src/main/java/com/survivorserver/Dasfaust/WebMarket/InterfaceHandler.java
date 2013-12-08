package com.survivorserver.Dasfaust.WebMarket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.survivorserver.Dasfaust.WebMarket.protocol.CreateRequest;
import com.survivorserver.Dasfaust.WebMarket.protocol.ItemList;
import com.survivorserver.Dasfaust.WebMarket.protocol.Protocol;
import com.survivorserver.Dasfaust.WebMarket.protocol.Reply;
import com.survivorserver.Dasfaust.WebMarket.protocol.SendRequest;
import com.survivorserver.Dasfaust.WebMarket.protocol.ViewerMeta;
import com.survivorserver.Dasfaust.WebMarket.protocol.WebItem;
import com.survivorserver.GlobalMarket.Listing;
import com.survivorserver.GlobalMarket.Mail;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.MarketStorage;
import com.survivorserver.GlobalMarket.HistoryHandler.MarketAction;
import com.survivorserver.GlobalMarket.Interface.Handler;
import com.survivorserver.GlobalMarket.Lib.SearchResult;

public class InterfaceHandler extends Handler {

	private WebMarket web;
	private Market market;
	private MarketStorage storage;
	private Map<String, WebViewer> viewers;
	
	public InterfaceHandler(WebMarket web) {
		this.web = web;
		market = web.market;
		storage = market.getStorage();
		viewers = Collections.synchronizedMap(new HashMap<String, WebViewer>());
	}
	
	public WebViewer getViewer(String name) {
		return viewers.containsKey(name) ? viewers.get(name) : null;
	}
	
	public void addViewer(WebViewer viewer) {
		if (!viewers.containsKey(viewer.getName())) {
			viewers.put(viewer.getName(), viewer);
		}
	}

	public void removeViewer(String name) {
		if (viewers.containsKey(name)) {
			viewers.remove(name);
		}
	}
	
	public ItemList getListings(ViewerMeta meta) {
		if (meta.search == null || meta.search.length() <= 1) {
			List<WebItem> listings = new ArrayList<WebItem>();
			synchronized(market.getStorage()) {
				for (Listing listing : storage.getListings(meta.name, meta.page, meta.pageSize, "")) {
					listings.add(new WebItem(market, listing));
				}
				return new ItemList(Protocol.VIEWTYPE_LISTINGS, storage.getAllListings().size(), listings);
			}
		} else {
			List<WebItem> listings = new ArrayList<WebItem>();
			synchronized(market.getStorage()) {
				SearchResult search = storage.getListings(meta.name, meta.page, meta.pageSize, meta.search, "");
				for (Listing listing : search.getPage()) {
					listings.add(new WebItem(market, listing));
				}
				return new ItemList(Protocol.VIEWTYPE_LISTINGS, search.getTotalFound(), listings);
			}
		}
	}
	
	public ItemList getOwnedListings(ViewerMeta meta) {
		List<WebItem> listings = new ArrayList<WebItem>();
		synchronized(market.getStorage()) {
			for (Listing listing : storage.getOwnedListings(meta.page, meta.pageSize, "", meta.name)) {
				listings.add(new WebItem(market, listing));
			}
			return new ItemList(Protocol.VIEWTYPE_LISTINGS_OWNED, storage.getAllListings().size(), listings);
		}
	}
	
	public ItemList getMail(ViewerMeta meta) {
		List<WebItem> mail = new ArrayList<WebItem>();
		for (Mail m : storage.getMail(meta.name, meta.page, meta.pageSize, "")) {
			mail.add(new WebItem(market, m));
		}
		return new ItemList(Protocol.VIEWTYPE_MAIL, mail.size(), mail);
	}
	
	public Reply getItemsForCreation(ViewerMeta meta) {
		if (web.getServer().getPlayer(meta.name) != null) {
			return new Reply(Protocol.REPLY_GENERAL_FAILURE, meta, Protocol.STATUS_PLAYER_ONLINE);
		}
		IOfflinePlayer player = new IOfflinePlayer(meta.name);
		if (!player.exists()) {
			return new Reply(Protocol.REPLY_GENERAL_FAILURE, meta, Protocol.STATUS_NO_INVENTORY);
		}
		ItemStack[] contents = player.getInventory().getContents();
		List<WebItem> inv = new ArrayList<WebItem>();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] != null && contents[i].getType() != Material.AIR) {
				inv.add(new WebItem(market, i, contents[i]));
			}
		}
		List<WebItem> creation = new ArrayList<WebItem>();
		int index = (meta.pageSize * meta.page) - meta.pageSize;
		while(inv.size() > index && creation.size() < meta.pageSize) {
			creation.add(inv.get(index));
			index++;
		}
		return new Reply(Protocol.REPLY_UPDATE_VIEW, meta, new ItemList(Protocol.VIEWTYPE_CREATE_FROM_INV, creation.size(), inv.size(), creation));
	}
	
	public ItemList getMailForCreation(ViewerMeta meta) {
		List<WebItem> mail = new ArrayList<WebItem>();
		for (Mail m : storage.getMail(meta.name, meta.page, meta.pageSize, "")) {
			mail.add(new WebItem(market, m));
		}
		return new ItemList(Protocol.VIEWTYPE_CREATE_FROM_MAIL, mail.size(), mail);
	}
	
	public Reply send(ViewerMeta meta, SendRequest request) {
		// TODO amount
		String name = request.name;
		int id = request.id;
		if (meta.name.equalsIgnoreCase(name)) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_BAD_REQUEST);
		}
		if (meta.viewType == Protocol.VIEWTYPE_CREATE_FROM_INV && web.getServer().getPlayer(meta.name) != null) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_PLAYER_ONLINE);
		}
		if (web.getServer().getPlayer(name) == null) {
			if (!new IOfflinePlayer(name).exists()) {
				return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_PLAYER_NOT_FOUND);
			}
		}
		synchronized(market.getStorage()) {
			if (meta.viewType == Protocol.VIEWTYPE_CREATE_FROM_INV) {
				IOfflinePlayer player = new IOfflinePlayer(meta.name);
				if (!player.exists()) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NO_INVENTORY);
				}
				PlayerInventory inv = player.getInventory();
				ItemStack toSend = inv.getItem(id);
				if (toSend == null || toSend.getType() == Material.AIR) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_FOUND);
				}
				storage.createMail(name, meta.name, toSend, 0, "");
				inv.setItem(id, new ItemStack(Material.AIR));
				player.setInventory(inv);
				player.savePlayerData();
				return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, name);
			} else {
				Mail mail = storage.getMail(id);
				if (mail == null) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_FOUND);
				}
				storage.createMail(name, meta.name, mail.itemId, mail.amount, "");
				storage.removeMail(id);
				return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, name);
			}
		}
	}
	
	public Reply create(ViewerMeta meta, CreateRequest request) {
		int id = request.id;
		int amount = request.amount;
		double price = request.price;
		if (price <= 0) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_BAD_REQUEST);
		}
		if (amount < 1) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_BAD_REQUEST);
		}
		if (meta.viewType == Protocol.VIEWTYPE_CREATE_FROM_INV && web.getServer().getPlayer(meta.name) != null) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_PLAYER_ONLINE);
		}
		if (web.getServer().getPlayer(meta.name) == null) {
			if (!new IOfflinePlayer(meta.name).exists()) {
				return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_PLAYER_NOT_FOUND);
			}
		}
		double maxPrice = market.getMaxPrice(meta.name, "");
		if (maxPrice > 0 && price >= maxPrice) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_PRICE_TOO_HIGH);
		}
		int maxListings = market.maxListings(meta.name, "");
		if (maxListings > 0 && storage.getNumListingsFor(meta.name, "") >= maxListings) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_SELLING_TOO_MANY_ITEMS);
		}
		// TODO fees
		synchronized(market.getStorage()) {
			if (meta.viewType == Protocol.VIEWTYPE_CREATE_FROM_INV) {
				IOfflinePlayer player = new IOfflinePlayer(meta.name);
				if (!player.exists()) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NO_INVENTORY);
				}
				PlayerInventory inv = player.getInventory();
				ItemStack toList = inv.getItem(id).clone();
				if (toList == null || toList.getType() == Material.AIR) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_FOUND);
				}
				if (market.itemBlacklisted(toList)) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_ITEM_BACLKLISTED);
				}
				if (amount > toList.getAmount()) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_ENOUGH_OF_ITEM);
				}
				if (amount < toList.getAmount()) {
					ItemStack keep = toList.clone();
					keep.setAmount(toList.getAmount() - amount);
					toList.setAmount(amount);
					inv.setItem(id, keep);
				} else {
					inv.setItem(id, new ItemStack(Material.AIR));
					player.setInventory(inv);
					player.savePlayerData();
				}
				storage.createListing(meta.name, toList, price, "");
				return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, null);
			} else {
				Mail mail = storage.getMail(id);
				if (mail == null) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_FOUND);
				}
				if (amount > mail.getAmount()) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_ENOUGH_OF_ITEM);
				}
				ItemStack toList = storage.getItem(mail.itemId, amount);
				if (market.itemBlacklisted(toList)) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_ITEM_BACLKLISTED);
				}
				storage.createListing(meta.name, toList, price, "");
				if (amount < mail.getAmount()) {
					mail.amount = mail.amount - amount;
					market.getInterfaceHandler().refreshViewer(meta.name, "Mail");
				} else {
					storage.removeMail(id);
				}
				return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, null);
			}
		}
	}
	
	public Reply retrieveMail(ViewerMeta meta, int id) {
		Mail mail = storage.getMail(id);
		if (mail == null) {
			return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NOT_FOUND);
		}
		synchronized(market.getStorage()) {
			Player player = web.getServer().getPlayer(meta.name);
			if (player != null) {
				Inventory inv = player.getInventory();
				if (inv.firstEmpty() == -1) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_FULL_INVENTORY);
				}
				double amount = mail.getPickup();
				if (amount > 0) {
					EconomyResponse response = market.getEcon().depositPlayer(meta.name, amount);
					if (!response.transactionSuccess()) {
						if (response.type == ResponseType.NOT_IMPLEMENTED) {
							market.log.severe(market.getEcon().getName() + " may not be compatible with GlobalMarket. It does not support the depositPlayer() function.");
						}
						return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_SERVER_ERROR);
					}
					player.sendMessage(ChatColor.GREEN + market.getLocale().get("picked_up_your_earnings", market.getEcon().format(market.getEcon().getBalance(player.getName()))));
					market.getStorage().nullifyMailPayment(mail.getId());
					if (market.enableHistory()) {
						market.getHistory().storeHistory(player.getName(), "You", MarketAction.EARNINGS_RETRIEVED, mail.getItemId(), mail.getAmount(), amount);
					}
				}
				inv.addItem(storage.getItem(mail.getItemId(), mail.getAmount()));
				storage.removeMail(mail.getId());
				return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, amount > 0 ? market.getEcon().format(amount) : null);
			} else {
				IOfflinePlayer offPlayer = new IOfflinePlayer(meta.name);
				if (!offPlayer.exists()) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_NO_INVENTORY);
				}
				PlayerInventory inv = offPlayer.getInventory();
				if (inv.firstEmpty() == -1) {
					return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_FULL_INVENTORY);
				}
				double amount = mail.getPickup();
				if (amount > 0) {
					EconomyResponse response = market.getEcon().depositPlayer(meta.name, amount);
					if (!response.transactionSuccess()) {
						if (response.type == ResponseType.NOT_IMPLEMENTED) {
							market.log.severe(market.getEcon().getName() + " may not be compatible with GlobalMarket. It does not support the depositPlayer() function.");
						}
						return new Reply(Protocol.REPLY_TRANSACTION_FAILURE, meta, Protocol.STATUS_SERVER_ERROR);
					}
					market.getStorage().nullifyMailPayment(mail.getId());
					if (market.enableHistory()) {
						market.getHistory().storeHistory(meta.name, "You", MarketAction.EARNINGS_RETRIEVED, mail.getItemId(), mail.getAmount(), amount);
					}
				}
				inv.addItem(storage.getItem(mail.getItemId(), mail.getAmount()));
				storage.removeMail(mail.getId());
				offPlayer.setInventory(inv);
				offPlayer.savePlayerData();
				return new Reply(Protocol.REPLY_TRANSACTION_SUCCESS, meta, amount > 0 ? market.getEcon().format(amount) : null);
			}
		}
	}
	
	@Override
	public void updateAllViewers() {
		final InterfaceHandler handler = this;
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Entry<String, WebViewer> entry : viewers.entrySet()) {
					WebViewer viewer = entry.getValue();
					viewer.updateMeta(market);
					switch(viewer.getMeta().viewType) {
						case 0:
							viewer.getSession().send(viewer.onRequestListings(handler));
							break;
						case 1:
							viewer.getSession().send(viewer.onRequestListingsOwned(handler));
							break;
						case 2:
							viewer.getSession().send(viewer.onRequestMail(handler));
							break;
						case 3:
							viewer.getSession().send(viewer.onRequestListingsCreate(handler));
							break;
						case 4:
							viewer.getSession().send(viewer.onRequestListingsCreate(handler));
							break;
						default:
							break;
					}
				}
			}
		}.runTaskAsynchronously(web);
	}

	@Override
	public void updateViewer(final String name) {
		final InterfaceHandler handler = this;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (viewers.containsKey(name)) {
					WebViewer viewer = viewers.get(name);
					viewer.updateMeta(market);
					switch(viewer.getMeta().viewType) {
						case 0:
							viewer.getSession().send(viewer.onRequestListings(handler));
							break;
						case 1:
							viewer.getSession().send(viewer.onRequestListingsOwned(handler));
							break;
						case 2:
							viewer.getSession().send(viewer.onRequestMail(handler));
							break;
						case 3:
							viewer.getSession().send(viewer.onRequestListingsCreate(handler));
							break;
						case 4:
							viewer.getSession().send(viewer.onRequestListingsCreate(handler));
							break;
						default:
							break;
					}
				}
			}
		}.runTaskAsynchronously(web);
	}

	@Override
	public void notifyPlayer(final String name, final String notification) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (viewers.containsKey(name)) {
					WebViewer viewer = viewers.get(name);
					viewer.updateMeta(market);
					viewer.notify(notification);
				}
			}
		}.runTaskAsynchronously(web);
	}
}
