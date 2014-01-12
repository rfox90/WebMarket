package com.survivorserver.Dasfaust.WebMarket.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.survivorserver.GlobalMarket.Listing;
import com.survivorserver.GlobalMarket.Mail;
import com.survivorserver.GlobalMarket.Market;

public class WebItem {

	public static List<Material> TOOLS = new ArrayList<Material>();
	
	static {
		TOOLS.add(Material.BOW);
		TOOLS.add(Material.CARROT_STICK);
		TOOLS.add(Material.FLINT_AND_STEEL);
		TOOLS.add(Material.SHEARS);
		
		TOOLS.add(Material.CHAINMAIL_BOOTS);
		TOOLS.add(Material.CHAINMAIL_CHESTPLATE);
		TOOLS.add(Material.CHAINMAIL_HELMET);
		TOOLS.add(Material.CHAINMAIL_LEGGINGS);
		
		TOOLS.add(Material.LEATHER_BOOTS);
		TOOLS.add(Material.LEATHER_CHESTPLATE);
		TOOLS.add(Material.LEATHER_HELMET);
		TOOLS.add(Material.LEATHER_LEGGINGS);
		
		TOOLS.add(Material.DIAMOND_CHESTPLATE);
		TOOLS.add(Material.DIAMOND_BOOTS);
		TOOLS.add(Material.DIAMOND_HELMET);
		TOOLS.add(Material.DIAMOND_HOE);
		TOOLS.add(Material.DIAMOND_LEGGINGS);
		TOOLS.add(Material.DIAMOND_AXE);
		TOOLS.add(Material.DIAMOND_PICKAXE);
		TOOLS.add(Material.DIAMOND_SPADE);
		TOOLS.add(Material.DIAMOND_SWORD);
		
		TOOLS.add(Material.GOLD_CHESTPLATE);
		TOOLS.add(Material.GOLD_BOOTS);
		TOOLS.add(Material.GOLD_HELMET);
		TOOLS.add(Material.GOLD_HOE);
		TOOLS.add(Material.GOLD_LEGGINGS);
		TOOLS.add(Material.GOLD_AXE);
		TOOLS.add(Material.GOLD_PICKAXE);
		TOOLS.add(Material.GOLD_SPADE);
		TOOLS.add(Material.GOLD_SWORD);
		
		TOOLS.add(Material.IRON_CHESTPLATE);
		TOOLS.add(Material.IRON_BOOTS);
		TOOLS.add(Material.IRON_HELMET);
		TOOLS.add(Material.IRON_HOE);
		TOOLS.add(Material.IRON_LEGGINGS);
		TOOLS.add(Material.IRON_AXE);
		TOOLS.add(Material.IRON_PICKAXE);
		TOOLS.add(Material.IRON_SPADE);
		TOOLS.add(Material.IRON_SWORD);
		
		TOOLS.add(Material.STONE_HOE);
		TOOLS.add(Material.STONE_AXE);
		TOOLS.add(Material.STONE_PICKAXE);
		TOOLS.add(Material.STONE_SPADE);
		TOOLS.add(Material.STONE_SWORD);
		
		TOOLS.add(Material.WOOD_HOE);
		TOOLS.add(Material.WOOD_AXE);
		TOOLS.add(Material.WOOD_PICKAXE);
		TOOLS.add(Material.WOOD_SPADE);
		TOOLS.add(Material.WOOD_SWORD);
		
		TOOLS.add(Material.EXP_BOTTLE);
	}
	
	public int id;
	public Map<String, Object> item;
	public String friendlyItemName;
	public String seller;
	public double price;
	public String friendlyPrice;
	public String sender;
	public int maxDamage;
	boolean isTool;
	boolean isInfinite = false;
	public int siblingCount = 0;
	
	/*
	 * Listing constructor
	 */
	public WebItem(Market market, Listing listing) {
		id = listing.getId();
		ItemStack stack = market.getStorage().getItem(listing.getItemId(), listing.getAmount());
		maxDamage = stack.getType().getMaxDurability();
		isTool = TOOLS.contains(stack.getType());
		item = stack.serialize();
		
		// Fix for enchanted book meta. Not serializable with Gson.
		if (item.containsKey("meta")) {
			if (item.get("meta") instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.get("meta");
				item.remove("meta");
				ItemMeta newMeta = new ItemStack(Material.APPLE).getItemMeta();
				if (meta.hasDisplayName()) {
					newMeta.setDisplayName(meta.getDisplayName());
				}
				if (meta.hasLore()) {
					newMeta.setLore(meta.getLore());
				}
				for (Entry<Enchantment, Integer> set : meta.getStoredEnchants().entrySet()) {
					newMeta.addEnchant(set.getKey(), set.getValue(), true);
				}
				item.put("meta", newMeta);
			}
		}
		
		ItemStack singular = stack.clone();
		singular.setAmount(1);
		friendlyItemName = market.getItemName(singular);
		seller = listing.getSeller();
		price = listing.getPrice();
		friendlyPrice = market.getEcon().format(price);
		isInfinite = listing.getSeller().equalsIgnoreCase(market.getInfiniteSeller());
		for (Listing l : listing.getStacked()) {
			siblingCount += l.getAmount();
		}
	}
	
	public WebItem(Market market, Mail mail) {
		id = mail.getId();
		ItemStack stack = market.getStorage().getItem(mail.getItemId(), mail.getAmount());
		maxDamage = stack.getType().getMaxDurability();
		isTool = TOOLS.contains(stack.getType());
		item = stack.serialize();
		
		// Fix for enchanted book meta. Not serializable with Gson.
		if (item.containsKey("meta")) {
			if (item.get("meta") instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.get("meta");
				item.remove("meta");
				ItemMeta newMeta = new ItemStack(Material.APPLE).getItemMeta();
				if (meta.hasDisplayName()) {
					newMeta.setDisplayName(meta.getDisplayName());
				}
				if (meta.hasLore()) {
					newMeta.setLore(meta.getLore());
				}
				for (Entry<Enchantment, Integer> set : meta.getStoredEnchants().entrySet()) {
					newMeta.addEnchant(set.getKey(), set.getValue(), true);
				}
				item.put("meta", newMeta);
			}
		}
		
		ItemStack singular = stack.clone();
		singular.setAmount(1);
		friendlyItemName = market.getItemName(singular);
		sender = mail.getSender();
	}
	
	public WebItem(Market market, int slot, ItemStack item) {
		id = slot;
		maxDamage = item.getType().getMaxDurability();
		isTool = TOOLS.contains(item.getType());
		this.item = item.serialize();
		
		// Fix for enchanted book meta. Not serializable with Gson.
		if (this.item.containsKey("meta")) {
			if (this.item.get("meta") instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta) this.item.get("meta");
				this.item.remove("meta");
				ItemMeta newMeta = new ItemStack(Material.APPLE).getItemMeta();
				if (meta.hasDisplayName()) {
					newMeta.setDisplayName(meta.getDisplayName());
				}
				if (meta.hasLore()) {
					newMeta.setLore(meta.getLore());
				}
				for (Entry<Enchantment, Integer> set : meta.getStoredEnchants().entrySet()) {
					newMeta.addEnchant(set.getKey(), set.getValue(), true);
				}
				this.item.put("meta", newMeta);
			}
		}
		
		ItemStack singular = item.clone();
		singular.setAmount(1);
		friendlyItemName = market.getItemName(singular);
	}
}
