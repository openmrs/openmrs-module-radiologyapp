package org.openmrs.module.radiologyapp.fragment.controller;

import org.apache.commons.lang.time.DateFormatUtils;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.module.radiologyapp.RadiologyStudy;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public class RadiologyTabFragmentController {

    public void controller(EmrContext emrContext, FragmentModel model,
                            @SpringBean RadiologyService radiologyService) {

        List<RadiologyStudy> studies = radiologyService.getRadiologyStudiesForPatient(emrContext.getCurrentPatient());
        model.addAttribute("studies", studies);

    }

    public SimpleObject getRadiologyStudyByAccessionNumber(@SpringBean("radiologyService") RadiologyService radiologyService,
                                                  @RequestParam("studyAccessionNumber") String studyAccessionNumber,
                                                  UiUtils uiUtils, EmrContext emrContext) {

        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber(studyAccessionNumber);
        SimpleObject simpleObject =  SimpleObject.fromObject(radiologyStudy, uiUtils, "procedure", "accessionNumber",
                "technician", "imagesAvailable");

        // TODO: add modality and images available

        simpleObject.put("datePerformed", DateFormatUtils.format(radiologyStudy.getDatePerformed(),
                "dd MMM yyyy hh:mm a", emrContext.getUserContext().getLocale()));

        return simpleObject;
    }

}
