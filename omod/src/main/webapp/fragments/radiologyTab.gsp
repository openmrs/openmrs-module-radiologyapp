
<%
    def dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy")
    ui.includeJavascript("radiologyapp", "radiologyTab.js")
%>

<!-- TODO: don't forget to localize all text, including "Yes" and "No" -->
<script type="text/template" id="radiologyStudyDetailsTemplate">
    <h2>{{- procedure}}</h2>
    <small>Accession Number:</small>
    <span class="report-value"> {{- accessionNumber }}</span><br/>
    <small>Performed by:</small>
    <span class="report-value"> {{- technician }}</span><br/>
    <small>Performed on:</small>
    <span class="report-value"> {{- datePerformed }}</span><br/>
    <small>Images available:</small>
    <span class="report-value"> {{- imagesAvailable ?  '${ ui.message("emr.yes")}' :  '${ ui.message("emr.no")}' }}</span><br/>
    <br/>
{{_.each(reports, function(report) { }}
<span class="radiology-report">
    <h2>Report</h2>
    <small>Reported by:</small>
    <span class="report-value"> {{- report.principalResultsInterpreter }}</span><br>
    <small>Report date:</small>
    <span class="report-value"> {{- report.reportDate }}</span><br>
    <small>Report type:</small>
    <span class="report-value"> {{- report.reportType }}</span>

    <h6>Report Information:</h6>
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