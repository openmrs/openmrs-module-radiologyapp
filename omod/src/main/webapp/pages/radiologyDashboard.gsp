<%
    // although called "patientDashboard" this is actually the patient visits screen, and clinicianfacing/patient is the main patient dashboard

    ui.decorateWith("appui", "standardEmrPage")

    ui.includeCss("coreapps", "patientdashboard/patientDashboard.css")

    ui.includeJavascript("uicommons", "bootstrap-collapse.js")
    ui.includeJavascript("uicommons", "bootstrap-transition.js")


%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.escapeJs(ui.format(patient.patient)) }" ,
            link: '${ ui.urlBind("/" + contextPath + dashboardUrl, [ patientId: patient.patient.id ] ) }'},
        { label: "${ ui.message("coreapps.clinicianfacing.radiology") }",
            link: "${ ui.pageLink("coreapps", "patientdashboard/radiologyDashboard", [app: "appointmentschedulingui.schedulingAppointmentApp"]) }" }
    ];

    jq(function(){
        // make sure we reload the page if the location is changes; this custom event is emitted by by the location selector in the header
        jq(document).on('sessionLocationChanged', function() {
            window.location.reload();
        });
    });

    var patient = { id: ${ patient.id } };
</script>


${ ui.includeFragment("coreapps", "patientHeader", [ patient: patient.patient, appContextModel: appContextModel ]) }
<div id="radiologyapp-id">
    ${ ui.includeFragment("radiologyapp", "radiologyTab", [ patient: patient ]) }
</div>
