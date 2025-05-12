<%
    ui.decorateWith("appui", "standardEmrPage", [includeBootstrap: true])
    ui.includeJavascript("uicommons", "knockout-2.2.1.js")
    ui.includeJavascript("radiologyapp", "radiologyOrder.js")

    ui.includeCss("radiologyapp", "radiologyOrder.css")

    def isThisVisitActive = activeVisit && activeVisit.visit == visit
    def areProviderLocationAndDateEditable = !isThisVisitActive || sessionContext.currentUser.hasPrivilege("Task: org.openmrs.module.radiologyapp.retroOrder")
%>

<script type="text/javascript" xmlns="http://www.w3.org/1999/html">

    var viewModel =   new StudiesViewModel(${orderables}, ${portableLocations},
            [ 'clinicalHistory',
              ${ areProviderLocationAndDateEditable ? '\'requestedBy\',\'requestedFrom\',\'requestedOn\'' : ''}], // provider/location/date information only mandatory for retrospective entry
            ${ contrastStudies ?: '[]'});

    jQuery(function() {
        jq('button.confirm').click(function(){

            if (!jq(this).attr("disabled")) {
                jq(this).closest("form").submit();
            }

            jq(this).attr('disabled', 'disabled');
            jq(this).addClass("disabled");
            jq(this).find('.icon-spin').css('display', 'inline-block');

        });

        ko.applyBindings(viewModel, jq('#contentForm').get(0) );

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
        { label: "${ ui.format(patient.familyName) }, ${ ui.format(patient.givenName) }", link:'${ui.urlBind("/" + contextPath + dashboardUrl, patient)}' },
        { label: "${ui.message("radiologyapp.task.order." + modality + ".label")}" }
    ];
</script>


${ ui.includeFragment("coreapps", "patientHeader", [ patient: patient ]) }

<div id="contentForm">
    <h1>${ ui.message("radiologyapp.order." + modality + ".title") }</h1>
    <form id="radiology-order" data-bind="submit: handleSubmit" action="${ ui.actionLink("radiologyapp", "radiologyRequisition", "orderRadiology",
            [ successUrl: returnUrl ?: ui.pageLink("coreapps", "patientdashboard/patientDashboard", [ patientId: patient.id, visitId: visit.id ]) ]) }" method="post">
        <input type="hidden" name="patient" value="${ patient.id }"/>
        <input type="hidden" name="modality" value="${ modality }"/>
        <input type="hidden" name="visit" value="${ visit.id }" />

        <div id="who-where-when-view" class="row <% if (areProviderLocationAndDateEditable) { %> hidden <% } %>">
            <div class="col-12 col-lg-4">
                <h5>${ ui.message("radiologyapp.order.requestedBy") }</h5>
                <span>${ ui.format(currentProvider) }</span>
            </div>
            <div class="col-12 col-lg-4">
                <h5>${ ui.message("radiologyapp.order.requestedFrom") }</h5>
                <span>${ ui.format(sessionContext.sessionLocation) }</span>
            </div>
            <div class="col-12 col-lg-4">
                <h5>${ ui.message("radiologyapp.order.requestedOn") }</h5>
                <span>${ ui.format(defaultOrderDate) }</span>
            </div>
        </div>

        <div id="who-where-when-edit" class="row <% if (!areProviderLocationAndDateEditable) { %> hidden <% } %>">
            <div class="col-12 col-lg-4">
                ${ ui.includeFragment("uicommons", "field/dropDown", [
                        id: "requestedBy",
                        label: "radiologyapp.order.requestedBy",
                        formFieldName: "requestedBy",
                        options: providers,
                        classes: ['required'],
                        initialValue: currentProvider.providerId
                ])}
            </div>
            <div class="col-12 col-lg-4">
                ${ ui.includeFragment("uicommons", "field/location", [
                        id: "requestedFrom",
                        label: "radiologyapp.order.requestedFrom",
                        formFieldName: "requestedFrom",
                        classes: ['required'],
                        withTag: "Login Location",
                        initialValue: sessionContext.sessionLocationId
                ])}
            </div>
            <div class="col-12 col-lg-4">
                ${ ui.includeFragment("uicommons", "field/datetimepicker", [
                        id: "requestedOn",
                        label: "radiologyapp.order.requestedOn",
                        formFieldName: "requestedOn",
                        useTime: false,
                        defaultDate: defaultOrderDate,
                        startDate:minOrderDate,
                        endDate: maxOrderDate,
                        classes: ['required']
                ])}
            </div>
        </div>

        <div class="row">
            <div class="left-column col-12 col-lg-6">
                <div class="question">
                    <label for="study-search">${ ui.message("radiologyapp.order.studySearchInstructions") }</label>
                    <input id="study-search" type="text"
                           autofocus="autofocus"
                           data-bind="autocomplete:searchTerm, search:convertedStudies, select:selectStudy, clearValue:function() { return true; }"
                           placeholder="${ ui.message("radiologyapp.order." + modality + ".studySearchPlaceholder") }"/>
                </div>
                <div class="question">
                    <label for="clinical-history-field">
                        <label>${ ui.message("radiologyapp.order.indication") } (${ ui.message("emr.formValidation.messages.requiredField.label") })</label>
                    </label>
                    <div id="clinical-history-field">
                        <textarea class="field-value" rows="5" cols="35" name="clinicalHistory" data-bind="value: clinicalHistory"></textarea>
                        <span class="field-error"  style="display: none" ></span>
                    </div>
                </div>

                <!-- fields to collection creatinine level, only used in contrast exams -->
                <span data-bind="visible: selectedStudiesIncludeContrastStudy()">
                    <div class="question">
                        <label for="creatinine-level-field">
                            <label>${ ui.message("radiologyapp.order.creatinineLevel") } (${ ui.message("emr.formValidation.messages.requiredField.label") })</label>
                        </label>
                        <div id="creatinine-level-field">
                            <input id="creatinine-level-field-input" name="creatinineLevel" class="field-value inline" style="width:50px;" size="10" data-bind="value: creatinineLevel"/>
                            <span class="inline">${ ui.message("radiologyapp.order.creatinineUnits") }</span>
                        </div>
                    </div>
                    <div class="question">
                        ${ ui.includeFragment("uicommons", "field/datetimepicker", [
                                id: "creatinineTestDate",
                                label: "radiologyapp.order.creatinineTestDate",
                                formFieldName: "creatinineTestDate",
                                useTime: false,
                                defaultDate: defaultCreatinineTestDate,
                                startDate:minCreatinineTestDate,
                                endDate: maxCreatinineTestDate,
                                classes: ['required']
                        ])}
                    </div>
                </span>
            </div>
            <div class="right-column col-12 col-lg-6">
                <div class="question">
                    <div class="detail-row">
                        ${ ui.includeFragment("uicommons", "field/radioButtons", [
                                label: ui.message("radiologyapp.order.timing"),
                                formFieldName: "urgency",
                                options: [
                                        [ value: "ROUTINE", label: ui.message("radiologyapp.order.timing.routine"), checked: true ],
                                        [ value: "STAT", label: ui.message("radiologyapp.order.timing.urgent") ]
                                ]
                        ]) }
                    </div>
                </div>

                <% if (modality.equalsIgnoreCase(xrayModalityCode)) { %>
                    <!-- for now, only x-ray (CR) orders can be portable -->
                    <div class="question">
                        <div class="detail-row">
                            <p><label>${ ui.message("radiologyapp.order.portableQuestion") }</label></p>
                            <p>
                                <input type="checkbox" class="field-value" value="portable" data-bind="checked: portable"/>
                                <span>${ ui.message("radiologyapp.yes") }</span>
                                <select name="examLocation" data-bind="enable:portable, options:locations, optionsText:'name', optionsValue:'id', optionsCaption:'${ ui.escapeJs(ui.message("radiologyapp.order.examLocationQuestion")) }', value:portableLocation">
                                </select>
                            </p>
                        </div>
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
        </div>

        <div class="row">
            <div class="col-12" id="bottom">
                <!-- note that setting type="reset" is necessary here to prevent the cancel button from actually submitting the form! -->
                <button type="reset" id="cancel" class="cancel" onclick="location.href = '${returnUrl ?: ui.pageLink("coreapps", "patientdashboard/patientDashboard", [patientId: patient.id])}'">
                    ${ ui.message("radiologyapp.cancel") }
                </button>
                <button type="submit" class="confirm" id="next" data-bind="css: { disabled: !isValid() }, enable: isValid()">
                    ${ ui.message("radiologyapp.save") }
                    <i class="icon-spinner icon-spin icon-2x" style="display: none; margin-left: 10px;"></i>
                </button>
                <br/><br/><br/><br/><br/><br/><br/><br/>
            </div>
        </div>
    </form>
</div>

<div id="creatinine-level-warning" class="dialog" style="display: none">
    <div class="dialog-header">
        <i class="icon-info-sign"></i>
        <h3>${ ui.message("radiologyapp.order.creatinineLevel") }</h3>
    </div>
    <div class="dialog-content">
        <p class="dialog-instructions">${ leadRadiologyTechName ? ui.message("radiologyapp.order.creatinineWarningWithContactInfo",
                leadRadiologyTechName, leadRadiologyTechContactInfo) : ui.message("radiologyapp.order.creatinineWarning") }</p>

        <button class="confirm right">${ ui.message("uicommons.confirm") }</button>
        <button class="cancel">${ ui.message("uicommons.cancel") }</button>
    </div>
</div>

