<div style="margin: 5px 0px 5px 10px;">
	<%
		boolean isFollowing = sourceObject.fetchIsFollowing()
		def followButtonTitle = isFollowing  ? 'Unfollow' : 'Follow'
	%>
	<button id="followButton" class="btn btn-info btn-small" onclick="followObject('${sourceObject.class.getCanonicalName()}', ${sourceObject.id}, this, '${uGroup.createLink(controller:'activityFeed', action:'follow')}');">${followButtonTitle}</button>
</div>

<r:script>
</r:script>
 