// WebMarket settings and locale
var settings;
var locale;
var items;
// Request/status/reply codes and viewer object
var protocol;
var viewer;
var request;
// Our WebSocket
var socket;
// Last message from the server
var message;
var currentPage;
var selectedSlot;
// Initialize
$(function () {
	// Add .format function to String
	String.prototype.format = function () {
		var args = arguments;
		return this.replace(/{(\d+)}/g, function (match, number) {
			return typeof args[number] != 'undefined' ? args[number] : match;
		});
	};
	// Load our settings...
	var set = false;
	$.ajax({
		url: "settings.json",
		async: false,
		dataType: "json",
		success: function (response) {
			settings = response;
			locale = settings.locale[settings.locale.selected];
		},
		error: function (request, status, error) {
			fatal("Unable to load settings.json!");
		}
	});
	$.ajax({
		url: "items.json",
		async: false,
		dataType: "json",
		success: function (response) {
			items = response;
			console.log(items);
			set = true;
		},
		error: function (request, status, error) {
			fatal("Unable to load items.json!");
		}
	});
	if (set) {
		// Make sure this browser supports WebSockets
		if (!("WebSocket" in window)) {
			fatal(locale.browser_does_not_support_websockets);
			return;
		}
		var prot = false;
		// Retrieve protocol info...
		$.ajax({
			url: "http://" + settings.connection.address + ":" + settings.connection.port + "/protocol",
			async: false,
			dataType: "json",
			success: function (response) {
				if (response.version != settings.version) {
					fatal("Protocol versions do not match!");
					return;
				}
				protocol = response.protocol;
				viewer = response.viewerMeta;
				request = response.request;
				request.meta = viewer;
				prot = true;
			},
			error: function (request, status, error) {
				fatal(locale.cant_connect_to_server);
			}
		});
		if (prot) {
			console.log("Loading complete!");
			// Register our events
			$("#browse-search").click(function (e) {
				e.preventDefault();
				var search = $("#browse-search-input").val();
				if (search != null && search.length > 0) {
					viewer.search = search;
					if (viewer.viewType == 0) {
						viewer.page = 1;
						sendRequest(2, null);
						$("#browse-search-cancel").toggleClass("hidden");
					}
				}
			});
			$("#browse-search-cancel").click(function (e) {
				e.preventDefault();
				viewer.search = "";
				viewer.page = 1;
				$("#browse-search-input").val("");
				sendRequest(2, null);
				$(this).toggleClass("hidden");
			});
			$("#browse-page-size").change(function () {
				viewer.page = 1;
				viewer.pageSize = $(this).val();
				$.cookie("market_pageSize", viewer.pageSize);
				sendRequest(2, null);
			});
			var savedSize = $.cookie("market_pageSize");
			if (savedSize != null) {
				viewer.pageSize = savedSize;
				$("#browse-page-size").val(savedSize);
			}
			$("#create-send").click(function (e) {
				e.preventDefault();
				var val = $("#create-inputIgn").val();
				if (val != null && val.length >= 3) {
					sendRequest(6, new SendRequest(currentPage[selectedSlot].id, val));
				}
			});
			$("#create-button").click(function (e) {
				e.preventDefault();
				var price = $("#create-inputPrice").val();
				var amt = $("#create-inputAmount").val();
				if (price > 0 && amt >= 1) {
					sendRequest(7, new CreateRequest(currentPage[selectedSlot].id, parseFloat(price), parseInt(amt)));
				}
			});
			connect();
		}
	}
});

function SendRequest(id, name) {
	this.id = id;
	this.name = name;
}

function CreateRequest(id, price, amount) {
	this.id = id;
	this.price = price;
	this.amount = amount;
}

