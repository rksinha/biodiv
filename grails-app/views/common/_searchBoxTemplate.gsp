<g:javascript>

$(document).ready(function(){
	$("#searchTextField").val('${responseHeader?.params?.q}')
	
	var cache = {},
		lastXhr;
	$("#searchTextField").catcomplete({
	 	 appendTo: '#mainSearchForm',
		 source:function( request, response ) {
				var term = request.term;
				if ( term in cache ) {
					response( cache[ term ] );
					return;
				}

				lastXhr = $.getJSON( "${createLink(action: 'nameTerms', controller:'search')}", request, function( data, status, xhr ) {
					cache[ term ] = data;
					if ( xhr === lastXhr ) {
						response( data );
					}
				});
			},focus: function( event, ui ) {
				$("#canName").val("");
				$( "#searchTextField" ).val( ui.item.label.replace(/<.*?>/g,"") );
				return false;
			},
			select: function( event, ui ) {
				if( ui.item.category == 'Names') {
					$( "#searchTextField" ).val( 'canonical_name:"'+ui.item.value+'" '+ui.item.label.replace(/<.*?>/g,'') );
				} else {
					$( "#searchTextField" ).val( ui.item.label.replace(/<.*?>/g,'') );
				}
				$( "#canName" ).val( ui.item.value );
				//$( "#name-description" ).html( ui.item.value ? ui.item.label.replace(/<.*?>/g,"")+" ("+ui.item.value+")" : "" );
				//ui.item.icon ? $( "#name-icon" ).attr( "src",  ui.item.icon).show() : $( "#name-icon" ).hide();
				$( "#searchbox" ).submit();
				return false;
			}
	}).data( "catcomplete" )._renderItem = function( ul, item ) {
			if(item.category == "General") {
				return $( "<li class='grid_4'  style='list-style:none;'></li>" )
					.data( "item.autocomplete", item )
					.append( "<a>" + item.label + "</a>" )
					.appendTo( ul );
			} else {
				if(!item.icon) {
					item.icon =  "${createLinkTo(file:"no-image.jpg", base:grailsApplication.config.speciesPortal.resources.serverURL)}"
				}  
				return $( "<li class='grid_4' style='list-style:none;'></li>" )
					.data( "item.autocomplete", item )
					.append( "<img class='group_icon' style='float:left; background:url(" + item.icon+" no-repeat); background-position:0 -100px; width:50px; height:50px;opacity:0.4;' class='ui-state-default icon' style='float:left' /><a>" + item.label + ((item.desc)?'<br>(' + item.desc + ')':'')+"</a>" )
					.appendTo( ul );
			}
		};;
	
	$( "#search" ).click(function() {
			$( "#searchbox" ).submit();
	});
});
</g:javascript>
	<div id="mainSearchForm">
	<form method="get"
		action="${createLink(controller:'search', action:'select') }"
		id="searchbox" class="form-horizontal">
		<div class="input-append">
			<input type="text" name="query" id="searchTextField" value=""
			size="26" class="span4"
			placeholder="Enter your search key" /><input id="search"  class="btn btn-primary searchButton" type="submit"
			value="Search"/></div>

		<g:hiddenField name="start" value="0" />
		<g:hiddenField name="rows" value="10" />
		<g:hiddenField name="sort" value="score desc" />
		<g:hiddenField name="fl" value="id, name" />

		<!-- 
		<g:hiddenField name="hl" value="true" />
		<g:hiddenField name="hl.fl" value="message" />
		<g:hiddenField name="hl.snippets" value="3" />
		 -->
		
	</form>
	</div>