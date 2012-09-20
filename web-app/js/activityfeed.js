var oldFeedProcessing = false;
var serverTimeDiff = null;

function loadOlderFeedsInAjax(targetComp){
	var url = $(targetComp).children('input[name="feedUrl"]').val();
	var feedType = $(targetComp).children('input[name="feedType"]').val();
	
	$.ajax({
 		url: url,
		dataType: "json",
		data: getFeedParams("older", targetComp),
		success: function(data) {
			if(data.showFeedListHtml){
				var htmlData = $(data.showFeedListHtml);
				dcorateCommentBody(htmlData.find('.yj-message-body'));
				htmlData = removeDuplicateFeed($(targetComp).children('ul'), htmlData, feedType, "older");
    			$(targetComp).children('ul').append(htmlData);
				$(targetComp).children('input[name="olderTimeRef"]').val(data.olderTimeRef);
				updateRelativeTime(data.currentTime);
				if(data.remainingFeedCount && data.remainingFeedCount > 0){
					$(targetComp).children('a').text("Show " + data.remainingFeedCount + " older feeds >>");
				}else{
					$(targetComp).children('a').hide();
				}
			}
			oldFeedProcessing = false;
		}, error: function(xhr, status, error) {
			oldFeedProcessing = false;
			alert(xhr.responseText);
	   	}
	});
}

function loadNewerFeedsInAjax(targetComp){
	var url = $(targetComp).children('input[name="feedUrl"]').val();
	var feedType = $(targetComp).children('input[name="feedType"]').val();
	
	$.ajax({ 
     	url:url,
		dataType: 'json', 
		data: getFeedParams("newer", targetComp),	
		success: function(data, statusText, xhr, form) {
			if(data.showFeedListHtml){
        		var refreshType = $(targetComp).children('input[name="refreshType"]').val();
        		if(refreshType == "auto"){
        			$(targetComp).hide().fadeIn(3000);
        		}
    			var htmlData = $(data.showFeedListHtml);
    			dcorateCommentBody(htmlData.find('.yj-message-body'));
    			htmlData = removeDuplicateFeed($(targetComp).children('ul'), htmlData, feedType, "newer");
    			$(targetComp).children('ul').prepend(htmlData);
    			$(targetComp).children('input[name="newerTimeRef"]').val(data.newerTimeRef);
    			updateRelativeTime(data.currentTime);
        	}
        	return false;
        },
        error:function (xhr, ajaxOptions, thrownError){
        	//alert("error ====");
        	//successHandler is used when ajax login succedes
        	var successHandler = this.success, errorHandler = undefined;
        	handleError(xhr, ajaxOptions, thrownError, successHandler, errorHandler);
		} 
 	});
}

function removeDuplicateFeed(parentList, newList, feedType, feedTimeType){
	if(feedType === "Specific"){
		return newList
	}
	if(feedTimeType == "older"){
		var newListStr = ""
		$(newList).each(function(index) {
			var liClass = $(this).attr("class");
			if(liClass){
				if(feedType === "GroupSpecific" && liClass.match("^species.groups.UserGroup")){
					newListStr = newListStr + '<li class="'+ liClass +'">' + $(this).html() + '</li>';
				}else{
					var selector = 'li[class="' + liClass +  '"]'
					var dupEle = $(parentList).children(selector);
					if(dupEle.attr("class") == undefined){
						newListStr = newListStr + '<li class="'+ liClass +'">' + $(this).html() + '</li>';
					}	
				}
			}
		});
		return $(newListStr)
	}
	
	$(newList).each(function(index) {
		var liClass = $(this).attr("class");
		if(liClass){
			if(!(feedType === "GroupSpecific" && liClass.match("^species.groups.UserGroup"))){
				var selector = 'li[class="' + liClass +  '"]'
				$(parentList).children(selector).remove();
			}
		}
		
	});
	return newList;
}