function connect() {
	console.log("Connecting to server...");
	socket = new WebSocket("ws://" + settings.connection.address + ":" + settings.connection.port);
	socket.onopen = function () {
		console.log("Connected to server!");
		$(window).unload(function () {
			send(1, null);
		});
		if (settings.authentication.type == "default") {
			if (viewer.name.length > 3 && viewer.password.length > 3) {
				request.req = protocol.REQUEST_LOGIN;
				socket.send(JSON.stringify(request));
			}
			$("#login").removeClass("hidden");
			info(settings.locale.accessing_the_market);
		} else {
			$.ajax({
				url: settings.authentication.login_script,
				dataType: "json",
				success: function (response) {
					viewer.name = response.name;
					viewer.password = response.password;
					request.req = protocol.REQUEST_LOGIN;
					socket.send(JSON.stringify(request));
				},
				error: function (request, status, er) {
					warning(locale.please_log_in.format(settings.authentication.forum_login_url));
				}
			});
		}
	};
	socket.onmessage = function (msg) {
		message = JSON.parse(msg.data);
		console.log("Message received:");
		console.log(message);
		if (message.rep == 0) {
			if ((message.data == 9 || message.data == 10) && viewer.viewType == protocol.VIEWTYPE_CREATE_FROM_INV) {
				$("#create-message").removeClass("callout-info");
				$("#create-message").addClass("callout-danger");
				$("#create-message").html(locale.response_codes[message.data]);
				return;
			}
			warning(locale.response_codes[message.data]);
			return;
		}
		viewer = message.meta;
		refreshViewerBlock();
		switch (message.rep) {
		case 1:
			// general success
			if (message.data = 4) {
				// login
				success(locale.welcome.format(viewer.name));
				$("#login").addClass("hidden");
				$("#user").toggleClass("hidden");
				$("#MarketTabs").toggleClass("hidden");
				sendRequest(2, null);
			}
			break;
		case 2:
			// update to our current view
			currentPage = message.data.currentPage;
			if (message.data.type == 0) {
				// listings
				if (viewer.totalListings > 0) {
					displayListings(message.data.currentPage, "#listings-container");
					initializeSelection();
				} else {
					$("#listings-container").html("<div style='margin-left: auto; margin-right: auto; font-weight: bold; text-align: center;'>" + locale.there_are_no_listings + "</div>");
				}
			} else if (message.data.type == 1) {
				// selling
				if (viewer.totalSelling > 0) {
					displayListings(message.data.currentPage, "#listings-selling-container");
					initializeSelection();
				} else {
					$("#listings-selling-container").html("<div style='margin-left: auto; margin-right: auto; font-weight: bold; text-align: center;'>" + locale.there_are_no_listings + "</div>");
				}
			} else if (message.data.type == 2) {
				// mail
				if (viewer.totalMail > 0) {
					displayMail(message.data, "#mail-container");
					initializeSelection();
				} else {
					$("#mail-container").html("<div style='margin-left: auto; margin-right: auto; font-weight: bold; text-align: center;'>" + locale.there_is_no_mail + "</div>");
				}
			} else if (message.data.type == 3) {
				// create
				if (message.data.totalPossible > 0) {
					displayCreatables(message.data, "#listings-create-container");
					$("#create-message").removeClass("callout-danger");
					$("#create-message").addClass("callout-info");
					$("#create-message").html(locale.click_an_item);
					initializeSelection();
				} else {
					$("#listings-create-container").html("<div style='margin-left: auto; margin-right: auto; font-weight: bold; text-align: center;'>" + locale.your_inventory_is_empty + "</div>");
				}
			} else if (message.data.type == 4) {
				// create (mail)
				$("#create-message").removeClass("callout-danger");
				$("#create-message").addClass("callout-info");
				$("#create-message").html(locale.click_an_item);
				if (viewer.totalMail > 0) {
					message.data.totalPossible = viewer.totalMail;
					displayCreatables(message.data, "#listings-create-mail-container");
					initializeSelection();
				} else {
					$("#listings-create-mail-container").html("<div style='margin-left: auto; margin-right: auto; font-weight: bold; text-align: center;'>" + locale.there_is_no_mail + "</div>");
				}
			}
			break;
		case 4:
			// notification
			info(message.data);
			break;
		case 5:
			// Transaction failure
			if (viewer.viewType == protocol.VIEWTYPE_CREATE_FROM_INV || viewer.viewType == protocol.VIEWTYPE_CREATE_FROM_MAIL) {
				$("#create-alert").removeClass("hidden");
				$("#create-alert").html(locale.transaction_failure + locale.response_codes[message.data]);
				return;
			}
			warning(locale.transaction_failure + "<strong>" + locale.response_codes[message.data] + "</strong>");
			break;
		case 6:
			// Transaction successful
			if (viewer.viewType == protocol.VIEWTYPE_CREATE_FROM_INV || viewer.viewType == protocol.VIEWTYPE_CREATE_FROM_MAIL) {
				$("#create-alert").addClass("hidden");
				$("#create-modal-close").click();
				if (message.data != null) {
					success("Your item has been sent to " + message.data);
				} else {
					success("Listing created!");
				}
				return;
			}
			if (viewer.viewType == protocol.VIEWTYPE_MAIL) {
				if (message.data == null) {
					success(locale.mail_sent_to_inventory);
				} else {
					success(locale.mail_sent_to_inventory_and_picked_up_earnings.format(stripColors(message.data)));
				}
				return;
			}
			success("Transaction successful: " + message.data);
			break;
		}
	};
	socket.onclose = function () {
		console.log("Connection closed");
		fatal((message != null && !isNaN(message.data)) ? locale.server_closed_connection.format(locale.response_codes[message.data]) : locale.cant_connect_to_server);
	};
}

