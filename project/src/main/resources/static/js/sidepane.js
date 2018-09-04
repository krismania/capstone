// controls the sidepane
sidepane = (function() {
	
	// the side pane element
	var sp = document.getElementById("sidepane-content");
	
	var openCallback = function() {console.log("Sidepane Opened")};
	var closeCallback = function() {console.log("Sidepane Closed")};
	
	var isOpen = false;
	
	return {
		
		setOpenCallback: function(callback) {
			openCallback = callback;
		},
		
		setCloseCallback: function(callback) {
			closeCallback = callback;
		},
		
		open: function() {
			if (!isOpen) {
				isOpen = true;
				openCallback();
			}
		},
		
		close: function() {
			if (isOpen) {
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
			var close = document.createElement("button");
			
			header.innerText = headerText;
			close.className = "exit";
			close.addEventListener("click", this.close);
			close.innerText = "╳";
			
			sp.appendChild(header);
			sp.appendChild(close);
		}
	
	}
	
})();
