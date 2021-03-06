<%@page import="species.utils.Utils"%>
<g:if test="${resource}">
<div class="notes" style="text-align: left;">
    <div>
        <div class="license license_div">
            <i class="slideUp icon-chevron-up pull-right"></i>

            <obv:rating model="['resource':resource, 'class':'gallery_rating']"/>

            <g:each in="${resource?.licenses}" var="l">
            <a href="${l?.url}" target="_blank"> <img class="icon" style="height:auto;margin-right:2px;"
                src="${createLinkTo(dir:'images/license', file: l?.name.value().toLowerCase().replaceAll('\\s+','')+'.png', absolute:true)}"
                alt="${l?.name.value()}" /> </a>
            </g:each>
            <g:if test="${resource.description}">
            <div class="span5 ellipsis multiline" style="margin-left:0px">${resource.description}</div>

            <div style="clear:both;"></div>
            </g:if>


        </div>
        <g:if test="${resource.contributors?.size() > 0}">
        <b>Contributors:</b>
        <ol>
            <g:each in="${resource.contributors}" var="a">
            <li>
            ${a?.name}
            </li>
            </g:each>
        </ol>
        </g:if>
        <g:if test="${resource.attributors?.size() > 0}">
        <b>Attributions:</b>
        <ol>
            <g:each in="${resource.attributors}" var="a">
            <li>
            ${a?.name}
            </li>
            </g:each>
        </ol>
        </g:if>
        <g:if test="${resource.url}">
        <a href="${resource.url}" target="_blank"><b>View image
                source</b> </a>
        </g:if>

    </div>
</div>
</g:if>
