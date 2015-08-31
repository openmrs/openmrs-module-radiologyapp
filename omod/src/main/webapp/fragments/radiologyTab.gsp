
<%
    def dateFormat = new java.text.SimpleDateFormat("dd MMM yyyy")
    ui.includeJavascript("radiologyapp", "radiologyTab.js")
    ui.includeCss("radiologyapp", "radiologyTab.css")

    def studyNumber = null
    if (param.studyordernumber) {
        studyNumber = param.studyordernumber[0]
    }
%>

<script type="text/template" id="radiologyStudyDetailsTemplate">
    <h2>{{- procedure ? procedure : '${ ui.message("radiologyapp.procedure.unknown")}' }}</h2>
    <div class="status-container">
        <i class="icon-time small"></i>
        {{- datePerformed }}
    </div>
    <small>${ ui.message('radiologyapp.orderNumber.label') }:</small>
    <span class="report-value"> {{- orderNumber }}</span><br/>
    <small>${ ui.message('radiologyapp.performedBy.label') }:</small>
    <span class="report-value"> {{- technician }}</span><br/>
    <small>${ ui.message('radiologyapp.imagesAvailable.label') }:</small>
    <span class="report-value"> {{- imagesAvailable ?  '${ ui.message("emr.yes")}' :  '${ ui.message("emr.no")}' }}</span>
    <br/>
{{_.each(reports, function(report) { }}
<span class="radiology-report">
    <div class="status-container">
        <span class="report-value">{{- report.reportType }} ${ ui.message('radiologyapp.report.text') }</span> ${ ui.message('radiologyapp.by.text') } <span class="report-value">{{- report.principalResultsInterpreter }}</span>
        <i class="icon-time small"></i>
        {{- report.reportDate }}
    </div>
    <pre class="report-body">{{- report.reportBody }}</pre>
</span>
    {{ }) }}
</script>
<script type="text/javascript">
    jq(function(){
        loadRadiologyTemplates("${ studyNumber }");
    });
</script>

<ul class="left-menu">
    <% studies.each {  %>
    <li class="menu-item viewRadiologyStudyDetails" studyOrderNumber="${it.orderNumber}">
        <span class="menu-date">
            <i class="icon-time"></i>
            ${dateFormat.format(it.datePerformed)}
        </span>
        <span class="menu-title">
            ${ it.procedure ? ui.format(it.procedure) :  ui.message("radiologyapp.procedure.unknown") }
        </span>
        <span class="arrow-border"></span>
        <span class="arrow"></span>
    </li>
    <% } %>
</ul>
<% if(studies.size == 0) { %>
    ${ ui.message("radiologyapp.noStudies.label")}
<% } %>
<% if(studies.size != 0) { %>
<div id="study-details" class="main-content"></div>
<% } %>
</div>