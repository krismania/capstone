var adminView = (function() {
	
	return {
		
		// creates a menu using the passed in list of buttons
		menu: function(items) {
			var menu = document.createElement("ul");
			menu.className = "menu";
			// add list elements for each item
			for (var i = 0; i < items.length; i++) {
				var li = document.createElement("li");
				li.appendChild(items[i]);
				menu.appendChild(li);
			}
			return menu;
		},
		
		console: function() {
			var container = document.createElement("div");
			var addVehicleBtn = document.createElement("button");
			var manageUserBtn = document.createElement("button");
			var hint = document.createElement("p");
			
			hint.innerText = "Tip: Click on a vehicle to view details about it";
			hint.className = "hint";
			
			addVehicleBtn.innerText = "Add New Vehicle";
			manageUserBtn.innerText = "Manage User";
			
			var adminMenu = this.menu([addVehicleBtn, manageUserBtn]);
			
			container.appendChild(adminMenu);
			container.appendChild(hint);
			
			return container;
		}
		
	}
	
})();

sidepane.appendHeader("ADMIN CONSOLE");
sidepane.append(adminView.console())
