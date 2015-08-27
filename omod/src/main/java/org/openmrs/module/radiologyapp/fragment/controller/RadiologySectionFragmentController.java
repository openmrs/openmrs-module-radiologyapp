package org.openmrs.module.radiologyapp.fragment.controller;


import org.openmrs.Patient;
import org.openmrs.module.coreapps.CoreAppsProperties;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.module.radiologyapp.RadiologyStudy;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class RadiologySectionFragmentController {

    public void controller(@RequestParam("patientId") Patient patient,
                           FragmentModel model,
                           @SpringBean CoreAppsProperties coreAppsProperties,
                           @SpringBean RadiologyService radiologyService) {

        List<RadiologyStudy> studies = radiologyService.getRadiologyStudiesForPatient(patient);
        if (studies != null && studies.size() > 5) {
            //display only the last 5 studies or reports
            studies = studies.subList(0, 5);
        }
        model.addAttribute("patient", patient);
        model.addAttribute("studies", studies);
        model.addAttribute("dashboardUrl", coreAppsProperties.getDashboardUrl());
    }

}
