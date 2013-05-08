package org.openmrs.module.radiologyapp.fragment.controller;

import org.apache.commons.lang.time.DateFormatUtils;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.radiologyapp.RadiologyReport;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.module.radiologyapp.RadiologyStudy;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
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

        // add the study
        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber(studyAccessionNumber);
        SimpleObject simpleObject =  SimpleObject.fromObject(radiologyStudy, uiUtils, "procedure", "accessionNumber",
                "technician", "imagesAvailable");

        simpleObject.put("datePerformed", DateFormatUtils.format(radiologyStudy.getDatePerformed(),
                "dd MMM yyyy hh:mm a", emrContext.getUserContext().getLocale()));


        // add any associated reports
        List<RadiologyReport> radiologyReports = radiologyService.getRadiologyReportsByAccessionNumber(studyAccessionNumber);
        List<SimpleObject> simpleRadiologyReports = new ArrayList<SimpleObject>();

        for (RadiologyReport radiologyReport : radiologyReports) {
            SimpleObject simpleRadiologyReport = SimpleObject.fromObject(radiologyReport,uiUtils, "reportType",
                    "reportBody", "principalResultsInterpreter");
            simpleRadiologyReport.put("reportDate", DateFormatUtils.format(radiologyReport.getReportDate(),
                    "dd MMM yyyy hh:mm a", emrContext.getUserContext().getLocale()));
            simpleRadiologyReports.add(simpleRadiologyReport);
        }

        simpleObject.put("reports", simpleRadiologyReports);

        return simpleObject;
    }

}
