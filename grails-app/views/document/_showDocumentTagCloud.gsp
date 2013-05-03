<%@ page import="content.eml.Document"%>

<div style="width:235px;">
	<h3>Project Tags</h3>
	<g:if test="${tags}">
		<tc:tagCloud bean="${Document}" controller="document" action="browser" sort="${true}" style
						color="${[start: '#084B91', end: '#9FBBE5']}"
						size="${[start: 12, end: 30, unit: 'px']}"
						paramName='tag'/>
						
		<span class="pull-right">more tags</span>
	</g:if>					
	<g:else>
		<span class="msg" style="padding-left: 50px;">No tags</span>
	</g:else>
</div>