function refreshViewerBlock() {
	$("#balance").html(stripColors(viewer.balanceFriendly));
	$("#userimg").attr('src', locale.minotar_address.format(viewer.name, 32));
	$("#browse-number").html(viewer.totalListings);
	$("#mail-number").html(viewer.totalMail);
}

function fatal(alert) {
	$("#login").addClass("hidden");
	if (!$("#user").hasClass("hidden")) {
		$("#user").addClass("hidden");
	}
	if (!$("#MarketTabs").hasClass("hidden")) {
		$("#MarketTabs").addClass("hidden");
	}
	$("#login > *").each(function () {
		$(this).addClass("disabled");
	});
	$("#alert").html("<p class='alert alert-danger'>" + alert + "</p>");
}

function warning(msg) {
	$("#alert").html("<p class='alert alert-warning'>" + msg + "</p>");
}

function success(msg) {
	$("#alert").html("<button type='button' class='close' style='margin-right: 3px;'>&times;</button><p class='alert alert-success'>" + msg + "</p>");
	$(".close").click(function () {
		$(this).siblings().each(function () {
			$(this).remove();
		});
		$(this).remove();
	});
}

function info(msg) {
	$("#alert").html("<button type='button' class='close' style='margin-right: 3px;'>&times;</button><p class='alert alert-info'>" + msg + "</p>");
	$(".close").click(function () {
		$(this).siblings().each(function () {
			$(this).remove();
		});
		$(this).remove();
	});
}

function updateView(view) {
	if (viewer.viewType != view) {
		viewer.viewType = view;
		sendRequest(2, null);
	}
}

function sendRequest(req, data) {
	request.req = req;
	request.data = data;
	request.meta = viewer;
	console.log(request);
	console.log(JSON.stringify(request));
	socket.send(JSON.stringify(request));
}

