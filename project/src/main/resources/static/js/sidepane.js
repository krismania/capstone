// controls the sidepane
sidepane = (function() {
	
	// the side pane element
	var sp = document.getElementById("sidepane-content");
	
	var openCallback = function() {console.log("Sidepane Opened")};
	var closeCallback = function() {console.log("Sidepane Closed")};
	
	var isOpen = false;
	var isStatic = false
	
	return {
		
		setOpenCallback: function(callback) {
			openCallback = callback;
		},
		
		setCloseCallback: function(callback) {
			closeCallback = callback;
		},
		
		// force the sidepane open & prevent closing
		setStatic: function(value) {
			isStatic = value;
			isOpen = true;
			openCallback();
		},
		
		open: function() {
			if (!isStatic && !isOpen) {
				isOpen = true;
				openCallback();
			}
		},
		
		close: function() {
			if (!isStatic && isOpen) {
				isOpen = false;
				closeCallback();
			}
		},
		
		// empty the sidepane
		clear: function() {
			while (sp.firstChild) {
				sp.removeChild(sp.firstChild);
			}
		},
		
		// append the given node without clearing
		append: function(node) {
			sp.appendChild(node);
		},
		
		// create a header with a close button
		appendHeader: function(headerText) {
			var header = document.createElement("h2");
			header.innerText = headerText;
			sp.appendChild(header);
			if (!isStatic) {
				var close = document.createElement("button");
				close.className = "exit";
				close.addEventListener("click", this.close);
				close.innerText = "╳";
				sp.appendChild(close);
			}
		}
	
	}
	
})();
