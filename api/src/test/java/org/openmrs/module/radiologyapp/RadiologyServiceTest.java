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

import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emr.EmrConstants;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.emr.TestUtils;
import org.openmrs.module.emr.order.EmrOrderService;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.db.EmrApiDAO;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.radiologyapp.db.RadiologyOrderDAO;
import org.openmrs.module.radiologyapp.exception.RadiologyAPIException;
import org.openmrs.module.radiologyapp.matchers.IsExpectedRadiologyStudy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class RadiologyServiceTest{

    private RadiologyServiceImpl radiologyService;

    private EmrApiProperties emrApiProperties;

    private RadiologyProperties radiologyProperties;

    private EncounterService encounterService;

    private RadiologyOrderDAO radiologyOrderDAO;

    private EmrOrderService emrOrderService;

    private ConceptService conceptService;

    private EmrApiDAO emrApiDAO;

    private EmrContext emrContext;

    private OrderType orderType;

    private Patient patient;

    private String clinicalHistory;

    private EncounterRole clinicianEncounterRole;

    private EncounterRole radiologyTechnicianEncounterRole;

    private EncounterRole principalResultsInterpreterEncounterRole;

    private EncounterType placeOrdersEncounterType;

    private EncounterType radiologyStudyEncounterType;

    private EncounterType radiologyReportEncounterType;

    private Visit currentVisit;

    private Provider provider;

    private Location currentLocation;

    private Location unknownLocation;

    private Provider unknownProvider;

    private ConceptSource emrConceptSource;

    private ConceptMapType sameAs;

    private Date currentDate = new Date();

    private Concept radiologyStudySetConcept;

    private Concept accessionNumberConcept;

    private Concept imagesAvailableConcept;

    private Concept procedureConcept;

    private Concept radiologyReportSetConcept;

    private Concept reportBodyConcept;

    private Concept reportTypeConcept;

    private Concept trueConcept = new Concept();

    private Concept falseConcept = new Concept();

    private ConceptDatatype booleanType;


    @Before
    public void setup() {

        PowerMockito.mockStatic(Context.class);

        User authenticatedUser = new User();
        PowerMockito.when(Context.getAuthenticatedUser()).thenReturn(authenticatedUser);

        trueConcept.setId(1000);
        falseConcept.setId(1001);

        sameAs = new ConceptMapType();
        emrConceptSource = new ConceptSource();
        emrConceptSource.setName(EmrConstants.EMR_CONCEPT_SOURCE_NAME);

        patient = new Patient();
        orderType = new OrderType();
        clinicalHistory = "Patient fell from a building";
        provider = new Provider();
        currentLocation = new Location();
        unknownLocation = new Location();
        unknownProvider = new Provider();

        currentVisit = new Visit();
        placeOrdersEncounterType = new EncounterType();
        radiologyStudyEncounterType = new EncounterType();
        radiologyReportEncounterType = new EncounterType();
        clinicianEncounterRole = new EncounterRole();
        radiologyTechnicianEncounterRole = new EncounterRole();
        principalResultsInterpreterEncounterRole = new EncounterRole();

        prepareMocks();
        setupRadiologyStudyAndRadiologyReportsConceptSets();

        radiologyService = new RadiologyServiceImpl();
        radiologyService.setEmrApiProperties(emrApiProperties);
        radiologyService.setRadiologyProperties(radiologyProperties);
        radiologyService.setEncounterService(encounterService);
        radiologyService.setEmrOrderService(emrOrderService);
        radiologyService.setRadiologyOrderDAO(radiologyOrderDAO);
        radiologyService.setConceptService(conceptService);
        radiologyService.setEmrApiDAO(emrApiDAO);
    }

    private void prepareMocks() {
        emrApiProperties = mock(EmrApiProperties.class);
        radiologyProperties = mock(RadiologyProperties.class);
        encounterService = mock(EncounterService.class);
        emrContext = mock(EmrContext.class);
        emrOrderService = mock(EmrOrderService.class);
        conceptService = mock(ConceptService.class);
        radiologyOrderDAO = mock(RadiologyOrderDAO.class);
        conceptService = mock(ConceptService.class);
        emrApiDAO = mock(EmrApiDAO.class);
        booleanType = mock(ConceptDatatype.class);

        VisitDomainWrapper currentVisitSummary = new VisitDomainWrapper(currentVisit);

        when(emrContext.getActiveVisit()).thenReturn(currentVisitSummary);
        when(radiologyProperties.getRadiologyOrderEncounterType()).thenReturn(placeOrdersEncounterType);
        when(radiologyProperties.getRadiologyStudyEncounterType()).thenReturn(radiologyStudyEncounterType);
        when(radiologyProperties.getRadiologyReportEncounterType()).thenReturn(radiologyReportEncounterType);
        when(radiologyProperties.getRadiologyTechnicianEncounterRole()).thenReturn(radiologyTechnicianEncounterRole);
        when(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()).thenReturn(principalResultsInterpreterEncounterRole);
        when(emrApiProperties.getOrderingProviderEncounterRole()).thenReturn(clinicianEncounterRole);
        when(emrApiProperties.getUnknownLocation()).thenReturn(unknownLocation);
        when(emrApiProperties.getUnknownProvider()).thenReturn(unknownProvider);
        when(emrContext.getSessionLocation()).thenReturn(currentLocation);
        when(radiologyProperties.getRadiologyTestOrderType()).thenReturn(orderType);
        when(booleanType.isBoolean()).thenReturn(true);
        when(Context.getConceptService()).thenReturn(conceptService);
        when(conceptService.getTrueConcept()).thenReturn(trueConcept);
        when(conceptService.getFalseConcept()).thenReturn(falseConcept);
        when(encounterService.saveEncounter(isA(Encounter.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return args[0];
            }
        });
    }

    private void setupRadiologyStudyAndRadiologyReportsConceptSets() {

        radiologyStudySetConcept = setupConcept(conceptService, "Radiology Study Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_STUDY_SET);
        accessionNumberConcept = setupConcept(conceptService, "Accession Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER);
        imagesAvailableConcept = setupConcept(conceptService, "Images Available", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_IMAGES_AVAILABLE);
        imagesAvailableConcept.setDatatype(booleanType);
        procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);
        radiologyReportSetConcept = setupConcept(conceptService, "Radiology Report Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_SET);
        reportBodyConcept = setupConcept(conceptService, "Report Body", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_BODY);
        reportTypeConcept = setupConcept(conceptService, "Report Type", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_TYPE);

        radiologyStudySetConcept.addSetMember(accessionNumberConcept);
        radiologyStudySetConcept.addSetMember(imagesAvailableConcept);
        radiologyStudySetConcept.addSetMember(procedureConcept);

        radiologyReportSetConcept.addSetMember(accessionNumberConcept);
        radiologyReportSetConcept.addSetMember(reportBodyConcept);
        radiologyReportSetConcept.addSetMember(reportTypeConcept);
        radiologyReportSetConcept.addSetMember(procedureConcept);
    }

    private Concept setupConcept(ConceptService mockConceptService, String name, String mappingCode) {
        Concept concept = new Concept();
        concept.addName(new ConceptName(name, Locale.ENGLISH));
        concept.addConceptMapping(new ConceptMap(new ConceptReferenceTerm(emrConceptSource, mappingCode, null), sameAs));
        when(mockConceptService.getConceptByMapping(mappingCode, emrConceptSource.getName())).thenReturn(concept);
        return concept;
    }


    @Test
    public void shouldPlaceARadiologyRequisitionWithOneStudyOnFixedMachine() {
        Concept study = new Concept();
        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setRequestedBy(provider);
        radiologyRequisition.setClinicalHistory(clinicalHistory);
        radiologyRequisition.addStudy(study);
        radiologyRequisition.setUrgency(Order.Urgency.STAT);

        Encounter encounter = radiologyService.placeRadiologyRequisition(emrContext, radiologyRequisition);

        assertThat(encounter, is(new IsExpectedRadiologyOrderEncounter(null, study)));
    }

    @Test
    public void shouldPlaceARadiologyRequisitionWithTwoStudiesOnPortableMachine() {
        Location examLocation = new Location();
        Concept study = new Concept();
        Concept secondStudy = new Concept();

        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setRequestedBy(provider);
        radiologyRequisition.setClinicalHistory(clinicalHistory);
        radiologyRequisition.addStudy(study);
        radiologyRequisition.addStudy(secondStudy);
        radiologyRequisition.setUrgency(Order.Urgency.STAT);
        radiologyRequisition.setExamLocation(examLocation);

        Encounter encounter = radiologyService.placeRadiologyRequisition(emrContext, radiologyRequisition);

        assertThat(encounter, new IsExpectedRadiologyOrderEncounter(examLocation, study, secondStudy));
    }

    @Test
    public void shouldPlaceOrderEvenIfThereIsNoVisit() {
        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);

        when(emrContext.getActiveVisit()).thenReturn(null);

        Encounter encounter = radiologyService.placeRadiologyRequisition(emrContext, radiologyRequisition);

        assertThat(encounter.getVisit(), is(nullValue()));
    }

    @Test
    public void saveRadiologyStudy_shouldCreateRadiologyStudyEncounter() {

        RadiologyStudy study = new RadiologyStudy();
        study.setPatient(patient);
        study.setDatePerformed(currentDate);
        study.setStudyLocation(currentLocation);
        study.setTechnician(provider);
        study.setAccessionNumber("123");
        study.setProcedure(new Concept());

        radiologyService.saveRadiologyStudy(study);

        verify(encounterService).saveEncounter(argThat(new IsExpectedRadiologyStudyEncounter(currentLocation, provider)));
    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyStudy_shouldFailIfAccessionNumberNotSpecified() {

        RadiologyStudy study = new RadiologyStudy();
        study.setPatient(patient);
        study.setDatePerformed(currentDate);
        study.setStudyLocation(currentLocation);
        study.setTechnician(provider);
        study.setProcedure(new Concept());

        radiologyService.saveRadiologyStudy(study);
    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyStudy_shouldFailIfPatientNotSpecified() {

        RadiologyStudy study = new RadiologyStudy();
        study.setDatePerformed(currentDate);
        study.setStudyLocation(currentLocation);
        study.setTechnician(provider);
        study.setAccessionNumber("123");
        study.setProcedure(new Concept());

        radiologyService.saveRadiologyStudy(study);
    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyStudy_shouldFailIfDatePerformedNotSpecified() {

        RadiologyStudy study = new RadiologyStudy();
        study.setPatient(patient);
        study.setStudyLocation(currentLocation);
        study.setTechnician(provider);
        study.setAccessionNumber("123");
        study.setProcedure(new Concept());

        radiologyService.saveRadiologyStudy(study);
    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyStudy_shouldFailIfAnotherStudyExistsWithSameAccessionNumber() {

        when(emrApiDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyStudyEncounterType, false))
                .thenReturn(Collections.singletonList(new Encounter()));

        RadiologyStudy study = new RadiologyStudy();
        study.setPatient(patient);
        study.setStudyLocation(currentLocation);
        study.setTechnician(provider);
        study.setAccessionNumber("123");
        study.setProcedure(new Concept());

        radiologyService.saveRadiologyStudy(study);
    }

    @Test
    public void saveRadiologyStudy_shouldNotFailIfTechnicianAndLocationNotSpecified() {

        RadiologyStudy study = new RadiologyStudy();
        study.setPatient(patient);
        study.setDatePerformed(currentDate);
        study.setStudyLocation(null);
        study.setTechnician(null);
        study.setAccessionNumber("123");
        study.setProcedure(new Concept());

        radiologyService.saveRadiologyStudy(study);

        verify(encounterService).saveEncounter(argThat(new IsExpectedRadiologyStudyEncounter(unknownLocation, unknownProvider)));
    }

    @Test
    public void saveRadiologyReport_shouldCreateRadiologyReportEncounter() {

        RadiologyReport report = new RadiologyReport();
        report.setPatient(patient);
        report.setReportDate(currentDate);
        report.setPrincipalResultsInterpreter(provider);
        report.setReportLocation(currentLocation);
        report.setAccessionNumber("123");
        report.setReportBody("test");
        report.setProcedure(new Concept());
        report.setReportType(new Concept());

        radiologyService.saveRadiologyReport(report);

        verify(encounterService).saveEncounter(argThat(new IsExpectedRadiologyReportEncounter(currentLocation, provider)));

    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyReport_shouldFailIfAccessionNumberNotSpecified() {

        RadiologyReport report = new RadiologyReport();
        report.setPatient(patient);
        report.setReportDate(currentDate);
        report.setPrincipalResultsInterpreter(provider);
        report.setReportLocation(currentLocation);
        report.setReportBody("test");
        report.setProcedure(new Concept());
        report.setReportType(new Concept());

        radiologyService.saveRadiologyReport(report);
    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyReport_shouldFailIfReportDateNotSpecified() {

        RadiologyReport report = new RadiologyReport();
        report.setPatient(patient);
        report.setPrincipalResultsInterpreter(provider);
        report.setReportLocation(currentLocation);
        report.setAccessionNumber("123");
        report.setReportBody("test");
        report.setProcedure(new Concept());
        report.setReportType(new Concept());

        radiologyService.saveRadiologyReport(report);
    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyReport_shouldFailIfPatientNotSpecified() {

        RadiologyReport report = new RadiologyReport();
        report.setReportDate(currentDate);
        report.setPrincipalResultsInterpreter(provider);
        report.setReportLocation(currentLocation);
        report.setAccessionNumber("123");
        report.setReportBody("test");
        report.setProcedure(new Concept());
        report.setReportType(new Concept());

        radiologyService.saveRadiologyReport(report);
    }


    @Test
    public void saveRadiologyReport_shouldNotFailIfInterpreterAndLocationNotSpecified() {

        RadiologyReport report = new RadiologyReport();
        report.setPatient(patient);
        report.setReportDate(currentDate);
        report.setPrincipalResultsInterpreter(null);
        report.setReportLocation(null);
        report.setAccessionNumber("123");
        report.setReportBody("test");
        report.setProcedure(new Concept());
        report.setReportType(new Concept());

        radiologyService.saveRadiologyReport(report);

        verify(encounterService).saveEncounter(argThat(new IsExpectedRadiologyReportEncounter(unknownLocation, unknownProvider)));

    }

    @Test
    public void getRadiologyStudiesShouldReturnAllRadiologyStudiesForPatient() {

        Date firstStudyDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Date secondStudyDate = new DateTime(2011, 10, 10, 10, 0, 0, 0).toDate();
        Provider firstStudyTechnician = new Provider();
        Provider secondStudyTechnician = new Provider();
        Location firstStudyLocation = new Location();
        Location secondStudyLocation = new Location();
        Concept firstStudyProcedure = new Concept();
        firstStudyProcedure.setId(111);
        Concept secondStudyProcedure = new Concept();
        secondStudyProcedure.setId(222);

        RadiologyStudy firstExpectedStudy = new RadiologyStudy();
        firstExpectedStudy.setDatePerformed(firstStudyDate);
        firstExpectedStudy.setTechnician(firstStudyTechnician);
        firstExpectedStudy.setStudyLocation(firstStudyLocation);
        firstExpectedStudy.setPatient(patient);
        firstExpectedStudy.setAccessionNumber("123");
        firstExpectedStudy.setImagesAvailable(true);
        firstExpectedStudy.setProcedure(firstStudyProcedure);

        RadiologyStudy secondExpectedStudy = new RadiologyStudy();
        secondExpectedStudy.setDatePerformed(secondStudyDate);
        secondExpectedStudy.setTechnician(secondStudyTechnician);
        secondExpectedStudy.setStudyLocation(secondStudyLocation);
        secondExpectedStudy.setPatient(patient);
        secondExpectedStudy.setAccessionNumber("456");
        secondExpectedStudy.setImagesAvailable(true);
        secondExpectedStudy.setProcedure(secondStudyProcedure);

        List<Encounter> encounters = new ArrayList<Encounter>();
        // note that we add these backwards, to test sorting
        encounters.add(setupRadiologyStudyEncounter(secondStudyDate, secondStudyLocation, patient, secondStudyTechnician,
                "456", secondStudyProcedure));
        encounters.add(setupRadiologyStudyEncounter(firstStudyDate, firstStudyLocation, patient, firstStudyTechnician,
                "123", firstStudyProcedure));

        when(encounterService.getEncounters(patient, null, null, null, null, Collections.singletonList(radiologyStudyEncounterType),
                null, null, null, false)).thenReturn(encounters);

        List<RadiologyStudy> radiologyStudies = radiologyService.getRadiologyStudiesForPatient(patient);
        assertThat(radiologyStudies.size(), is(2));
        assertTrue(new IsExpectedRadiologyStudy(firstExpectedStudy).matches(radiologyStudies.get(0)));
        assertTrue(new IsExpectedRadiologyStudy(secondExpectedStudy).matches(radiologyStudies.get(1)));
    }

    @Test
    public void getRadiologyStudiesShouldNotFailIfNoObsGroup() {

        Date firstStudyDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Date secondStudyDate = new DateTime(2011, 10, 10, 10, 0, 0, 0).toDate();
        Provider firstStudyTechnician = new Provider();
        Provider secondStudyTechnician = new Provider();
        Location firstStudyLocation = new Location();
        Location secondStudyLocation = new Location();

        RadiologyStudy firstExpectedStudy = new RadiologyStudy();
        firstExpectedStudy.setDatePerformed(firstStudyDate);
        firstExpectedStudy.setTechnician(firstStudyTechnician);
        firstExpectedStudy.setStudyLocation(firstStudyLocation);
        firstExpectedStudy.setPatient(patient);

        RadiologyStudy secondExpectedStudy = new RadiologyStudy();
        secondExpectedStudy.setDatePerformed(secondStudyDate);
        secondExpectedStudy.setTechnician(secondStudyTechnician);
        secondExpectedStudy.setStudyLocation(secondStudyLocation);
        secondExpectedStudy.setPatient(patient);

        List<Encounter> encounters = new ArrayList<Encounter>();
        // note that we add these backwards, to test sorting
        encounters.add(setupRadiologyStudyEncounterWithoutObsGroup(secondStudyDate, secondStudyLocation, patient, secondStudyTechnician));
        encounters.add(setupRadiologyStudyEncounterWithoutObsGroup(firstStudyDate, firstStudyLocation, patient, firstStudyTechnician));

        when(encounterService.getEncounters(patient, null, null, null, null, Collections.singletonList(radiologyStudyEncounterType),
                null, null, null, false)).thenReturn(encounters);

        List<RadiologyStudy> radiologyStudies = radiologyService.getRadiologyStudiesForPatient(patient);
        assertThat(radiologyStudies.size(), is(2));
        assertTrue(new IsExpectedRadiologyStudy(firstExpectedStudy).matches(radiologyStudies.get(0)));
        assertTrue(new IsExpectedRadiologyStudy(secondExpectedStudy).matches(radiologyStudies.get(1)));
    }


    @Test
    public void getRadiologyStudiesShouldReturnEmptyListIfNoStudiesForPatient() {

        when(encounterService.getEncounters(patient, null, null, null, null, Collections.singletonList(radiologyStudyEncounterType),
                null, null, null, false)).thenReturn(null);

        List<RadiologyStudy> radiologyStudies = radiologyService.getRadiologyStudiesForPatient(patient);
        assertThat(radiologyStudies.size(), is(0));

    }

    @Test
    public void getRadiologyStudyByAccessionNumberShouldReturnRadiologyStudyWithAccessionNumber() {

        Date studyDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Provider studyTechnician = new Provider();
        Location studyLocation = new Location();
        Concept studyProcedure = new Concept();
        studyProcedure.setId(111);

        RadiologyStudy expectedStudy = new RadiologyStudy();
        expectedStudy.setDatePerformed(studyDate);
        expectedStudy.setTechnician(studyTechnician);
        expectedStudy.setStudyLocation(studyLocation);
        expectedStudy.setPatient(patient);
        expectedStudy.setAccessionNumber("123");
        expectedStudy.setImagesAvailable(true);
        expectedStudy.setProcedure(studyProcedure);

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(setupRadiologyStudyEncounter(studyDate, studyLocation, patient, studyTechnician,
                "123", studyProcedure));

        when(emrApiDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyStudyEncounterType, false))
                .thenReturn(encounters);

        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("123");
        assertTrue(new IsExpectedRadiologyStudy(expectedStudy).matches(radiologyStudy));
    }

    @Test
    public void getRadiologyStudyByAccessionNumberShouldNotFailIfMultipleResults() {

        Date firstStudyDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Date secondStudyDate = new DateTime(2011, 10, 10, 10, 0, 0, 0).toDate();
        Provider firstStudyTechnician = new Provider();
        Provider secondStudyTechnician = new Provider();
        Location firstStudyLocation = new Location();
        Location secondStudyLocation = new Location();
        Concept firstStudyProcedure = new Concept();
        firstStudyProcedure.setId(111);
        Concept secondStudyProcedure = new Concept();
        secondStudyProcedure.setId(222);

        RadiologyStudy firstStudy = new RadiologyStudy();
        firstStudy.setDatePerformed(firstStudyDate);
        firstStudy.setTechnician(firstStudyTechnician);
        firstStudy.setStudyLocation(firstStudyLocation);
        firstStudy.setPatient(patient);
        firstStudy.setAccessionNumber("123");
        firstStudy.setImagesAvailable(true);
        firstStudy.setProcedure(firstStudyProcedure);

        RadiologyStudy secondStudy = new RadiologyStudy();
        secondStudy.setDatePerformed(secondStudyDate);
        secondStudy.setTechnician(secondStudyTechnician);
        secondStudy.setStudyLocation(secondStudyLocation);
        secondStudy.setPatient(patient);
        secondStudy.setAccessionNumber("456");
        secondStudy.setImagesAvailable(true);
        secondStudy.setProcedure(secondStudyProcedure);

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(setupRadiologyStudyEncounter(firstStudyDate, firstStudyLocation, patient, firstStudyTechnician,
                "123", firstStudyProcedure));
        encounters.add(setupRadiologyStudyEncounter(secondStudyDate, secondStudyLocation, patient, secondStudyTechnician,
                "456", secondStudyProcedure));


        when(emrApiDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyStudyEncounterType, false))
                .thenReturn(encounters);

        // should just return the first study
        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("123");
        assertTrue(new IsExpectedRadiologyStudy(firstStudy).matches(radiologyStudy));
    }


    @Test
    public void getRadiologyStudyByAccessionNumberShouldReturnNullIfNoMatchingStudy() {
        when(emrApiDAO.getEncountersByObsValueText(any(Concept.class), any(String.class)
                , any(EncounterType.class), eq(false))).thenReturn(null);

        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("1234");
        assertNull(radiologyStudy);
    }


    private Encounter setupRadiologyStudyEncounter(Date datePerformed, Location location, Patient patient,
                                                   Provider provider, String accessionNumber, Concept procedure) {
        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyStudyEncounterType);
        encounter.setEncounterDatetime(datePerformed);
        encounter.setLocation(location);
        encounter.setPatient(patient);
        encounter.addProvider(radiologyTechnicianEncounterRole, provider);

        Obs radiologyStudyObsGroup = new Obs();
        radiologyStudyObsGroup.setId(222);
        radiologyStudyObsGroup.setConcept(radiologyStudySetConcept);

        Obs accessionNumberObs = new Obs();
        accessionNumberObs.setConcept(accessionNumberConcept);
        accessionNumberObs.setValueText(accessionNumber);
        radiologyStudyObsGroup.addGroupMember(accessionNumberObs);

        Obs imagesAvailableObs = new Obs();
        imagesAvailableObs.setConcept(imagesAvailableConcept);
        imagesAvailableObs.setValueCoded(trueConcept);
        radiologyStudyObsGroup.addGroupMember(imagesAvailableObs);

        Obs procedureObs = new Obs();
        procedureObs.setConcept(procedureConcept);
        procedureObs.setValueCoded(procedure);
        radiologyStudyObsGroup.addGroupMember(procedureObs);

        encounter.addObs(radiologyStudyObsGroup);

        return encounter;
    }

    private Encounter setupRadiologyStudyEncounterWithoutObsGroup(Date datePerformed, Location location, Patient patient, Provider provider) {
        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyStudyEncounterType);
        encounter.setEncounterDatetime(datePerformed);
        encounter.setLocation(location);
        encounter.setPatient(patient);
        encounter.addProvider(radiologyTechnicianEncounterRole, provider);

        return encounter;
    }

    // TODO: could move the rest of these matchers out into separate classes in matchers package

    private class IsExpectedOrder extends ArgumentMatcher<Order> {
        private Location expectedLocation;
        private Concept expectedStudy;

        public IsExpectedOrder(Location expectedLocation, Concept expectedStudy) {
            this.expectedLocation = expectedLocation;
            this.expectedStudy = expectedStudy;
        }

        @Override
        public boolean matches(Object o) {
            RadiologyOrder actual = (RadiologyOrder) o;

            try {
                assertThat(actual.getOrderType(), is(orderType));
                assertThat(actual.getPatient(), is(patient));
                assertThat(actual.getConcept(), is(expectedStudy));
                assertThat(actual.getStartDate(), TestUtils.isJustNow());
                assertThat(actual.getUrgency(), is(Order.Urgency.STAT));
                assertThat(actual.getClinicalHistory(), is(clinicalHistory));
                assertThat(actual.getExamLocation(), is(expectedLocation));
                return true;
            } catch (AssertionError e) {
                return false;
            }
        }
    }

    private class IsExpectedRadiologyOrderEncounter extends ArgumentMatcher<Encounter> {

        private Concept[] expectedStudies;
        private List<IsExpectedOrder> expectedOrders = new ArrayList<IsExpectedOrder>();

        public IsExpectedRadiologyOrderEncounter(Location expectedLocation, Concept... expectedStudies) {
            this.expectedStudies = expectedStudies;

            for (Concept expectedStudy : expectedStudies) {
                expectedOrders.add(new IsExpectedOrder(expectedLocation, expectedStudy));
            }
        }

        @Override
        public boolean matches(Object o) {
            Encounter encounter = (Encounter) o;

            Set<Provider> providersByRole = encounter.getProvidersByRole(clinicianEncounterRole);
            assertThat(encounter.getEncounterType(), is(placeOrdersEncounterType));
            assertThat(providersByRole.size(), is(1));
            assertThat(providersByRole.iterator().next(), is(provider));
            assertThat(encounter.getPatient(), is(patient));
            assertThat(encounter.getLocation(), is(currentLocation));
            assertThat(encounter.getEncounterDatetime(), notNullValue());
            assertThat(encounter.getVisit(), is(currentVisit));
            assertThat(encounter.getOrders().size(), is(expectedStudies.length));
            new IsIterableContainingInAnyOrder(expectedOrders).matches(encounter.getOrders());

            return true;
        }
    }

    private class IsExpectedRadiologyStudyEncounter extends ArgumentMatcher<Encounter> {

        Location expectedLocation;

        Provider expectedTechnician;

        public IsExpectedRadiologyStudyEncounter(Location expectedLocation, Provider expectedTechnician) {
            this.expectedLocation = expectedLocation;
            this.expectedTechnician = expectedTechnician;
        }

        @Override
        public boolean matches(Object o) {
            Encounter encounter = (Encounter) o;

            assertThat(encounter.getEncounterType(), is(radiologyStudyEncounterType));
            assertThat(encounter.getPatient(), is(patient));
            assertThat(encounter.getEncounterDatetime(), is(currentDate));
            assertThat(encounter.getLocation(), is(expectedLocation));
            assertThat(encounter.getProvidersByRole(radiologyTechnicianEncounterRole).size(), is(1));
            assertThat(encounter.getProvidersByRole(radiologyTechnicianEncounterRole).iterator().next(), is(expectedTechnician));

            // just test that the obs are there (we test that the obs group is packaged correctly in RadiologyStudyConceptSetTest)
            assertThat(encounter.getObsAtTopLevel(false).size(), is(1));
            assertThat(encounter.getObsAtTopLevel(false).iterator().next().getGroupMembers().size(), is(2));  // only two because we aren't including images available concept
            return true;
        }

    }

    private class IsExpectedRadiologyReportEncounter extends ArgumentMatcher<Encounter> {

        Location expectedLocation;

        Provider expectedInterpreter;

        public IsExpectedRadiologyReportEncounter(Location expectedLocation, Provider expectedInterpreter) {
            this.expectedLocation = expectedLocation;
            this.expectedInterpreter = expectedInterpreter;
        }


        @Override
        public boolean matches(Object o) {
            Encounter encounter = (Encounter) o;

            assertThat(encounter.getEncounterType(), is(radiologyReportEncounterType));
            assertThat(encounter.getPatient(), is(patient));
            assertThat(encounter.getEncounterDatetime(), is(currentDate));
            assertThat(encounter.getLocation(), is(expectedLocation));
            assertThat(encounter.getProvidersByRole(principalResultsInterpreterEncounterRole).size(), is(1));
            assertThat(encounter.getProvidersByRole(principalResultsInterpreterEncounterRole).iterator().next(), is(expectedInterpreter));

            // just test that the obs are there (we test that the obs group is packaged correctly in RadiologyReportConceptSetTest)
            assertThat(encounter.getObsAtTopLevel(false).size(), is(1));
            assertThat(encounter.getObsAtTopLevel(false).iterator().next().getGroupMembers().size(), is(4));
            return true;
        }

    }
}
