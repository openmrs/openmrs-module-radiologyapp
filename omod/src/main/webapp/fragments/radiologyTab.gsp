
<%
    def dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy")
%>

<!-- TODO: change so that it doesn't use visits styling! -->

<ul id="visits-list">
    <% studies.each {  %>
    <li class="viewVisitDetails" >
        <span class="visit-date">
            ${ui.format(it.procedure)}
        </span>
        <span class="visit-primary-diagnosis">
            <i class="icon-time"></i>
            ${dateFormat.format(it.datePerformed)}
        </span>
        <span class="arrow-border"></span>
        <span class="arrow"></span>
    </li>
    <% } %>
    <% if(studies.size == 0) { %>
    ${ ui.message("emr.patientDashBoard.noVisits")}
    <% } %>
</ul>