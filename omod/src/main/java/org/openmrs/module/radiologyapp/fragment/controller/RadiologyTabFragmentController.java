package org.openmrs.module.radiologyapp.fragment.controller;

import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.module.radiologyapp.RadiologyStudy;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;

import java.util.List;

public class RadiologyTabFragmentController {

    public void controller(EmrContext emrContext, FragmentModel model,
                            @SpringBean RadiologyService radiologyService) {

        List<RadiologyStudy> studies = radiologyService.getRadiologyStudiesForPatient(emrContext.getCurrentPatient());
        model.addAttribute("studies", studies);

    }

}
