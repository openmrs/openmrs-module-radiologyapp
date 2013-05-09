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

import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.radiologyapp.exception.RadiologyAPIException;
import org.openmrs.module.radiologyapp.matchers.IsExpectedRadiologyReport;
import org.openmrs.module.radiologyapp.matchers.IsExpectedRadiologyStudy;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RadiologyServiceComponentTest extends BaseModuleContextSensitiveTest {

    @Autowired
    @Qualifier("radiologyService")
    RadiologyService radiologyService;

    @Autowired
    @Qualifier("patientService")
    private PatientService patientService;

    @Autowired
    @Qualifier("providerService")
    private ProviderService providerService;

    @Qualifier("orderService")
    private OrderService orderService;

    @Autowired
    @Qualifier("conceptService")
    private ConceptService conceptService;

    @Autowired
    @Qualifier("locationService")
    private LocationService locationService;

    @Autowired
    @Qualifier("encounterService")
    private EncounterService encounterService;

    @Autowired
    @Qualifier("emrApiProperties")
    private EmrApiProperties emrApiProperties;

    @Autowired
    @Qualifier("radiologyProperties")
    private RadiologyProperties radiologyProperties;

    @Before
    public void beforeAllTests() throws Exception {
        executeDataSet("radiologyServiceComponentTestDataset.xml");
    }

    @Test
    public void testThatServiceIsConfiguredCorrectly() {
        Assert.assertNotNull("Couldn't autowire RadiologyService", radiologyService);
        Assert.assertNotNull("Couldn't get RadiologyService from Context", Context.getService(RadiologyService.class));
    }

    @Test
    public void placeRadiologyRequistion_shouldPlaceARadiologyRequisition() {

        Patient patient = patientService.getPatient(6);

        // sanity check
        Assert.assertEquals(0, encounterService.getEncountersByPatient(patient).size());

        EmrContext emrContext = mock(EmrContext.class);
        when(emrContext.getSessionLocation()).thenReturn(locationService.getLocation(1));
        when(emrContext.getActiveVisit()).thenReturn(null);

        RadiologyRequisition requisition = new RadiologyRequisition();

        requisition.setPatient(patient);
        requisition.setStudies(Collections.singleton(conceptService.getConcept(18)));
        requisition.setUrgency(Order.Urgency.STAT);

        radiologyService.placeRadiologyRequisition(emrContext, requisition);

        List<Encounter> encounters = encounterService.getEncountersByPatient(patient);
        Assert.assertEquals(1, encounters.size());

        Set<Order> orders = encounters.get(0).getOrders();
        Assert.assertEquals(1, orders.size());

    }

    @Test
    public void getRadiologyOrderByAccessionNumber_shouldRetrieveOrderByAccessionNumber() {

        Patient patient = patientService.getPatient(6);

        EmrContext emrContext = mock(EmrContext.class);
        when(emrContext.getSessionLocation()).thenReturn(locationService.getLocation(1));
        when(emrContext.getActiveVisit()).thenReturn(null);

        RadiologyRequisition requisition = new RadiologyRequisition();

        requisition.setPatient(patient);
        requisition.setStudies(Collections.singleton(conceptService.getConcept(18)));
        requisition.setUrgency(Order.Urgency.STAT);

        radiologyService.placeRadiologyRequisition(emrContext, requisition);

        List<Encounter> encounters = encounterService.getEncountersByPatient(patient);
        Set<Order> orders = encounters.get(0).getOrders();

        String accessionNumber = orders.iterator().next().getAccessionNumber();

        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber(accessionNumber);
        Assert.assertNotNull(radiologyOrder);
        Assert.assertEquals(conceptService.getConcept(18), radiologyOrder.getConcept());
        Assert.assertEquals(Order.Urgency.STAT, radiologyOrder.getUrgency());
    }

    @Test
    public void saveRadiologyStudy_shouldSaveARadiologyStudy() {

        Date timeOfStudy = new Date();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        RadiologyStudy radiologyStudy = new RadiologyStudy();
        radiologyStudy.setPatient(patient);
        radiologyStudy.setProcedure(procedure);
        radiologyStudy.setImagesAvailable(true);
        radiologyStudy.setAccessionNumber("12345");
        radiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        radiologyStudy.setDatePerformed(timeOfStudy);

        Encounter encounter = radiologyService.saveRadiologyStudy(radiologyStudy);

        assertThat(encounter.getPatient(), is(patient));
        assertThat(encounter.getEncounterType(), is(radiologyProperties.getRadiologyStudyEncounterType()));
        assertThat(encounter.getEncounterDatetime(), is(timeOfStudy));
        assertThat(encounter.getLocation(), is(emrApiProperties.getUnknownLocation()));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole()).size(), is(1));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole()).iterator().next(), is(emrApiProperties.getUnknownProvider()));

        assertThat(encounter.getObsAtTopLevel(false).size(), is(1));

        Obs radiologyStudyObsSet = encounter.getObsAtTopLevel(false).iterator().next();

        assertThat(radiologyStudyObsSet.getGroupMembers().size(), is(3));
        assertThat(radiologyStudyObsSet.getOrder().getAccessionNumber(), is("12345"));

        Obs accessionNumberObs = null;
        Obs procedureObs = null;
        Obs imagesAvailableObs = null;

        // hack, just reference the concepts by their ids in the test dataset
        for (Obs obs : radiologyStudyObsSet.getGroupMembers()) {
            if (obs.getConcept().getId() == 1004) {
                accessionNumberObs = obs;
            }
            if (obs.getConcept().getId() == 1003) {
                procedureObs  = obs;
            }
            if (obs.getConcept().getId() == 1005) {
                imagesAvailableObs = obs;
            }
        }

        assertNotNull(accessionNumberObs);
        assertNotNull(procedureObs);
        assertNotNull(imagesAvailableObs);

        assertThat(accessionNumberObs.getValueText(), is("12345"));
        assertThat(procedureObs.getValueCoded(), is(procedure));
        assertThat(imagesAvailableObs.getValueAsBoolean(), is(true));

    }

    @Test(expected = RadiologyAPIException.class)
    public void saveRadiologyStudy_shouldFailIfAttemptingToSaveRadiologyStudyWithSameAccessionNumberAsExistingStudy() {

        Date timeOfStudy = new Date();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        RadiologyStudy firstRadiologyStudy = new RadiologyStudy();
        firstRadiologyStudy.setPatient(patient);
        firstRadiologyStudy.setProcedure(procedure);
        firstRadiologyStudy.setImagesAvailable(true);
        firstRadiologyStudy.setAccessionNumber("12345");
        firstRadiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        firstRadiologyStudy.setDatePerformed(timeOfStudy);

        RadiologyStudy secondRadiologyStudy = new RadiologyStudy();
        secondRadiologyStudy.setPatient(patient);
        secondRadiologyStudy.setProcedure(procedure);
        secondRadiologyStudy.setImagesAvailable(true);
        secondRadiologyStudy.setAccessionNumber("12345");
        secondRadiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        secondRadiologyStudy.setDatePerformed(timeOfStudy);

        radiologyService.saveRadiologyStudy(firstRadiologyStudy);
        radiologyService.saveRadiologyStudy(secondRadiologyStudy);

    }

    @Test
    public void saveRadiologyReport_shouldSaveARadiologyReport() {

        Date timeOfStudy = new Date();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        Concept reportType = conceptService.getConcept(1009);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        RadiologyReport radiologyReport = new RadiologyReport();
        radiologyReport.setPatient(patient);
        radiologyReport.setProcedure(procedure);
        radiologyReport.setReportType(reportType);
        radiologyReport.setReportBody("Some test report");
        radiologyReport.setAccessionNumber("12345");
        radiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        radiologyReport.setReportDate(timeOfStudy) ;

        Encounter encounter = radiologyService.saveRadiologyReport(radiologyReport);

        assertThat(encounter.getPatient(), is(patient));
        assertThat(encounter.getEncounterType(), is(radiologyProperties.getRadiologyReportEncounterType()));
        assertThat(encounter.getEncounterDatetime(), is(timeOfStudy));
        assertThat(encounter.getLocation(), is(emrApiProperties.getUnknownLocation()));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()).size(), is(1));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()).iterator().next(), is(emrApiProperties.getUnknownProvider()));

        assertThat(encounter.getObsAtTopLevel(false).size(), is(1));

        Obs radiologyStudyObsSet = encounter.getObsAtTopLevel(false).iterator().next();


        assertThat(radiologyStudyObsSet.getGroupMembers().size(), is(4));
        assertThat(radiologyStudyObsSet.getOrder().getAccessionNumber(), is("12345"));

        Obs accessionNumberObs = null;
        Obs procedureObs = null;
        Obs reportBodyObs = null;
        Obs reportTypeObs = null;

        // hack, just reference the concepts by their ids in the test dataset
        for (Obs obs : radiologyStudyObsSet.getGroupMembers()) {
            if (obs.getConcept().getId() == 1004) {
                accessionNumberObs = obs;
            }
            if (obs.getConcept().getId() == 1003) {
                procedureObs  = obs;
            }
            if (obs.getConcept().getId() == 1007) {
                reportTypeObs = obs;
            }
            if (obs.getConcept().getId() == 1008) {
                reportBodyObs = obs;
            }
        }

        assertNotNull(accessionNumberObs);
        assertNotNull(procedureObs);
        assertNotNull(reportTypeObs);
        assertNotNull(reportBodyObs);

        assertThat(accessionNumberObs.getValueText(), is("12345"));
        assertThat(procedureObs.getValueCoded(), is(procedure));
        assertThat(reportBodyObs.getValueText(), is("Some test report"));
        assertThat(reportTypeObs.getValueCoded(), is(reportType));

    }

    @Test
    public void getRadiologyStudiesForPatient_shouldRetrieveRadiologyStudiesForPatient() {

        // first create a couple studies

        Date timeOfFirstStudy = new DateTime(2012,1,1,10,10,10,10).toDate();
        Date timeOfSecondStudy = new DateTime(2013,4,3,20,20,20,20).toDate();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        // location and provider from test database
        Location location = locationService.getLocation(2);
        Provider provider = providerService.getProvider(1);

        RadiologyStudy firstRadiologyStudy = new RadiologyStudy();
        firstRadiologyStudy.setPatient(patient);
        firstRadiologyStudy.setProcedure(procedure);
        firstRadiologyStudy.setImagesAvailable(true);
        firstRadiologyStudy.setAccessionNumber("12345");
        firstRadiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        firstRadiologyStudy.setDatePerformed(timeOfFirstStudy);
        firstRadiologyStudy.setStudyLocation(emrApiProperties.getUnknownLocation());
        firstRadiologyStudy.setTechnician(emrApiProperties.getUnknownProvider());

        radiologyService.saveRadiologyStudy(firstRadiologyStudy);

        RadiologyStudy secondRadiologyStudy = new RadiologyStudy();
        secondRadiologyStudy.setPatient(patient);
        secondRadiologyStudy.setProcedure(procedure);
        secondRadiologyStudy.setImagesAvailable(false);
        secondRadiologyStudy.setAccessionNumber("678910");
        secondRadiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        secondRadiologyStudy.setDatePerformed(timeOfSecondStudy);
        secondRadiologyStudy.setStudyLocation(location);
        secondRadiologyStudy.setTechnician(provider);

        radiologyService.saveRadiologyStudy(secondRadiologyStudy);

        // now fetch the studies for the patient
        List<RadiologyStudy> studies = radiologyService.getRadiologyStudiesForPatient(patient);
        assertThat(studies.size(), is (2));

        assertTrue(new IsExpectedRadiologyStudy(secondRadiologyStudy).matches(studies.get(0)));
        assertTrue(new IsExpectedRadiologyStudy(firstRadiologyStudy).matches(studies.get(1)));
    }

    @Test
    public void getRadiologyStudiesForPatient_shouldDeriveRadiologyStudyFromRadiologyReports() {

        Date firstTimeOfReport = new DateTime(2012,1,1,10,10,10,10).toDate();
        Date secondTimeOfReport = new DateTime(2012,1,2,10,10,10,10).toDate();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        Concept reportType = conceptService.getConcept(1009);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        // first create a couple reports
        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setPatient(patient);
        firstRadiologyReport.setProcedure(procedure);
        firstRadiologyReport.setReportType(reportType);
        firstRadiologyReport.setReportBody("Some test report");
        firstRadiologyReport.setAccessionNumber("12345");
        firstRadiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        firstRadiologyReport.setReportDate(firstTimeOfReport) ;
        firstRadiologyReport.setPrincipalResultsInterpreter(emrApiProperties.getUnknownProvider());
        firstRadiologyReport.setReportLocation(emrApiProperties.getUnknownLocation());

        radiologyService.saveRadiologyReport(firstRadiologyReport);

        RadiologyReport secondRadiologyReport = new RadiologyReport();
        secondRadiologyReport.setPatient(patient);
        secondRadiologyReport.setProcedure(procedure);
        secondRadiologyReport.setReportType(reportType);
        secondRadiologyReport.setReportBody("Another test report");
        secondRadiologyReport.setAccessionNumber("67890");
        secondRadiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        secondRadiologyReport.setReportDate(secondTimeOfReport);
        secondRadiologyReport.setPrincipalResultsInterpreter(emrApiProperties.getUnknownProvider());
        secondRadiologyReport.setReportLocation(emrApiProperties.getUnknownLocation());

        radiologyService.saveRadiologyReport(secondRadiologyReport);

        // create the expected studies
        RadiologyStudy firstExpectedRadiologyStudy = new RadiologyStudy();
        firstExpectedRadiologyStudy.setDatePerformed(firstTimeOfReport);
        firstExpectedRadiologyStudy.setAccessionNumber("12345");
        firstExpectedRadiologyStudy.setProcedure(procedure);
        firstExpectedRadiologyStudy.setPatient(patient);

        RadiologyStudy secondExpectedRadiologyStudy = new RadiologyStudy();
        secondExpectedRadiologyStudy.setDatePerformed(secondTimeOfReport);
        secondExpectedRadiologyStudy.setAccessionNumber("67890");
        secondExpectedRadiologyStudy.setProcedure(procedure);
        secondExpectedRadiologyStudy.setPatient(patient);

        List<RadiologyStudy> radiologyStudies = radiologyService.getRadiologyStudiesForPatient(patient);
        assertThat(radiologyStudies.size(), is(2));
        assertTrue(new IsExpectedRadiologyStudy(firstExpectedRadiologyStudy).matches(radiologyStudies.get(1)));
        assertTrue(new IsExpectedRadiologyStudy(secondExpectedRadiologyStudy).matches(radiologyStudies.get(0)));
    }

    @Test
    public void getRadiologyStudyByAccessionNumber_shouldRetrieveRadiologyStudyByAccessionNumber() {

        // first create a study
        Date timeOfStudy = new DateTime(2012,1,1,10,10,10,10).toDate();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        // location and provider from test database
        Location location = locationService.getLocation(2);
        Provider provider = providerService.getProvider(1);

        RadiologyStudy expectedRadiologyStudy = new RadiologyStudy();
        expectedRadiologyStudy.setPatient(patient);
        expectedRadiologyStudy.setProcedure(procedure);
        expectedRadiologyStudy.setImagesAvailable(true);
        expectedRadiologyStudy.setAccessionNumber("12345");
        expectedRadiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        expectedRadiologyStudy.setDatePerformed(timeOfStudy);
        expectedRadiologyStudy.setStudyLocation(emrApiProperties.getUnknownLocation());
        expectedRadiologyStudy.setTechnician(emrApiProperties.getUnknownProvider());

        radiologyService.saveRadiologyStudy(expectedRadiologyStudy);

        // fetch the study
        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("12345");

        assertTrue(new IsExpectedRadiologyStudy(expectedRadiologyStudy).matches(radiologyStudy));
    }

    @Test
    public void getRadiologyStudyByAccessionNumber_shouldDeriveRadiologyStudyFromRadiologyReports() {

        Date firstTimeOfReport = new DateTime(2012,1,1,10,10,10,10).toDate();
        Date secondTimeOfReport = new DateTime(2012,1,2,10,10,10,10).toDate();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        Concept reportType = conceptService.getConcept(1009);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        // first create a couple reports
        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setPatient(patient);
        firstRadiologyReport.setProcedure(procedure);
        firstRadiologyReport.setReportType(reportType);
        firstRadiologyReport.setReportBody("Some test report");
        firstRadiologyReport.setAccessionNumber("12345");
        firstRadiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        firstRadiologyReport.setReportDate(firstTimeOfReport) ;
        firstRadiologyReport.setPrincipalResultsInterpreter(emrApiProperties.getUnknownProvider());
        firstRadiologyReport.setReportLocation(emrApiProperties.getUnknownLocation());

        radiologyService.saveRadiologyReport(firstRadiologyReport);

        RadiologyReport secondRadiologyReport = new RadiologyReport();
        secondRadiologyReport.setPatient(patient);
        secondRadiologyReport.setProcedure(procedure);
        secondRadiologyReport.setReportType(reportType);
        secondRadiologyReport.setReportBody("Another test report");
        secondRadiologyReport.setAccessionNumber("12345");
        secondRadiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        secondRadiologyReport.setReportDate(secondTimeOfReport);
        secondRadiologyReport.setPrincipalResultsInterpreter(emrApiProperties.getUnknownProvider());
        secondRadiologyReport.setReportLocation(emrApiProperties.getUnknownLocation());

        radiologyService.saveRadiologyReport(secondRadiologyReport);

        // create the expected study
        RadiologyStudy expectedRadiologyStudy = new RadiologyStudy();
        expectedRadiologyStudy.setDatePerformed(firstTimeOfReport);
        expectedRadiologyStudy.setAccessionNumber("12345");
        expectedRadiologyStudy.setProcedure(procedure);
        expectedRadiologyStudy.setPatient(patient);

        RadiologyStudy radiologyStudy = radiologyService.getRadiologyStudyByAccessionNumber("12345");
        assertTrue(new IsExpectedRadiologyStudy(expectedRadiologyStudy).matches(radiologyStudy));
    }

    @Test
    public void getRadiologyReportsByAccessionNumber_shouldRetrieveRadiologyReportsByAccessionNumber() {

        Date firstTimeOfReport = new DateTime(2012,1,1,10,10,10,10).toDate();
        Date secondTimeOfReport = new DateTime(2012,1,2,10,10,10,10).toDate();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        Concept reportType = conceptService.getConcept(1009);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        // first create a couple reports
        RadiologyReport firstExpectedRadiologyReport = new RadiologyReport();
        firstExpectedRadiologyReport.setPatient(patient);
        firstExpectedRadiologyReport.setProcedure(procedure);
        firstExpectedRadiologyReport.setReportType(reportType);
        firstExpectedRadiologyReport.setReportBody("Some test report");
        firstExpectedRadiologyReport.setAccessionNumber("12345");
        firstExpectedRadiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        firstExpectedRadiologyReport.setReportDate(firstTimeOfReport) ;
        firstExpectedRadiologyReport.setPrincipalResultsInterpreter(emrApiProperties.getUnknownProvider());
        firstExpectedRadiologyReport.setReportLocation(emrApiProperties.getUnknownLocation());

        radiologyService.saveRadiologyReport(firstExpectedRadiologyReport);

        RadiologyReport secondExpectedRadiologyReport = new RadiologyReport();
        secondExpectedRadiologyReport.setPatient(patient);
        secondExpectedRadiologyReport.setProcedure(procedure);
        secondExpectedRadiologyReport.setReportType(reportType);
        secondExpectedRadiologyReport.setReportBody("Another test report");
        secondExpectedRadiologyReport.setAccessionNumber("12345");
        secondExpectedRadiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        secondExpectedRadiologyReport.setReportDate(secondTimeOfReport);
        secondExpectedRadiologyReport.setPrincipalResultsInterpreter(emrApiProperties.getUnknownProvider());
        secondExpectedRadiologyReport.setReportLocation(emrApiProperties.getUnknownLocation());

        radiologyService.saveRadiologyReport(secondExpectedRadiologyReport);

        // now fetch the reports
        List<RadiologyReport> radiologyReports = radiologyService.getRadiologyReportsByAccessionNumber("12345");

        assertThat(radiologyReports.size(), is(2));
        assertTrue(new IsExpectedRadiologyReport(firstExpectedRadiologyReport).matches(radiologyReports.get(1)));
        assertTrue(new IsExpectedRadiologyReport(secondExpectedRadiologyReport).matches(radiologyReports.get(0)));

    }

}
