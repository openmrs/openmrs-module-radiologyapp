package org.openmrs.module.radiologyapp.page.controller;


import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.context.AppContextModel;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.coreapps.CoreAppsConstants;
import org.openmrs.module.coreapps.CoreAppsProperties;
import org.openmrs.module.coreapps.contextmodel.PatientContextModel;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.event.ApplicationEventService;
import org.openmrs.module.emrapi.patient.PatientDomainWrapper;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.ui.framework.annotation.InjectBeans;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.page.Redirect;
import org.springframework.web.bind.annotation.RequestParam;

public class RadiologyDashboardPageController {
    public Object controller(@RequestParam("patientId") Patient patient,
                             PageModel model,
                             @InjectBeans PatientDomainWrapper patientDomainWrapper,
                             @SpringBean("adtService") AdtService adtService,
                             @SpringBean("coreAppsProperties") CoreAppsProperties coreAppsProperties,
                             @SpringBean("applicationEventService") ApplicationEventService applicationEventService,
                             UiSessionContext sessionContext) {

        if (!Context.hasPrivilege(CoreAppsConstants.PRIVILEGE_PATIENT_VISITS)) {
            return new Redirect("coreapps", "noAccess", "");
        }
        else if (patient.isVoided() || patient.isPersonVoided()) {
            return new Redirect("coreapps", "patientdashboard/deletedPatient", "patientId=" + patient.getId());
        }

        patientDomainWrapper.setPatient(patient);
        model.addAttribute("patient", patientDomainWrapper);


        Location visitLocation = null;
        try {
            visitLocation = adtService.getLocationThatSupportsVisits(sessionContext.getSessionLocation());
        } catch (IllegalArgumentException ex) {
            // location does not support visits
        }

        VisitDomainWrapper activeVisit = null;
        if (visitLocation != null) {
            activeVisit = adtService.getActiveVisit(patient, visitLocation);
        }
        model.addAttribute("activeVisit", activeVisit);

        AppContextModel contextModel = sessionContext.generateAppContextModel();
        contextModel.put("patient", new PatientContextModel(patient));
        model.addAttribute("appContextModel", contextModel);

        model.addAttribute("dashboardUrl", coreAppsProperties.getDashboardUrl());

        applicationEventService.patientViewed(patient, sessionContext.getCurrentUser());

        return null;
    }

}
