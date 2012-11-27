
<%@page import="org.springframework.security.acls.domain.BasePermission"%>

<%@page import="org.springframework.security.acls.domain.BasePermission"%>
<%@page import="species.utils.ImageType"%>
<%@page import="species.utils.Utils"%>
<%@ page import="species.groups.UserGroup"%>
<html>
<head>
<meta name="layout" content="main" />
<g:set var="entityName" value="${userGroupInstance.name}" />
<title><g:message code="default.show.label"
		args="[userGroupInstance.name]" />
</title>
<script src="https://maps.googleapis.com/maps/api/js?sensor=false"></script>
<r:require modules="userGroups_show" />
</head>
<body>
	<div class="observation span12 bodymarker">
		<uGroup:showSubmenuTemplate  model="['entityName':'About Us']"/>
		<uGroup:rightSidebar model="['userGroupInstance':userGroupInstance]"/>
		<div class="center_panel">
		<div class="super-section userGroup-section">
			<div class="description notes_view">
				${userGroupInstance.description}
			</div>
			<div class="section">
				<div class="prop">
					<span class="name"><i class="icon-time"></i>Founded</span>
					<obv:showDate
						model="['userGroupInstance':userGroupInstance, 'propertyName':'foundedOn']" />
				</div>
			</div>
		</div>
		
		
		<div class="super-section">
			<h3>Driven by</h3>
			<div class="section">
				<h5>
					Founders
				</h5>
				<div id="founders_sidebar"></div>
				<g:link url="${uGroup.createLink(mapping:'userGroup', action:'founders','userGroup':userGroupInstance)}">...</g:link>
			</div>

			<div class="section">
				<h5>
					Members
				</h5>
				<div id="members_sidebar"></div>
				<g:link url="${uGroup.createLink(mapping:'userGroup', action:'user', 'userGroup':userGroupInstance)}">...</g:link>
			</div>
		</div>

		<div class="super-section">
			<h3>Interests</h3>
			<div class="section">
				<h5>
					Species Groups
				</h5>
				<uGroup:interestedSpeciesGroups
					model="['userGroupInstance':userGroupInstance]" />
			</div>

			<div class="section">
				<h5>
					Habitat
				</h5>
				<uGroup:interestedHabitats
					model="['userGroupInstance':userGroupInstance]" />

			</div>
			<div class="section">
				<h5>
					Location
				</h5>
				<uGroup:showLocation model="['userGroupInstance':userGroupInstance]" />
			</div>
			<div class="section">
				<uGroup:showAllTags
					model="['tagFilterByProperty':'UserGroup' , 'tagFilterByPropertyValue':userGroupInstance, 'isAjaxLoad':true]" />
			</div>
		</div>

		<!-- div class="super-section userGroup-section">
			<div class="description notes_view" name="contactEmail">
				Contact us by filling in the following feedback form.</div>
		</div-->
		</div>
	</div>


<r:script>
			function reloadMembers(url) {
				var membersUrl = (url)?url:"${uGroup.createLink(mapping:'userGroup', action:'user', userGroup:userGroupInstance) }"  
				$.ajax({
			       	url: membersUrl,
			           method: "GET",
			           dataType: "json",
			           data:{'isAjaxLoad':true,'onlyMembers':true},
			           success:
					function(data) {
			           	var html='';
			           	$.each(data.result, function(i, item) {
			           		html +="<a href='"+"${uGroup.createLink(controller:'SUser', action:'show')}/"+item.id+"'>"	+ 
			           				"<img src='"+item.icon+"' class='pull-left small_profile_pic'title='"+item.name+"'>"+ 
			           				"</a>";
			           	});
			           	$("#members_sidebar").html(html);
			           }, error: function(xhr, status, error) {
						handleError(xhr, status, error, undefined, function() {
			               	var msg = $.parseJSON(xhr.responseText);
			                   $(".alertMsg").html(msg.msg).removeClass('alert-success').addClass('alert-error');
						});
			           }
				});
			}
			function reloadFounders(url) {
				var foundersUrl = (url)?url:"${uGroup.createLink(mapping:'userGroup', action:'founders','userGroup':userGroupInstance) }"  
				$.ajax({
			       		url: foundersUrl,
			           method: "GET",
			           dataType: "json",
			           data:{'isAjaxLoad':true},
			           success: function(data) {
			           	var html = "";
			           	$.each(data.result, function(i, item) {
			           		html += "<a	href='"+"${uGroup.createLink(controller:'SUser', action:'show')}/"+item.id+"'>"+
			           				"<img src='"+item.icon+"' class='pull-left small_profile_pic' title='"+item.name+"'>"+ "</a>";
			           	});
			           	$("#founders_sidebar").html(html);
			           }, 
			           error: function(xhr, status, error) {
						handleError(xhr, status, error, undefined, function() {
			               	var msg = $.parseJSON(xhr.responseText);
			                   $(".alertMsg").html(msg.msg).removeClass('alert-success').addClass('alert-error');
						});
			           }
				});
			}
			$(document).ready(function(){
				reloadFounders();
				reloadMembers();
			});
		</r:script>
</body>
</html>