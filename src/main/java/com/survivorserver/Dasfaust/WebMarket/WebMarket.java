package com.survivorserver.Dasfaust.WebMarket;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.survivorserver.Dasfaust.WebMarket.netty.WebSocketServer;
import com.survivorserver.Dasfaust.WebMarket.protocol.Protocol;
import com.survivorserver.Dasfaust.WebMarket.protocol.ViewType;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.MetricsLite;

public class WebMarket extends JavaPlugin {

	public Logger log;
	public Market market;
	private WebSocketServer server;
	private InterfaceHandler handler;
	public AuthManager auth;
	public boolean mcpcp = false;
	
	public void onEnable() {
		log = getLogger();
		market = Market.getMarket();
		reloadConfig();
		getConfig().addDefault("server.port", 8080);
		getConfig().addDefault("server.forumAuth.allowFrom", "127.0.0.1");
		getConfig().addDefault("interface.disable_create_from_inv", false);
		getConfig().addDefault("interface.disable_sending_from_inv", false);
		getConfig().addDefault("interface.disable_create_from_mail", false);
		getConfig().addDefault("interface.disable_sending_from_mail", false);
		getConfig().addDefault("interface.disable_mail_pickup", false);
		getConfig().addDefault("enable_metrics", true);
		getConfig().options().copyDefaults(true);
		saveConfig();
		if (getConfig().getBoolean("enable_metrics")) {
			try {
			    MetricsLite metrics = new MetricsLite(this);
			    metrics.start();
			} catch (Exception e) {
			    log.info("Failed to start Metrics!");
			}
		}
		try {
            Class.forName("me.dasfaust.GlobalMarket.MarketCompanion");
            log.info("Market Forge mod detected!");
            mcpcp = true;
        } catch(Exception ignored) {}
		handler = new InterfaceHandler(this);
		market.getInterfaceHandler().registerHandler(handler);
		auth = new AuthManager(this);
		server = new WebSocketServer(getConfig().getInt("server.port"), getLogger(), this);
		server.start();
		market.getCmd().registerSubCommand(new RegisterCommand(market, market.getLocale(), auth));
	}
	
	public void onDisable() {
		market.getInterfaceHandler().unregisterHandler(handler);
		market.getCmd().unregisterSubCommand(RegisterCommand.class);
		server.shutDown();
	}
	
	public InterfaceHandler getHandler() {
		return handler;
	}
	
	public boolean disableCreation(ViewType viewType) {
		if (viewType == ViewType.CREATE_FROM_INV) {
			return getConfig().getBoolean("interface.disable_create_from_inv");
		} else if (viewType == ViewType.CREATE_FROM_MAIL) {
			return getConfig().getBoolean("interface.disable_create_from_mail");
		}
		return false;
	}
	
	public boolean disableSending(ViewType viewType) {
		if (viewType == ViewType.CREATE_FROM_INV) {
			return getConfig().getBoolean("interface.disable_sending_from_inv");
		} else if (viewType == ViewType.CREATE_FROM_MAIL) {
			return getConfig().getBoolean("interface.disable_sending_from_mail");
		}
		return false;
	}
	
	public boolean disablePickup() {
		return getConfig().getBoolean("interface.disable_mail_pickup");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (commandLabel.equalsIgnoreCase("webmarket")) {
			if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				if (sender.hasPermission("globalmarket.admin")) {
					int port = getConfig().getInt("server.port");
					reloadConfig();
					if (getConfig().getInt("server.port") != port) {
						log.info("Port changed, cycling server...");
						server.shutDown();
						server = new WebSocketServer(getConfig().getInt("server.port"), getLogger(), this);
						server.start();
					}
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&b&lWebMarket&r] Config reloaded"));
				} else {
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou don't have permission for this command"));
				}
			} else {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "[&b&lWebMarket&r] /webmarket reload [&bReloads config.yml&r]"));
			}
			return true;
		}
		return false;
	}
}
