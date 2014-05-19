package com.survivorserver.Dasfaust.WebMarket.netty;

import java.util.logging.Logger;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonSyntaxException;
import org.bukkit.craftbukkit.libs.com.google.gson.internal.StringMap;

import com.survivorserver.Dasfaust.WebMarket.WebMarket;
import com.survivorserver.Dasfaust.WebMarket.WebViewer;
import com.survivorserver.Dasfaust.WebMarket.mojang.profiles.Profile;
import com.survivorserver.Dasfaust.WebMarket.protocol.CreateRequest;
import com.survivorserver.Dasfaust.WebMarket.protocol.Protocol;
import com.survivorserver.Dasfaust.WebMarket.protocol.Reply;
import com.survivorserver.Dasfaust.WebMarket.protocol.Request;
import com.survivorserver.Dasfaust.WebMarket.protocol.RequestCode;
import com.survivorserver.Dasfaust.WebMarket.protocol.SendRequest;
import com.survivorserver.GlobalMarket.Market;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;

public class WebSocketSession {

	public Logger log;
	private WebMarket web;
	private ChannelHandlerContext ctx;
	private Gson gson;
	private WebViewer viewer;

	public WebSocketSession(WebMarket web, ChannelHandlerContext ctx, Gson gson, Logger log) {
		this.web = web;
		this.ctx = ctx;
		this.gson = gson;
		this.log = log;
	}

	public ChannelHandlerContext getContext() {
		return ctx;
	}

	public void onDisconnect() {
		if (viewer != null) {
			web.getHandler().removeViewer(viewer.getName());
			viewer = null;
		}
	}

	public void onMessage(String message) {
		final Request req;
		try {
			req = gson.fromJson(message, Request.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			send(new Reply(Protocol.REPLY_GENERAL_FAILURE, null, Protocol.STATUS_INVALID_JSON));
			return;
		}
		// Are we logged in?
		if (viewer == null) {
			if (req.getCode() != RequestCode.LOGIN) {
				log.info(ctx.channel().remoteAddress().toString());
				if (req.getCode() == RequestCode.LOGIN_XENFORO && web.auth.canAuthorize(ctx.channel().remoteAddress().toString())) {
					web.auth.addUser(req.meta.name, req.meta.password);
				} else {
					send(new Reply(Protocol.REPLY_GENERAL_FAILURE, null, Protocol.STATUS_LOGIN_EXPECTED));
				}
				return;
			} else {
				Profile profile = web.auth.getProfile(req.meta.name);
				if (profile == null) {
					send(new Reply(Protocol.REPLY_GENERAL_FAILURE, null, Protocol.STATUS_PLAYER_NOT_FOUND));
				} else {
					req.meta.name = profile.getName();
					if (!web.auth.check(req.meta.name, req.meta.password)) {
						send(new Reply(Protocol.REPLY_GENERAL_FAILURE, null, Protocol.STATUS_INVALID_CREDENTIALS));
					} else if (web.getHandler().getViewer(req.meta.name) != null) {
						send(new Reply(Protocol.REPLY_GENERAL_FAILURE, null, Protocol.STATUS_VIEWER_ALREADY_ACTIVE));
					} else {
						viewer = new WebViewer(this, req.meta);
						web.getHandler().addViewer(viewer);
						viewer.updateMeta(web.market, req.meta);
						send(new Reply(Protocol.REPLY_GENERAL_SUCCESS, viewer.getMeta(), Protocol.STATUS_UPDATE_VIEWER));
					}
				}
			}
		} else {
			// Update the viewer's data
			viewer.updateMeta(web.market, req.meta);
			// Handle their request
			this.log.info(req.getClass().toString());
			switch (req.getCode()) {
			// Logout
			case LOGOUT:
				send(viewer.onLogout());
				break;
			// Update view
			case UPDATE_VIEW:
				switch (viewer.getMeta().viewType) {
				case 0:
					send(viewer.onRequestListings(web.getHandler()));
					break;
				case 1:
					send(viewer.onRequestListingsOwned(web.getHandler()));
					break;
				case 2:
					send(viewer.onRequestMail(web.getHandler()));
					break;
				case 3:
					send(viewer.onRequestListingsCreate(web.getHandler()));
					break;
				case 4:
					send(viewer.onRequestListingsCreate(web.getHandler()));
					break;
				default:
					break;
				}
				break;
			// Purchase
			case BUY:
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							send(viewer.buy(web.market, (int) Double.parseDouble(req.data.toString())));
						} catch (Exception e) {
							send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_BAD_REQUEST));
						}
					}
				}.runTask(web);
				break;
			// Cancel
			case CANCEL:
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							send(viewer.cancel(web.market, (int) Double.parseDouble(req.data.toString())));
						} catch (Exception e) {
							send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_BAD_REQUEST));
						}
					}
				}.runTask(web);
				break;
			// Send
			case SEND:
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!web.disableSending(req.meta.getType())) {
							try {
								@SuppressWarnings("unchecked")
								StringMap<Object> map = (StringMap<Object>) req.data;
								send(viewer.send(web.getHandler(), new SendRequest(((Double) map.get("id")).intValue(), (String) map.get("name"))));
								updateView();
							} catch (Exception e) {
								e.printStackTrace();
								send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_BAD_REQUEST));
							}
						} else {
							send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_DISABLED_BY_SERVER));
						}
					}
				}.runTask(web);
				break;
			// Create
			case CREATE_LISTING:
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!web.disableCreation(req.meta.getType())) {
							try {
								@SuppressWarnings("unchecked")
								StringMap<Object> map = (StringMap<Object>) req.data;
								send(viewer.create(web.getHandler(), new CreateRequest(((Double) map.get("id")).intValue(), ((Double) map.get("amount")).intValue(), (Double) map.get("price"))));
								updateView();
							} catch (Exception e) {
								e.printStackTrace();
								send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_BAD_REQUEST));
							}
						} else {
							send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_DISABLED_BY_SERVER));
						}
					}
				}.runTask(web);
			case PICKUP:
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!web.disablePickup()) {
							try {
								send(viewer.pickup(web.getHandler(), (int) Double.parseDouble(req.data.toString())));
							} catch (Exception e) {
								send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_BAD_REQUEST));
							}
						} else {
							send(new Reply(Protocol.REPLY_TRANSACTION_FAILURE, viewer.getMeta(), Protocol.STATUS_DISABLED_BY_SERVER));
						}
					}
				}.runTask(web);
				break;
			case WORLD_LIST:
				send(new Reply(Protocol.REPLY_WORLD_LIST,viewer.getMeta(),"test"));
				break;
			default:
				this.log.info("Unmapable request");
				send(new Reply(Protocol.REPLY_GENERAL_FAILURE, null, Protocol.STATUS_UNKNOWN_REQUEST));
				break;
			}
		}
	}

	public void updateView() {
		switch (viewer.getMeta().viewType) {
		case 0:
			send(viewer.onRequestListings(web.getHandler()));
			break;
		case 1:
			send(viewer.onRequestListingsOwned(web.getHandler()));
			break;
		case 2:
			send(viewer.onRequestMail(web.getHandler()));
			break;
		case 3:
			send(viewer.onRequestListingsCreate(web.getHandler()));
			break;
		case 4:
			send(viewer.onRequestListingsCreate(web.getHandler()));
			break;
		default:
			break;
		}
	}

	public void send(String message) {
		ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
	}

	public void send(Reply reply) {
		ByteBuf buf = Unpooled.copiedBuffer(gson.toJson(reply), CharsetUtil.UTF_8);
		ctx.channel().writeAndFlush(new TextWebSocketFrame(buf));
	}
}
