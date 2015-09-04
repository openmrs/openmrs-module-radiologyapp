<%
    ui.includeCss("radiologyapp", "radiologyOrder.css")
    def patient = config.patient
%>
<div class="info-section">
    <div class="info-header">
        <i class="icon-camera"></i>
        <h3>${ ui.message("radiologyapp.app.orders").toUpperCase() }</h3>
        <% if (context.hasPrivilege("Task: org.openmrs.module.radiologyapp.tab")) { %>
        <a href="/${contextPath}/coreapps/patientdashboard/patientDashboard.page?patientId=${patient.id}" class="right">
            <i class="icon-share-alt edit-action" title="${ ui.message("coreapps.edit") }"></i>
        </a>
        <% } %>
    </div>
    <div class="info-body">
        <% if (orders && orders.isEmpty()) { %>
        ${ui.message("coreapps.none")}
        <% } %>
        <ul>
            <% orders.each { it -> %>
            <li class="clear">
                <a href="/${contextPath}/coreapps/patientdashboard/patientDashboard.page?visitId=${it.encounter.visit.id}&patientId=${patient.id}" class="visit-link">
                    ${ ui.formatDatePretty(it.dateCreated) }
                </a>
                <div class="studytag ">
                    ${ it.concept ? ui.format(it.concept) :  ui.message("radiologyapp.procedure.unknown") }
                </div>
            </li>
            <% } %>
        </ul>
    </div>
</div>

