package com.survivorserver.Dasfaust.WebMarket.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.GsonBuilder;

import com.survivorserver.Dasfaust.WebMarket.MetaExclusion;
import com.survivorserver.Dasfaust.WebMarket.WebMarket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
	
	private WebMarket web;
	private Logger log;
	private WebSocketServerHandshaker handshaker;
	private List<WebSocketSession> sessions;
	private Gson gson;
	
	public WebSocketServerHandler(WebMarket web, Logger log) {
		this.web = web;
		this.log = log;
		sessions = new ArrayList<WebSocketSession>();
		gson = new GsonBuilder().addSerializationExclusionStrategy(new MetaExclusion()).create();;
	}
	
	public WebSocketSession findSession(ChannelHandlerContext ctx) {
		for (WebSocketSession ses : sessions) {
			if (ses.getContext().equals(ctx)) {
				return ses;
			}
		}
		return null;
	}
	
	public WebSocketSession findSession(Channel channel) {
		for (WebSocketSession ses : sessions) {
			if (ses.getContext().channel().equals(channel)) {
				return ses;
			}
		}
		return null;
	}
	
	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
	    if (msg instanceof FullHttpRequest) {
	        handleHttpRequest(ctx, (FullHttpRequest) msg);
	    } else if (msg instanceof WebSocketFrame) {
	    	WebSocketSession session = null;
			for (WebSocketSession ses : sessions) {
				if (ses.getContext().equals(ctx)) {
					session = ses;
				}
			}
			if (session == null) {
				session = new WebSocketSession(web, ctx, gson, log);
				sessions.add(session);
			}
	        handleWebSocketFrame(session, (WebSocketFrame) msg);
	    }
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	    ctx.flush();
	}
	
	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
	    // Handle a bad request.
	    if (!req.getDecoderResult().isSuccess()) {
	        sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
	        return;
	    }
	    // Allow only GET methods.
	    if (req.getMethod() != GET) {
	        sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
	        return;
	    }
	    // Send the demo page and favicon.ico
	    if ("/protocol".equals(req.getUri())) {
	        ByteBuf content = HttpPageProtocol.getContent(web, gson);
	        FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, content);
	        req.headers().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
	        res.headers().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
	        res.headers().add(ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS");
	        res.headers().add(ACCESS_CONTROL_ALLOW_HEADERS, "X-Requested-With, Content-Type, Content-Length");
	        res.headers().add(CONTENT_TYPE, "text/javascript; charset=UTF-8");
	        setContentLength(res, content.readableBytes());
	
	        sendHttpResponse(ctx, req, res);
	        return;
	    } else if ("/".equals(req.getUri())) {
	    	// Handshake
	        try { 
	        	WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
	            handshaker = wsFactory.newHandshaker(req);
	            if (handshaker == null) {
	                WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
	            } else {
	                handshaker.handshake(ctx.channel(), req);
	            }
	        } catch(WebSocketHandshakeException e) {
	        	sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
	        }
	    } else {
	    	sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
	    }
	}
	
	private void handleWebSocketFrame(WebSocketSession con, WebSocketFrame frame) {
	    // Check for closing frame
	    if (frame instanceof CloseWebSocketFrame) {
	    	con.onDisconnect();
	    	sessions.remove(con);
	        handshaker.close(con.getContext().channel(), (CloseWebSocketFrame) frame.retain());
	        return;
	    }
	    if (frame instanceof PingWebSocketFrame) {
	    	con.getContext().channel().write(new PongWebSocketFrame(frame.content().retain()));
	        return;
	    }
	    if (frame instanceof PongWebSocketFrame) {
	    	con.getContext().channel().write(new PingWebSocketFrame(frame.content().retain()));
	        return;
	    }
	    if (!(frame instanceof TextWebSocketFrame)) {
	        throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
	    }
	    String request = ((TextWebSocketFrame) frame).text();
	    con.onMessage(request);
	}
	
	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
	    // Generate an error page if response getStatus code is not OK (200).
	    if (res.getStatus().code() != 200) {
	        ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
	        res.content().writeBytes(buf);
	        buf.release();
	        setContentLength(res, res.content().readableBytes());
	    }
	    // Send the response and close the connection if necessary.
	    ChannelFuture f = ctx.channel().writeAndFlush(res);
	    if (!isKeepAlive(req) || res.getStatus().code() != 200) {
	        f.addListener(ChannelFutureListener.CLOSE);
	    }
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	    cause.printStackTrace();
	    ctx.close();
	    WebSocketSession ses = findSession(ctx);
	    if (ses != null) {
	    	ses.onDisconnect();
	    }
	}
	 
	private static String getWebSocketLocation(FullHttpRequest req) {
	    return "ws://" + req.headers().get(HOST);
	}
}
