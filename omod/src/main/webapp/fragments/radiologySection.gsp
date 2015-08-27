<%
    def patient = config.patient
%>
<div class="info-section">
    <div class="info-header">
        <i class="icon-camera"></i>
        <h3>${ ui.message("coreapps.clinicianfacing.radiology").toUpperCase() }</h3>
        <% if (context.hasPrivilege("Task: org.openmrs.module.radiologyapp.tab")) { %>
        <a href='${ui.pageLink("radiologyapp", "radiologyDashboard", [patientId: patient.id, returnUrl: ui.urlBind("/" + contextPath + dashboardUrl, [ patientId: patient.patient.id ] )])}' class="right">
            <i class="icon-share-alt edit-action" title="${ ui.message("coreapps.edit") }"></i>
        </a>
        <% } %>
    </div>
    <div class="info-body">
        <% if (studies.isEmpty()) { %>
        ${ui.message("coreapps.none")}
        <% } %>
        <ul>
            <% studies.each { it -> %>
            <li class="clear">
                <a href="" class="visit-link">
                    ${ ui.formatDatePretty(it.datePerformed) }
                </a>
                <div class="tag">
                    ${ it.procedure ? ui.format(it.procedure) :  ui.message("radiologyapp.procedure.unknown") }
                </div>
            </li>
            <% } %>
        </ul>
    </div>
</div>

