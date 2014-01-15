package com.survivorserver.Dasfaust.WebMarket.netty;

import java.util.logging.Logger;

import com.survivorserver.Dasfaust.WebMarket.WebMarket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class WebSocketServer extends Thread {

	private Logger log;
	private final int port;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private Channel ch;
	private WebMarket web;
	

	public WebSocketServer(int port, Logger log, WebMarket web) {
		this.port = port;
		this.log = log;
		this.web = web;
	}

	public void run() {
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("codec-http", new HttpServerCodec());
					pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
					final WebSocketServerHandler handler = new WebSocketServerHandler(web, log);
					pipeline.addLast("handler", handler);
					ChannelFuture closeFuture = ch.closeFuture();
					closeFuture.addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							WebSocketSession ses = handler.findSession(future.channel());
							if (ses != null) {
								ses.onDisconnect();
							}
						}
					});
				}
			});
			
			ChannelFuture future = b.bind(port).sync();
			if (!future.isSuccess()) {
				log.severe("Couldn't start the server! Is something already running on port " + port + "? Change it and use /webmarket reload");
				bossGroup.shutdownGracefully().await();
				workerGroup.shutdownGracefully().await();
				return;
			}
			ch = future.channel();
			log.info("Server started on port " + port);
			ch.closeFuture().sync();
			
		} catch (InterruptedException ignored) {
			shutDown();
		}
	}

	public synchronized void shutDown() {
		log.info("Stopping server...");
		ch.close().syncUninterruptibly();
		ch.pipeline().close().syncUninterruptibly();
		bossGroup.shutdownGracefully().awaitUninterruptibly();
		workerGroup.shutdownGracefully().awaitUninterruptibly();
		log.info("Server stopped");
	}
}
