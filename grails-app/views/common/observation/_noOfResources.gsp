
<%@page import="species.Resource.ResourceType"%>
<%
def iconClass='';
def noOfResources = instance.fetchResourceCount();
%>
        <g:if test="${noOfResources}">
        <div class="story-footer" style="right:0px;${bottom?:''}z-index:5;${bottom?'background-color:whitesmoke':''}">
                    <g:each in="${noOfResources}" var="no">
                        <div class="footer-item">
                            <i class="${no[0].iconClass()}" title="No of ${no[0].value()}s"></i>
                                <span class="">${no[1]}</span>
                        </div>
                    </g:each>
                </div>
        </g:if>