function displayItemStack(itemMeta, containerName) {
	var html = "";
	var item = itemMeta.item;
	var itemName = formatItemName(itemMeta.item, itemMeta.friendlyItemName);
	html += "<div id='selected-item' class='itemStackTd' title='" + buildBasicToolTip(itemMeta, itemName) + "'><span class='itemStackQuantity'>" + (item.amount == null ? 1 : item.amount) + "</span>" + getItemImage(itemMeta) + itemName + "</div>";
	$(containerName).html(html);
	$('#selected-item').tooltipsy({
		alignTo: 'element',
		offset: [0, 1],
		css: {
			'padding': '2px',
			'color': 'white',
			'background-color': 'rgba(21, 1, 37, 0.9)',
			'text-shadow': 'none',
			'box-sizing': 'border-box',
			'border-radius': '4px',
			'font-size': '12pt'
		}
	});
}

function displayMail(listMeta, containerName) {
	console.log("Displaying mail");
	var itemList = listMeta.currentPage;
	var container = $(containerName);
	var html = "<div class='row'><div class='col-md-4'>";
	html += "<table class='table table-striped table-hover'>";
	html += "<thead><tr><th>Item</th><th class='itemTableButtons'>Functions</th></tr></thead>";
	var cP = 0;
	var colSize = Math.floor(viewer.pageSize / 3);
	for (var i = 0; i < itemList.length; i++) {
		var listing = itemList[i];
		var item = listing.item;
		var itemName = formatItemName(listing.item, listing.friendlyItemName);
		if (cP == colSize) {
			html += "</table></div>";
			html += "<div class='col-md-4'>";
			html += "<table class='table table-striped table-hover'>";
			html += "<thead><tr class='hidden-sm hidden-xs'><th>Item</th><th class='itemTableButtons'>Functions</th></tr></thead>";
			cP = 0;
		}
		html += "<tr>";
		html += "<td id='" + viewer.viewType + "-" + i + "' data-slot='" + i + "' data-toggle='modal' data-target='#book-view' class='MailTip itemStackTd MarketSelectable' title='" + buildMailToolTip(listing, itemName) + "'><span class='itemStackQuantity'>" + (listing.isInfinite ? "<span style='font-weight: bold; font-size: 11pt;'>&infin;</span>" : (item.amount == null ? 1 : item.amount)) + "</span>" + getItemImage(listing) + itemName + "</td>";
		html += "<td class='itemStackTd itemTableButtons'>" + buildMailButtons(listing) + "</td>";
		html += "</tr>";
		cP++;
	}
	html += "</table></div></div>";
	container.html(stripColors(html));
	$('.MailTip').tooltipsy({
		alignTo: 'element',
		offset: [0, -1],
		css: {
			'padding': '2px',
			'color': 'white',
			'background-color': 'rgba(21, 1, 37, 0.9)',
			'text-shadow': 'none',
			'box-sizing': 'border-box',
			'border-radius': '4px',
			'font-size': '12pt'
		}
	});
	$("#browse-page-container").pagination({
		items: viewer.totalMail,
		itemsOnPage: viewer.pageSize,
		currentPage: viewer.page,
		hrefText: null,
		cssStyle: "pagination",
		onPageClick: function (pageNumber, event) {
			viewer.page = pageNumber;
			sendRequest(2, null);
		}
	});
}

