<%
    ui.decorateWith("appui", "standardEmrPage")

    ui.includeJavascript("radiologyapp", "knockout-2.1.0.js")
    ui.includeJavascript("radiologyapp", "radiologyOrder.js")

    ui.includeCss("radiologyapp", "radiologyOrder.css")
%>

<script type="text/javascript" xmlns="http://www.w3.org/1999/html">
    jQuery(function() {
        jq('button.confirm').click(function(){

            if (!jq(this).attr("disabled")) {
                jq(this).closest("form").submit();
            }

            jq(this).attr('disabled', 'disabled');
            jq(this).addClass("disabled");

        });

        ko.applyBindings( new StudiesViewModel(${orderables}, ${portableLocations}), jq('#contentForm').get(0) );

        // Preventing form submission when pressing enter on study-search input field
        jq('#study-search').bind('keypress', function(eventKey) {
            if(event.keyCode == 13) {
                event.preventDefault();
                return false;
            }
        });
    });
</script>

<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.format(patient.familyName) }, ${ ui.format(patient.givenName) }", link:'${ui.pageLink("coreapps", "patientdashboard/patientDashboard", [patientId: patient.id])}' },
        { label: "${ui.message("radiologyapp.task.order." + modality + ".label")}" }
    ];
</script>


${ ui.includeFragment("emr", "patientHeader", [ patient: patient ]) }

<div id="contentForm">
    <h1>${ ui.message("radiologyapp.order." + modality + ".title") }</h1>
    <form action="${ ui.actionLink("radiologyapp", "radiologyRequisition", "orderRadiology") }" data-bind="submit: isValid">
        <input type="hidden" name="successUrl" value="${ ui.pageLink("coreapps", "patientdashboard/patientDashboard", [ patientId: patient.id ]) }"/>
        <input type="hidden" name="patient" value="${ patient.id }"/>
        <input type="hidden" name="requestedBy" value="${ currentProvider.id }"/>


        <div class="left-column">
            <label for="study-search">${ ui.message("radiologyapp.order.studySearchInstructions") }</label>
            <input id="study-search" type="text"
                   autofocus="autofocus"
                   data-bind="autocomplete:searchTerm, search:convertedStudies, select:selectStudy, clearValue:function() { return true; }"
                   placeholder="${ ui.message("radiologyapp.order." + modality + ".studySearchPlaceholder") }"/>
            ${ ui.includeFragment("emr", "field/textarea", [ label:"<label>" + ui.message("radiologyapp.order.indication") + "</label>", formFieldName: "clinicalHistory", labelPosition: "top", rows: 5, cols: 35 ]) }
        </div>

        <div class="right-column">
            <div class="row">
                ${ ui.includeFragment("uicommons", "field/radioButtons", [
                        label: ui.message("radiologyapp.order.timing"),
                        formFieldName: "urgency",
                        options: [
                                [ value: "ROUTINE", label: ui.message("radiologyapp.order.timing.routine"), checked: true ],
                                [ value: "STAT", label: ui.message("radiologyapp.order.timing.urgent") ]
                        ]
                ]) }
            </div>

            <% if (modality.equalsIgnoreCase(org.openmrs.module.radiologyapp.RadiologyConstants.XRAY_MODALITY_CODE)) { %>
                <!-- for now, only x-ray (CR) orders can be portable -->
                <div class="row">
                    <p><label>${ ui.message("radiologyapp.order.portableQuestion") }</label></p>
                    <p>
                        <input type="checkbox" class="field-value" value="portable" data-bind="checked: portable"/>
                        <span>${ ui.message("radiologyapp.yes") }</span>
                        <select name="examLocation" data-bind="enable:portable, options:locations, optionsText:'name', optionsValue:'id', optionsCaption:'${ ui.escapeJs(ui.message("radiologyapp.order.examLocationQuestion")) }', value:portableLocation">
                        </select>
                    </p>
                </div>
            <% } %>


             <div data-bind="visible: selectedStudies().length == 0">
                <span style="color: blue;">${ ui.message("radiologyapp.order.noStudiesSelected") }</span>
            </div>
            <div data-bind="visible: selectedStudies().length > 0">
                <label>${ ui.message("radiologyapp.order.selectedStudies") }</label>
                <ul id="selected-studies" data-bind="foreach: selectedStudies">
                    <li>
                        <input type="hidden" data-bind="value: id" name="studies" />
                        <span data-bind="text: name"></span>
                        <span data-bind="click: \$root.unselectStudy">X</span>
                    </li>
                </ul>
            </div>
        </div>

        <div id="bottom">
            <button id="cancel" class="cancel" onclick="location.href = emr.pageLink('coreapps', 'patientdashboard/patientDashboard', { patientId: <%= patient.id %> })">
                ${ ui.message("radiologyapp.cancel") }
            </button>
            <button type="submit" class="confirm" id="next" data-bind="css: { disabled: !isValid() }, enable: isValid()">
                ${ ui.message("radiologyapp.save") }
            </button>
        </div>
    </form>
</div>

