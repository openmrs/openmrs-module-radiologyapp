
<%
    def dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy")
    ui.includeJavascript("radiologyapp", "radiologyTab.js")
%>

<style>
    pre {
        font-size: 14px;
    }
</style>


<!-- TODO: don't forget to localize all text, including "Yes" and "No" -->

<script type="text/template" id="radiologyStudyDetailsTemplate">

    <h2>{{- procedure}}</h2>

    Accession Number: {{- accessionNumber }}<br/>
    Performed by {{- technician }} on {{- datePerformed }}<br/>
    Images available: {{- imagesAvailable ?  '${ ui.message("emr.yes")}' :  '${ ui.message("emr.no")}' }}<br/>

    <br/>

    {{_.each(reports, function(report) { }}
-----------------------------------------------------------------------------------------
<h6>Reported by {{- report.principalResultsInterpreter }} on {{- report.reportDate }}</h6>
<h6>Report type: {{- report.reportType }}</h6>

 <pre>
{{- report.reportBody }}
</pre>
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
    ${ ui.message("radiologyapp.noStudies.label")}
    <% } %>
</ul>

<!-- TODO: we can remove this outer div once we set the study-details div to the same styling as the visit-details div -->
<div id="visit-details">
<div id="study-details">

</div>
</div>