function getFeedParams(timeLine, targetComp){
	var feedParams = {}; 
	
	feedParams["rootHolderId"] = $(targetComp).children('input[name="rootHolderId"]').val();
	feedParams["rootHolderType"] = $(targetComp).children('input[name="rootHolderType"]').val();
	feedParams["activityHolderId"] = $(targetComp).children('input[name="activityHolderId"]').val();
	feedParams["activityHolderType"] = $(targetComp).children('input[name="activityHolderType"]').val();
	feedParams["feedType"] = $(targetComp).children('input[name="feedType"]').val();
	feedParams["feedCategory"] = $(targetComp).children('input[name="feedCategory"]').val();
	feedParams["feedClass"] = $(targetComp).children('input[name="feedClass"]').val();
	feedParams["feedPermission"] = $(targetComp).children('input[name="feedPermission"]').val();
	
	feedParams["refreshType"] = $(targetComp).children('input[name="refreshType"]').val();
	feedParams["timeLine"] = timeLine;
	if(timeLine === "newer"){
		feedParams["refTime"] = $(targetComp).children('input[name="newerTimeRef"]').val();
	}else{
		feedParams["refTime"] = $(targetComp).children('input[name="olderTimeRef"]').val();
	}
	return feedParams;
}

function setUpFeedForTarget(targetComp){
	if(targetComp === null){
		return; 
	}
	
	//var feedType = $(targetComp).children('input[name="feedType"]').val();
	var refreshType = $(targetComp).children('input[name="refreshType"]').val();
	//var url = $(targetComp).children('input[name="feedUrl"]').val();
	
	if(refreshType === "auto"){
		pollForFeeds(targetComp); //to get newer feeds
		autoLoadOnScroll(targetComp); // to get older feeds on scroll bottom
		loadOlderFeedsInAjax(targetComp); // to load some feeds to start with
	}
}

function setUpFeed(timeUrl){
	initRelativeTime(timeUrl);
	setUpFeedForTarget(getTargetComp());
}

function initRelativeTime(url){
	if(!serverTimeDiff){
		$.ajax({
	 		url: url,
			dataType: "json",
			success: function(data) {
				serverTimeDiff = parseInt(data) - new Date().getTime();
				$('body').timeago({serverTimeDiff:serverTimeDiff});
			}, error: function(xhr, status, error) {
				alert(xhr.responseText);
		   	}
		});	
	}
}

function updateRelativeTime(){
	$('.timeago').timeago({serverTimeDiff:serverTimeDiff});
}

function pollForFeeds(targetComp){
	window.setInterval(function(){
		if($(window).scrollTop() < 250 ){
			loadNewerFeedsInAjax(targetComp);
		}
	}, 2000);
} 

function autoLoadOnScroll(targetComp){
	$(window).scroll(function() {
		if(oldFeedProcessing){
			return false;
		}
		if($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
			oldFeedProcessing = true;
			loadOlderFeedsInAjax(targetComp);
		}
	});	
}

function removeActivity(targetComp){
	$(targetComp).closest('.activityFeed-container').parent().remove();
}

function getTargetComp(){
	var targetComp = $('.activityfeedAll');
	if(targetComp.length > 0){
		return targetComp;
	}
	
	targetComp = $('.activityfeedGeneric');
	if(targetComp.length > 0){
		return targetComp;
	}

	targetComp = $('.activityfeedMyFeeds');
	if(targetComp.length > 0){
		return targetComp;
	}

	targetComp = $('.activityfeedGroupSpecific');
	if(targetComp.length > 0){
		return targetComp;
	}
		
	targetComp = $('.activityfeedSpecific');
	if(targetComp.length == 1){
		return targetComp;
	}
	return null;
}

function updateFeeds(){
	var targetComp = getTargetComp();
	if(targetComp){
		var refreshType = $(targetComp).children('input[name="refreshType"]').val();
		if(refreshType == "manual"){
			loadNewerFeedsInAjax(targetComp);	
		}
	}
}

function updateFeedComponent(targetComp, feedCategory){
	 $(targetComp).children('input[name="feedCategory"]').val(feedCategory);
	 $(targetComp).children('input[name="newerTimeRef"]').val("");
	 $(targetComp).children('input[name="olderTimeRef"]').val("");
	 $(targetComp).children('ul').empty();
	 loadOlderFeedsInAjax(targetComp);
}

$('.feed_filter_label').click(function(){
	var caret = '<span class="caret"></span>'
	if($.trim(($(this).html())) == $.trim($("#feedFilterButton").html().replace(caret, ''))){
		$("#feedFilter").hide();
		return false;
	}
	$('.feed_filter_label.active').removeClass('active');
	$(this).addClass('active');
    $("#feedFilterButton").html($(this).html() + caret);
    $("#feedFilter").hide();
    var feedCategory =  $(this).attr("value");
    var targetComp =  $(this).closest(".feedFilterDiv").next(".activityfeed");
    updateFeedComponent(targetComp, feedCategory);
    return false;   
});

