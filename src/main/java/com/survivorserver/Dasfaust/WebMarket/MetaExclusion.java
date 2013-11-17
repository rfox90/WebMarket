package com.survivorserver.Dasfaust.WebMarket;

import org.bukkit.craftbukkit.libs.com.google.gson.ExclusionStrategy;
import org.bukkit.craftbukkit.libs.com.google.gson.FieldAttributes;

public class MetaExclusion implements ExclusionStrategy {

	@Override
	public boolean shouldSkipClass(Class<?> c) {
		return false;
		//return c.getName().contains("CraftMetaEnchantedBook");
	}

	@Override
	public boolean shouldSkipField(FieldAttributes a) {
		return false;
	}
}
