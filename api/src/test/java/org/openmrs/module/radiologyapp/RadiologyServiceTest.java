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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
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
import org.openmrs.module.emr.EmrProperties;
import org.openmrs.module.emr.TestUtils;
import org.openmrs.module.emr.order.EmrOrderService;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.radiologyapp.db.RadiologyOrderDAO;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class RadiologyServiceTest{

    private RadiologyServiceImpl radiologyService;
    private EmrProperties emrProperties;
    private EmrApiProperties emrApiProperties;
    private RadiologyProperties radiologyProperties;
    private EncounterService encounterService;
    private RadiologyOrderDAO radiologyOrderDAO;
    private EmrOrderService emrOrderService;
    private ConceptService conceptService;
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

    @Before
    public void setup() {
        PowerMockito.mockStatic(Context.class);
        User authenticatedUser = new User();
        PowerMockito.when(Context.getAuthenticatedUser()).thenReturn(authenticatedUser);

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

        radiologyService = new RadiologyServiceImpl();
        radiologyService.setEmrProperties(emrProperties);
        radiologyService.setEmrApiProperties(emrApiProperties);
        radiologyService.setRadiologyProperties(radiologyProperties);
        radiologyService.setEncounterService(encounterService);
        radiologyService.setEmrOrderService(emrOrderService);
        radiologyService.setRadiologyOrderDAO(radiologyOrderDAO);
        radiologyService.setConceptService(conceptService);
    }

    private void prepareMocks() {
        emrProperties = mock(EmrProperties.class);
        emrApiProperties = mock(EmrApiProperties.class);
        radiologyProperties = mock(RadiologyProperties.class);
        encounterService = mock(EncounterService.class);
        emrContext = mock(EmrContext.class);
        emrOrderService = mock(EmrOrderService.class);
        conceptService = mock(ConceptService.class);
        radiologyOrderDAO = mock(RadiologyOrderDAO.class);

        VisitDomainWrapper currentVisitSummary = new VisitDomainWrapper(currentVisit);
        when(emrContext.getActiveVisit()).thenReturn(currentVisitSummary);
        when(radiologyProperties.getRadiologyOrderEncounterType()).thenReturn(placeOrdersEncounterType);
        when(radiologyProperties.getRadiologyStudyEncounterType()).thenReturn(radiologyStudyEncounterType);
        when(radiologyProperties.getRadiologyReportEncounterType()).thenReturn(radiologyReportEncounterType);
        when(radiologyProperties.getRadiologyTechnicianEncounterRole()).thenReturn(radiologyTechnicianEncounterRole);
        when(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()).thenReturn(principalResultsInterpreterEncounterRole);
        when(emrProperties.getOrderingProviderEncounterRole()).thenReturn(clinicianEncounterRole);
        when(emrApiProperties.getUnknownLocation()).thenReturn(unknownLocation);
        when(emrApiProperties.getUnknownProvider()).thenReturn(unknownProvider);
        when(emrContext.getSessionLocation()).thenReturn(currentLocation);
        when(radiologyProperties.getRadiologyTestOrderType()).thenReturn(orderType);
        when(encounterService.saveEncounter(isA(Encounter.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return args[0];
            }
        });
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
    public void shouldCreateRadiologyStudyEncounter() {

        Concept radiologyStudySetConcept = setupConcept(conceptService, "Radiology Study Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_STUDY_SET);
        Concept accessionNumberConcept = setupConcept(conceptService, "Accession Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER);
        Concept imagesAvailableConcept = setupConcept(conceptService, "Images Available", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_IMAGES_AVAILABLE);
        Concept procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

        radiologyStudySetConcept.addSetMember(accessionNumberConcept);
        radiologyStudySetConcept.addSetMember(imagesAvailableConcept);
        radiologyStudySetConcept.addSetMember(procedureConcept);

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

    @Test
    public void shouldNotFailIfTechnicianAndLocationNotSpecified() {

        Concept radiologyStudySetConcept = setupConcept(conceptService, "Radiology Study Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_STUDY_SET);
        Concept accessionNumberConcept = setupConcept(conceptService, "Accession Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER);
        Concept imagesAvailableConcept = setupConcept(conceptService, "Images Available", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_IMAGES_AVAILABLE);
        Concept procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

        radiologyStudySetConcept.addSetMember(accessionNumberConcept);
        radiologyStudySetConcept.addSetMember(imagesAvailableConcept);
        radiologyStudySetConcept.addSetMember(procedureConcept);

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
    public void shouldCreateRadiologyReportEncounter() {

        Concept radiologyReportSetConcept = setupConcept(conceptService, "Radiology Report Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_SET);
        Concept accessionNumberConcept = setupConcept(conceptService, "Accession Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER);
        Concept reportBodyConcept = setupConcept(conceptService, "Report Body", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_BODY);
        Concept reportTypeConcept = setupConcept(conceptService, "Report Type", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_TYPE);;
        Concept procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

        radiologyReportSetConcept.addSetMember(accessionNumberConcept);
        radiologyReportSetConcept.addSetMember(reportBodyConcept);
        radiologyReportSetConcept.addSetMember(reportTypeConcept);
        radiologyReportSetConcept.addSetMember(procedureConcept);

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

    @Test
    public void shouldNotFailIfInterpreterAndLocationNotSpecified() {

        Concept radiologyReportSetConcept = setupConcept(conceptService, "Radiology Report Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_SET);
        Concept accessionNumberConcept = setupConcept(conceptService, "Accession Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER);
        Concept reportBodyConcept = setupConcept(conceptService, "Report Body", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_BODY);
        Concept reportTypeConcept = setupConcept(conceptService, "Report Type", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_TYPE);;
        Concept procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

        radiologyReportSetConcept.addSetMember(accessionNumberConcept);
        radiologyReportSetConcept.addSetMember(reportBodyConcept);
        radiologyReportSetConcept.addSetMember(reportTypeConcept);
        radiologyReportSetConcept.addSetMember(procedureConcept);

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

    private Concept setupConcept(ConceptService mockConceptService, String name, String mappingCode) {
        Concept concept = new Concept();
        concept.addName(new ConceptName(name, Locale.ENGLISH));
        concept.addConceptMapping(new ConceptMap(new ConceptReferenceTerm(emrConceptSource, mappingCode, null), sameAs));
        when(mockConceptService.getConceptByMapping(mappingCode, emrConceptSource.getName())).thenReturn(concept);
        return concept;
    }
}
