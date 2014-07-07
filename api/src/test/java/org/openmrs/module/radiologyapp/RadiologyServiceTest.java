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

import org.apache.commons.lang3.StringUtils;
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
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.db.EmrEncounterDAO;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.idgen.validator.LuhnMod10IdentifierValidator;
import org.openmrs.module.radiologyapp.db.RadiologyOrderDAO;
import org.openmrs.module.radiologyapp.exception.RadiologyAPIException;
import org.openmrs.module.radiologyapp.matchers.IsExpectedRadiologyReport;
import org.openmrs.module.radiologyapp.matchers.IsExpectedRadiologyStudy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.co.it.modular.hamcrest.date.DateMatchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

    private ConceptService conceptService;
    
    private UserService userService;

    private EmrEncounterDAO emrEncounterDAO;

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

    private Person providerPerson;

    private User providerUserAccount;

    private Provider anotherProvider;

    private Person anotherProviderPerson;

    private User anotherProviderUserAccount;

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

    private Concept creatinineLevelConcept;

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
        emrConceptSource.setName(EmrApiConstants.EMR_CONCEPT_SOURCE_NAME);

        patient = new Patient();
        orderType = new OrderType();
        clinicalHistory = "Patient fell from a building";

        providerPerson = new Person();
        provider = new Provider();
        provider.setPerson(providerPerson);
        providerUserAccount = new User(1);

        anotherProviderPerson = new Person();
        anotherProvider = new Provider();
        anotherProvider.setPerson(anotherProviderPerson);
        anotherProviderUserAccount = new User(2);

        currentLocation = new Location();
        unknownLocation = new Location();
        unknownProvider = new Provider();

        currentVisit = new Visit();
        currentVisit.setStartDatetime(currentDate);
        placeOrdersEncounterType = new EncounterType();
        radiologyStudyEncounterType = new EncounterType();
        radiologyReportEncounterType = new EncounterType();
        clinicianEncounterRole = new EncounterRole();
        radiologyTechnicianEncounterRole = new EncounterRole();
        principalResultsInterpreterEncounterRole = new EncounterRole();
        creatinineLevelConcept = new Concept();

        prepareMocks();
        setupRadiologyStudyAndRadiologyReportsConceptSets();

        radiologyService = new RadiologyServiceImpl();
        radiologyService.setEmrApiProperties(emrApiProperties);
        radiologyService.setRadiologyProperties(radiologyProperties);
        radiologyService.setEncounterService(encounterService);
        radiologyService.setRadiologyOrderDAO(radiologyOrderDAO);
        radiologyService.setConceptService(conceptService);
        radiologyService.setUserService(userService);
        radiologyService.setEmrEncounterDAO(emrEncounterDAO);
    }

    private void prepareMocks() {
        emrApiProperties = mock(EmrApiProperties.class);
        radiologyProperties = mock(RadiologyProperties.class);
        encounterService = mock(EncounterService.class);
        conceptService = mock(ConceptService.class);
        radiologyOrderDAO = mock(RadiologyOrderDAO.class);
        conceptService = mock(ConceptService.class);
        userService = mock(UserService.class);
        emrEncounterDAO = mock(EmrEncounterDAO.class);
        booleanType = mock(ConceptDatatype.class);

        VisitDomainWrapper currentVisitSummary = new VisitDomainWrapper(currentVisit);

        when(radiologyProperties.getRadiologyOrderEncounterType()).thenReturn(placeOrdersEncounterType);
        when(radiologyProperties.getRadiologyStudyEncounterType()).thenReturn(radiologyStudyEncounterType);
        when(radiologyProperties.getRadiologyReportEncounterType()).thenReturn(radiologyReportEncounterType);
        when(radiologyProperties.getRadiologyTechnicianEncounterRole()).thenReturn(radiologyTechnicianEncounterRole);
        when(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()).thenReturn(principalResultsInterpreterEncounterRole);
        when(radiologyProperties.getCreatinineLevelConcept()).thenReturn(creatinineLevelConcept);
        when(emrApiProperties.getOrderingProviderEncounterRole()).thenReturn(clinicianEncounterRole);
        when(emrApiProperties.getUnknownLocation()).thenReturn(unknownLocation);
        when(emrApiProperties.getUnknownProvider()).thenReturn(unknownProvider);
        when(radiologyProperties.getRadiologyTestOrderType()).thenReturn(orderType);
        when(booleanType.isBoolean()).thenReturn(true);
        when(Context.getConceptService()).thenReturn(conceptService);
        when(conceptService.getTrueConcept()).thenReturn(trueConcept);
        when(conceptService.getFalseConcept()).thenReturn(falseConcept);
        when(userService.getUsersByPerson(providerPerson, false)).thenReturn(Arrays.asList(providerUserAccount));
        when(userService.getUsersByPerson(anotherProviderPerson, false)).thenReturn(Arrays.asList(anotherProviderUserAccount));
        when(encounterService.saveEncounter(isA(Encounter.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Encounter encounter = (Encounter) (args[0]);

                // mimic setting the order id on the order
                int orderId = 1;
                for (Order order : encounter.getOrders()) {
                    order.setId(orderId);
                    orderId++;
                }
                return encounter;
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
    public void placeRadiologyRequisition_shouldPlaceARadiologyRequisitionWithOneStudyOnFixedMachine()
        throws Exception {
        Concept study = new Concept();
        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setClinicalHistory(clinicalHistory);
        radiologyRequisition.addStudy(study);
        radiologyRequisition.setUrgency(Order.Urgency.STAT);
        radiologyRequisition.setRequestedBy(provider);
        radiologyRequisition.setRequestedFrom(currentLocation);
        radiologyRequisition.setVisit(currentVisit);

        Encounter encounter = radiologyService.placeRadiologyRequisition(radiologyRequisition);

        assertThat(encounter, is(new IsExpectedRadiologyOrderEncounter(null, currentLocation, provider, providerUserAccount, null, null, null, study)));
        assertThat(encounter.getOrders().iterator().next().getAccessionNumber(), is("0000000018"));
    }

    @Test
    public void placeRadiologyRequisition_shouldPlaceARadiologyRequisitionWithTwoStudiesOnPortableMachine()
        throws Exception {
        Location examLocation = new Location();
        Concept study = new Concept();
        Concept secondStudy = new Concept();

        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setClinicalHistory(clinicalHistory);
        radiologyRequisition.addStudy(study);
        radiologyRequisition.addStudy(secondStudy);
        radiologyRequisition.setUrgency(Order.Urgency.STAT);
        radiologyRequisition.setExamLocation(examLocation);
        radiologyRequisition.setRequestedBy(provider);
        radiologyRequisition.setRequestedFrom(currentLocation);
        radiologyRequisition.setVisit(currentVisit);


        Encounter encounter = radiologyService.placeRadiologyRequisition(radiologyRequisition);

        assertThat(encounter, new IsExpectedRadiologyOrderEncounter(examLocation, currentLocation, provider,  providerUserAccount, null, null, null, study, secondStudy));
    }

    @Test
    public void placeRadiologyRequisition_shouldPlaceOrderEvenIfThereIsNoVisit()
        throws Exception {
        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setRequestedBy(provider);
        radiologyRequisition.setRequestedFrom(currentLocation);

        Encounter encounter = radiologyService.placeRadiologyRequisition(radiologyRequisition);

        assertThat(encounter.getVisit(), is(nullValue()));
    }

    @Test
    public void placeRadiologyRequisition_shouldPlaceARadiologyRequisitionWithCustomOrderDateAndOrderLocationAndProvider()
            throws Exception {

        Location orderLocation = new Location();
        Date orderDate = new DateTime(2012, 5, 25, 13, 10, 5).toDate();  // just an arbitrary date
        currentVisit.setStartDatetime(new DateTime(2012, 5, 25, 0, 0, 0).toDate());

        Concept study = new Concept();
        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setClinicalHistory(clinicalHistory);
        radiologyRequisition.addStudy(study);
        radiologyRequisition.setUrgency(Order.Urgency.STAT);
        radiologyRequisition.setRequestedBy(anotherProvider);
        radiologyRequisition.setRequestedFrom(orderLocation);
        radiologyRequisition.setRequestedOn(orderDate);
        radiologyRequisition.setVisit(currentVisit);

        Encounter encounter = radiologyService.placeRadiologyRequisition(radiologyRequisition);

        assertThat(encounter, is(new IsExpectedRadiologyOrderEncounter(null, orderLocation, anotherProvider,  anotherProviderUserAccount, orderDate, null, null, study)));
    }

    @Test
    public void placeRadiologyRequisition_shouldTimeShiftEncounterDatetimeAsVisit()
            throws Exception {

        // this test just confirmed that EncounterDomainWrapper.attachToVisit() is still working as well expect

        Location orderLocation = new Location();
        Date orderDate = new DateTime(2012, 5, 25, 0, 0, 0).toDate();  // a date without a time component
        currentVisit.setStartDatetime(new DateTime(2012, 5, 25, 10, 10, 10).toDate());  // visit started on that date

        Concept study = new Concept();
        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setClinicalHistory(clinicalHistory);
        radiologyRequisition.addStudy(study);
        radiologyRequisition.setUrgency(Order.Urgency.STAT);
        radiologyRequisition.setRequestedBy(provider);
        radiologyRequisition.setRequestedFrom(orderLocation);
        radiologyRequisition.setRequestedOn(orderDate);
        radiologyRequisition.setVisit(currentVisit);

        Encounter encounter = radiologyService.placeRadiologyRequisition(radiologyRequisition);

        // note that since the visit started *after* the order, the orderDate should be visit startdate
        assertThat(encounter, is(new IsExpectedRadiologyOrderEncounter(null, orderLocation, provider,  providerUserAccount, currentVisit.getStartDatetime(), null, null, study)));
    }

    @Test
    public void placeRadiologyRequisition_shouldStoreCreatinineValue()
            throws Exception {
        Concept study = new Concept();
        RadiologyRequisition radiologyRequisition = new RadiologyRequisition();
        radiologyRequisition.setPatient(patient);
        radiologyRequisition.setClinicalHistory(clinicalHistory);
        radiologyRequisition.addStudy(study);
        radiologyRequisition.setUrgency(Order.Urgency.STAT);
        radiologyRequisition.setRequestedBy(provider);
        radiologyRequisition.setRequestedFrom(currentLocation);
        radiologyRequisition.setVisit(currentVisit);
        radiologyRequisition.setCreatinineLevel(1.8);
        Date creatinineTestDate = new DateTime(2014, 01, 20, 0, 0, 0).toDate();
        radiologyRequisition.setCreatinineTestDate(creatinineTestDate);

        Encounter encounter = radiologyService.placeRadiologyRequisition(radiologyRequisition);

        assertThat(encounter, is(new IsExpectedRadiologyOrderEncounter(null, currentLocation, provider, providerUserAccount, null, 1.8, creatinineTestDate, study)));
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

        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyStudyEncounterType, false))
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
    public void getRadiologyStudiesForPatient_shouldReturnAllRadiologyStudiesForPatient() {

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
    public void getRadiologyStudiesForPatient_shouldNotFailIfNoObsGroup() {

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
    public void getRadiologyStudiesForPatient_shouldReturnEmptyListIfNoStudiesForPatient() {

        when(encounterService.getEncounters(patient, null, null, null, null, Collections.singletonList(radiologyStudyEncounterType),
                null, null, null, false)).thenReturn(null);

        List<RadiologyStudy> radiologyStudies = radiologyService.getRadiologyStudiesForPatient(patient);
        assertThat(radiologyStudies.size(), is(0));

    }

    @Test
    public void getRadiologyStudiesForPatient_shouldDeriveRadiologyStudyFromReports() {

        // first, create a couple reports (with two different accession numbers)
        Concept prelimReport = new Concept();
        Concept finalReport = new Concept();
        Concept procedure = new Concept();
        procedure.setId(111);

        Date firstReportDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Provider firstReporter = new Provider();
        Location firstLocation = new Location();

        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setAccessionNumber("123");
        firstRadiologyReport.setReportDate(firstReportDate);
        firstRadiologyReport.setProcedure(procedure);
        firstRadiologyReport.setPatient(patient);
        firstRadiologyReport.setPrincipalResultsInterpreter(firstReporter);
        firstRadiologyReport.setReportLocation(firstLocation);
        firstRadiologyReport.setReportType(prelimReport);
        firstRadiologyReport.setReportBody("Some prelim report");

        Date secondReportDate = new DateTime(2012, 12, 30, 12, 0, 0, 0).toDate();
        Provider secondReporter = new Provider();
        Location secondLocation = new Location();

        RadiologyReport secondRadiologyReport = new RadiologyReport();
        secondRadiologyReport.setAccessionNumber("456");
        secondRadiologyReport.setReportDate(secondReportDate);
        secondRadiologyReport.setProcedure(procedure);
        secondRadiologyReport.setPatient(patient);
        secondRadiologyReport.setPrincipalResultsInterpreter(secondReporter);
        secondRadiologyReport.setReportLocation(secondLocation);
        secondRadiologyReport.setReportType(finalReport);
        secondRadiologyReport.setReportBody("Another prelim report");

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(setupRadiologyReportEncounter(firstRadiologyReport));
        encounters.add(setupRadiologyReportEncounter(secondRadiologyReport));

        // return an empty list when trying to fetch studies
        when(encounterService.getEncounters(patient, null, null, null, null, Collections.singletonList(radiologyStudyEncounterType),
                null, null, null, false)).thenReturn(new ArrayList<Encounter>());

        when(encounterService.getEncounters(patient, null, null, null, null, Collections.singletonList(radiologyReportEncounterType),
                null, null, null, false)).thenReturn(encounters);

        RadiologyStudy firstExpectedStudy = new RadiologyStudy();
        firstExpectedStudy.setDatePerformed(firstReportDate);
        firstExpectedStudy.setPatient(patient);
        firstExpectedStudy.setProcedure(procedure);
        firstExpectedStudy.setAccessionNumber("123");

        RadiologyStudy secondExpectedStudy = new RadiologyStudy();
        secondExpectedStudy.setDatePerformed(secondReportDate);
        secondExpectedStudy.setPatient(patient);
        secondExpectedStudy.setProcedure(procedure);
        secondExpectedStudy.setAccessionNumber("456");

        List<RadiologyStudy> radiologyStudies = radiologyService.getRadiologyStudiesForPatient(patient);

        assertThat(radiologyStudies.size(), is(2));
        assertTrue(new IsExpectedRadiologyStudy(secondExpectedStudy).matches(radiologyStudies.get(0)));
        assertTrue(new IsExpectedRadiologyStudy(firstExpectedStudy).matches(radiologyStudies.get(1)));
    }

    @Test
    public void getRadiologyStudyByAccessionNumber_shouldReturnRadiologyStudyWithAccessionNumber() {

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

        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyStudyEncounterType, false))
                .thenReturn(encounters);

        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("123");
        assertTrue(new IsExpectedRadiologyStudy(expectedStudy).matches(radiologyStudy));
    }

    @Test
    public void getRadiologyStudyByAccessionNumber_shouldNotFailIfMultipleResults() {

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


        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyStudyEncounterType, false))
                .thenReturn(encounters);

        // should just return the first study
        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("123");
        assertTrue(new IsExpectedRadiologyStudy(firstStudy).matches(radiologyStudy));
    }


    @Test
    public void getRadiologyStudyByAccessionNumber_shouldReturnNullIfNoMatchingStudy() {
        when(emrEncounterDAO.getEncountersByObsValueText(any(Concept.class), any(String.class)
                , any(EncounterType.class), eq(false))).thenReturn(null);

        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("1234");
        assertNull(radiologyStudy);
    }

    @Test
    public void getRadiologyStudyByAccessionNumber_shouldDeriveRadiologyStudyFromReports() {

        // first, create a couple reports

        Concept prelimReport = new Concept();
        Concept finalReport = new Concept();
        Concept procedure = new Concept();
        procedure.setId(111);

        Date firstReportDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Provider firstReporter = new Provider();
        Location firstLocation = new Location();

        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setAccessionNumber("123");
        firstRadiologyReport.setReportDate(firstReportDate);
        firstRadiologyReport.setProcedure(procedure);
        firstRadiologyReport.setPatient(patient);
        firstRadiologyReport.setPrincipalResultsInterpreter(firstReporter);
        firstRadiologyReport.setReportLocation(firstLocation);
        firstRadiologyReport.setReportType(prelimReport);
        firstRadiologyReport.setReportBody("Some prelim report");

        Date secondReportDate = new DateTime(2012, 12, 30, 12, 0, 0, 0).toDate();
        Provider secondReporter = new Provider();
        Location secondLocation = new Location();

        RadiologyReport secondRadiologyReport = new RadiologyReport();
        secondRadiologyReport.setAccessionNumber("123");
        secondRadiologyReport.setReportDate(secondReportDate);
        secondRadiologyReport.setProcedure(procedure);
        secondRadiologyReport.setPatient(patient);
        secondRadiologyReport.setPrincipalResultsInterpreter(secondReporter);
        secondRadiologyReport.setReportLocation(secondLocation);
        secondRadiologyReport.setReportType(finalReport);
        secondRadiologyReport.setReportBody("Some final report");

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(setupRadiologyReportEncounter(firstRadiologyReport));
        encounters.add(setupRadiologyReportEncounter(secondRadiologyReport));

        // return an empty list when trying to fetch studies
        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyStudyEncounterType, false))
                .thenReturn(new ArrayList<Encounter>());

        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyReportEncounterType, false))
                .thenReturn(encounters);

        RadiologyStudy expectedStudy = new RadiologyStudy();
        expectedStudy.setDatePerformed(firstReportDate);
        expectedStudy.setPatient(patient);
        expectedStudy.setProcedure(procedure);
        expectedStudy.setAccessionNumber("123");

        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("123");
        assertTrue(new IsExpectedRadiologyStudy(expectedStudy).matches(radiologyStudy));
    }


    @Test
    public void getRadiologyReportsByAccessionNumber_shouldReturnRadiologyReportsWithAccessionNumber() {

        Concept prelimReport = new Concept();
        Concept finalReport = new Concept();
        Concept procedure = new Concept();
        procedure.setId(111);

        Date firstReportDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Provider firstReporter = new Provider();
        Location firstLocation = new Location();

        RadiologyReport firstExpectedRadiologyReport = new RadiologyReport();
        firstExpectedRadiologyReport.setAccessionNumber("123");
        firstExpectedRadiologyReport.setReportDate(firstReportDate);
        firstExpectedRadiologyReport.setProcedure(procedure);
        firstExpectedRadiologyReport.setPatient(patient);
        firstExpectedRadiologyReport.setPrincipalResultsInterpreter(firstReporter);
        firstExpectedRadiologyReport.setReportLocation(firstLocation);
        firstExpectedRadiologyReport.setReportType(prelimReport);
        firstExpectedRadiologyReport.setReportBody("Some prelim report");

        Date secondReportDate = new DateTime(2012, 12, 30, 12, 0, 0, 0).toDate();
        Provider secondReporter = new Provider();
        Location secondLocation = new Location();

        RadiologyReport secondExpectedRadiologyReport = new RadiologyReport();
        secondExpectedRadiologyReport.setAccessionNumber("123");
        secondExpectedRadiologyReport.setReportDate(secondReportDate);
        secondExpectedRadiologyReport.setProcedure(procedure);
        secondExpectedRadiologyReport.setPatient(patient);
        secondExpectedRadiologyReport.setPrincipalResultsInterpreter(secondReporter);
        secondExpectedRadiologyReport.setReportLocation(secondLocation);
        secondExpectedRadiologyReport.setReportType(finalReport);
        secondExpectedRadiologyReport.setReportBody("Some final report");

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(setupRadiologyReportEncounter(firstExpectedRadiologyReport));
        encounters.add(setupRadiologyReportEncounter(secondExpectedRadiologyReport));

        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyReportEncounterType, false))
                .thenReturn(encounters);

        List<RadiologyReport> radiologyReports = radiologyService.getRadiologyReportsByAccessionNumber("123");
        assertThat(radiologyReports.size(), is(2));

        // should now be in reverse order since we sort by report date with most recent first
        assertTrue(new IsExpectedRadiologyReport(secondExpectedRadiologyReport).matches(radiologyReports.get(0)));
        assertTrue(new IsExpectedRadiologyReport(firstExpectedRadiologyReport).matches(radiologyReports.get(1)));
    }

    @Test
    public void getRadiologyReportsByAccessionNumber_shouldNotFailIfNoObsGroups() {

        Concept prelimReport = new Concept();
        Concept finalReport = new Concept();
        Concept procedure = new Concept();
        procedure.setId(111);

        Date firstReportDate = new DateTime(2012, 12, 25, 12, 0, 0, 0).toDate();
        Provider firstReporter = new Provider();
        Location firstLocation = new Location();

        RadiologyReport firstExpectedRadiologyReport = new RadiologyReport();
        firstExpectedRadiologyReport.setReportDate(firstReportDate);
        firstExpectedRadiologyReport.setPatient(patient);
        firstExpectedRadiologyReport.setPrincipalResultsInterpreter(firstReporter);
        firstExpectedRadiologyReport.setReportLocation(firstLocation);

        Date secondReportDate = new DateTime(2012, 12, 30, 12, 0, 0, 0).toDate();
        Provider secondReporter = new Provider();
        Location secondLocation = new Location();

        RadiologyReport secondExpectedRadiologyReport = new RadiologyReport();
        secondExpectedRadiologyReport.setReportDate(secondReportDate);
        secondExpectedRadiologyReport.setPatient(patient);
        secondExpectedRadiologyReport.setPrincipalResultsInterpreter(secondReporter);
        secondExpectedRadiologyReport.setReportLocation(secondLocation);

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(setupRadiologyReportEncounterWithoutObsGroup(firstExpectedRadiologyReport));
        encounters.add(setupRadiologyReportEncounterWithoutObsGroup(secondExpectedRadiologyReport));

        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyReportEncounterType, false))
                .thenReturn(encounters);

        List<RadiologyReport> radiologyReports = radiologyService.getRadiologyReportsByAccessionNumber("123");
        assertThat(radiologyReports.size(), is(2));

        // should now be in reverse order since we sort by report date with most recent first
        assertTrue(new IsExpectedRadiologyReport(secondExpectedRadiologyReport).matches(radiologyReports.get(0)));
        assertTrue(new IsExpectedRadiologyReport(firstExpectedRadiologyReport).matches(radiologyReports.get(1)));
    }

    @Test
    public void getRadiologyReportsByAccessionNumber_shouldReturnEmptyListIfNoMatchingStudies() {

        when(emrEncounterDAO.getEncountersByObsValueText(accessionNumberConcept, "123", radiologyReportEncounterType, false))
                .thenReturn(new ArrayList<Encounter>());

        List<RadiologyReport> radiologyReports = radiologyService.getRadiologyReportsByAccessionNumber("123");
        assertThat(radiologyReports.size(), is(0));

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

    private Encounter setupRadiologyReportEncounter(RadiologyReport report) {

        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyReportEncounterType);
        encounter.setEncounterDatetime(report.getReportDate());
        encounter.setLocation(report.getReportLocation());
        encounter.setPatient(report.getPatient());
        encounter.setProvider(principalResultsInterpreterEncounterRole, report.getPrincipalResultsInterpreter());

        Obs radiologyReportObsGroup = new Obs();
        radiologyReportObsGroup.setConcept(radiologyReportSetConcept);
        radiologyReportObsGroup.setId(333);

        Obs accessionNumberObs = new Obs();
        accessionNumberObs.setConcept(accessionNumberConcept);
        accessionNumberObs.setValueText(report.getAccessionNumber());
        radiologyReportObsGroup.addGroupMember(accessionNumberObs);

        Obs procedureObs = new Obs();
        procedureObs.setConcept(procedureConcept);
        procedureObs.setValueCoded(report.getProcedure());
        radiologyReportObsGroup.addGroupMember(procedureObs);

        Obs reportTypeObs = new Obs();
        reportTypeObs.setConcept(reportTypeConcept);
        reportTypeObs.setValueCoded(report.getReportType());
        radiologyReportObsGroup.addGroupMember(reportTypeObs);

        Obs reportBodyObs = new Obs();
        reportBodyObs.setConcept(reportBodyConcept);
        reportBodyObs.setValueText(report.getReportBody());
        radiologyReportObsGroup.addGroupMember(reportBodyObs);

        encounter.addObs(radiologyReportObsGroup);

        return encounter;
    }

    private Encounter setupRadiologyReportEncounterWithoutObsGroup(RadiologyReport report) {

        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyReportEncounterType);
        encounter.setEncounterDatetime(report.getReportDate());
        encounter.setLocation(report.getReportLocation());
        encounter.setPatient(report.getPatient());
        encounter.setProvider(principalResultsInterpreterEncounterRole, report.getPrincipalResultsInterpreter());

        return encounter;
    }


    // TODO: could move the rest of these matchers out into separate classes in matchers package

    private class IsExpectedOrder extends ArgumentMatcher<Order> {
        private Location expectedLocation;
        private Concept expectedStudy;
        private Date expectedOrderDate;
        private User expectedOrderer;
        private String expectedAccessionNumber;

        public IsExpectedOrder(Location expectedLocation, Date expectedOrderDate, User expectedOrderer, Concept expectedStudy) {
            this.expectedLocation = expectedLocation;
            this.expectedStudy = expectedStudy;
            this.expectedOrderDate = expectedOrderDate;
            this.expectedOrderer = expectedOrderer;
        }

        @Override
        public boolean matches(Object o) {
            RadiologyOrder actual = (RadiologyOrder) o;

            try {
                assertThat(actual.getOrderType(), is(orderType));
                assertThat(actual.getPatient(), is(patient));
                assertThat(actual.getConcept(), is(expectedStudy));
                assertThat(actual.getUrgency(), is(Order.Urgency.STAT));
                assertThat(actual.getClinicalHistory(), is(clinicalHistory));
                assertThat(actual.getExamLocation(), is(expectedLocation));
                assertThat(actual.getOrderer(), is(expectedOrderer));
                assertThat(actual.getAccessionNumber(), is(StringUtils.leftPad(new LuhnMod10IdentifierValidator().getValidIdentifier(actual.getId().toString()), 10, "0")));

                if (expectedOrderDate != null) {
                    assertThat(actual.getStartDate(), is(expectedOrderDate));
                }
                else {
                    assertThat(actual.getStartDate(), DateMatchers.within(1, TimeUnit.SECONDS, new Date()));
                }
            }
            catch (AssertionError e) {
                return false;
            }

            return true;
        }
    }

    private class IsExpectedRadiologyOrderEncounter extends ArgumentMatcher<Encounter> {

        private Concept[] expectedStudies;
        private List<IsExpectedOrder> expectedOrders = new ArrayList<IsExpectedOrder>();
        private Provider expectedOrderer;
        private Date expectedOrderDate;
        private Location expectedOrderLocation;
        private User expectedOrderedUserAccount;
        private Double expectedCreatinineLevel;
        private Date expectedCreatinineTestDate;

        public IsExpectedRadiologyOrderEncounter(Location expectedLocation, Location expectedOrderLocation,
                                                 Provider expectedOrderer, User expectedOrdererUserAccount, Date expectedOrderDate,
                                                 Double expectedCreatinineLevel, Date expectedCreatinineTestDate, Concept... expectedStudies) {

            this.expectedStudies = expectedStudies;
            this.expectedOrderer = expectedOrderer;
            this.expectedOrderDate = expectedOrderDate;
            this.expectedOrderLocation = expectedOrderLocation;
            this.expectedOrderedUserAccount = expectedOrdererUserAccount;
            this.expectedCreatinineLevel = expectedCreatinineLevel;
            this.expectedCreatinineTestDate = expectedCreatinineTestDate;

            int expectedId = 1;
            for (Concept expectedStudy : expectedStudies) {
                expectedOrders.add(new IsExpectedOrder(expectedLocation, expectedOrderDate, expectedOrdererUserAccount, expectedStudy));
                expectedId++;
            }

        }

        @Override
        public boolean matches(Object o) {
            Encounter encounter = (Encounter) o;

            Set<Provider> providersByRole = encounter.getProvidersByRole(clinicianEncounterRole);
            assertThat(encounter.getEncounterType(), is(placeOrdersEncounterType));
            assertThat(providersByRole.size(), is(1));
            assertThat(providersByRole.iterator().next(), is(expectedOrderer));
            assertThat(encounter.getPatient(), is(patient));
            assertThat(encounter.getLocation(), is(expectedOrderLocation));
            assertThat(encounter.getVisit(), is(currentVisit));
            assertThat(encounter.getOrders().size(), is(expectedStudies.length));

            if (expectedOrderDate != null) {
                assertThat(encounter.getEncounterDatetime(), is(expectedOrderDate));
            }
            else {
                assertThat(encounter.getEncounterDatetime(), notNullValue());
            }

            assertTrue(new IsIterableContainingInAnyOrder(expectedOrders).matches(encounter.getOrders()));

            if (expectedCreatinineLevel != null) {
                assertFalse(encounter.getObs().isEmpty());
                // will need to fix this once/if we have multiple obs on this encounter
                Obs creatinineObs = encounter.getObs().iterator().next();
                assertThat(creatinineObs.getValueNumeric(), is (expectedCreatinineLevel));
                assertThat(creatinineObs.getObsDatetime(), is(expectedCreatinineTestDate));
            }
            else {
                // will need to fix this once/if we have multiple obs on this encounter
                assertTrue(encounter.getObs().isEmpty());
            }

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