function displayCreatables(listMeta, containerName) {
	console.log("Displaying creatables");
	var itemList = listMeta.currentPage;
	var container = $(containerName);
	var html = "<div class='row'><div class='col-md-3'>";
	html += "<table class='table table-striped table-hover'>";
	html += "<thead><tr><th>Item</th></thead>";
	var cP = 0;
	var colSize = viewer.pageSize / 4;
	for (var i = 0; i < itemList.length; i++) {
		var listing = itemList[i];
		var item = listing.item;
		var itemName = formatItemName(listing.item, listing.friendlyItemName);
		if (cP == colSize) {
			html += "</table></div>";
			html += "<div class='col-md-3'>";
			html += "<table class='table table-striped table-hover'>";
			html += "<thead><tr class='hidden-sm hidden-xs'><th>Item</th></thead>";
			cP = 0;
		}
		html += "<tr id='" + viewer.viewType + "-" + i + "' data-slot='" + i + "' class='itemStackTr MarketSelectable' data-toggle='modal' data-target='#create-dialog'>";
		html += "<td class='itemStackTd CreateTip' title='" + buildBasicToolTip(listing, itemName) + "'><span class='itemStackQuantity'>" + (listing.isInfinite ? "<span style='font-weight: bold; font-size: 11pt;'>&infin;</span>" : (item.amount == null ? 1 : item.amount)) + "</span>" + getItemImage(listing) + itemName + "</td>";
		html += "</tr>";
		cP++;
	}
	html += "</table></div></div>";
	container.html(stripColors(html));
	$('.CreateTip').tooltipsy({
		alignTo: 'element',
		offset: [0, -1],
		css: {
			'padding': '2px',
			'color': 'white',
			'background-color': 'rgba(21, 1, 37, 0.9)',
			'text-shadow': 'none',
			'box-sizing': 'border-box',
			'border-radius': '4px',
			'font-size': '12pt'
		}
	});
	$("#browse-page-container").pagination({
		items: (viewer.viewType == 3 ? listMeta.totalPossible : viewer.totalMail),
		itemsOnPage: viewer.pageSize,
		currentPage: viewer.page,
		hrefText: null,
		cssStyle: "pagination",
		onPageClick: function (pageNumber, event) {
			viewer.page = pageNumber;
			sendRequest(2, null);
		}
	});
}

function displayListings(itemList, containerName) {
	console.log("Displaying listings");
	var container = $(containerName);
	var html = "<div class='row'><div class='col-md-6'>";
	html += "<table class='table table-striped table-hover'>";
	html += "<thead><tr><th>Item</th><th>Price</th><th class='itemTableButtons'>Functions</th></tr></thead>";
	var cP = 0;
	var colSize = viewer.pageSize / 2;
	for (var i = 0; i < itemList.length; i++) {
		var listing = itemList[i];
		var item = listing.item;
		var itemName = formatItemName(listing.item, listing.friendlyItemName);
		if (cP == colSize) {
			html += "</table></div>";
			html += "<div class='col-md-6'>";
			html += "<table class='table table-striped table-hover'>";
			html += "<thead><tr class='hidden-sm hidden-xs'><th>Item</th><th>Price</th><th class='itemTableButtons'>Functions</th></tr></thead>";
			cP = 0;
		}
		html += "<tr id='" + viewer.viewType + "-" + i + "' data-slot='" + i + "' class='itemStackTr MarketSelectable'>";
		html += "<td class='itemStackTd ListingTip' title='" + buildListingToolTip(listing, itemName) + "'><span class='itemStackQuantity'>" + (listing.isInfinite ? "<span style='font-weight: bold; font-size: 11pt;'>&infin;</span>" : (item.amount == null ? 1 : item.amount)) + "</span>" + getItemImage(listing) + itemName + "</td>";
		html += "<td class='itemStackTd'>" + listing.friendlyPrice + "</td>";
		html += "<td class='itemStackTd itemTableButtons'>" + buildListingButtons(listing) + "</td>";
		html += "</tr>";
		cP++;
	}
	html += "</table></div></div>";
	container.html(stripColors(html));
	$('.ListingTip').tooltipsy({
		alignTo: 'element',
		offset: [0, -1],
		css: {
			'padding': '2px',
			'color': 'white',
			'background-color': 'rgba(21, 1, 37, 0.9)',
			'text-shadow': 'none',
			'box-sizing': 'border-box',
			'border-radius': '4px',
			'font-size': '12pt'
		}
	});
	$("#browse-page-container").pagination({
		items: (viewer.viewType == 0 ? viewer.totalListings : viewer.totalSelling),
		itemsOnPage: viewer.pageSize,
		currentPage: viewer.page,
		hrefText: null,
		cssStyle: "pagination",
		onPageClick: function (pageNumber, event) {
			viewer.page = pageNumber;
			sendRequest(2, null);
		}
	});
}

