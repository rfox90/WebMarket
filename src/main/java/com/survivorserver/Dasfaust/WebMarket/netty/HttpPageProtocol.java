package com.survivorserver.Dasfaust.WebMarket.netty;

import net.minecraft.util.com.google.gson.Gson;

import com.survivorserver.Dasfaust.WebMarket.WebMarket;
import com.survivorserver.Dasfaust.WebMarket.protocol.Protocol;
import com.survivorserver.Dasfaust.WebMarket.protocol.Request;
import com.survivorserver.Dasfaust.WebMarket.protocol.ViewerMeta;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

public class HttpPageProtocol {

	public static ByteBuf getContent(WebMarket web, Gson gson) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"version\":" + "\"" + web.getDescription().getVersion() + "\"");
		sb.append(",\"protocol\":");
		sb.append(gson.toJson(Protocol.serialize()));
		sb.append(",\"viewerMeta\":");
		sb.append(gson.toJson(ViewerMeta.serialize()));
		sb.append(",\"request\":");
		sb.append(gson.toJson(Request.serialize()));
		sb.append("}");
		return Unpooled.copiedBuffer(sb.toString(), CharsetUtil.UTF_8);
	}
}
