;(function($, window, document, undefined) {
	var $win = $(window);
	var $doc = $(document);

	$doc.ready(function() {
		$('.section-bg img').fullscreener();	

		$('.popup-open').magnificPopup({
			type: "ajax",
			closeOnContentClick: false,
			callbacks: {
				ajaxContentAdded: function() {
					$('input.field, textarea.field').doPlaceholders();
				}
			}
		})

		$win.on('resize', function (){
			$(".entry").css({
				height: 'auto'
			});
			$(".entry").equalizeHeight();	
		})	

		$.fn.equalizeHeight = function() {
			var maxHeight = 0, itemHeight;

			for (var i = 0; i < this.length; i++) {
				itemHeight = $(this[i]).height();
				
				if (maxHeight < itemHeight) {
					maxHeight = itemHeight;
				}
			}
			return this.height(maxHeight);
		}

		$(".entry").equalizeHeight();	

		$.fn.doPlaceholders = function() {
			var $fields = this.filter(function () {
				// Don't re-initialize
				return !$(this).data('didPlaceholders');
			});
			 
			$fields.on('focus blur', function(event) {
				var placeholder = this.title;
				if (event.type === 'focus' && placeholder === this.value) {
					this.value = '';
				} else if (event.type === 'blur' && this.value === '') {
					this.value = placeholder;
				}
			});
			 
			// Set the initial value to the title
			$fields.each(function() {
				if (this.value === '') {
					this.value = this.title;
				}
			});
			 
			// Mark the fields as initialized
			$fields.data('didPlaceholders', true);
			 
			return $fields;
		};
		 
		$('input.field, textarea.field').doPlaceholders();
	});
})(jQuery, window, document);
