/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.radiologyapp;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.radiologyapp.importer.RadiologyImportResult;
import org.openmrs.module.radiologyapp.importer.RadiologyReportImportRequest;
import org.openmrs.module.radiologyapp.importer.RadiologyStudyImportRequest;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RadiologyImportComponentTest extends BaseModuleContextSensitiveTest {

    @Autowired
    @Qualifier("radiologyService")
    RadiologyService radiologyService;

    @Autowired
    @Qualifier("patientService")
    private PatientService patientService;

    @Autowired
    @Qualifier("providerService")
    private ProviderService providerService;

    @Autowired
    @Qualifier("encounterService")
    private EncounterService encounterService;

    @Autowired
    @Qualifier("conceptService")
    private ConceptService conceptService;

    @Autowired
    @Qualifier("radiologyProperties")
    private RadiologyProperties radiologyProperties;

    @Before
    public void beforeAllTests() throws Exception {
        executeDataSet("radiologyServiceComponentTestDataset.xml");
    }

    private RadiologyStudyImportRequest request(String accession) {
        PatientIdentifier identifier = patientService.getPatient(6).getPatientIdentifier();
        RadiologyStudyImportRequest request = new RadiologyStudyImportRequest();
        request.setAccessionNumber(accession);
        request.setDatePerformed(DateUtil.truncateToSeconds(new Date()));
        request.setImagesAvailable(true);
        request.setPatientIdentifierTypeUuid(identifier.getIdentifierType().getUuid());
        request.setPatientIdentifier(identifier.getIdentifier());
        return request;
    }

    private int studyEncounterCount() {
        Patient patient = patientService.getPatient(6);
        return encounterService.getEncounters(patient, null, null, null, null,
                Collections.singletonList(radiologyProperties.getRadiologyStudyEncounterType()),
                null, null, null, false).size();
    }

    private Encounter createAndGetEncounter(RadiologyStudyImportRequest request) {
        RadiologyImportResult result = radiologyService.importRadiologyStudy(request);
        assertEquals(RadiologyImportResult.Outcome.CREATED, result.getOutcome());
        assertNotNull(result.getEncounterUuid());
        return encounterService.getEncounterByUuid(result.getEncounterUuid());
    }

    @Test
    public void importRadiologyStudy_shouldCreateStudyForOrder() {
        RadiologyStudyImportRequest request = request("12345");
        Encounter encounter = createAndGetEncounter(request);

        assertEquals(radiologyProperties.getUnknownLocation(), encounter.getLocation());
        assertEquals(Collections.singleton(radiologyProperties.getUnknownProvider()),
                encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole()));

        RadiologyStudy study = radiologyService.getRadiologyStudyByOrderNumber("12345");
        assertEquals(request.getDatePerformed(), study.getDatePerformed());
        assertTrue(study.getImagesAvailable());
    }

    @Test
    public void importRadiologyStudy_shouldReturnAlreadyExistsForSecondStudy() {
        createAndGetEncounter(request("12345"));
        RadiologyImportResult result = radiologyService.importRadiologyStudy(request("12345"));

        assertEquals(RadiologyImportResult.Outcome.ALREADY_EXISTS, result.getOutcome());
        assertNull(result.getEncounterUuid());
        assertEquals(1, studyEncounterCount());
    }

    @Test
    public void importRadiologyStudy_shouldReturnNoMatchingOrderForUnknownAccession() {
        RadiologyImportResult result = radiologyService.importRadiologyStudy(request("nope"));
        assertEquals(RadiologyImportResult.Outcome.NO_MATCHING_ORDER, result.getOutcome());
        assertEquals(0, studyEncounterCount());
    }

    @Test
    public void importRadiologyStudy_shouldReturnNoMatchingOrderForNonRadiologyOrder() {
        RadiologyImportResult result = radiologyService.importRadiologyStudy(request("99999"));
        assertEquals(RadiologyImportResult.Outcome.NO_MATCHING_ORDER, result.getOutcome());
        assertEquals(0, studyEncounterCount());
    }

    @Test
    public void importRadiologyStudy_shouldReturnPatientMismatchWhenIdentifierDiffers() {
        RadiologyStudyImportRequest request = request("12345");
        request.setPatientIdentifier("WRONG");
        RadiologyImportResult result = radiologyService.importRadiologyStudy(request);

        assertEquals(RadiologyImportResult.Outcome.PATIENT_MISMATCH, result.getOutcome());
        assertEquals(0, studyEncounterCount());
    }

    @Test
    public void importRadiologyStudy_shouldUseTechnicianWhenProviderUuidExists() {
        Provider provider = providerService.getProvider(1);
        RadiologyStudyImportRequest request = request("12345");
        request.setTechnicianProviderUuid(provider.getUuid());

        Encounter encounter = createAndGetEncounter(request);
        assertEquals(Collections.singleton(provider),
                encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole()));
    }

    @Test
    public void importRadiologyStudy_shouldUseUnknownProviderWhenTechnicianUuidUnknown() {
        RadiologyStudyImportRequest request = request("12345");
        request.setTechnicianProviderUuid(UUID.randomUUID().toString());

        Encounter encounter = createAndGetEncounter(request);
        assertEquals(Collections.singleton(radiologyProperties.getUnknownProvider()),
                encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void importRadiologyStudy_shouldRejectMissingDatePerformed() {
        RadiologyStudyImportRequest request = request("12345");
        request.setDatePerformed(null);
        radiologyService.importRadiologyStudy(request);
    }

    @Test(expected = IllegalArgumentException.class)
    public void importRadiologyStudy_shouldRejectUnknownIdentifierType() {
        // unknown accession too: proves the identifier type is validated before the order lookup
        RadiologyStudyImportRequest request = request("nope");
        request.setPatientIdentifierTypeUuid(UUID.randomUUID().toString());
        radiologyService.importRadiologyStudy(request);
    }

    private RadiologyReportImportRequest reportRequest(String accession, String type, Date reportDate, String body) {
        PatientIdentifier identifier = patientService.getPatient(6).getPatientIdentifier();
        RadiologyReportImportRequest request = new RadiologyReportImportRequest();
        request.setAccessionNumber(accession);
        request.setReportType(type);
        request.setReportDate(reportDate);
        request.setReportBody(body);
        request.setPatientIdentifierTypeUuid(identifier.getIdentifierType().getUuid());
        request.setPatientIdentifier(identifier.getIdentifier());
        return request;
    }

    private RadiologyReportImportRequest finalRequest(String accession) {
        return reportRequest(accession, "final", secondsFromNow(-3600), "Final body");
    }

    private Concept reportTypeConcept(String code) {
        return conceptService.getConceptByMapping(code, EmrApiConstants.EMR_CONCEPT_SOURCE_NAME);
    }

    private Date secondsFromNow(int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, seconds);
        return calendar.getTime();
    }

    @Test
    public void importRadiologyReport_shouldCreateFinalReportAndStudy() {
        RadiologyReportImportRequest request = finalRequest("12345");
        request.setDatePerformed(secondsFromNow(-3600));

        RadiologyImportResult result = radiologyService.importRadiologyReport(request);

        assertEquals(RadiologyImportResult.Outcome.CREATED, result.getOutcome());
        assertNotNull(result.getEncounterUuid());
        assertTrue(result.isStudyCreated());

        List<RadiologyReport> reports = radiologyService.getRadiologyReportsByOrderNumber("12345");
        assertEquals(1, reports.size());
        assertEquals(reportTypeConcept(RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_FINAL), reports.get(0).getReportType());
        assertEquals("Final body", reports.get(0).getReportBody());
        assertEquals(radiologyProperties.getUnknownLocation(),
                encounterService.getEncounterByUuid(result.getEncounterUuid()).getLocation());

        assertEquals(1, studyEncounterCount());
        assertEquals(request.getDatePerformed(), radiologyService.getRadiologyStudyByOrderNumber("12345").getDatePerformed());
    }

    @Test
    public void importRadiologyReport_shouldUseReportDateForCreatedStudyWhenNoDatePerformed() {
        RadiologyReportImportRequest request = finalRequest("12345");
        request.setReportDate(secondsFromNow(-120));

        radiologyService.importRadiologyReport(request);

        assertEquals(1, studyEncounterCount());
        assertEquals(request.getReportDate(), radiologyService.getRadiologyStudyByOrderNumber("12345").getDatePerformed());
    }

    @Test
    public void importRadiologyReport_shouldNotCreateStudyWhenOneExists() {
        createAndGetEncounter(request("12345"));

        RadiologyImportResult result = radiologyService.importRadiologyReport(finalRequest("12345"));

        assertEquals(RadiologyImportResult.Outcome.CREATED, result.getOutcome());
        assertFalse(result.isStudyCreated());
        assertEquals(1, studyEncounterCount());
    }

    @Test
    public void importRadiologyReport_shouldReturnAlreadyExistsForSecondFinal() {
        radiologyService.importRadiologyReport(finalRequest("12345"));

        RadiologyImportResult result = radiologyService.importRadiologyReport(
                reportRequest("12345", "final", secondsFromNow(-60), "Different body"));

        assertEquals(RadiologyImportResult.Outcome.ALREADY_EXISTS, result.getOutcome());
        assertNull(result.getEncounterUuid());
        assertEquals(1, radiologyService.getRadiologyReportsByOrderNumber("12345").size());
    }

    @Test
    public void importRadiologyReport_shouldCreateCorrectionAfterFinal() {
        radiologyService.importRadiologyReport(finalRequest("12345"));

        RadiologyImportResult result = radiologyService.importRadiologyReport(
                reportRequest("12345", "correction", secondsFromNow(-60), "Corrected body"));

        assertEquals(RadiologyImportResult.Outcome.CREATED, result.getOutcome());
        assertFalse(result.isStudyCreated());
        List<RadiologyReport> reports = radiologyService.getRadiologyReportsByOrderNumber("12345");
        assertEquals(2, reports.size());
        int corrections = 0;
        for (RadiologyReport report : reports) {
            if (reportTypeConcept(RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_CORRECTION).equals(report.getReportType())) {
                corrections++;
            }
        }
        assertEquals(1, corrections);
    }

    @Test
    public void importRadiologyReport_shouldTreatCorrectionWithSameSecondAsExisting() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 120);
        Date first = calendar.getTime();
        calendar.set(Calendar.MILLISECOND, 980);
        Date second = calendar.getTime();

        assertEquals(RadiologyImportResult.Outcome.CREATED,
                radiologyService.importRadiologyReport(reportRequest("12345", "correction", first, "One")).getOutcome());
        RadiologyImportResult result = radiologyService.importRadiologyReport(reportRequest("12345", "correction", second, "One"));

        assertEquals(RadiologyImportResult.Outcome.ALREADY_EXISTS, result.getOutcome());
        assertEquals(1, radiologyService.getRadiologyReportsByOrderNumber("12345").size());
    }

    @Test
    public void importRadiologyReport_shouldCreateSecondCorrectionWithLaterDate() {
        radiologyService.importRadiologyReport(reportRequest("12345", "correction", secondsFromNow(-10), "One"));

        RadiologyImportResult result = radiologyService.importRadiologyReport(
                reportRequest("12345", "correction", secondsFromNow(-9), "Two"));

        assertEquals(RadiologyImportResult.Outcome.CREATED, result.getOutcome());
        assertEquals(2, radiologyService.getRadiologyReportsByOrderNumber("12345").size());
    }

    @Test
    public void importRadiologyReport_shouldUseRadiologistWhenProviderUuidExists() {
        Provider provider = providerService.getProvider(1);
        RadiologyReportImportRequest request = finalRequest("12345");
        request.setRadiologistProviderUuid(provider.getUuid());

        RadiologyImportResult result = radiologyService.importRadiologyReport(request);

        Encounter encounter = encounterService.getEncounterByUuid(result.getEncounterUuid());
        assertEquals(Collections.singleton(provider),
                encounter.getProvidersByRole(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()));
    }

    @Test
    public void importRadiologyReport_shouldReturnNoMatchingOrderAndPatientMismatch() {
        assertEquals(RadiologyImportResult.Outcome.NO_MATCHING_ORDER,
                radiologyService.importRadiologyReport(finalRequest("nope")).getOutcome());

        RadiologyReportImportRequest mismatch = finalRequest("12345");
        mismatch.setPatientIdentifier("WRONG");
        assertEquals(RadiologyImportResult.Outcome.PATIENT_MISMATCH,
                radiologyService.importRadiologyReport(mismatch).getOutcome());

        assertEquals(0, studyEncounterCount());
        assertEquals(0, radiologyService.getRadiologyReportsByOrderNumber("12345").size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void importRadiologyReport_shouldRejectUnknownReportType() {
        radiologyService.importRadiologyReport(reportRequest("12345", "preliminary", secondsFromNow(0), "Body"));
    }

    @Test
    public void importRadiologyStudy_shouldClampFutureDatePerformedToNow() {
        RadiologyStudyImportRequest request = request("12345");
        request.setDatePerformed(secondsFromNow(2 * 3600));
        Date before = new Date();

        Encounter encounter = createAndGetEncounter(request);

        assertFalse(encounter.getEncounterDatetime().after(new Date()));
        assertFalse(encounter.getEncounterDatetime().before(DateUtil.truncateToSeconds(before)));
    }

    @Test
    public void importRadiologyReport_shouldClampFutureReportDateAndCreatedStudyToNow() {
        RadiologyReportImportRequest request = reportRequest("12345", "final", secondsFromNow(2 * 3600), "Body");

        RadiologyImportResult result = radiologyService.importRadiologyReport(request);

        assertEquals(RadiologyImportResult.Outcome.CREATED, result.getOutcome());
        Date now = new Date();
        assertFalse(encounterService.getEncounterByUuid(result.getEncounterUuid()).getEncounterDatetime().after(now));
        assertFalse(radiologyService.getRadiologyStudyByOrderNumber("12345").getDatePerformed().after(now));
    }

    @Test
    public void importRadiologyStudy_shouldTreatVoidedOrderAsNoMatchingOrder() {
        Context.getOrderService().getOrderByOrderNumber("12345").setVoided(true);

        assertEquals(RadiologyImportResult.Outcome.NO_MATCHING_ORDER,
                radiologyService.importRadiologyStudy(request("12345")).getOutcome());
        assertEquals(0, studyEncounterCount());
    }

    @Test
    public void importRadiologyReport_shouldTreatVoidedOrderAsNoMatchingOrder() {
        Context.getOrderService().getOrderByOrderNumber("12345").setVoided(true);

        assertEquals(RadiologyImportResult.Outcome.NO_MATCHING_ORDER,
                radiologyService.importRadiologyReport(finalRequest("12345")).getOutcome());
        assertEquals(0, radiologyService.getRadiologyReportsByOrderNumber("12345").size());
    }
}