function getItemImage(itemMeta) {
	var data;
	if (itemMeta.isTool) {
		data = 0;
	} else {
		data = (itemMeta.item.damage == null ? 0 : itemMeta.item.damage);
	}
	var loc;
	console.log(itemMeta.item.type + "_" + data);
	if (itemMeta.item.type == "POTION") {
		if (items.potions.hasOwnProperty(data)) {
			loc = items.potions[data].image;
		} else {
			loc = "http://survivorserver.com/market/textures/missing.png";
		}
	} else {
		if (items.hasOwnProperty(itemMeta.item.type + "_" + data)) {
			loc = items[itemMeta.item.type + "_" + data];
		} else {
			loc = "http://survivorserver.com/market/textures/missing.png";
		}
	}
	return "<img class='itemStackImage' src='" + loc + "' />";
}

function formatItemName(item, friendlyName) {
	var html = '<span class="';
	if (item.meta != null && item.meta.enchantments != null) {
		html += 'itemNameEnchanted ';
	}
	if ((item.meta != null && item.meta.displayName != null) || (item.meta != null && item.meta.title != null)) {
		html += 'itemNameCustom';
	}
	html += '">';
	if ((item.meta != null && item.meta.displayName != null) || (item.meta != null && item.meta.title != null)) {
		html += escapeHtml((item.meta.displayName == null ? item.meta.title : item.meta.displayName));
	} else {
		html += escapeHtml((friendlyName == null ? item.type : friendlyName));
	}
	html += '</span>';
	return html;
}

function buildMailButtons(listing) {
	var html = "";
	html += "<button class='btn btn-sm btn-success' data-toggle='tooltip' title='Send to inventory' onclick='pickup(" + listing.id + ");'><span style='text-shadow: 1px 1px rgb(28, 28, 28);' class='glyphicon glyphicon-share-alt'></span></button>";
	return html;
}

function buildListingButtons(listing) {
	var html = "";
	if (listing.seller == viewer.name || viewer.isAdmin) {
		html += "<button class='btn btn-sm btn-danger' data-toggle='tooltip' title='Cancel'" + (listing.seller != viewer.name ? "style='margin-right: 5px;'" : "") + " onclick='cancel(" + listing.id + ");'><span style='text-shadow: 1px 1px rgb(28, 28, 28);' class='glyphicon glyphicon-remove-sign'></span></button>";
	}
	if (listing.seller != viewer.name) {
		html += "<button class='btn btn-sm btn-success' data-toggle='tooltip' title='Buy' onclick='buy(" + listing.id + ");'><span style='text-shadow: 1px 1px rgb(28, 28, 28);' class='glyphicon glyphicon-usd'></span></button>";
	}
	return html;
}

function stripColors(string) {
	return string.replace(/§[0-9A-FK-OR]/gi, '');
}

function buy(id) {
	sendRequest(4, id);
}

function cancel(id) {
	sendRequest(5, id);
}

function pickup(id) {
	sendRequest(8, id);
}

function initializeSelection() {
	$(".MarketSelectable").click(function (e) {
		selectedSlot = $(this).attr("data-slot");
		console.log("Selected listing id " + currentPage[selectedSlot].id);
		if (viewer.viewType == 2) {
			var itemMeta = currentPage[selectedSlot];
			if (itemMeta.item.meta != null && itemMeta.item.meta.pages != null) {
				displayItemStack(itemMeta, "#book-dialog-title");
				$("#book-dialog-body").html("");
				for (var i = 0; i < itemMeta.item.meta.pages.length; i++) {
					$("#book-dialog-body").append("<code style='float: right; border-right: 1px solid #cccccc; border-top: 1px solid #cccccc; border-top-left-radius: 0px;  border-bottom-right-radius: 0px;'>" + (i + 1) + "</code>");
					$("#book-dialog-body").append('<pre>' + itemMeta.item.meta.pages[i] + '</pre>');
				}
			}
		}
		if (viewer.viewType == 3 || viewer.viewType == 4) {
			if (viewer.viewType == 4) {
				$("#listings-create-container").html("");
			}
			displayItemStack(currentPage[selectedSlot], "#create-dialog-selected");
			var amount = currentPage[selectedSlot].item.amount;
			$("#create-alert").addClass('hidden');
			$("#create-inputAmount").val(amount == null ? 1 : amount);
			$("#create-inputAmount").attr("max", amount == null ? 1 : amount);
			$("#create-inputAmount").attr("min", 1);
		}
	});
}

