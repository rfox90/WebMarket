package com.survivorserver.Dasfaust.WebMarket;

import net.minecraft.util.com.google.gson.ExclusionStrategy;
import net.minecraft.util.com.google.gson.FieldAttributes;

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
