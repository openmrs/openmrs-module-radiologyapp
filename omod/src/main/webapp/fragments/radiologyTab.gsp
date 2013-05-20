
<%
    def dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy")
    ui.includeJavascript("radiologyapp", "radiologyTab.js")
%>

<!-- TODO: don't forget to localize all text, including "Yes" and "No" -->
<script type="text/template" id="radiologyStudyDetailsTemplate">
    <h2>{{- procedure}}</h2>
    <div class="visit-status">
        <i class="icon-time small"></i>
        {{- datePerformed }}
    </div>
    <small>Accession number:</small>
    <span class="report-value"> {{- accessionNumber }}</span><br/>
    <% if (technician != null) { %>
    <small>Performed by:</small>
    <span class="report-value"> {{- technician }}</span><br/>
    <% } %>
    <small>Images available:</small>
    <span class="report-value"> {{- imagesAvailable ?  '${ ui.message("emr.yes")}' :  '${ ui.message("emr.no")}' }}</span>
    <br/>
{{_.each(reports, function(report) { }}
<span class="radiology-report">
    <div class="visit-status">
        <span class="report-value">{{- report.reportType }} report</span> by <span class="report-value">{{- report.principalResultsInterpreter }}</span>
        <i class="icon-time small"></i>
        {{- report.reportDate }}
    </div>
    <pre>{{- report.reportBody }}</pre>
</span>
    {{ }) }}
</script>
<script type="text/javascript">
    jq(function(){
        loadRadiologyTemplates();
    });
</script>

<!-- TODO: change so that it doesn't use visits styling! -->

<ul id="visits-list">
    <% studies.each {  %>
    <li class="viewRadiologyStudyDetails" studyAccessionNumber="${it.accessionNumber}">
        <span class="visit-date">
            <i class="icon-time"></i>
            ${dateFormat.format(it.datePerformed)}
        </span>
        <span class="visit-primary-diagnosis">
            ${ui.format(it.procedure)}
        </span>
        <span class="arrow-border"></span>
        <span class="arrow"></span>
    </li>
    <% } %>
    <% if(studies.size == 0) { %>
    ${ ui.message("radiologyapp.noStudies.label")}
    <% } %>
</ul>

<!-- TODO: we can remove this outer div once we set the study-details div to the same styling as the visit-details div -->
<div id="visit-details">
<div id="study-details">

</div>
</div>