function buildListingToolTip(listing, itemName) {
	var item = listing.item;
	var html = '<div class="toolTipInner">';
	html += '<div style="font-weight: bold; float: left;">' + itemName + '</div>';
	html += '<div class="toolTipSeller"><img style="display: inline-block; height: 16px; width: 16px;" src="' + locale.minotar_address.format(listing.seller, 16) + '" /> ' + listing.seller + '</div>';
	html += '<div class="clearfix"></div>';
	if (item.type == "POTION") {
		if (items.potions.hasOwnProperty(item.damage)) {
			html += '<div style="color: #bebebe;">' + items.potions[item.damage].lore + '</div>';
		}
	}
	if (item.meta != null) {
		var meta = item.meta;
		console.log(meta);
		if (meta.enchantments != null) {
			var enchantments = meta.enchantments;
			for (enchantment in enchantments) {
				var ench = (items.enchants[enchantment] == null ? enchantment : items.enchants[enchantment]);
				html += '<div style="color: #bebebe;">' + ench + ' ' + romanize(enchantments[enchantment]) + '</div>';
			}
		}
		if (meta.lore != null) {
			var lore = '';
			for (var i = 0; i < meta.lore.length; i++) {
				lore += escapeHtml(meta.lore[i] + ' ');
			}
			html += '<div style="font-style: italic;">' + lore + '</div>';
		}
		if (meta.attributes != null) {
			html += '<div style="margin-top: 12px;"></div>';
			var attr = meta.attributes.list;
			for (var i = 0; i < attr.length; i++) {
				html += '<div style="color: #3f3ffe;">+' + attr[i].map.Amount.data + ' ' + attr[i].map.AttributeName.data + '</div>';
			}
		}
	}
	if (listing.isTool) {
		if (item.damage != null) {
			html += '<div style="color: #bebebe;">Durability: ' + (listing.maxDamage - item.damage) + ' / ' + listing.maxDamage + '</div>';
		} else {
			html += '<div style="color: #bebebe;">Durability: ' + listing.maxDamage + ' / ' + listing.maxDamage + '</div>';
		}
	}
	html += '</div>';
	return html;
}

function buildBasicToolTip(webItem, itemName) {
	var item = webItem.item;
	var html = '<div class="toolTipInner">';
	html += '<div style="font-weight: bold;">' + itemName + '</div>';
	if (item.type == "POTION") {
		if (items.potions.hasOwnProperty(item.damage)) {
			html += '<div style="color: #bebebe;">' + items.potions[item.damage].lore + '</div>';
		}
	}
	if (item.meta != null) {
		var meta = item.meta;
		console.log(meta);
		if (meta.enchantments != null) {
			var enchantments = meta.enchantments;
			for (enchantment in enchantments) {
				var ench = (items.enchants[enchantment] == null ? enchantment : items.enchants[enchantment]);
				html += '<div style="color: #bebebe;">' + ench + ' ' + romanize(enchantments[enchantment]) + '</div>';
			}
		}
		if (meta.lore != null) {
			var lore = '';
			for (var i = 0; i < meta.lore.length; i++) {
				lore += escapeHtml(meta.lore[i] + ' ');
			}
			html += '<div style="font-style: italic;">' + lore + '</div>';
		}
		if (meta.attributes != null) {
			html += '<div style="margin-top: 12px;"></div>';
			var attr = meta.attributes.list;
			for (var i = 0; i < attr.length; i++) {
				html += '<div style="color: #3f3ffe;">+' + attr[i].map.Amount.data + ' ' + attr[i].map.AttributeName.data + '</div>';
			}
		}
	}
	if (webItem.isTool) {
		if (item.damage != null) {
			html += '<div style="color: #bebebe;">Durability: ' + (webItem.maxDamage - webItem.damage) + ' / ' + webItem.maxDamage + '</div>';
		} else {
			html += '<div style="color: #bebebe;">Durability: ' + webItem.maxDamage + ' / ' + webItem.maxDamage + '</div>';
		}
	}
	html += '</div>';
	return html;
}

