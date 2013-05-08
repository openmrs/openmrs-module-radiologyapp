
<%
    def dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy")
    ui.includeJavascript("radiologyapp", "radiologyTab.js")
%>



<script type="text/template" id="radiologyStudyDetailsTemplate">

    <h4>{{- procedure}}</h4>

    Accession Number: {{- accessionNumber }}<br/>
    Performed by {{- technician }} on {{- datePerformed }}<br/>
    Images available: {{- imagesAvailable }}<br/>

    <br/>

    {{_.each(reports, function(report) { }}
<pre>
Reported by {{- report.principalResultsInterpreter }} on {{- report.reportDate }}
Report type: {{- report.reportType }}

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

<ul id="study-details">

</ul>