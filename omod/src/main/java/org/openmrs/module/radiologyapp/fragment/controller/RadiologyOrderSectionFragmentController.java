package org.openmrs.module.radiologyapp.fragment.controller;


import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.module.coreapps.CoreAppsProperties;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class RadiologyOrderSectionFragmentController {
    public void controller(@RequestParam("patientId") Patient patient,
                           FragmentModel model,
                           @SpringBean CoreAppsProperties coreAppsProperties,
                           @SpringBean RadiologyService radiologyService) {

        List<Order> orders = radiologyService.getUnfulfilledRadiologyOrdersForPatient(patient);
        if (orders != null && orders.size() > 5) {
            //display only the last 5 radiology orders
            orders = orders.subList(0, 5);
        }
        model.addAttribute("patient", patient);
        model.addAttribute("orders", orders);
        model.addAttribute("dashboardUrl", coreAppsProperties.getDashboardUrl());

    }
}