function buildMailToolTip(webItem, itemName) {
	var item = webItem.item;
	var html = '<div class="toolTipInner">';
	//html += '<div style="font-weight: bold;">' + itemName + '</div>';
	html += '<div style="font-weight: bold; float: left;">' + itemName + '</div>';
	var sender = (webItem.sender == null ? '' : 'Sender: ' + webItem.sender);
	html += '<div class="toolTipSeller">' + sender + '</div>';
	html += '<div class="clearfix"></div>';
	if (item.type == "POTION") {
		if (items.potions.hasOwnProperty(item.damage)) {
			html += '<div style="color: #bebebe;">' + items.potions[item.damage].lore + '</div>';
		}
	}
	if (item.meta != null) {
		var meta = item.meta;
		console.log(meta);
		if (meta.enchantments != null) {
			var enchantments = meta.enchantments;
			for (enchantment in enchantments) {
				var ench = (items.enchants[enchantment] == null ? enchantment : items.enchants[enchantment]);
				html += '<div style="color: #bebebe;">' + ench + ' ' + romanize(enchantments[enchantment]) + '</div>';
			}
		}
		if (meta.lore != null) {
			var lore = '';
			for (var i = 0; i < meta.lore.length; i++) {
				lore += escapeHtml(meta.lore[i] + ' ');
			}
			html += '<div style="font-style: italic;">' + lore + '</div>';
		}
		if (meta.attributes != null) {
			html += '<div style="margin-top: 12px;"></div>';
			var attr = meta.attributes.list;
			for (var i = 0; i < attr.length; i++) {
				html += '<div style="color: #3f3ffe;">+' + attr[i].map.Amount.data + ' ' + attr[i].map.AttributeName.data + '</div>';
			}
		}
	}
	if (webItem.isTool) {
		if (item.damage != null) {
			html += '<div style="color: #bebebe;">Durability: ' + (webItem.maxDamage - webItem.damage) + ' / ' + webItem.maxDamage + '</div>';
		} else {
			html += '<div style="color: #bebebe;">Durability: ' + webItem.maxDamage + ' / ' + webItem.maxDamage + '</div>';
		}
	}
	if (webItem.item.meta != null && webItem.item.meta.pages != null) {
		html += '<div style="color: #fefe3f; font-style: italic;">(Click to view contents)</div>';
	}
	html += '</div>';
	return html;
}
var entityMap = {
	"&": "&amp;",
	"<": "&lt;",
	">": "&gt;",
	'"': '&quot;',
	"'": '&#39;',
	"/": '&#x2F;'
};

function escapeHtml(string) {
	return String(string).replace(/[&<>"'\/]/g, function (s) {
		return entityMap[s];
	});
}

function romanize(num) {
	if (!+num)
		return false;
	var digits = String(+num).split(""),
		key = ["", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM",
			"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC",
			"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"
		],
		roman = "",
		i = 3;
	while (i--)
		roman = (key[+digits.pop() + (i * 10)] || "") + roman;
	return Array(+digits.join("") + 1).join("M") + roman;
}