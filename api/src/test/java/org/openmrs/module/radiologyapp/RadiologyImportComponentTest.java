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
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Provider;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.radiologyapp.importer.RadiologyImportResult;
import org.openmrs.module.radiologyapp.importer.RadiologyStudyImportRequest;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
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
}
