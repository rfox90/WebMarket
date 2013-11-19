package com.survivorserver.Dasfaust.WebMarket;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.survivorserver.GlobalMarket.LocaleHandler;
import com.survivorserver.GlobalMarket.Market;
import com.survivorserver.GlobalMarket.Command.SubCommand;

public class RegisterCommand extends SubCommand {

	AuthManager auth;
	
	public RegisterCommand(Market market, LocaleHandler locale, AuthManager auth) {
		super(market, locale);
		this.auth = auth;
	}

	@Override
	public String getCommand() {
		return "register";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermissionNode() {
		return "webmarket.register";
	}

	@Override
	public String getHelp() {
		return "/market register <password> [Creates or changes your password for the web interface]";
	}

	@Override
	public boolean allowConsoleSender() {
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, String[] args) {
		if (args.length != 2) {
			return false;
		}
		sender.sendMessage(auth.store(sender.getName(), args[1]) ? (ChatColor.GREEN + "Password changed.") : (ChatColor.GREEN + "Password registered."));
		return true;
	}
}