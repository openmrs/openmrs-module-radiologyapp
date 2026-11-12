package org.openmrs.module.radiologyapp.web.controller;

import org.openmrs.api.context.Context;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.module.radiologyapp.importer.RadiologyImportResult;
import org.openmrs.module.radiologyapp.importer.RadiologyReportImportRequest;
import org.openmrs.module.radiologyapp.importer.RadiologyStudyImportRequest;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints that let an external system (e.g. a PACS or an integration layer) import radiology studies and reports against
 * existing radiology orders
 */
@Controller
public class RadiologyImportRestController extends BaseRestController {

    @RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/radiologyapp/study", method = RequestMethod.POST)
    @ResponseBody
    public Object postStudy(@RequestBody Map<String, Object> body) {
        RadiologyStudyImportRequest request = new RadiologyStudyImportRequest();
        request.setAccessionNumber(asString(body, "accessionNumber"));
        request.setDatePerformed(asDate(body, "datePerformed"));
        request.setImagesAvailable(asBoolean(body, "imagesAvailable"));
        request.setTechnicianProviderUuid(asString(body, "technicianProviderUuid"));
        request.setPatientIdentifierTypeUuid(asString(body, "patientIdentifierTypeUuid"));
        request.setPatientIdentifier(asString(body, "patientIdentifier"));

        return toResponse(Context.getService(RadiologyService.class).importRadiologyStudy(request));
    }

    @RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/radiologyapp/report", method = RequestMethod.POST)
    @ResponseBody
    public Object postReport(@RequestBody Map<String, Object> body) {
        RadiologyReportImportRequest request = new RadiologyReportImportRequest();
        request.setAccessionNumber(asString(body, "accessionNumber"));
        request.setReportDate(asDate(body, "reportDate"));
        request.setReportType(asString(body, "reportType"));
        request.setReportBody(asString(body, "reportBody"));
        request.setRadiologistProviderUuid(asString(body, "radiologistProviderUuid"));
        request.setDatePerformed(asDate(body, "datePerformed"));
        request.setPatientIdentifierTypeUuid(asString(body, "patientIdentifierTypeUuid"));
        request.setPatientIdentifier(asString(body, "patientIdentifier"));

        return toResponse(Context.getService(RadiologyService.class).importRadiologyReport(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return new ResponseEntity<Map<String, String>>(Collections.singletonMap("error", e.getMessage()),
                HttpStatus.BAD_REQUEST);
    }

    private Map<String, Object> toResponse(RadiologyImportResult result) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("outcome", result.getOutcome().name());
        response.put("encounterUuid", result.getEncounterUuid());
        return response;
    }

    private String asString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : value.toString();
    }

    private Boolean asBoolean(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new IllegalArgumentException(key + " must be true or false");
    }

    // a missing date is passed through as null (the service decides if it is required); an unparseable one is rejected
    private Date asDate(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        Date date;
        try {
            date = (Date) ConversionUtil.convert(value, Date.class);
        }
        catch (RuntimeException e) {
            date = null;
        }
        if (date == null) {
            throw new IllegalArgumentException("Invalid date for " + key);
        }
        return date;
    }
}
