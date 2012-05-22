<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="layout" content="main" />
<g:set var="entityName"
	value="${message(code: 'observation.label', default: 'Search Results')}" />
<title><g:message code="default.list.label" args="[entityName]" />
</title>
<link rel="stylesheet"
	href="${resource(dir:'css',file:'tagit/tagit-custom.css')}"
	type="text/css" media="all" />
<script type="text/javascript"
	src="http://maps.google.com/maps/api/js?sensor=true"></script>
<g:javascript src="location/google/markerclusterer.js"></g:javascript>

<g:javascript src="tagit.js"></g:javascript>
<g:javascript src="jquery/jquery.autopager-1.0.0.js"></g:javascript>
<g:javascript
	src="jquery/jquery-history-1.7.1/scripts/bundled/html4+html5/jquery.history.js" />

</head>
<body>
	<div class="container outer-wrapper">
		<div class="row">
			<div class="span12">
				<div class="page-header clearfix">
						<h1>
							<g:message code="default.observation.heading" args="[entityName]" />
						</h1>
				</div>

				<g:if test="${flash.message}">
					<div class="message">
						${flash.message}
					</div>
				</g:if>
				
				<ul id="searchResultsTabs" class="nav nav-tabs">
				  <li><a href="${createLink(controller:'species', action:'search')}" >Species</a></li>
				  <li><a href="${createLink(controller:'observation', action:'search')}" >Observations</a></li>
				  <li><a href="${createLink(controller:'SUser', action:'search')}" >Users</a></li>
				</ul>
				<div class="searchResults">
					<obv:showObservationsListWrapper/>
				</div>


			</div>
		</div>
	</div>
</body>
</html>