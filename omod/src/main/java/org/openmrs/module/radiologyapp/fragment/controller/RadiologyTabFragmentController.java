package org.openmrs.module.radiologyapp.fragment.controller;

import org.apache.commons.lang.time.DateFormatUtils;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.emrapi.patient.PatientDomainWrapper;
import org.openmrs.module.radiologyapp.RadiologyReport;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.module.radiologyapp.RadiologyStudy;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

public class RadiologyTabFragmentController {

    public void controller(@FragmentParam("patient") PatientDomainWrapper patient, FragmentModel model,
                            @SpringBean RadiologyService radiologyService) {

        List<RadiologyStudy> studies = radiologyService.getRadiologyStudiesForPatient(patient.getPatient());
        model.addAttribute("studies", studies);

    }

    public SimpleObject getRadiologyStudyByOrderNumber(@SpringBean("radiologyService") RadiologyService radiologyService,
                                                       @RequestParam("studyOrderNumber") String studyOrderNumber,
                                                       UiUtils uiUtils, UiSessionContext uiSessionContext) {

        // add the study
        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByOrderNumber(studyOrderNumber);
        SimpleObject simpleObject =  SimpleObject.fromObject(radiologyStudy, uiUtils, "procedure", "orderNumber",
                "technician", "imagesAvailable");

        simpleObject.put("datePerformed", DateFormatUtils.format(radiologyStudy.getDatePerformed(),
                "dd MMM yyyy hh:mm a", uiSessionContext.getLocale()));


        // add any associated reports
        List<RadiologyReport> radiologyReports = radiologyService.getRadiologyReportsByOrderNumber(studyOrderNumber);
        List<SimpleObject> simpleRadiologyReports = new ArrayList<SimpleObject>();

        for (RadiologyReport radiologyReport : radiologyReports) {
            SimpleObject simpleRadiologyReport = SimpleObject.fromObject(radiologyReport,uiUtils, "reportType",
                    "reportBody", "principalResultsInterpreter");
            simpleRadiologyReport.put("reportDate", DateFormatUtils.format(radiologyReport.getReportDate(),
                    "dd MMM yyyy hh:mm a", uiSessionContext.getLocale()));
            simpleRadiologyReports.add(simpleRadiologyReport);
        }

        simpleObject.put("reports", simpleRadiologyReports);

        return simpleObject;
    }

}
